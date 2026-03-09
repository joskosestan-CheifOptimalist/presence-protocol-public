# Experiments

This file tracks active engineering experiments.

## Current experiments

### BLE server power behaviour
Goal:
Understand battery impact of server role on Samsung S21.

Observations:
- repeated connection churn from many BLE addresses
- frequent HELLO_RX / REPLY_TX cycles
- device remained charging but battery percent did not increase

Next steps:
- investigate server encounter cooldown
- examine BLE advertising settings
- confirm scanning behaviour on server device

### Single-app architecture direction
Goal:
Converge server/client logic into one Play Store app.

Questions:
- should roles switch dynamically?
- should scanning run in windows?
- how should advertising be scheduled?
