# CLAW Execution Protocol

## Objective
Document the repeatable process for Presence Protocol Android MVP execution. Updated 2026-02-24.

## Phase Workflow
1. **Brief Intake** – Confirm requirements + acceptance criteria in `CLAWBOT_MOBILE_BRIEF.md`.
2. **State Audit** – Run repo inventory, confirm build health, capture findings to `CLAWBOT_MOBILE_LOG.md`.
3. **Plan** – Produce gap-to-spec plan (dependencies, files, changes, risks) before touching code.
4. **Implementation** – Execute the approved phase only. Keep diffs minimal and scoped.
5. **Build + Logs** – Run `JAVA_HOME=<corretto17> ./gradlew assembleDebug`. Capture output path + success in log.
6. **QA Docs** – Update `docs/CURRENT_MILESTONE.md` + `docs/QA_STATUS.md` with phase status.
7. **Human Test Handoff** – Provide READY FOR JOSKO TEST script with precise steps + expected results; halt pending feedback.

## BLE Discovery Protocol
- Advertising + scanning must include service UUID `7d3a2d6b-9b7a-4f2a-9e5e-0c9d6f1b1c01`.
- Runtime permissions requested on activity start for Android 12+ (SCAN/CONNECT/ADVERTISE).
- Metrics emitted via Flow: `peersNearby` (≤10s window), `peersSeenLast10Minutes` (rolling 10m).
- Logs recorded via `DashboardViewModel.appendLog` for each peer sighting.

## Test Evidence Requirements
- Build logs stored in `CLAWBOT_MOBILE_LOG.md`.
- QA outcomes recorded per phase/device in `docs/QA_STATUS.md`.
- Handoff instructions must name the exact APK path and permissions to grant.
