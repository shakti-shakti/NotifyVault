---
name: Navigation back stack
description: NotifyVault's system-back behavior for Compose route and overlay navigation.
---

Top-level destinations are treated as root tabs, while detail/settings sub-screens are pushed onto a small in-memory stack. System back pops nested screens, returns top-level tabs to Vault, and exits only from Vault.

**Why:** Replacing a single route string left Android with no history, so the system back button closed the activity from screens that should have returned to the previous screen.

**How to apply:** Route callbacks should use the shared navigation helpers instead of assigning route state directly. Full-screen picker sheets should register their own back handler and call their close callback before the activity-level handler runs.