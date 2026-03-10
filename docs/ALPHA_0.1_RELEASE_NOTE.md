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

## Structured vs raw proof

Archived strongest structured-path evidence:
- `docs/qa_logs/RUN_014/VERIFY_OUTPUT.md`
- shows `DEVICE_B_SIGNATURE`
- shows `deviceASignatureValid=true`
- shows `deviceBSignatureValid=true`
- shows full `PP_TICKET JSON` on both devices

Current strongest live spine proof:
- Alpha 0.2 raw probe path
- raw HELLO / raw REPLY transport succeeds live across two devices
- ticket generation succeeds
- ledger credit succeeds
- dashboard UI refresh reflects the updated totals

Interpretation:
- RUN_014 is the strongest archived structured verification evidence
- Alpha 0.2 raw probe is the strongest current live end-to-end spine proof
- final goal remains restoring structured transport with true bilateral verification
