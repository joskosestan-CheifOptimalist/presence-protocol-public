# RUN_020

## Objective
Test discovery persistence / mining loop stability over an extended observation window.

## Result
PASS WITH DESIGN CAVEAT

## Evidence Files
- run020_a17.log
- run020_s23.log
- SUMMARY_A17.md
- SUMMARY_S23.md

## Confirmed
- Discovery remained alive over an extended window
- Repeated TARGET_MATCH entries persisted on both devices
- Repeated PP_DISCOVERY PEER_SEEN entries persisted on both devices
- A17 TARGET_MATCH count: 4912
- A17 PEER_SEEN count: 58
- S23 TARGET_MATCH count: 6104
- S23 PEER_SEEN count: 60

## Interpretation
The BLE discovery layer is persisting. UI drops of peersNearby to zero are likely explained by current scan-window design rather than total discovery failure.

## Design Caveat
Current controller settings:
- SCAN_WINDOW_MS = 15000
- SCAN_IDLE_MS = 45000
- NEARBY_WINDOW_MS = 10000

This means nearby count can legitimately fall to zero during the long idle period between scan windows.

## Next Action
Decide whether to:
- reduce scan idle time, or
- increase nearby window, or
- update UI wording so zero-nearby during scan idle is not misleading.
