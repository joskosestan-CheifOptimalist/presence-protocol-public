package com.presenceprotocol.domain.encounter

import java.time.Instant

sealed interface EncounterEvent {
    val occurredAt: Instant

    data class PeerDetected(
        val peers: EncounterPeers,
        val payload: CanonicalEncounterPayload,
        override val occurredAt: Instant = payload.occurredAt
    ) : EncounterEvent

    data class HandshakeNegotiated(
        override val occurredAt: Instant,
        val handshakeNonce: String
    ) : EncounterEvent

    data class SignatureCollected(
        override val occurredAt: Instant,
        val signature: EncounterSignature
    ) : EncounterEvent

    data class QueuedForVerification(
        override val occurredAt: Instant
    ) : EncounterEvent

    data class BackendDecision(
        val verification: EncounterVerification,
        override val occurredAt: Instant = verification.occurredAt
    ) : EncounterEvent

    data class RewardIssued(
        val receiptId: String,
        override val occurredAt: Instant
    ) : EncounterEvent
}
