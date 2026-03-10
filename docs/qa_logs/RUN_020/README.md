# RUN_020 — stable reward peer identity despite rotating BLE transport MACs

Branch:
- alpha-0.2-work

Devices:
- A17 = R5GYC0FZ6RY
- S23 = R5CR700RAQF

Confirmed:
- discovery continued to observe rotating BLE transport MAC addresses
- GATT connection still initiated using transport MAC addresses
- CBOR reply now carries stable app instance identity
- reward identity now uses stable app instance id instead of BLE MAC
- PP_TICKET GENERATED peer= now matches stable reward peer identity
- duplicate suppression now keys off stable reward peer identity
- second rotating transport MAC from same nearby device is suppressed as duplicate within same heartbeat window

Observed proof pattern:
- A17 first saw transport peer 63:6D:75:91:91:03 and credited reward peer 68881f18-607e-44d1-b225-6e8c8ba0d87f
- A17 then saw transport peer 5E:C3:FA:2E:03:F1 and suppressed duplicate for reward peer 68881f18-607e-44d1-b225-6e8c8ba0d87f
- S23 first saw transport peer 60:F7:9D:7C:F8:52 and credited reward peer 1692a145-78e7-4daf-8cd8-6f77b4751b98
- S23 then saw transport peer 58:B5:9A:18:CA:86 and suppressed duplicate for reward peer 1692a145-78e7-4daf-8cd8-6f77b4751b98

Key conclusion:
- BLE address randomization no longer causes duplicate rewarded peers
- reward identity is now stable at application layer
- alpha duplicate prevention is materially stronger than RUN_019 baseline
