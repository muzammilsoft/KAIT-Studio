# Changelog

All notable changes to KAIT Studio will be documented in this file.

## [0.1.2] - 2026-11-11

### Changed

- Language picker in the More tab is now a dropdown menu instead of four buttons in a row.
- Chat messages are now bubbles on opposite sides: your messages on one side, the model's on the other (with a different colour each), instead of full-width cards stacked in one column.
- Thinking is now a per-model capability, not a default part of messaging. Each gallery model declares `supports_thinking` in `gallery.json`; the gallery card and the chat header say whether a model supports thinking. Models without it show the plain answer only, with no thinking card. TinyBili does not support thinking.

## [0.1.1] - 2026-11-10

### Fixed

- Model loading crashed on Android 10 and older with `NoSuchMethodError: readAllBytes` on `ZipInputStream` (Java 9 API, Android only provides it from API 33). The NPZ reader now uses Kotlin's buffered `readBytes()`, which works on every supported API level. Found by a crash on a Huawei Y8p (Android 10) when loading the bundled TinyBili model.

## [0.1.0] - 2026-11-10

First release.

### Added

- On-device runner for TAIT `.npz` mini models (single-block MiniGPT engine in Kotlin).
- Bundled model TinyBili (`tinyGPT0.1.npz`, 1,072,206 bytes), trained with TAIT 2.3.0 on `mixed-1.jsonl` (6666 examples, 50 epochs).
- Four tabs: Chat, Models, Datasets, More.
- Import of user-provided `.npz` models from device storage.
- Generation controls: temperature, top-k, top-p.
- Thinking / answer split in chat output.
- Offline gallery (`gallery.json`) and dataset index (`datasets.json`).
- Cairo font (Regular 400 / Bold 700 static instances) and Lucide icons.
- GitHub Actions CI building debug and release APKs.
- Documentation: README (English, Arabic, Simplified Chinese), `docs/ENGINE.md`, `docs/CONTRIBUTING.md`.
