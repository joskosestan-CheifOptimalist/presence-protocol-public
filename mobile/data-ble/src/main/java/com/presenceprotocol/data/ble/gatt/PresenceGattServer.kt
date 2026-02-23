package com.presenceprotocol.data.ble.gatt

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.util.UUID

/**
 * Phase 2A: GATT transport skeleton (Peripheral / Server role).
 *
 * This hosts the Presence service + characteristics.
 * Handshake logic comes later; for now we just:
 *  - start/stop server
 *  - expose service UUID + HELLO/REPLY/RESULT characteristics
 *  - log key lifecycle events
 */
class PresenceGattServer(
    private val context: Context
) {
    private var gattServer: BluetoothGattServer? = null
    private var serverStarted: Boolean = false

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
            Log.d(TAG, "onWrite char=${characteristic.uuid} from=${device.address} bytes=${value.size} responseNeeded=$responseNeeded")
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
            Log.d(TAG, "onDescriptorWrite desc=${descriptor.uuid} from=${device.address} bytes=${value.size} responseNeeded=$responseNeeded")
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf())
            }
        }
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

        // Build service + characteristics
        val service = BluetoothGattService(
            PresenceGattUuids.PRESENCE_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val hello = BluetoothGattCharacteristic(
            PresenceGattUuids.HELLO_CHAR_UUID,
            /* properties */ BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            /* permissions */ BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        val reply = BluetoothGattCharacteristic(
            PresenceGattUuids.REPLY_CHAR_UUID,
            /* properties */ BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            /* permissions */ BluetoothGattCharacteristic.PERMISSION_READ
        )

        // Standard CCCD required for notifications
        val cccd = BluetoothGattDescriptor(
            UUID.fromString(CCCD_UUID),
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        reply.addDescriptor(cccd)

        val result = BluetoothGattCharacteristic(
            PresenceGattUuids.RESULT_CHAR_UUID,
            /* properties */ BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            /* permissions */ BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        service.addCharacteristic(hello)
        service.addCharacteristic(reply)
        service.addCharacteristic(result)

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
        Log.d(TAG, "Presence GATT server stopped")
    }

    companion object {
        private const val TAG = "PresenceGatt"
        private const val CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    }
}
