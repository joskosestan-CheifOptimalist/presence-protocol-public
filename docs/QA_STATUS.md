# QA Status

## Phase 1 — BLE Discovery (Scan + Advertise + UI peer counter)
- Date: 2026-02-24 (Auckland)
- Devices:
  - Samsung S23 (R5CR700RAQF) — PASS
  - Samsung A17 (R5GYC0FZ6RY) — PASS
- Human test:
  - Walked ~10m away: peers dropped to 0.
  - Returned to range: peers returned to 1.
- Log evidence:
  - docs/qa_logs/phase1_s23.log
  - docs/qa_logs/phase1_a17.log
## Phase 2A — GATT Transport (Server Start)

- PASS: GATT server starts + discovery starts on both devices
  - A17 log: docs/qa_logs/phase2a_a17_gatt.log
  - S23 log: docs/qa_logs/phase2a_s23_gatt.log
