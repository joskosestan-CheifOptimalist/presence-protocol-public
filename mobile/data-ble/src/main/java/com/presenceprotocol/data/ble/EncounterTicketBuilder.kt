package com.presenceprotocol.data.ble

import java.util.UUID

object EncounterTicketBuilder {
    fun build(
        peerId: String,
        deviceAEphemeralKey: String,
        helloHash: String,
        replyHash: String,
        appVersion: String,
        deviceASignature: String,
        deviceBSignature: String
    ): EncounterTicket {
        val now = System.currentTimeMillis()
        return EncounterTicket(
            encounterId = UUID.randomUUID().toString(),
            deviceAEphemeralKey = deviceAEphemeralKey,
            deviceBEphemeralKey = peerId,
            helloHash = helloHash,
            replyHash = replyHash,
            handshakeTimestamp = now,
            heartbeatId = HeartbeatClock.heartbeatId(now),
            epochId = HeartbeatClock.epochId(now),
            heartbeatIndexInEpoch = HeartbeatClock.heartbeatIndexInEpoch(now),
            nonce = UUID.randomUUID().toString(),
            protocolVersion = "presence_v1",
            appVersion = appVersion,
            deviceASignature = deviceASignature,
            deviceBSignature = deviceBSignature
        )
    }
}
