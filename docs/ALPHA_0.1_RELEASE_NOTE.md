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

## Alpha 0.2 working proof

Raw probe transport now works end to end across two Android devices.

Verified chain:
- HELLO raw write succeeds
- raw notify reply succeeds
- handshake completion fires
- encounter ticket is generated
- mining ledger is credited
- dashboard UI refresh shows updated verifiedToday / yield / total

Temporary alpha mechanism:
- raw probe path uses `PPH1` / `PPR1`
- ticket minting currently uses a raw-probe verification bypass
- this is transport-proof instrumentation, not final bilateral cryptographic verification
