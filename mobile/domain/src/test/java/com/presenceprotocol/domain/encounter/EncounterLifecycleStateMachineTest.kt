package com.presenceprotocol.domain.encounter

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

class EncounterLifecycleStateMachineTest {

    private lateinit var cooldownPolicy: InMemoryEncounterCooldownPolicy
    private lateinit var telemetry: RecordingTelemetry
    private lateinit var stateMachine: EncounterLifecycleStateMachine
    private val generator = EncounterIdGenerator()

    @Before
    fun setUp() {
        cooldownPolicy = InMemoryEncounterCooldownPolicy(Duration.ofMinutes(30))
        telemetry = RecordingTelemetry()
        stateMachine = EncounterLifecycleStateMachine(generator, cooldownPolicy, telemetry)
    }

    @Test
    fun `start generates deterministic id`() {
        val payload = payload(bytes = byteArrayOf(0x01, 0x02))
        val detected = EncounterEvent.PeerDetected(peers(), payload)

        val first = stateMachine.start(detected)
        val second = stateMachine.start(detected)

        assertThat(first.id.value).isEqualTo(second.id.value)
        assertThat(telemetry.transitions.first().to).isEqualTo(EncounterState.DETECTED)
    }

    @Test
    fun `cooldown prevents encounter`() {
        val now = Instant.parse("2024-10-10T10:15:30Z")
        val peers = peers()
        cooldownPolicy.recordReward(peers, now)

        val exception = assertThrows(IllegalStateException::class.java) {
            stateMachine.start(EncounterEvent.PeerDetected(peers, payload(occurredAt = now.plusSeconds(60))))
        }

        assertThat(exception).hasMessageThat().contains("Cooldown active")
        assertThat(telemetry.invariantViolations.map { it.first }).contains(
            EncounterTelemetry.ProtocolInvariant.COOLDOWN_ENFORCED
        )
    }

    @Test
    fun `happy path transitions`() {
        val record = stateMachine.start(EncounterEvent.PeerDetected(peers(), payload()))
        val handshake = stateMachine.apply(record, EncounterEvent.HandshakeNegotiated(record.createdAt.plusSeconds(1), "nonce"))
        val signed = collectSignatures(handshake)
        val pending = stateMachine.apply(signed, EncounterEvent.QueuedForVerification(signed.updatedAt.plusSeconds(1)))
        val accepted = stateMachine.apply(
            pending,
            EncounterEvent.BackendDecision(
                EncounterVerification(
                    EncounterVerification.VerificationStatus.ACCEPTED,
                    backendSignature = byteArrayOf(0x0A),
                    message = "ok",
                    occurredAt = pending.updatedAt.plusSeconds(5)
                )
            )
        )
        val rewarded = stateMachine.apply(
            accepted,
            EncounterEvent.RewardIssued(
                receiptId = "rcpt-123",
                occurredAt = accepted.updatedAt.plusSeconds(1)
            )
        )

        assertThat(rewarded.state).isEqualTo(EncounterState.REWARDED)
        assertThat(rewarded.rewardReceiptId).isEqualTo("rcpt-123")
        assertThat(cooldownPolicy.isCoolingDown(rewarded.peers, rewarded.updatedAt.plusSeconds(10))).isTrue()
        assertThat(telemetry.transitions.map { it.to }).containsAtLeast(
            EncounterState.DETECTED,
            EncounterState.HANDSHAKING,
            EncounterState.SIGNED,
            EncounterState.PENDING_VERIFICATION,
            EncounterState.ACCEPTED,
            EncounterState.REWARDED
        )
    }

    @Test
    fun `cannot queue without both signatures`() {
        val record = stateMachine.start(EncounterEvent.PeerDetected(peers(), payload()))
        val handshake = stateMachine.apply(
            record,
            EncounterEvent.HandshakeNegotiated(record.createdAt.plusSeconds(1), "nonce")
        )
        val singleSig = addSignature(handshake, signer = handshake.peers.self)

        val error = assertThrows(IllegalStateException::class.java) {
            stateMachine.apply(singleSig, EncounterEvent.QueuedForVerification(singleSig.updatedAt))
        }

        assertThat(error).hasMessageThat().contains("Expected state SIGNED")
    }

    @Test
    fun `reward requires backend acceptance`() {
        val record = stateMachine.start(EncounterEvent.PeerDetected(peers(), payload()))
        val handshake = stateMachine.apply(
            record,
            EncounterEvent.HandshakeNegotiated(record.createdAt.plusSeconds(1), "nonce")
        )
        val signed = collectSignatures(handshake)

        val error = assertThrows(IllegalStateException::class.java) {
            stateMachine.apply(
                signed,
                EncounterEvent.RewardIssued("oops", signed.updatedAt.plusSeconds(1))
            )
        }

        assertThat(error).hasMessageThat().contains("Expected state ACCEPTED")
    }

    private fun collectSignatures(record: EncounterRecord): EncounterRecord {
        val first = addSignature(record, record.peers.self)
        return addSignature(first, record.peers.peer)
    }

    private fun addSignature(record: EncounterRecord, signer: PeerId): EncounterRecord {
        val signature = EncounterEvent.SignatureCollected(
            occurredAt = record.updatedAt.plusSeconds(1),
            signature = EncounterSignature(
                signer = signer,
                signature = byteArrayOf(0x01, 0x02),
                payloadId = record.id,
                signedAt = record.updatedAt.plusSeconds(1)
            )
        )
        return stateMachine.apply(record, signature)
    }

    private fun payload(
        bytes: ByteArray = byteArrayOf(0x01, 0x02, 0x03),
        nonce: ByteArray = byteArrayOf(0x0A, 0x0B),
        occurredAt: Instant = Instant.parse("2024-10-10T10:15:30Z")
    ) = CanonicalEncounterPayload(bytes, nonce, occurredAt)

    private fun peers(): EncounterPeers = EncounterPeers(
        self = PeerId("self-1"),
        peer = PeerId("peer-1")
    )

    private class RecordingTelemetry : EncounterTelemetry {
        val transitions = CopyOnWriteArrayList<Transition>()
        val invariantViolations = CopyOnWriteArrayList<Pair<EncounterTelemetry.ProtocolInvariant, Map<String, String>>>()

        override fun onStateTransition(record: EncounterRecord, from: EncounterState?, to: EncounterState) {
            transitions += Transition(from, to)
        }

        override fun onInvariantViolation(
            invariant: EncounterTelemetry.ProtocolInvariant,
            record: EncounterRecord?,
            metadata: Map<String, String>
        ) {
            invariantViolations += invariant to metadata
        }
    }

    private data class Transition(val from: EncounterState?, val to: EncounterState)
}
