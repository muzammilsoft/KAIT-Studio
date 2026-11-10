# Contributing to KAIT Studio

## How to contribute

1. Fork the repository and create a branch from `main`.
2. Make your change. Keep it small and focused.
3. Build the project and run the engine test (see below).
4. Open a pull request describing what changed and why.

Bug reports and feature ideas are welcome as GitHub issues. Please include
the app version, the device/Android version, and steps to reproduce.

## Building the project

Requirements: JDK 17, Android SDK.

```bash
./gradlew assembleDebug   # debug APK
./gradlew assembleRelease # release APK (signed with the test keystore)
```

CI (`.github/workflows/build.yml`) runs both builds on every push to `main`
and every pull request, and uploads both APKs as artifacts.

## Running the engine test

The parity test compares the Kotlin engine against the reference TAIT
(NumPy) engine. Run it from the `engine` module:

```bash
./gradlew :engine:testDebugUnitTest
```

The test log is written to `engine/PARITY-TEST-LOG.txt`. If the test fails,
do not adjust the expected values by hand — find the real divergence first.

## Rules: do not change MiniGPT math

The inference engine must reproduce TAIT's MiniGPT exactly:

- Do **not** change the forward order, the attention masking, the
  activation function, or the weight layout documented in
  [ENGINE.md](ENGINE.md).
- Do **not** add layer normalization, dropout, or any extra layer.
- Do **not** change the BPE merge order or the separator/end token handling.
- Any change that alters logits or generated tokens must be validated by
  the parity test before merging.

Performance refactors (faster matmul, less allocation) are welcome as long
as the parity test still passes bit-for-bit within float tolerance.

## Code style

- Kotlin, following the existing code in the module you touch.
- Keep UI strings in `strings.xml`; do not hard-code user-visible text.
- Add new icons as vector drawables converted from Lucide SVGs with
  `tools/lucide_to_vector.py`.
