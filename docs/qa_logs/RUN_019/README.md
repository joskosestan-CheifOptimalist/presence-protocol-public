# RUN_019 — two-device soak test after duplicate-credit cooldown

Branch:
- alpha-0.2-work

Devices:
- A17 = R5GYC0FZ6RY
- S23 = R5CR700RAQF

Confirmed:
- CBOR handshake remained live end-to-end
- MTU negotiation remained stable
- PP_VERIFY remained true for both signatures
- PP_TICKET GENERATED remained functional
- PIPE_LEDGER_CREDIT remained functional
- rapid duplicate encounter credits were suppressed
- repeated same-peer credits reopened at approximately 5-minute intervals
- no runaway crediting observed during 10–15 minute soak

Observed pattern:
- suppression events repeated during cooldown window
- new encounter credit occurred only after cooldown expiry
- both devices showed symmetric behavior

Conclusion:
- duplicate encounter prevention is now stable enough for alpha soak conditions without breaking live CBOR proof
