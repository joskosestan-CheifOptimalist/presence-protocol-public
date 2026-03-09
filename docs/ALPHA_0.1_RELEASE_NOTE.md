# Presence Protocol Alpha 0.1

Status:
- Buildable baseline confirmed
- Encounter -> handshake -> ticket -> in-memory ledger credit path present
- Dashboard heartbeat cleanup completed
- Non-wired dashboard fields removed

Known limitations:
- Signature verification is not yet true bilateral verification
- Mining ledger is not yet persistent
- Pending encounter state machine not implemented
- Relay placeholder not implemented
- Formal PIPE_* instrumentation not implemented

Verification:
- ./mobile/gradlew :app:assembleDebug -> BUILD SUCCESSFUL
