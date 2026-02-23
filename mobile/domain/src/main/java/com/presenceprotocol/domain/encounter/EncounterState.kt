package com.presenceprotocol.domain.encounter

/**
 * Protocol-defined encounter states. Keep this in sync with
 * `../presence-protocol/docs/protocol_state_model.md`.
 */
enum class EncounterState {
    DETECTED,
    HANDSHAKING,
    SIGNED,
    PENDING_VERIFICATION,
    ACCEPTED,
    REJECTED,
    REWARDED
}
