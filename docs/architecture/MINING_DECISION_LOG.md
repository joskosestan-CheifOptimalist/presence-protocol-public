# Mining Decision Log

## 2026-03-08

- RUN_005 remains partial pass.
- RUN_006 remains partial pass focused on observability.
- RUN_007 is the handshake reference baseline.
- RUN_008 is the first verified EncounterTicket stub generation pass.
- RUN_009 is the first verified full EncounterTicket JSON logging pass.
- RUN_010 is the first verified real-field EncounterTicket pass.

Verified RUN_010 enrichment:
- real helloHash
- real replyHash
- real appVersion

Open issue:
- S23 still shows:
  - PP_HANDSHAKE SERVICES_DISCOVERED
  - PP_HANDSHAKE FAIL reason=missing chars

Next target:
- investigate and fix S23 missing chars path
- reduce remaining EncounterTicket placeholders
- likely next field target: deviceAEphemeralKey
