# Presence Protocol Architecture (Claude v1) — Working Notes

This file is a **working summary + action extraction** for the PDF in the same folder:

- `Presence_Protocol_Architecture_Claude_v1.pdf`

## Key “good moves” to adopt now (stage-appropriate)

1) **Keystore-backed device identity (non-exportable signing key)**
- Make device identity hard to copy.
- Foundation for any later reward, reputation, or fraud scoring.

2) **Mutual-nonce encounter proofs**
- Each side contributes entropy (nonceA + nonceB), preventing “proof forwarding”.
- Proof should be invalid without both participants present.

3) **Epoch-bounded session IDs + dedupe**
- Short-lived epoch windows limit replay value.
- Deduplicate by `sessionId` locally (and eventually on-chain / relay-side).

4) **Log-driven verification first**
- Before “tokenomics / chain”, make it verifiable via logcat and deterministic IDs.

## Minimal MVP proof skeleton (what we implement next)

- `epoch`: Int64 (e.g., floor(now / EPOCH_SECONDS))
- `nonceA`: ByteArray (16–32 bytes)
- `nonceB`: ByteArray? (unknown until exchange)
- `sessionId`: hash(epoch || nonceA || nonceB) (pending until nonceB known)
- `pubKeyA`: ByteArray (from Keystore key)
- `sigA`: signature over canonical bytes (epoch, nonceA, nonceB?, ephId?)
- `sigB`: optional later (when B signs)

## Implementation sequence (lowest risk)

1) Keystore signing utility + logs (`PP_ID`)
2) Nonce + epoch + sessionId derivation + logs (`PP_PROOF`)
3) EncounterProof data class + local store + dedupe
4) Wire into existing BLE flow with minimal touch:
   - generate nonceA on mining start
   - finalize when nonceB arrives via GATT

## Checkmarkers

- App builds + installs.
- Keystore key created/loaded (alias logged).
- `PP_PROOF` logs show `epoch` + `sessionId`.
- Local dedupe logs show `new|dup`.
