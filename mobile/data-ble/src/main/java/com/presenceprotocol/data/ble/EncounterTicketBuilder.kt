package com.presenceprotocol.data.ble

import java.security.MessageDigest
import java.util.UUID

object EncounterTicketBuilder {
    fun build(
        peerId: String,
        deviceAEphemeralKey: String,
        helloHash: String,
        replyHash: String,
        handshakeTimestampMs: Long,
        appVersion: String,
        deviceASignature: String,
        deviceBSignature: String
    ): EncounterTicket {
        val heartbeatId = HeartbeatClock.heartbeatId(handshakeTimestampMs)
        val epochId = HeartbeatClock.epochId(handshakeTimestampMs)
        val heartbeatIndexInEpoch = HeartbeatClock.heartbeatIndexInEpoch(handshakeTimestampMs)

        return EncounterTicket(
            encounterId = deterministicEncounterId(
                peerId = peerId,
                helloHash = helloHash,
                replyHash = replyHash,
                heartbeatId = heartbeatId,
                epochId = epochId,
                heartbeatIndexInEpoch = heartbeatIndexInEpoch
            ),
            deviceAEphemeralKey = deviceAEphemeralKey,
            deviceBEphemeralKey = peerId,
            helloHash = helloHash,
            replyHash = replyHash,
            handshakeTimestamp = handshakeTimestampMs,
            heartbeatId = heartbeatId,
            epochId = epochId,
            heartbeatIndexInEpoch = heartbeatIndexInEpoch,
            nonce = UUID.randomUUID().toString(),
            protocolVersion = "presence_v1",
            appVersion = appVersion,
            deviceASignature = deviceASignature,
            deviceBSignature = deviceBSignature
        )
    }

    private fun deterministicEncounterId(
        peerId: String,
        helloHash: String,
        replyHash: String,
        heartbeatId: Long,
        epochId: Long,
        heartbeatIndexInEpoch: Int
    ): String {
        val material = listOf(
            "presence_v1",
            peerId,
            helloHash,
            replyHash,
            heartbeatId.toString(),
            epochId.toString(),
            heartbeatIndexInEpoch.toString()
        ).joinToString("|")

        val digest = MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }

        return listOf(
            hex.substring(0, 8),
            hex.substring(8, 12),
            hex.substring(12, 16),
            hex.substring(16, 20),
            hex.substring(20, 32)
        ).joinToString("-")
    }

}
