# Reward Ledger State Machine

States:
pending_verification
verified
reward_issued
revoked

Transitions:
pending_verification -> verified
verified -> reward_issued
verified -> revoked

Ledger fields:
- encounter_id
- receipt_id
- account_id
- reward_amount
- state
- timestamps
