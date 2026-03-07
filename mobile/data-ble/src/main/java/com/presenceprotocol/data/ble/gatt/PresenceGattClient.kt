package com.presenceprotocol.data.ble.gatt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.presenceprotocol.data.ble.PresenceHandshakeCoordinator
import com.presenceprotocol.core.common.cbor.PresenceCborPackets
import com.presenceprotocol.core.common.handshake.HelloPacket
import java.security.SecureRandom
import java.util.UUID

class PresenceGattClient(
    private val context: Context,
    private val handshakeCoordinator: PresenceHandshakeCoordinator
) {
    private val secureRandom = SecureRandom()
    private val lastAttempts = mutableMapOf<String, Long>()

    private var bluetoothGatt: BluetoothGatt? = null
    private var replyCharacteristic: BluetoothGattCharacteristic? = null
    private var helloCharacteristic: BluetoothGattCharacteristic? = null
    private var currentAddress: String? = null
    private var pendingHelloBytes: ByteArray? = null
    private val serviceDiscoveryTimeoutHandler = Handler(Looper.getMainLooper())
    private var serviceDiscoveryTimeout: Runnable? = null

    @SuppressLint("MissingPermission")
    fun onPeerSeen(device: BluetoothDevice) {
        val address = device.address ?: return
        val now = SystemClock.elapsedRealtime()
        if (bluetoothGatt != null) return
        val last = lastAttempts[address] ?: 0L
        if (now - last < CONNECT_BACKOFF_MS) return
        lastAttempts[address] = now
        Log.d(TAG, "CLIENT_CONNECT addr=$address")
        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
        currentAddress = address
    }

    fun stop() {
        cleanup("stop")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "CLIENT_STATE addr=${gatt.device.address} status=$status newState=$newState")
            if (status != BluetoothGatt.GATT_SUCCESS || newState != BluetoothProfile.STATE_CONNECTED) {
                handshakeCoordinator.markFailure(gatt.device.address, "stateChange status=$status newState=$newState")
                cleanup("stateChange")
                return
            }
            handshakeCoordinator.markGattConnected(gatt.device.address)
            run {
                val ok = gatt.discoverServices()
                Log.d(TAG, "DISCOVER_SERVICES_REQUEST addr=" + gatt.device.address + " ok=" + ok)
                serviceDiscoveryTimeout?.let { serviceDiscoveryTimeoutHandler.removeCallbacks(it) }
                serviceDiscoveryTimeout = Runnable {
                    Log.d(TAG, "SERVICE_DISCOVERY_TIMEOUT addr=" + gatt.device.address)
                    handshakeCoordinator.markFailure(gatt.device.address, "service discovery timeout")
                    cleanup("service discovery timeout")
                }
                serviceDiscoveryTimeoutHandler.postDelayed(serviceDiscoveryTimeout!!, 5000)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            serviceDiscoveryTimeout?.let { serviceDiscoveryTimeoutHandler.removeCallbacks(it) }
            serviceDiscoveryTimeout = null
            if (status != BluetoothGatt.GATT_SUCCESS) {
                handshakeCoordinator.markFailure(gatt.device.address, "services status=$status")
                cleanup("services status=$status")
                return
            }
            handshakeCoordinator.markServicesDiscovered(gatt.device.address)
            val service = gatt.getService(PresenceGattUuids.PRESENCE_SERVICE_UUID)
            replyCharacteristic = service?.getCharacteristic(PresenceGattUuids.REPLY_CHAR_UUID)
            helloCharacteristic = service?.getCharacteristic(PresenceGattUuids.HELLO_CHAR_UUID)
            if (replyCharacteristic == null || helloCharacteristic == null) {
                handshakeCoordinator.markFailure(gatt.device.address, "missing chars")
                cleanup("missing chars")
                return
            }
            enableNotifications(gatt)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.uuid.toString().equals(CCCD_UUID, ignoreCase = true)) {
                Log.d(TAG, "CCCD_TX addr=${gatt.device.address} status=$status")
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    writeHello(gatt)
                } else {
                    handshakeCoordinator.markFailure(gatt.device.address, "cccd status=$status")
                    cleanup("cccd status=$status")
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic.uuid == PresenceGattUuids.HELLO_CHAR_UUID) {
                Log.d(TAG, "HELLO_TX addr=${gatt.device.address} status=$status bytes=${pendingHelloBytes?.size ?: 0}")
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    handshakeCoordinator.markCharWrite(gatt.device.address)
                } else {
                    handshakeCoordinator.markFailure(gatt.device.address, "hello write fail status=$status")
                    cleanup("hello write fail")
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == PresenceGattUuids.REPLY_CHAR_UUID) {
                Log.d(TAG, "REPLY_RX addr=${gatt.device.address} bytes=${value.size}")
                handshakeCoordinator.markNotifyReceived(gatt.device.address)
                handshakeCoordinator.markComplete(gatt.device.address)
                cleanup("reply received")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt) {
        val reply = replyCharacteristic ?: return
        gatt.setCharacteristicNotification(reply, true)
        val descriptor = reply.getDescriptor(UUID.fromString(CCCD_UUID)) ?: run {
            cleanup("missing cccd")
            return
        }
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }

    @SuppressLint("MissingPermission")
    private fun writeHello(gatt: BluetoothGatt) {
        val characteristic = helloCharacteristic ?: return
        val payload = byteArrayOf(0x50, 0x50, 0x48, 0x31)
        pendingHelloBytes = payload
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        characteristic.value = payload
        gatt.writeCharacteristic(characteristic)
    }

    @SuppressLint("MissingPermission")
    private fun cleanup(reason: String) {
        val gatt = bluetoothGatt ?: return
        Log.d(TAG, "CLIENT_CLEANUP reason=$reason addr=${gatt.device.address}")
        gatt.disconnect()
        gatt.close()
        bluetoothGatt = null
        replyCharacteristic = null
        helloCharacteristic = null
        pendingHelloBytes = null
        currentAddress = null
    }

    companion object {
        private const val TAG = "PresenceGattClient"
        private const val CONNECT_BACKOFF_MS = 30_000L
        private const val CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    }
}
