# Changelog

All notable changes to KAIT Studio will be documented in this file.

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
