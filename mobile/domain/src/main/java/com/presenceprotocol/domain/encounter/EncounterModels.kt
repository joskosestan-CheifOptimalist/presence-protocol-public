package com.presenceprotocol.domain.encounter

import java.security.MessageDigest
import java.time.Instant
import java.util.Locale

@JvmInline
value class EncounterId(val value: String)

@JvmInline
value class PeerId(val value: String) {
    init {
        require(value.isNotBlank()) { "PeerId cannot be blank" }
    }
}

/** Represents the local + remote peer pairing for cooldown tracking. */
data class EncounterPeers(val self: PeerId, val peer: PeerId) {
    fun canonicalPairKey(): String =
        listOf(self.value, peer.value).sorted().joinToString(separator = "#")
}

/** Immutable canonical payload captured during handshake. */
data class CanonicalEncounterPayload(
    val bytes: ByteArray,
    val nonce: ByteArray,
    val occurredAt: Instant
) {
    init {
        require(bytes.isNotEmpty()) { "Canonical payload bytes missing" }
        require(nonce.isNotEmpty()) { "Nonce required" }
    }

    fun deterministicInput(): ByteArray {
        val timestampBytes = occurredAt.toString().toByteArray()
        return bytes + nonce + timestampBytes
    }
}

/** Holds metadata about a signature provided by a peer. */
data class EncounterSignature(
    val signer: PeerId,
    val signature: ByteArray,
    val payloadId: EncounterId,
    val signedAt: Instant
)

/**
 * Backend verification artifact. Reward receipts depend on this.
 */
data class EncounterVerification(
    val status: VerificationStatus,
    val backendSignature: ByteArray?,
    val message: String?,
    val occurredAt: Instant
) {
    enum class VerificationStatus { ACCEPTED, REJECTED }
}

/**
 * Immutable Encounter record used by the state machine.
 */
data class EncounterRecord(
    val id: EncounterId,
    val peers: EncounterPeers,
    val payload: CanonicalEncounterPayload,
    val state: EncounterState,
    val signatures: Map<PeerId, EncounterSignature>,
    val handshakeNonce: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val verification: EncounterVerification? = null,
    val rewardReceiptId: String? = null
) {
    fun requireSignatureCount(expected: Int) {
        require(signatures.size == expected) {
            "Expected $expected signatures but found ${signatures.size}"
        }
    }
}

/** Deterministic EncounterId generator (SHA-256 hex). */
class EncounterIdGenerator {
    fun generate(payload: CanonicalEncounterPayload): EncounterId {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(payload.deterministicInput())
        val hex = hash.joinToString(separator = "") { byte ->
            String.format(Locale.US, "%02x", byte)
        }
        return EncounterId(hex)
    }
}
