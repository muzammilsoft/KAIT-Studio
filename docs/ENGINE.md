# KAIT Studio Engine

How the on-device inference engine works: the `.npz` format, the forward pass,
generation rules, and the thinking/answer split.

## 1. The `.npz` format

A TAIT model is a NumPy `.npz` archive (a zip of `.npy` arrays). KAIT Studio
loads the arrays below. Shapes are shown for the bundled model, TinyBili
(`tinyGPT0.1.npz`, 1,072,206 bytes, `vocab` 1003, `d_model` 64, `d_ff` 128,
`block_size` 64).

### Embeddings

| Key | Shape (TinyBili) | Dtype | Meaning |
| --- | --- | --- | --- |
| `wte` | (1003, 64) | float32 | Token embeddings: `vocab` × `d_model` |
| `wpe` | (64, 64) | float32 | Position embeddings: `block_size` × `d_model` |

### Attention (single head)

| Key | Shape | Dtype | Meaning |
| --- | --- | --- | --- |
| `Wq` | (64, 64) | float64 | Query projection |
| `Wk` | (64, 64) | float64 | Key projection |
| `Wv` | (64, 64) | float64 | Value projection |
| `Wo` | (64, 64) | float64 | Output projection |

### MLP (feed-forward)

| Key | Shape | Dtype | Meaning |
| --- | --- | --- | --- |
| `W1` | (64, 128) | float64 | First linear layer: `d_model` → `d_ff` |
| `b1` | (128,) | float64 | First layer bias |
| `W2` | (128, 64) | float64 | Second linear layer: `d_ff` → `d_model` |
| `b2` | (64,) | float64 | Second layer bias |

### Output head

| Key | Shape | Dtype | Meaning |
| --- | --- | --- | --- |
| `Whead` | (64, 1003) | float64 | Logits projection: `d_model` → `vocab` |
| `bhead` | (1003,) | float64 | Logits bias |

### Tokenizer (BPE merges) and control ids

| Key | Shape | Dtype | Meaning |
| --- | --- | --- | --- |
| `merge_a` | (744,) | int32 | First token of each BPE merge pair |
| `merge_b` | (744,) | int32 | Second token of each BPE merge pair |
| `merge_id` | (744,) | int32 | Resulting merged token id for each pair |
| `sep_id` | scalar | int64 | Separator token id |
| `end_id` | scalar | int64 | End-of-sequence token id |
| `tok_pad_id` | scalar | int64 | Tokenizer padding id |
| `pad_id` | scalar | int64 | Padding id |
| `block_size` | scalar | int64 | Maximum context length (64) |

The tokenizer is a byte-level BPE tokenizer: text is encoded to byte ids,
then the 744 merge rules in `merge_a` / `merge_b` → `merge_id` are applied
in order. Decoding reverses the merges back to bytes, then to text.

## 2. Forward order

The model is a single-block MiniGPT. For an input sequence of token ids:

1. **Embed**: `x = wte[token] + wpe[position]` for each position.
2. **Attention**: compute `Q = x·Wq`, `K = x·Wk`, `V = x·Wv`; causal
   single-head attention scores `softmax(Q·Kᵀ / √d_model)` (masked so a
   position only attends to itself and earlier positions); output `x = x + attn·Wo`
   (residual connection).
3. **MLP**: `x = x + W2·gelu(x·W1 + b1) + b2` (residual connection).
4. **Head**: `logits = x·Whead + bhead`; the logits at the last position
   give the next-token distribution.

No layer normalization is used; the math must match TAIT's MiniGPT exactly —
see [CONTRIBUTING.md](CONTRIBUTING.md).

## 3. Generation rules

Generation is autoregressive: sample the next token from the last-position
logits, append it, repeat until `end_id` is produced or the maximum length
is reached. The prompt is truncated to `block_size` tokens from the left
(oldest tokens dropped first) if it is longer than the context window.

- **temperature** (default 0.7): logits are divided by the temperature
  before softmax. Lower values make output more deterministic; `0` means
  greedy decoding (always pick the highest-logit token).
- **top_k** (default 40): only the `k` highest-logit tokens are kept;
  the rest are set to −∞ before sampling. `0` disables top-k filtering.
- **top_p** (nucleus sampling, default 0.9): tokens are kept in descending
  probability order until their cumulative probability reaches `p`;
  the rest are discarded. `1.0` disables top-p filtering.

Temperature is applied first, then top-k, then top-p, then sampling from
the remaining distribution.

## 4. Thinking / answer split

TinyBili was trained on examples with three fields: `prompt`, `reasoning`,
`response`. At generation time the model emits its reasoning first and then
its answer, separated by the separator token (`sep_id`). KAIT Studio splits
the generated text at that separator:

- text before the separator → shown as **Thinking**;
- text after the separator → shown as the **Answer**.

If no separator is generated, the whole output is shown as the answer.

## 5. Parity test result

The parity test compares the Kotlin engine against the reference TAIT
(NumPy) engine on the same prompts and checks that logits and generated
tokens match.

> The parity test is still running; `engine/PARITY-TEST-LOG.txt` does not
> exist yet in this workspace. This section will be updated with the real
> numbers from that log once the engine work lands. No numbers are claimed
> here.
