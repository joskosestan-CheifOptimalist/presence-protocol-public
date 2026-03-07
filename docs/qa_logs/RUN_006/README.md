# RUN_006 Results

**Title:** Handshake observability and callback instrumentation
**Date:** 2026-03-08
**Status:** PARTIAL PASS

## Objective
Make the GATT handshake path observable and confirm where the flow stops.

## Confirmed changes
- Added handshake lifecycle logging in `PresenceGattClient.kt`
- Wired callback stages into `PresenceHandshakeCoordinator`
- Added logs for:
  - `PP_HANDSHAKE GATT_CONNECTED`
  - `PP_HANDSHAKE SERVICES_DISCOVERED`
  - `PP_HANDSHAKE NOTIFY_RECEIVED`
  - `PP_HANDSHAKE HANDSHAKE_COMPLETE`
  - `PP_HANDSHAKE FAIL`

## Confirmed findings
- The callback chain advanced beyond `CONNECT_START`
- Successful end-to-end handshake was observed on one side during RUN_006 testing
- `BOTH` mode remained noisy / asymmetric
- S23 client path showed `status=133` failures in some attempts

## Conclusion
RUN_006 succeeded in making the handshake path observable and exposing the real bottlenecks.
It was not the final clean reference pass because device symmetry and `BOTH` mode behavior were still unresolved.

## Handoff to RUN_007
Use role-separated APKs:
- S23 = server
- A17 = client

Reference evidence for full clean pass is captured in RUN_007.
