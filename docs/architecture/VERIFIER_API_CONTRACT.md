# Verifier API Contract

Endpoint:
POST /verifyEncounter

Request:
{ encounter_ticket }

Validation steps:
1 schema validation
2 signature verification
3 nonce check
4 duplicate encounter check
5 cooldown enforcement
6 anomaly detection

Success:
returns AttestationReceipt

Failure:
returns rejection reason
