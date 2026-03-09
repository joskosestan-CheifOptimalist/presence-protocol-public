# RUN_014

## Objective
Verify bilateral signatures under the current hash-based signature model.

## Result
PASS

## Evidence Files
- run014_a17.log
- run014_s23.log
- VERIFY_OUTPUT.md

## Confirmed
- deviceBSignature generated on both devices
- deviceASignatureValid=true on both devices
- deviceBSignatureValid=true on both devices
- PP_TICKET JSON emitted on both devices with both signatures populated

## Verification Model
Current implementation verifies:
- deviceASignature against helloHash
- deviceBSignature against replyHash

## Conclusion
RUN_014 confirms repeatable bilateral signed EncounterTicket generation with local verification under the current hash-based signature model.

## Verification summary

`VERIFY_OUTPUT.md` captures the strongest structured-path proof from RUN_014.

Observed on both devices:
- `PP_HANDSHAKE DEVICE_B_SIGNATURE`
- `PP_VERIFY deviceASignatureValid=true`
- `PP_VERIFY deviceBSignatureValid=true`
- `PP_TICKET JSON`

Interpretation:
- the structured handshake path did reach ticket generation
- verification at this stage was still local/self-consistency verification
- this run should be treated as the best archived pre-raw-probe verification evidence
