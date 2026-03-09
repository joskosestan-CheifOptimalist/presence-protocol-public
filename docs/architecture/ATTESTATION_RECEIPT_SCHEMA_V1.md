# Attestation Receipt Schema V1

Proof issued by verifier for valid encounter.

Fields:
- receipt_id
- encounter_id
- verifier_id
- reward_amount
- issued_at
- status
- verifier_signature

Rules:
- encounter must exist
- receipt_id must be unique
- verifier signature must validate
