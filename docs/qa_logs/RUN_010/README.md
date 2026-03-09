# RUN_010 Results

**Title:** First real-field EncounterTicket pass
**Date:** 2026-03-08
**Status:** PASS

## Objective
Verify that EncounterTicket JSON logging now includes real enriched fields instead of only placeholders.

## Setup
- S23: server debug APK
- A17: client debug APK

## Evidence
- docs/qa_logs/RUN_010/run010_s23.log
- docs/qa_logs/RUN_010/run010_a17.log

## Confirmed outcomes
- A17 completed handshake successfully
- A17 logged:
  - PP_HANDSHAKE HANDSHAKE_COMPLETE
  - PP_TICKET GENERATED
  - PP_TICKET JSON
- EncounterTicket JSON now includes real values for:
  - helloHash
  - replyHash
  - appVersion

## Open issue
- S23 still showed:
  - PP_HANDSHAKE SERVICES_DISCOVERED
  - PP_HANDSHAKE FAIL reason=missing chars

## Conclusion
RUN_010 is the first verified run where EncounterTicket JSON included real enriched fields from live handshake flow.
