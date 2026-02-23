package com.presenceprotocol.domain.encounter

import java.time.Instant

class EncounterLifecycleStateMachine(
    private val idGenerator: EncounterIdGenerator,
    private val cooldownPolicy: EncounterCooldownPolicy,
    private val telemetry: EncounterTelemetry = EncounterTelemetry.Noop
) {

    fun start(event: EncounterEvent.PeerDetected): EncounterRecord {
        if (cooldownPolicy.isCoolingDown(event.peers, event.occurredAt)) {
            telemetry.onInvariantViolation(
                EncounterTelemetry.ProtocolInvariant.COOLDOWN_ENFORCED,
                record = null,
                metadata = mapOf(
                    "peer" to event.peers.peer.value,
                    "self" to event.peers.self.value,
                    "occurredAt" to event.occurredAt.toString()
                )
            )
            error("Cooldown active for peer pair ${event.peers.canonicalPairKey()}")
        }
        val id = idGenerator.generate(event.payload)
        val record = EncounterRecord(
            id = id,
            peers = event.peers,
            payload = event.payload,
            state = EncounterState.DETECTED,
            signatures = emptyMap(),
            createdAt = event.occurredAt,
            updatedAt = event.occurredAt
        )
        telemetry.onStateTransition(record, null, EncounterState.DETECTED)
        return record
    }

    fun apply(record: EncounterRecord, event: EncounterEvent): EncounterRecord {
        val next = when (event) {
            is EncounterEvent.PeerDetected -> error("Encounter already started")
            is EncounterEvent.HandshakeNegotiated -> requireState(record, EncounterState.DETECTED) {
                record.copy(
                    state = EncounterState.HANDSHAKING,
                    handshakeNonce = event.handshakeNonce,
                    updatedAt = event.occurredAt
                )
            }
            is EncounterEvent.SignatureCollected -> handleSignature(record, event)
            is EncounterEvent.QueuedForVerification -> requireState(record, EncounterState.SIGNED) {
                record.requireSignatureCount(2)
                record.copy(
                    state = EncounterState.PENDING_VERIFICATION,
                    updatedAt = event.occurredAt
                )
            }
            is EncounterEvent.BackendDecision -> handleBackendDecision(record, event)
            is EncounterEvent.RewardIssued -> handleReward(record, event)
        }
        telemetry.onStateTransition(next, record.state, next.state)
        return next
    }

    private fun handleSignature(
        record: EncounterRecord,
        event: EncounterEvent.SignatureCollected
    ): EncounterRecord {
        if (record.state != EncounterState.HANDSHAKING && record.state != EncounterState.SIGNED) {
            error("Cannot collect signatures from state ${record.state}")
        }
        if (event.signature.payloadId != record.id) {
            telemetry.onInvariantViolation(
                EncounterTelemetry.ProtocolInvariant.IMMUTABLE_SIGNED_PAYLOAD,
                record,
                metadata = mapOf(
                    "expected" to record.id.value,
                    "provided" to event.signature.payloadId.value
                )
            )
            error("Signature payload mismatch")
        }
        if (record.signatures.containsKey(event.signature.signer)) {
            error("Duplicate signature for ${event.signature.signer.value}")
        }
        val updated = record.signatures + (event.signature.signer to event.signature)
        val nextState = if (updated.size >= 2) EncounterState.SIGNED else EncounterState.HANDSHAKING
        val updatedRecord = record.copy(
            signatures = updated,
            state = nextState,
            updatedAt = event.occurredAt
        )
        if (nextState == EncounterState.SIGNED && updated.size < 2) {
            telemetry.onInvariantViolation(
                EncounterTelemetry.ProtocolInvariant.MUTUAL_SIGNATURES,
                record,
                metadata = mapOf("signatures" to updated.size.toString())
            )
            error("Both signatures required to mark encounter as Signed")
        }
        if (nextState == EncounterState.SIGNED) {
            updatedRecord.requireSignatureCount(2)
        }
        return updatedRecord
    }

    private fun handleBackendDecision(
        record: EncounterRecord,
        event: EncounterEvent.BackendDecision
    ): EncounterRecord = requireState(record, EncounterState.PENDING_VERIFICATION) {
        val verification = event.verification
        val nextState = when (verification.status) {
            EncounterVerification.VerificationStatus.ACCEPTED -> EncounterState.ACCEPTED
            EncounterVerification.VerificationStatus.REJECTED -> EncounterState.REJECTED
        }
        record.copy(
            state = nextState,
            verification = verification,
            updatedAt = event.occurredAt
        )
    }

    private fun handleReward(
        record: EncounterRecord,
        event: EncounterEvent.RewardIssued
    ): EncounterRecord = requireState(record, EncounterState.ACCEPTED) {
        val verification = record.verification
        if (verification == null || verification.status != EncounterVerification.VerificationStatus.ACCEPTED) {
            telemetry.onInvariantViolation(
                EncounterTelemetry.ProtocolInvariant.BACKEND_VERIFIED_REWARD,
                record,
                metadata = mapOf("reason" to "Missing acceptance before reward")
            )
            error("Cannot reward without backend acceptance")
        }
        val rewarded = record.copy(
            state = EncounterState.REWARDED,
            rewardReceiptId = event.receiptId,
            updatedAt = event.occurredAt
        )
        cooldownPolicy.recordReward(record.peers, event.occurredAt)
        rewarded
    }

    private inline fun requireState(
        record: EncounterRecord,
        expected: EncounterState,
        crossinline block: () -> EncounterRecord
    ): EncounterRecord {
        if (record.state != expected) {
            error("Expected state $expected but was ${record.state}")
        }
        return block()
    }
}
