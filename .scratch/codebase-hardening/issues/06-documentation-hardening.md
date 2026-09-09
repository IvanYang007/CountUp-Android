# 06: Documentation Hardening: Undo Transparency, Alarm Clarification & Build Portability

**What to build:**
Update project documentation (`README.md` and `TECH_HANDOFF.md`) to reflect the app's real behavior and ensure portability across developer workstations:
1. Document the in-app reset undo whisper and widget reset recovery banner so users know streaks are recoverable.
2. Clarify that midnight rollover is a battery-friendly, inexact non-wakeup RTC alarm aligning with Doze maintenance windows (preserving zero permissions).
3. Replace hardcoded Windows machine paths (`D:\Android\Sdk`) with standard `local.properties` instructions.

**Blocked by:**
01: Fix Post-Reboot Midnight Rollover via Widget Lifecycle
02: UUID-Authoritative Deduplication for Backup Merge

**Status:**
ready-for-agent

- [x] Add a subsection in `README.md` explaining how to recover accidental widget resets using the in-app recovery banner and card whispers.
- [x] Clarify `README.md` Section 6 to note that rollover uses battery-friendly non-wakeup RTC scheduling to maintain zero permissions.
- [x] Replace personal machine paths in `README.md` Section 2 with portable environment instructions (`ANDROID_HOME` / `local.properties`).
- [x] Update `TECH_HANDOFF.md` to reflect the reboot alarm fix and UUID merge deduplication.
