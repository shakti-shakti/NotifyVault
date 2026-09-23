---
name: Notification capture pipeline
description: Durable security and ordering constraints for NotifyVault notification capture and OTP delivery.
---

The listener must exclude NotifyVault-owned notifications before processing, serialize fingerprint decisions on the IO path, persist only new or changed source notifications, and keep OTP delivery ephemeral and separately deduplicated.

**Why:** The archive is sensitive vault content, while OTP alerts are short-lived convenience output; mixing the two creates recursion, duplicate alerts, and unnecessary credential exposure.

**How to apply:** Preserve the ordering of own-package exclusion → fingerprint/dedup decision → Room write/history → OTP notification, and clear the masked OTP log at every vault-lock boundary.