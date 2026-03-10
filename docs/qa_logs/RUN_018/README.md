# RUN_018

Live two-device CBOR recovery proof.

Observed:
- REQUEST_MTU requested=185
- MTU_CHANGED mtu=517
- HELLO_RX bytes=29
- REPLY_TX bytes=133
- REPLY_RX bytes=133
- PP_VERIFY deviceASignatureValid=true
- PP_VERIFY deviceBSignatureValid=true
- PP_TICKET GENERATED
- PIPE_LEDGER_CREDIT
- PIPE_UI_REFRESH totals updated

Conclusion:
Structured CBOR transport is functioning end-to-end again.

## RUN_019 pure CBOR-only proof
- Devices: A17=R5GYC0FZ6RY, S23=R5CR700RAQF
- Transport mode: CBOR only
- Confirmed: REQUEST_MTU 185, MTU_CHANGED 517, HELLO_BUILD 29, HELLO_RX 29, REPLY_TX 133, REPLY_RX 133, PP_VERIFY true/true, PP_TICKET GENERATED, PIPE_LEDGER_CREDIT, PIPE_UI_REFRESH
- Raw probe path removed from coordinator and transport source.
