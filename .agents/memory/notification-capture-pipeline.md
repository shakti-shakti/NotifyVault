---
name: Notification capture pipeline
description: Durable security and ordering constraints for NotifyVault notification capture and OTP delivery.
---

The listener must exclude NotifyVault-owned notifications before processing, serialize fingerprint decisions on the IO path, persist only new or changed source notifications, and keep OTP delivery separately deduplicated while retaining the local masked OTP history across lock and reboot.

**Why:** The archive is sensitive vault content, while OTP alerts are short-lived delivery output; separating delivery deduplication from the persisted masked history avoids recursion and duplicate alerts without losing the user’s stored record.

**How to apply:** Preserve the ordering of own-package exclusion → fingerprint/dedup decision → Room write/history → OTP notification, and only clear the masked OTP log through explicit user data-clearing or emergency-wipe actions.