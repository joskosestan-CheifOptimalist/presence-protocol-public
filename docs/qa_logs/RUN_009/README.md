# RUN_009 Results

**Title:** Full EncounterTicket JSON logging after handshake completion
**Date:** 2026-03-08
**Status:** PASS

## Objective
Verify that handshake completion now triggers both EncounterTicket generation and full JSON logging in live app flow.

## Setup
- S23: server debug APK
- A17: client debug APK

## Evidence
- docs/qa_logs/RUN_009/run009d_s23.log
- docs/qa_logs/RUN_009/run009d_a17.log

## Confirmed outcomes
- discovery restored in SERVER_ONLY mode after enabling discovery start
- A17 completed handshake successfully
- A17 logged:
  - PP_HANDSHAKE HANDSHAKE_COMPLETE
  - PP_TICKET GENERATED
  - PP_TICKET JSON { ... }

## Notes
- Current EncounterTicket payload still uses placeholder values for several fields.
- S23 also showed a follow-up issue on one path:
  - PP_HANDSHAKE FAIL reason=missing chars

## Conclusion
RUN_009 is the first verified run where a completed handshake generated and logged the full EncounterTicket JSON payload in live device flow.
