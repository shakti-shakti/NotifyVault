---
name: Android build tooling
description: Local Android build verification depends on the repository's Gradle wrapper/toolchain alignment.
---

The Android project uses an AGP version whose minimum Gradle version must match the wrapper; do not assume the system Gradle binary can build it.

**Why:** The checkout did not include a Gradle launcher script, and the available system Gradle was older than the AGP minimum, so local compilation was blocked independently of source changes.

**How to apply:** When verification is requested, inspect the wrapper launcher and distribution URL first. Do not change AGP or Gradle versions just to make an ad-hoc local build pass.