---
name: Vault lock security
description: Durable security constraints for NotifyVault lock lifecycle work.
---

NotifyVault must never fall back to plaintext credential storage. The lock boundary is enforced across cold start, configured background timeout, screen-off, and reboot; changing or disabling a lock requires the current credential.

**Why:** The feature pack treats the notification archive as sensitive vault content and explicitly requires old-credential verification before any lock change.

**How to apply:** Preserve the encrypted preferences path, PBKDF2 salted hashes, exact configured PIN length, and process/manifest lifecycle boundaries when extending lock settings or screens.