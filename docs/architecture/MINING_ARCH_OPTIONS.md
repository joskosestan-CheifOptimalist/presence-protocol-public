# Mining Architecture Options for Presence Protocol

## Shared protocol baseline
A mineable event starts from a verified device encounter:
1. BLE discovery
2. GATT connection
3. hello/reply exchange
4. encounter ticket creation
5. verifier decision
6. reward ledger update
7. optional chain settlement

## Option 1 - Fast MVP
Mineable event:
- A completed handshake plus a signed EncounterTicket from both devices.

Verifier flow:
- App submits EncounterTicket to backend verifier.
- Backend checks replay, cooldown, device pair history, and signature validity.
- If valid, backend writes reward to off-chain ledger.

Wallet linkage:
- User links wallet in app once.
- Wallet address is bound to account/device profile off-chain.

Reward flow:
- Off-chain balance updates immediately.
- Periodic batched settlement to chain later.

Fraud controls:
- cooldown window
- duplicate encounter rejection
- signed ticket requirement
- server-side anomaly checks

Cardano/Midnight placement:
- Cardano: later settlement / claim rail
- Midnight: later privacy-preserving attestation layer

Pros:
- fastest to ship
- easiest to debug
- uses RUN_007 validation directly

Cons:
- centralized verifier
- weaker sovereignty

Implementation order:
1. EncounterTicket schema
2. verifier API
3. reward ledger
4. wallet binding
5. batched settlement

## Option 2 - Balanced MVP+
Mineable event:
- A completed handshake plus signed EncounterTicket plus verifier-issued AttestationReceipt.

Verifier flow:
- Backend validates EncounterTicket.
- Backend issues AttestationReceipt signed by verifier.
- Reward ledger updates from AttestationReceipt.

Wallet linkage:
- Wallet linked at account level.
- AttestationReceipt references account + wallet binding.

Reward flow:
- Rewards accrue off-chain from receipts.
- User can claim periodically to Cardano.
- Midnight can later hold privacy-preserving attestations.

Fraud controls:
- all Fast MVP controls
- attestation receipts
- stronger device/account rate limits
- suspicious graph detection
- optional relay corroboration later

Cardano/Midnight placement:
- Cardano: claims / settlement
- Midnight: private proof or selective disclosure later

Pros:
- good balance of speed and defensibility
- clean migration path
- better investor story

Cons:
- more backend work
- still partially centralized at MVP

Implementation order:
1. EncounterTicket schema
2. verifier + AttestationReceipt
3. reward ledger
4. wallet claim flow
5. settlement job

## Option 3 - High-Sovereignty
Mineable event:
- A completed handshake plus multi-party or network-level attestation.

Verifier flow:
- Multiple attestations or decentralized validator consensus.
- Rewards only after quorum or proof threshold.

Wallet linkage:
- wallet-native identity / credential model

Reward flow:
- receipt or proof settles directly into token logic

Fraud controls:
- strongest attestation model
- strongest auditability
- highest operational complexity

Cardano/Midnight placement:
- Cardano + Midnight deeply integrated from early stage

Pros:
- strongest long-term sovereignty
- strongest anti-spoofing path

Cons:
- slowest to ship
- highest complexity
- poor first milestone choice

Implementation order:
1. credential model
2. validator network
3. proof format
4. chain-integrated reward logic

## Recommendation
Recommended near-term path: Option 2 - Balanced MVP+.
Reason:
- keeps RUN_007 handshake success useful immediately
- adds verifier-issued receipt
- avoids pretending full decentralization too early
