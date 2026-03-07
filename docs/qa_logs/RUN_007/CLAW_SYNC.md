# Claw Sync

Run order must be preserved exactly:

- RUN_005 = partial pass
- RUN_006 = partial pass (observability + callback instrumentation)
- RUN_007 = pass (role-separated end-to-end handshake)

Do not skip RUN_006 in summaries.
Do not describe RUN_005 as the latest successful run.
Use RUN_007 as the latest confirmed full pass.
