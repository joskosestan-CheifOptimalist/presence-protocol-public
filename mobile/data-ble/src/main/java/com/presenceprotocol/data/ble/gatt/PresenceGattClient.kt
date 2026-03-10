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
import com.presenceprotocol.core.common.config.TransportConfig
import com.presenceprotocol.core.common.config.TransportMode
import com.presenceprotocol.core.common.handshake.HelloPacket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
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
    private var lastHelloHash: String? = null
    private var lastReplyHash: String? = null
    private var lastDeviceBEphemeralKey: String? = null
    private var lastDeviceBSignature: String? = null
    private var lastRemoteAppInstanceId: String? = null
    private var missingCharsRetryUsed: Boolean = false
    private var mtuRequested: Boolean = false
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
            mtuRequested = true
            val mtuOk = gatt.requestMtu(185)
            Log.d(TAG, "REQUEST_MTU addr=${gatt.device.address} ok=$mtuOk requested=185")
            if (!mtuOk) {
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

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d(TAG, "MTU_CHANGED addr=${gatt.device.address} mtu=$mtu status=$status")
            val ok = gatt.discoverServices()
            Log.d(TAG, "DISCOVER_SERVICES_AFTER_MTU addr=${gatt.device.address} ok=$ok")
            serviceDiscoveryTimeout?.let { serviceDiscoveryTimeoutHandler.removeCallbacks(it) }
            serviceDiscoveryTimeout = Runnable {
                Log.d(TAG, "SERVICE_DISCOVERY_TIMEOUT addr=" + gatt.device.address)
                handshakeCoordinator.markFailure(gatt.device.address, "service discovery timeout")
                cleanup("service discovery timeout")
            }
            serviceDiscoveryTimeoutHandler.postDelayed(serviceDiscoveryTimeout!!, 5000)
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

            gatt.services?.forEach { svc ->
                Log.d(TAG, "DISCOVERED_SERVICE addr=${gatt.device.address} uuid=${svc.uuid}")
                svc.characteristics?.forEach { ch ->
                    Log.d(TAG, "DISCOVERED_CHAR addr=${gatt.device.address} service=${svc.uuid} uuid=${ch.uuid}")
                }
            }

            val service = gatt.getService(PresenceGattUuids.PRESENCE_SERVICE_UUID)
            replyCharacteristic = service?.getCharacteristic(PresenceGattUuids.REPLY_CHAR_UUID)
            helloCharacteristic = service?.getCharacteristic(PresenceGattUuids.HELLO_CHAR_UUID)

            Log.d(
                TAG,
                "SERVICE_LOOKUP addr=${gatt.device.address} serviceFound=${service != null} " +
                    "replyFound=${replyCharacteristic != null} helloFound=${helloCharacteristic != null}"
            )

            if (replyCharacteristic == null || helloCharacteristic == null) {
                if (!missingCharsRetryUsed) {
                    missingCharsRetryUsed = true
                    Log.d(TAG, "MISSING_CHARS_RETRY addr=${gatt.device.address} delayMs=250")
                    serviceDiscoveryTimeout = Runnable {
                        val ok = gatt.discoverServices()
                        Log.d(TAG, "DISCOVER_SERVICES_RETRY addr=${gatt.device.address} ok=$ok")
                    }
                    serviceDiscoveryTimeoutHandler.postDelayed(serviceDiscoveryTimeout!!, 250)
                    return
                }
                handshakeCoordinator.markFailure(gatt.device.address, "missing chars")
                cleanup("missing chars")
                return
            }

            missingCharsRetryUsed = false
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

                if (value.contentEquals(byteArrayOf(0x50, 0x50, 0x52, 0x31))) {
                    Log.d(TAG, "REPLY_RX_RAW addr=${gatt.device.address} bytes=${value.size}")
                    lastDeviceBEphemeralKey = gatt.device.address
                    lastDeviceBSignature = "device_b_sig_placeholder"
                    lastReplyHash = sha256Hex(value)
                } else {
                    val reply = try {
                        PresenceCborPackets.decodeReply(value)
                    } catch (t: Throwable) {
                        Log.w(TAG, "REPLY_RX decode failed addr=${gatt.device.address} err=${t.message}")
                        cleanup("reply decode failed")
                        return
                    }

                    if (reply.appId != "presence-protocol") {
                        Log.e(TAG, "PP_SUPPRESS app_id peer=${gatt.device.address} appId=${reply.appId}")
                        cleanup("reply app id mismatch")
                        return
                    }

                    if (reply.appInstanceId.isBlank()) {
                        Log.e(TAG, "PP_SUPPRESS app_instance_id peer=${gatt.device.address} missing=true")
                        cleanup("reply app instance id missing")
                        return
                    }

                    lastRemoteAppInstanceId = reply.appInstanceId
                    lastDeviceBEphemeralKey = reply.appInstanceId
                    lastDeviceBSignature = "device_b_sig_placeholder"
                    lastReplyHash = sha256Hex(value)
                }

                val appVersion = getAppVersion()
                val transportPeerId = gatt.device.address
                val stablePeerId = lastRemoteAppInstanceId ?: lastDeviceBEphemeralKey ?: transportPeerId
                handshakeCoordinator.markNotifyReceived(transportPeerId)
                handshakeCoordinator.markComplete(
                    transportPeerId,
                    helloHash = lastHelloHash ?: "hello_hash_missing",
                    replyHash = lastReplyHash ?: "reply_hash_missing",
                    appVersion = appVersion,
                    deviceBEphemeralKey = stablePeerId,
                    deviceBSignature = lastDeviceBSignature ?: "device_b_sig_missing"
                )
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

        val payload = PresenceCborPackets.encodeHello(
            HelloPacket(
                version = 1,
                sessionId = ByteArray(1).also { secureRandom.nextBytes(it) },
                nonce = ByteArray(1).also { secureRandom.nextBytes(it) },
                clientPublicKey = ByteArray(1),
                timestampSeconds = 1L
            )
        )

        pendingHelloBytes = payload
        lastHelloHash = sha256Hex(payload)

        Log.d(TAG, "HELLO_BUILD addr=${gatt.device.address} mode=${TransportConfig.transportMode} bytes=${payload.size} hash=${lastHelloHash}")
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        characteristic.value = payload
        Log.d(TAG, "HELLO_WRITE_REQUEST addr=${gatt.device.address} writeType=${characteristic.writeType} bytes=${payload.size}")
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
        lastHelloHash = null
        lastReplyHash = null
        lastDeviceBEphemeralKey = null
        lastDeviceBSignature = null
        lastRemoteAppInstanceId = null
        missingCharsRetryUsed = false
        mtuRequested = false
        currentAddress = null
    }

    private fun parseReplyEnvelope(text: String): Map<String, String> {
        return text.split(";")
            .mapNotNull {
                val idx = it.indexOf("=")
                if (idx <= 0) null else it.substring(0, idx) to it.substring(idx + 1)
            }
            .toMap()
    }

    private fun getAppVersion(): String {
        return try {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            pkg.versionName ?: "unknown"
        } catch (_: Throwable) {
            "unknown"
        }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "PresenceGattClient"
        private const val CONNECT_BACKOFF_MS = 30_000L
        private const val CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    }
}
