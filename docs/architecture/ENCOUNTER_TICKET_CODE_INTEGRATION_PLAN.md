# Encounter Ticket Code Integration Plan

Goal:
Generate EncounterTicket in app code immediately after successful handshake completion.

Target trigger:
- PP_HANDSHAKE HANDSHAKE_COMPLETE

Planned files:
- mobile/data-ble/src/main/java/com/presenceprotocol/data/ble/EncounterTicket.kt
- mobile/data-ble/src/main/java/com/presenceprotocol/data/ble/EncounterTicketBuilder.kt

Implementation steps:
1. add Kotlin data model
2. add builder
3. trigger builder after handshake complete
4. serialize ticket to JSON
5. log ticket locally
6. defer backend upload until verifier client exists

Reference baseline:
- RUN_007
