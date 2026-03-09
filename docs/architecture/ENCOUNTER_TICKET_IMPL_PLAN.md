# Encounter Ticket Implementation Plan

Goal:
Create EncounterTicket in the mobile app immediately after successful handshake completion.

Implementation target:
- trigger on PP_HANDSHAKE HANDSHAKE_COMPLETE
- build EncounterTicket object
- serialize to JSON
- log locally before backend upload exists

Planned code units:
- EncounterTicket.kt
- EncounterTicketBuilder.kt

Immediate tasks:
1. define Kotlin data model
2. map handshake values to ticket fields
3. create builder at handshake completion
4. log generated ticket
5. defer upload until verifier client exists

Reference baseline:
- RUN_007 is the handshake reference pass
