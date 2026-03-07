# RUN_007 Results

**Title:** Role-separated handshake validation
**Date:** 2026-03-08
**Status:** PASS

## Setup
- S23: server debug APK
- A17: client debug APK

## Evidence
- `docs/qa_logs/RUN_007/run007b_s23.log`
- `docs/qa_logs/RUN_007/run007b_a17.log`

## Confirmed outcomes
- S23 server reached `PP_HANDSHAKE SERVER_READY true`
- S23 server processed `HELLO_RX` and `REPLY_TX`
- A17 client reached:
  - `PP_HANDSHAKE CONNECT_START`
  - `PP_HANDSHAKE GATT_CONNECTED`
  - `PP_HANDSHAKE SERVICES_DISCOVERED`
  - `CCCD_TX`
  - `REPLY_RX`
  - `PP_HANDSHAKE NOTIFY_RECEIVED`
  - `PP_HANDSHAKE HANDSHAKE_COMPLETE`

## Conclusion
The role-separated path is confirmed working end-to-end.
`BOTH` mode remains a separate stability concern and is not the reference pass condition for this run.
