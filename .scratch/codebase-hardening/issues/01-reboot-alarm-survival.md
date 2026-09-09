# 01: Fix Post-Reboot Midnight Rollover via Widget Lifecycle

**What to build:**
Ensure home-screen widget day counters resume advancing automatically across midnight after a device reboot. Because `RECEIVE_BOOT_COMPLETED` is omitted to preserve zero permissions, widget providers must idempotently re-register the midnight alarm in their standard `onUpdate()` lifecycle callback.

**Blocked by:**
None (can start immediately)

**Status:**
ready-for-agent

- [x] Idempotently call `MidnightAlarmReceiver.scheduleMidnightAlarm(context)` during widget update passes in `CountUpWidgetReceiver` and auxiliary widget providers.
- [x] Verify that alarm re-registration does not spawn redundant pending intents or crash when invoked repeatedly.
- [x] Add unit test verifying widget update passes trigger alarm re-registration.
