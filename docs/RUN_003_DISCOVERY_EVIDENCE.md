# RUN_003 Discovery Evidence

## Summary
UUID-filtered BLE discovery is now confirmed working between two Android devices.

## Validation Status
- Build: successful
- Install: successful
- Scan filter: Presence UUID filter active
- Match detection: successful
- Direct GATT connect during this run: intentionally disabled

## Presence UUID
- 7d3a2d6b-9b7a-4f2a-9e5e-0c9d6f1b1c01

## Observed Match Evidence
Representative log lines:
- PP_DISCOVERY: startScanning() -> PP_DISCOVERY START_SCAN filters=BluetoothLeScanFilter [...]
- PP_DISCOVERY TARGET_MATCH addr=52:A9:A8:DF:C2:60 name=null rssi=-53 uuids=7d3a2d6b-9b7a-4f2a-9e5e-0c9d6f1b1c01

## Signal Characteristics
- Observed RSSI range during validation: approximately -53 to -63 dBm

## Interpretation
This confirms the prototype can:
1. advertise the Presence Protocol service UUID
2. discover a peer using UUID-filtered BLE scan
3. repeatedly detect the same peer under live conditions

## Next Milestone
Re-enable controlled GATT connection logic for single-peer handshake validation.
