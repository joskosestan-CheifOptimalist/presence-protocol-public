# RUN_021 — verified encounter architecture confirmed, live BLE proof still placeholder-backed

## Objective
Verify whether Presence Protocol already has a real verified presence event pipeline or only a partially completed one.

## Findings
Confirmed the repo contains substantial verified encounter architecture:
- EncounterTicket
- EncounterTicketBuilder
- EncounterStore / FileEncounterStore
- EncounterLifecycleStateMachine
- EncounterCooldownPolicy
- MiningLedger
- PresenceHandshakeCoordinator

Confirmed positive architecture:
- deterministic encounterId exists
- encounter tickets include dual-signature fields
- lifecycle requires two signatures before SIGNED
- backend acceptance is required before REWARDED in domain lifecycle
- cooldown policy exists for rewarded peer pairs
- file persistence exists for encounter tickets

Confirmed current live transport limitation:
- PresenceGattClient still uses `device_b_sig_placeholder`
- PresenceGattServer still returns placeholder `serverPublicKey` and `signature` byte arrays
- PresenceHandshakeCoordinator falls back to generating responder proof locally
- local key is used for verification of both sides in current live path

Confirmed current ledger limitation:
- MiningLedger.recordEncounter() is still called after ticket persistence in handshake completion path
- ledger credit is therefore not yet gated on true remote cryptographic proof

## Conclusion
Presence Protocol has a serious verified encounter architecture, but the live BLE handshake is still in the placeholder-to-proof transition stage. The protocol is not yet at authoritative mutually verified mining credit.

## Next required step
1. remove placeholder-based responder proof fallback
2. require real responder public key and responder signature in BLE reply
3. verify responder proof against responder key on client side
4. only credit ledger after real verified encounter acceptance
