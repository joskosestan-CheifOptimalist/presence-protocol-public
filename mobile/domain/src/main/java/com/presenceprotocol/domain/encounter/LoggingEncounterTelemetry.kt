package com.presenceprotocol.domain.encounter

/** Simple logger-backed telemetry implementation until a proper metrics sink is wired. */
class LoggingEncounterTelemetry(
    private val log: (String) -> Unit = { message -> println("[EncounterTelemetry] $message") }
) : EncounterTelemetry {

    override fun onStateTransition(record: EncounterRecord, from: EncounterState?, to: EncounterState) {
        log("transition id=${record.id.value} from=${from ?: "NEW"} to=$to")
    }

    override fun onInvariantViolation(
        invariant: EncounterTelemetry.ProtocolInvariant,
        record: EncounterRecord?,
        metadata: Map<String, String>
    ) {
        log("invariant=$invariant record=${record?.id?.value ?: "none"} metadata=$metadata")
    }
}
