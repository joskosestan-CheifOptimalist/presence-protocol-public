# Next Step

## Immediate engineering priority
Reduce server-side BLE power drain without breaking successful handshakes.

## Decision
Implement server-side encounter throttling before further protocol expansion.

## Reason
Observed issue is repeated successful connection churn, not failed BLE transport.

## Acceptance target
- server still completes valid handshakes
- repeated encounters are throttled
- S21 battery can increase while plugged in during idle/field test
- logs clearly show when a peer is accepted vs throttled
