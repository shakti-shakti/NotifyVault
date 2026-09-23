---
name: App icon branding
description: NotifyVault's supplied transparent app icon and the boundary between brand and source-app icons.
---

The supplied NotifyVault PNG is the canonical brand asset. Use it unchanged for the launcher and NotifyVault-owned notifications, with no generated background, border, adaptive-icon surface, crop, or recoloring. Installed-app icons in captured notifications and app pickers remain source-specific.

**Why:** The user requires the exact transparent artwork everywhere the app represents itself, while source-specific icons are needed to identify the original notification sender.

**How to apply:** Change only NotifyVault-owned icon references when updating branding; do not replace AppInfoResolver/AppPicker source icons with the NotifyVault logo.