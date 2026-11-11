package com.kgsoft.kait.studio.ui

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.kgsoft.kait.studio.R
import com.kgsoft.kait.studio.data.ChatMessage
import com.kgsoft.kait.studio.engine.GenerationConfig
import com.kgsoft.kait.studio.engine.KaitEngine
import com.kgsoft.kait.studio.engine.LoadedModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

enum class Tab {
    CHAT, MODELS, DATASETS, MORE
}

/** Shared state for the whole app: the loaded model, the chat, the knobs. */
class StudioState(val context: Context) {
    var model by mutableStateOf<LoadedModel?>(null)
    var modelName by mutableStateOf("")
    /**
     * Whether the loaded model supports thinking. Thinking is a per-model
     * capability declared in the gallery (`supports_thinking`); it is never
     * assumed. False means the chat shows the plain answer only.
     */
    var modelSupportsThinking by mutableStateOf(false)
    var modelLoading by mutableStateOf(false)
    var modelError by mutableStateOf("")
    val messages = mutableStateListOf<ChatMessage>()
    var isGenerating by mutableStateOf(false)
    var temperature by mutableFloatStateOf(0.7f)
    var topK by mutableIntStateOf(8)
    var maxTokens by mutableIntStateOf(60)

    val modelsDir: File get() = File(context.filesDir, "models")

    fun generationConfig(): GenerationConfig {
        return GenerationConfig(
            maxNewTokens = maxTokens,
            temperature = temperature,
            topK = topK,
            topP = 0.9f,
        )
    }

    /** Load the bundled TinyBili model from assets. */
    suspend fun loadBundledModel() {
        modelLoading = true
        modelError = ""
        try {
            val loaded = withContext(Dispatchers.Default) {
                context.assets.open("models/tinyGPT0.1.npz").use { input ->
                    KaitEngine.load(input)
                }
            }
            model = loaded
            modelName = "TinyBili"
            modelSupportsThinking = gallerySupportsThinking("tinybili")
        } catch (e: Exception) {
            modelError = e.toString()
        } finally {
            modelLoading = false
        }
    }

    /** Thinking support of a gallery model, matched by id or file name. */
    private fun gallerySupportsThinking(id: String, fileName: String = ""): Boolean {
        return runCatching {
            com.kgsoft.kait.studio.data.Repository.loadGalleryModels(context)
                .firstOrNull { it.id == id || (fileName.isNotEmpty() && it.fileName == fileName) }
                ?.supportsThinking ?: false
        }.getOrDefault(false)
    }

    /** Load a model file previously downloaded to filesDir/models. */
    suspend fun loadModelFile(file: File) {
        modelLoading = true
        modelError = ""
        try {
            val loaded = withContext(Dispatchers.Default) {
                FileInputStream(file).use { input ->
                    KaitEngine.load(input)
                }
            }
            model = loaded
            modelName = file.nameWithoutExtension
            // A file with no gallery entry declares nothing, so it is
            // treated as a non-thinking model. A downloaded gallery model
            // keeps the thinking flag from its gallery entry.
            modelSupportsThinking = gallerySupportsThinking("", file.name)
        } catch (e: Exception) {
            modelError = e.toString()
        } finally {
            modelLoading = false
        }
    }

    /** Generate a reply for [prompt] and append it to the chat. */
    suspend fun sendPrompt(prompt: String) {
        val current = model ?: return
        messages.add(ChatMessage(role = "user", text = prompt))
        isGenerating = true
        try {
            val result = withContext(Dispatchers.Default) {
                current.generate(prompt, generationConfig())
            }
            // Thinking is shown only for models that declare it. For any
            // other model the whole generated text is the answer, with no
            // think-tag parsing assumed.
            messages.add(
                ChatMessage(
                    role = "model",
                    text = if (modelSupportsThinking) result.answer else result.raw,
                    reasoning = if (modelSupportsThinking) result.reasoning else "",
                    millis = result.millis,
                    newTokens = result.newTokens,
                )
            )
        } catch (e: Exception) {
            messages.add(
                ChatMessage(
                    role = "model",
                    text = e.toString(),
                    isError = true,
                )
            )
        } finally {
            isGenerating = false
        }
    }
}

@Composable
fun KAITStudioApp() {
    val context = LocalContext.current
    val state = remember { StudioState(context.applicationContext) }
    var tab by remember { mutableStateOf(Tab.CHAT) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.CHAT,
                    onClick = { tab = Tab.CHAT },
                    icon = { Icon(painterResource(R.drawable.ic_message_square), contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_chat)) },
                )
                NavigationBarItem(
                    selected = tab == Tab.MODELS,
                    onClick = { tab = Tab.MODELS },
                    icon = { Icon(painterResource(R.drawable.ic_boxes), contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_models)) },
                )
                NavigationBarItem(
                    selected = tab == Tab.DATASETS,
                    onClick = { tab = Tab.DATASETS },
                    icon = { Icon(painterResource(R.drawable.ic_database), contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_datasets)) },
                )
                NavigationBarItem(
                    selected = tab == Tab.MORE,
                    onClick = { tab = Tab.MORE },
                    icon = { Icon(painterResource(R.drawable.ic_settings), contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_more)) },
                )
            }
        }
    ) { padding ->
        when (tab) {
            Tab.CHAT -> ChatScreen(state = state, modifier = Modifier.padding(padding))
            Tab.MODELS -> ModelsScreen(state = state, modifier = Modifier.padding(padding))
            Tab.DATASETS -> DatasetsScreen(modifier = Modifier.padding(padding))
            Tab.MORE -> MoreScreen(modifier = Modifier.padding(padding))
        }
    }
}
