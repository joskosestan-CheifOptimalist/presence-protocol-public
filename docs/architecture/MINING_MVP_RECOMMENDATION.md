# Recommended MVP Architecture

## Recommendation
Choose Option 2 - Balanced MVP+.

## Why
RUN_007 proved the role-separated handshake can complete end-to-end on real devices.
That means the next build step is not BLE work.
The next build step is turning a verified handshake into a rewardable encounter.

## Exact next build step
1. Define EncounterTicket
2. Build verifier service
3. Issue AttestationReceipt
4. Update off-chain reward ledger
5. Bind rewards to linked wallet
6. Add periodic claim / settlement flow

## EncounterTicket
Minimum fields:
- encounter_id
- device_a ephemeral key / id
- device_b ephemeral key / id
- timestamps
- handshake result
- signatures / proof material
- app version
- anti-replay nonce

## Verifier service
Backend responsibilities:
- validate EncounterTicket
- reject duplicates
- enforce cooldowns
- run anomaly checks
- issue AttestationReceipt
- update reward ledger

## Wallet linkage
- user links wallet in app
- wallet stored off-chain against account
- receipts credit that account
- claims settle later to Cardano

## Chain interaction at MVP
- no direct per-encounter on-chain write
- use periodic batched settlement or claim flow
- keep Midnight as later privacy layer, not MVP dependency

## Deferred
- BOTH mode as reference condition
- decentralized validator network
- multi-sig reward consensus
- full privacy-preserving on-chain proof system

## Evidence basis
RUN_007 is the current reference pass.
It proves handshake completion, notification receipt, and full reply path.
Therefore the architecture should now move to EncounterTicket + verifier + reward ledger.
