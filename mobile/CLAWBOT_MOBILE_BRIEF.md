# Mobile Execution Brief (Clawbot)

This is the authoritative runbook for the mobile engineer agent. Follow it literally.

## Mission
Implement and ship the Presence Miner reference app that proves the protocol while respecting all protocol invariants, state models, and execution rules defined in `../presence-protocol`.

## Non-Negotiable Directives
1. **Repo-first discipline**
   - Before making changes: print a summarized repo tree, identify modules, record build commands.
   - Work directly in this repo via CLI (no IDE automation assumed).

2. **Build + run loop**
   - After every significant change: `./gradlew assembleDebug`.
   - Run affected unit tests (`./gradlew test` or module-specific) when touching logic.
   - Install on emulator/device at least once per milestone via `adb install` + launch activity; capture `adb logcat` on failure.

3. **Architecture boundaries**
   - Maintain package separation: `ble/`, `crypto/`, `encounter/`, `ledger/`, `sync/`, `ui/`.
   - No UI state mutation outside ViewModels.

4. **Deterministic handshake**
   - Implement Presence Handshake v0.1 exactly.
   - Include unit tests for `enc_id` determinism and signature verification.

5. **Testing deliverables**
   - Maintain a living test matrix (devices × scenarios) in repo docs.
   - Instrument logging for: encounter captured, signed, queued, uploaded, verified, rewarded.

6. **Debug panel**
   - Hidden "Developer" screen (long-press title) must show last 50 BLE events, last 20 encounters, last sync result, and export logs to file.

7. **Performance + battery guardrails**
   - Implement 10s scan / 20s rest duty cycle (configurable for testing).
   - Use WorkManager with charging/Wi-Fi constraints optional for uploads.

8. **Acceptance gates**
   - No PR unless: builds succeed, unit tests pass, 30-min background run test logged, and two-device handshake verified.

## Protocol Alignment Requirements
- Adhere to `../presence-protocol/PROTOCOL_TODO.md` (Decision Log, invariants, state model, reference flows).
- Respect `docs/protocol_invariants.md`, `docs/protocol_state_model.md`, and `docs/protocol_invariants.md`.
- Features must pass the Narrative Consistency Check and state expected Protocol Metric impact before merge.
- Presence Miner is a reference implementation, not the protocol.

## Execution Rules (Reinforced)
- Build/test before merge; attach logs to summaries.
- Architecture deviations → Decision Log entry (coordinate with core repo).
- Document diagrams/state updates before touching core flows.
- Record expected changes to metrics (encounters/day, sync success, battery impact, etc.).

## Immediate Objectives
1. Inventory repo and document module responsibilities + build/test commands.
2. Anchor docs with executable state machine + tests reflecting `protocol_state_model.md` and invariants.
3. Implement Encounter Lifecycle state machine in domain layer + tests for invariants (mutual signatures, deterministic IDs, cooldown, backend-verified rewards placeholder).
4. Wire telemetry/logging for each state transition (even if backend stubbed).
5. Produce first `./gradlew assembleDebug` + emulator install report.

Log every major action (inventory, build outputs, test results) in your working notes or task tracker before moving to next step.
