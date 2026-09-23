---
name: Notification capture pipeline
description: Durable security and ordering constraints for NotifyVault notification capture and OTP delivery.
---

The listener must exclude NotifyVault-owned notifications before processing, serialize fingerprint decisions on the IO path, persist only new or changed source notifications, and keep OTP delivery separately deduplicated while retaining the local masked OTP history across lock and reboot. Listener recovery should use NotificationListenerService.requestRebind() rather than toggling the manifest component.

**Why:** The archive is sensitive vault content, while OTP alerts are short-lived delivery output; separating delivery deduplication from the persisted masked history avoids recursion and duplicate alerts without losing the user’s stored record.

**How to apply:** Preserve the ordering of own-package exclusion → fingerprint/dedup decision → Room write/history → OTP notification, and only clear the masked OTP log through explicit user data-clearing or emergency-wipe actions.

**Background recovery:** Notification processing runs off the listener callback thread; OTP delivery claims a hashed source/code pair before alerting, and boot/heartbeat recovery requests a system rebind. Android still requires user-granted Notification Access, POST_NOTIFICATIONS, and OEM battery/autostart exemptions for best-effort background delivery.

**Why:** Reprocessing active notifications when the app opens exposed that work was being delayed behind archive writes, while component toggling could interfere with Android’s listener binding.

**How to apply:** Keep delivery deduplication independent from archive deduplication, and treat the system settings grants as prerequisites rather than something app code can silently guarantee.