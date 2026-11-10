# KAIT Studio

**KAIT Studio 是 TAIT `.npz` 迷你模型的本机运行器。**

它加载使用 [TAIT](https://github.com/muzammilsoft/tait)（手机 AI 训练工具包）训练的小型 Transformer 模型，并在 Android 手机上完全本地运行——模型装入设备后，不需要服务器，也不需要联网。

English: [README.md](README.md) · العربية: [README.ar.md](README.ar.md)

## 功能

- **本机推理**——模型通过移植到 Kotlin 的小型引擎在本地运行，您的提示词永远不会离开手机。
- **内置模型**——TinyBili 内置于应用中，无需下载即可聊天。
- **导入自己的 `.npz`**——导入任何用 TAIT 训练的 `.npz` 模型并运行。
- **思考与答案视图**——模型输出分为思考部分和最终答案。
- **离线模型库与数据集**——浏览内置模型及其训练数据集。

## 标签页

| 标签页 | 功能 |
| --- | --- |
| **聊天** | 与已加载的模型对话，可调节温度、top-k 和 top-p。 |
| **模型库** | 内置模型（TinyBili）和已导入的 `.npz` 模型。 |
| **数据集** | 训练数据集，例如 `mixed`——TinyBili 的训练数据集。 |
| **设置** | 模型、生成与应用设置。 |

## 安装

1. 从[发布页面](../../releases)下载最新的 APK，或自行构建（见下文）。
2. 如手机提示，请允许安装来自未知来源的应用。
3. 打开 APK 并安装。无需账号，除导入模型所需的存储访问外无需其他权限。

## 运行 TinyBili

1. 打开应用——TinyBili 已内置，会自动加载。
2. 进入**聊天**标签页。
3. 输入消息并发送，模型将在设备上回答。

TinyBili 的信息（来自其 `tait-meta.json`）：

- 使用 TAIT **2.3.0** 在 `mixed-1.jsonl` 上训练——**6666** 条样本，**50** 轮（epochs）
- `vocab` **1003**，`d_model` **64**，`d_ff` **128**，`block_size` **64**
- `final_loss` **0.490027681430927**，`val_loss` **1.0366690878082827**
- 文件：`tinyGPT0.1.npz`，大小 **1,072,206 字节**
- SHA-256：`1a2b1c51ba9b4f01bb5bff781653825587a7e06ca187a77423c6b30063e3b28d`
- 来源：<https://huggingface.co/muzammilsoft/tinybili>

## 导入自己的 `.npz`

1. 使用 TAIT 训练模型并导出 `.npz` 文件（如果有 `tait-meta.json`，也一并导出）。
2. 在应用中进入**模型库 → 导入**，从存储中选择 `.npz` 文件。
3. 模型将出现在模型库中，点击即可加载，然后在**聊天**标签页中与它对话。

导入的模型必须使用 [docs/ENGINE.md](docs/ENGINE.md) 中记录的 MiniGPT `.npz` 布局。

## 签名——测试密钥，不是生产密钥

调试版 APK 和发布版 APK 均使用固定的**测试**密钥库签名
（`app/debug.keystore`，别名 `kait-debug`）。它是开发/测试签名密钥，
**不是生产密钥**。密钥库和密钥的密码均为固定值 `android`。
请勿使用此密钥库签署任何打算作为生产应用分发的内容；
如需发布生产应用，请生成自己的发布密钥。

## 从源码构建

```bash
./gradlew assembleDebug   # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease # app/build/outputs/apk/release/app-release.apk
```

需要 JDK 17。详见 [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)。

## 引擎

推理引擎记录在 [docs/ENGINE.md](docs/ENGINE.md)：`.npz` 格式、forward 顺序、
生成规则（温度 / top-k / top-p）以及思考与答案的拆分。

## 链接

- TAIT — <https://github.com/muzammilsoft/tait>
- TinyBili（Hugging Face）— <https://huggingface.co/muzammilsoft/tinybili>
- KAIT Studio — <https://github.com/muzammilsoft/KAIT-Studio>

## 许可证

MIT——见 [LICENSE](LICENSE)。Copyright (c) 2026 Khartoum Ghoul。

图标：[Lucide](https://lucide.dev)（ISC 许可证）。字体：[Cairo](https://github.com/google/fonts/tree/main/ofl/cairo)（SIL OFL 1.1）。
