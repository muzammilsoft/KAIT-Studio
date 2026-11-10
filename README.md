# KAIT Studio

**KAIT Studio is an on-device runner for TAIT `.npz` mini models.**

It loads small transformer models trained with [TAIT](https://github.com/muzammilsoft/tait) (Termux AI Training Toolkit) and runs them entirely on your Android phone — no server, no internet needed after the model is on the device.

Arabic: [README.ar.md](README.ar.md) · 简体中文: [README.zh-CN.md](README.zh-CN.md)

## Features

- **On-device inference** — the model runs locally with a small NumPy-style engine ported to Kotlin. Your prompts never leave the phone.
- **Bundled model** — TinyBili ships inside the app, ready to chat with no download.
- **Import your own `.npz`** — bring any TAIT-trained `.npz` model and run it.
- **Thinking + answer view** — the model output is split into a thinking part and a final answer.
- **Offline gallery & datasets** — browse the bundled model and the dataset it was trained on.

## Tabs

| Tab | What it does |
| --- | --- |
| **Chat** | Talk to the loaded model. Adjust temperature, top-k and top-p. |
| **Gallery** | The bundled model (TinyBili) and any imported `.npz` models. |
| **Datasets** | Training datasets, e.g. `mixed` — the dataset TinyBili trained on. |
| **Settings** | Model, generation and app settings. |

## Install

1. Download the latest APK from the [Releases](../../releases) page, or build it yourself (see below).
2. On your phone, allow installation from unknown sources if asked.
3. Open the APK and install. No account, no permissions beyond storage access for importing models.

## Run TinyBili

1. Open the app — TinyBili is bundled and loads automatically.
2. Go to the **Chat** tab.
3. Type a message and send. The model answers on-device.

TinyBili facts (from its `tait-meta.json`):

- Trained with TAIT **2.3.0** on `mixed-1.jsonl` — **6666** examples, **50** epochs
- `vocab` **1003**, `d_model` **64**, `d_ff` **128**, `block_size` **64**
- `final_loss` **0.490027681430927**, `val_loss` **1.0366690878082827**
- File: `tinyGPT0.1.npz`, **1,072,206 bytes**
- SHA-256: `1a2b1c51ba9b4f01bb5bff781653825587a7e06ca187a77423c6b30063e3b28d`
- Source: <https://huggingface.co/muzammilsoft/tinybili>

## Import your own `.npz`

1. Train a model with TAIT and export its `.npz` (plus its `tait-meta.json` if you have one).
2. In the app, go to **Gallery → Import** and pick the `.npz` file from your storage.
3. The model appears in the Gallery; tap it to load it, then chat with it in the **Chat** tab.

Imported models must use the same MiniGPT `.npz` layout documented in [docs/ENGINE.md](docs/ENGINE.md).

## Signing — test key, not a production key

Both the debug APK and the release APK are signed with a fixed **test** keystore
(`app/debug.keystore`, alias `kait-debug`). It is a development/test signing key,
**not a production key**. Store and key passwords are the fixed value `android`.
Do not use this keystore to sign anything you intend to distribute as a production app;
generate your own release key for that.

## Build from source

```bash
./gradlew assembleDebug   # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease # app/build/outputs/apk/release/app-release.apk
```

Requires JDK 17. See [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md).

## Releases

APKs are attached to GitHub releases by the [Release workflow](.github/workflows/release.yml),
never uploaded by hand: the local GitHub credential cannot reach the asset-upload
server, but the workflow runs on GitHub itself with `GITHUB_TOKEN`.

- **New version:** create the release (tag `vX.Y.Z`, Arabic changelog in the body),
  then run the workflow manually with that tag, or simply push the tag — the
  workflow builds both APKs and attaches them to the release.
- **Existing release missing its APKs:** run the workflow manually
  (Actions → Release → Run workflow) and give the release tag.

## Engine

The inference engine is documented in [docs/ENGINE.md](docs/ENGINE.md): the `.npz`
format, forward order, generation rules (temperature / top-k / top-p) and the
thinking/answer split.

## Links

- TAIT — <https://github.com/muzammilsoft/tait>
- TinyBili on Hugging Face — <https://huggingface.co/muzammilsoft/tinybili>
- KAIT Studio — <https://github.com/muzammilsoft/KAIT-Studio>

## License

MIT — see [LICENSE](LICENSE). Copyright (c) 2026 Khartoum Ghoul.

Icons: [Lucide](https://lucide.dev) (ISC license). Font: [Cairo](https://github.com/google/fonts/tree/main/ofl/cairo) (SIL OFL 1.1).
