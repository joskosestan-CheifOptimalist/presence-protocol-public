package com.presenceprotocol.data.ble.gatt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.presenceprotocol.core.common.cbor.PresenceCborPackets
import com.presenceprotocol.core.common.config.TransportConfig
import com.presenceprotocol.core.common.config.TransportMode
import com.presenceprotocol.core.common.handshake.ReplyPacket
import java.util.UUID

/**
 * Phase 2B: HELLO → REPLY transport proof (Peripheral / Server role).
 */
class PresenceGattServer(
    private val context: Context
) {
    private var gattServer: BluetoothGattServer? = null
    private var serverStarted: Boolean = false
    private var replyCharacteristic: BluetoothGattCharacteristic? = null
    private val notifyEnabled = mutableSetOf<String>()

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(BluetoothManager::class.java)

    private val callback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange device=${device.address} status=$status newState=$newState")
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (characteristic.uuid == PresenceGattUuids.HELLO_CHAR_UUID) {
                handleHelloWrite(device, value)
            } else {
                Log.d(TAG, "onWrite char=${characteristic.uuid} from=${device.address} bytes=${value.size}")
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf())
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (descriptor.uuid.toString().equals(CCCD_UUID, ignoreCase = true)) {
                val enabled = value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                if (enabled) {
                    notifyEnabled.add(device.address)
                } else if (value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                    notifyEnabled.remove(device.address)
                }
                Log.d(TAG, "CCCD device=${device.address} enabled=$enabled")
            } else {
                Log.d(TAG, "onDescriptorWrite desc=${descriptor.uuid} from=${device.address} bytes=${value.size}")
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf())
            }
        }
    }

    private fun handleHelloWrite(device: BluetoothDevice, value: ByteArray) {
        when (TransportConfig.transportMode) {
            TransportMode.RAW_PROBE -> {
                if (value.contentEquals(byteArrayOf(0x50, 0x50, 0x48, 0x31))) {
                    Log.d(TAG, "HELLO_RX_RAW addr=${device.address} bytes=${value.size}")
                    val payload = byteArrayOf(0x50, 0x50, 0x52, 0x31)
                    val ok = notifyIfEnabled(device, payload)
                    Log.d(TAG, "REPLY_TX_RAW addr=${device.address} ok=$ok bytes=${payload.size}")
                } else {
                    Log.w(TAG, "HELLO_RX_RAW unexpected addr=${device.address} bytes=${value.size}")
                }
            }

            TransportMode.CBOR_PROBE -> {
                try {
                    val hello = PresenceCborPackets.decodeHello(value)
                    Log.d(
                        TAG,
                        "HELLO_RX addr=${device.address} bytes=${value.size} ver=${hello.version} sid=${hello.sessionId.size} nonce=${hello.nonce.size}"
                    )
                    val reply = ReplyPacket(
                        version = hello.version,
                        sessionId = hello.sessionId,
                        nonce = hello.nonce,
                        serverPublicKey = ByteArray(32),
                        signature = ByteArray(64),
                        statusCode = 0
                    )
                    val payload = PresenceCborPackets.encodeReply(reply)
                    val ok = notifyIfEnabled(device, payload)
                    Log.d(TAG, "REPLY_TX addr=${device.address} ok=$ok bytes=${payload.size}")
                } catch (t: Throwable) {
                    Log.w(TAG, "HELLO_RX decode failed from=${device.address} err=${t.message}")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun notifyIfEnabled(device: BluetoothDevice, payload: ByteArray): Boolean {
        val server = gattServer ?: return false
        val characteristic = replyCharacteristic ?: return false
        if (!notifyEnabled.contains(device.address)) return false
	characteristic.value = payload
	return server.notifyCharacteristicChanged(device, characteristic, false)
    }

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (serverStarted) return true

        val mgr = bluetoothManager
        if (mgr == null) {
            Log.w(TAG, "BluetoothManager unavailable")
            return false
        }

        val server = mgr.openGattServer(context, callback)
        if (server == null) {
            Log.w(TAG, "openGattServer returned null")
            return false
        }

        val service = BluetoothGattService(
            PresenceGattUuids.PRESENCE_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val hello = BluetoothGattCharacteristic(
            PresenceGattUuids.HELLO_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        val reply = BluetoothGattCharacteristic(
            PresenceGattUuids.REPLY_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        val cccd = BluetoothGattDescriptor(
            UUID.fromString(CCCD_UUID),
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        reply.addDescriptor(cccd)

        val result = BluetoothGattCharacteristic(
            PresenceGattUuids.RESULT_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        service.addCharacteristic(hello)
        service.addCharacteristic(reply)
        service.addCharacteristic(result)
        replyCharacteristic = reply

        val ok = server.addService(service)
        Log.d(TAG, "GattServer addService ok=$ok service=${PresenceGattUuids.PRESENCE_SERVICE_UUID}")

        gattServer = server
        serverStarted = true
        Log.d(TAG, "Presence GATT server started")
        return true
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!serverStarted) return
        try {
            gattServer?.close()
        } catch (_: Throwable) {
        }
        gattServer = null
        serverStarted = false
        notifyEnabled.clear()
        replyCharacteristic = null
        Log.d(TAG, "Presence GATT server stopped")
    }

    companion object {
        private const val TAG = "PresenceGatt"
        private const val CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    }
}
