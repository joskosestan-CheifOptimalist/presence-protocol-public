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
import android.os.SystemClock
import android.util.Log
import com.presenceprotocol.core.common.cbor.PresenceCborPackets
import com.presenceprotocol.core.common.handshake.HelloPacket
import java.security.SecureRandom
import java.util.UUID

class PresenceGattClient(
    private val context: Context
) {
    private val secureRandom = SecureRandom()
    private val lastAttempts = mutableMapOf<String, Long>()

    private var bluetoothGatt: BluetoothGatt? = null
    private var replyCharacteristic: BluetoothGattCharacteristic? = null
    private var helloCharacteristic: BluetoothGattCharacteristic? = null
    private var currentAddress: String? = null
    private var pendingHelloBytes: ByteArray? = null

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
                cleanup("stateChange")
                return
            }
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                cleanup("services status=$status")
                return
            }
            val service = gatt.getService(PresenceGattUuids.PRESENCE_SERVICE_UUID)
            replyCharacteristic = service?.getCharacteristic(PresenceGattUuids.REPLY_CHAR_UUID)
            helloCharacteristic = service?.getCharacteristic(PresenceGattUuids.HELLO_CHAR_UUID)
            if (replyCharacteristic == null || helloCharacteristic == null) {
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
                if (status != BluetoothGatt.GATT_SUCCESS) {
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
        val packet = HelloPacket(
            version = 1,
            sessionId = ByteArray(16).also { secureRandom.nextBytes(it) },
            nonce = ByteArray(32).also { secureRandom.nextBytes(it) },
            clientPublicKey = ByteArray(32),
            timestampSeconds = System.currentTimeMillis() / 1000
        )
        val payload = PresenceCborPackets.encodeHello(packet)
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
