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

## Shared transport mode switch

A shared transport switch now exists in `core-common`:

- `RAW_PROBE` = strongest live proof path
- `CBOR_PROBE` = structured transport recovery path

Latest live validation:
- both devices completed raw probe transport
- both devices generated tickets
- both devices credited ledger
- both devices refreshed UI totals

## Alpha 0.2 CBOR recovery proof

Structured CBOR transport has now been restored live across two Android devices.

Verified live:
- client requests MTU 185
- negotiated MTU reaches 517
- server receives full CBOR hello payload (`bytes=29`)
- server decodes HELLO successfully
- server sends CBOR reply successfully
- client receives reply successfully
- signature verification returns true for both device A and device B
- encounter ticket is generated
- mining ledger is credited
- dashboard UI refresh reflects updated totals

Key fix:
- CBOR transport was being truncated at default ATT payload size
- MTU negotiation plus no-response write mode for CBOR resolved the truncation path

- 2026-03-10: pure CBOR-only transport re-proven live on A17 (R5GYC0FZ6RY) and S23 (R5CR700RAQF); MTU 185->517; HELLO 29 bytes; REPLY 133 bytes; PP_VERIFY true/true; PP_TICKET GENERATED; PIPE_LEDGER_CREDIT; PIPE_UI_REFRESH.
