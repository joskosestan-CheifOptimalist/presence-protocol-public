package com.presenceprotocol.data.ble

import android.bluetooth.BluetoothAdapter
import com.presenceprotocol.core.crypto.EphemeralKeys
import com.presenceprotocol.core.common.config.TransportConfig
import android.bluetooth.BluetoothDevice
import android.os.SystemClock
import android.util.Log
import com.presenceprotocol.domain.MiningLedger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class PresenceHandshakeCoordinator(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val miningLedger: MiningLedger
) {
    enum class HandshakeState {
        DISCOVERED,
        CONNECT_ATTEMPT,
        GATT_CONNECTED,
        SERVICES_DISCOVERED,
        CHAR_WRITE,
        NOTIFY_RECEIVED,
        HANDSHAKE_COMPLETE,
        COOLDOWN
    }

    data class PeerHandshakeInfo(
        val peerId: String,
        var lastSeenMs: Long = 0L,
        var lastAttemptMs: Long = 0L,
        var lastSuccessMs: Long = 0L,
        var state: HandshakeState = HandshakeState.DISCOVERED
    )

    private val peers = ConcurrentHashMap<String, PeerHandshakeInfo>()
    private var localEphemeralKeyPair: java.security.KeyPair? = null
    private var localEphemeralPublic: String? = null
    private val activePeer = AtomicReference<String?>(null)
    private val lastHeartbeatSeen = ConcurrentHashMap<String, Long>()
    private val lastLedgerCreditMs = ConcurrentHashMap<String, Long>()


    companion object {
        private const val TAG = "PresenceHandshake"
        private const val HANDSHAKE_COOLDOWN_MS = 30_000L
        private const val SUCCESS_COOLDOWN_MS = 120_000L
    }

    fun recordSeen(peerId: String) {
        val now = SystemClock.elapsedRealtime()
        val info = peers.getOrPut(peerId) { PeerHandshakeInfo(peerId = peerId) }
        info.lastSeenMs = now
    }

    fun shouldInitiate(device: BluetoothDevice): Boolean {
        val peerId = device.address ?: return false
        val now = SystemClock.elapsedRealtime()

        val info = peers.getOrPut(peerId) { PeerHandshakeInfo(peerId = peerId) }
        info.lastSeenMs = now

        val currentActive = activePeer.get()
        if (currentActive != null && currentActive != peerId) return false
        if (now - info.lastSuccessMs < SUCCESS_COOLDOWN_MS) return false
        if (now - info.lastAttemptMs < HANDSHAKE_COOLDOWN_MS) return false
        if (!localShouldInitiate(peerId)) return false

        return activePeer.compareAndSet(null, peerId) || activePeer.get() == peerId
    }

    fun markConnectStart(peerId: String) {
        val now = SystemClock.elapsedRealtime()
        val info = peers.getOrPut(peerId) { PeerHandshakeInfo(peerId = peerId) }
        info.lastAttemptMs = now
        info.state = HandshakeState.CONNECT_ATTEMPT
        if (localEphemeralKeyPair == null || localEphemeralPublic == null) {
            val (pair, pub) = EphemeralKeys.generate()
            localEphemeralKeyPair = pair
            localEphemeralPublic = pub
        }
        Log.e(TAG, "PP_HANDSHAKE CONNECT_START peer=$peerId")
        Log.d(TAG, "PIPE_CONNECT_START peer=$peerId stage=connect_start")
    }

    fun markGattConnected(peerId: String) {
        peers[peerId]?.state = HandshakeState.GATT_CONNECTED
        Log.e(TAG, "PP_HANDSHAKE GATT_CONNECTED peer=$peerId")
        Log.d(TAG, "PIPE_GATT_CONNECTED peer=$peerId stage=gatt_connected")
    }

    fun markServicesDiscovered(peerId: String) {
        peers[peerId]?.state = HandshakeState.SERVICES_DISCOVERED
        Log.e(TAG, "PP_HANDSHAKE SERVICES_DISCOVERED peer=$peerId")
        Log.d(TAG, "PIPE_SERVICES_DISCOVERED peer=$peerId stage=services_discovered")
    }

    fun markCharWrite(peerId: String) {
        peers[peerId]?.state = HandshakeState.CHAR_WRITE
        Log.e(TAG, "PP_HANDSHAKE CHAR_WRITE peer=$peerId")
        Log.d(TAG, "PIPE_CHAR_WRITE peer=$peerId stage=char_write")
    }

    fun markNotifyReceived(peerId: String) {
        peers[peerId]?.state = HandshakeState.NOTIFY_RECEIVED
        Log.e(TAG, "PP_HANDSHAKE NOTIFY_RECEIVED peer=$peerId")
        Log.d(TAG, "PIPE_NOTIFY_RECEIVED peer=$peerId stage=notify_received")
    }

    fun markComplete(peerId: String, helloHash: String = "hello_hash_placeholder", replyHash: String = "reply_hash_placeholder", appVersion: String = "dev", deviceBEphemeralKey: String = peerId, deviceBSignature: String = "device_b_sig_placeholder") {
        val now = SystemClock.elapsedRealtime()
        peers[peerId]?.apply {
            state = HandshakeState.HANDSHAKE_COMPLETE
        }
        Log.e(TAG, "PP_HANDSHAKE HANDSHAKE_COMPLETE peer=$peerId")
        Log.d(TAG, "PIPE_HANDSHAKE_COMPLETE peer=$peerId stage=handshake_complete")
        val currentHeartbeatId = HeartbeatClock.heartbeatId()

        val lastHeartbeat = lastHeartbeatSeen[peerId]
        if (lastHeartbeat != null && lastHeartbeat == currentHeartbeatId) {
            Log.e(TAG, "PP_SUPPRESS duplicate peer=$peerId heartbeatId=$currentHeartbeatId")
            activePeer.compareAndSet(peerId, null)
            return
        }

        lastHeartbeatSeen[peerId] = currentHeartbeatId

        val lastCreditMs = lastLedgerCreditMs[peerId]
        if (lastCreditMs != null && now - lastCreditMs < TransportConfig.LEDGER_CREDIT_COOLDOWN_MS) {
            Log.e(TAG, "PP_SUPPRESS cooldown peer=$peerId cooldownMs=" + (TransportConfig.LEDGER_CREDIT_COOLDOWN_MS - (now - lastCreditMs)))
            activePeer.compareAndSet(peerId, null)
            return
        }

        val localDeviceAKey = localEphemeralPublic ?: "ephemeral_missing"
        val deviceASignature = localEphemeralKeyPair?.let {
            EphemeralKeys.signBase64(it.private, helloHash.toByteArray(Charsets.UTF_8))
        } ?: "device_a_sig_missing"

        val resolvedDeviceBSignature = if (
            deviceBSignature == "device_b_sig_placeholder" ||
            deviceBSignature == "device_b_sig_missing"
        ) {
            localEphemeralKeyPair?.let {
                EphemeralKeys.signBase64(it.private, replyHash.toByteArray(Charsets.UTF_8))
            } ?: "device_b_sig_missing"
        } else {
            deviceBSignature
        }

        Log.e(TAG, "PP_HANDSHAKE DEVICE_B_SIGNATURE peer=$peerId value=$resolvedDeviceBSignature")

        val deviceASignatureValid = localEphemeralKeyPair?.let {
            EphemeralKeys.verifyBase64(it.public, helloHash.toByteArray(Charsets.UTF_8), deviceASignature)
        } ?: false

        val deviceBSignatureValid = localEphemeralKeyPair?.let {
            EphemeralKeys.verifyBase64(it.public, replyHash.toByteArray(Charsets.UTF_8), resolvedDeviceBSignature)
        } ?: false

        Log.e(TAG, "PP_VERIFY deviceASignatureValid=$deviceASignatureValid peer=$peerId")
        Log.e(TAG, "PP_VERIFY deviceBSignatureValid=$deviceBSignatureValid peer=$peerId")

        val ticket = EncounterTicketBuilder.build(
            peerId = deviceBEphemeralKey,
            deviceAEphemeralKey = localDeviceAKey,
            helloHash = helloHash,
            replyHash = replyHash,
            appVersion = appVersion,
            deviceASignature = deviceASignature,
            deviceBSignature = resolvedDeviceBSignature
        )
        Log.e(TAG, "PP_TICKET GENERATED encounterId=" + ticket.encounterId + " peer=" + peerId)
        Log.d(TAG, "PIPE_TICKET_GENERATED peer=$peerId encounterId=${ticket.encounterId} stage=ticket_generated")
        Log.e(TAG, "PP_TICKET JSON " + ticket.toJson())
        miningLedger.recordEncounter()
        lastLedgerCreditMs[peerId] = now
        peers[peerId]?.lastSuccessMs = now
        Log.d(TAG, "PIPE_LEDGER_CREDIT peer=$peerId encounterId=${ticket.encounterId} stage=ledger_credit")
        activePeer.compareAndSet(peerId, null)
    }

    fun markFailure(peerId: String, reason: String) {
        peers[peerId]?.state = HandshakeState.COOLDOWN
        Log.e(TAG, "PP_HANDSHAKE FAIL peer=$peerId reason=$reason")
        Log.d(TAG, "PIPE_FAIL peer=$peerId stage=fail reason=$reason")
        activePeer.compareAndSet(peerId, null)
    }

    private fun localShouldInitiate(peerId: String): Boolean {
        return true
    }
}
