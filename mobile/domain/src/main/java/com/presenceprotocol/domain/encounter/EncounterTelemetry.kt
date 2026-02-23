package com.presenceprotocol.domain.encounter

/** Telemetry/logging hook for state transitions + invariant violations. */
interface EncounterTelemetry {
    fun onStateTransition(record: EncounterRecord, from: EncounterState?, to: EncounterState)
    fun onInvariantViolation(
        invariant: ProtocolInvariant,
        record: EncounterRecord?,
        metadata: Map<String, String> = emptyMap()
    )

    enum class ProtocolInvariant {
        MUTUAL_SIGNATURES,
        BACKEND_VERIFIED_REWARD,
        DETERMINISTIC_IDS,
        COOLDOWN_ENFORCED,
        IMMUTABLE_SIGNED_PAYLOAD
    }

    object Noop : EncounterTelemetry {
        override fun onStateTransition(record: EncounterRecord, from: EncounterState?, to: EncounterState) = Unit
        override fun onInvariantViolation(
            invariant: ProtocolInvariant,
            record: EncounterRecord?,
            metadata: Map<String, String>
        ) = Unit
    }
}
