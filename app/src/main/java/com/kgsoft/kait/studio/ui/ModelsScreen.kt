package com.kgsoft.kait.studio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kgsoft.kait.studio.R
import com.kgsoft.kait.studio.data.GalleryModel
import com.kgsoft.kait.studio.data.HubModel
import com.kgsoft.kait.studio.data.Repository
import kotlinx.coroutines.launch
import java.io.File

/**
 * Models: the curated gallery first (TinyBili), then a live public search
 * on the Hugging Face Hub. Downloads go to filesDir/models and can be
 * activated for chat.
 */
@Composable
fun ModelsScreen(state: StudioState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gallery = remember { Repository.loadGalleryModels(context) }
    var query by remember { mutableStateOf("tait") }
    var hubResults by remember { mutableStateOf<List<HubModel>>(emptyList()) }
    var hubLoading by remember { mutableStateOf(false) }
    var hubError by remember { mutableStateOf("") }
    var downloadingId by remember { mutableStateOf("") }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadError by remember { mutableStateOf("") }
    var localFiles by remember { mutableStateOf(Repository.listLocalModels(state.modelsDir)) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = stringResource(R.string.models_gallery_title))
        }
        items(gallery) { model ->
            ModelCard(
                model = model,
                isActive = state.modelName == model.name,
                isDownloaded = localFiles.any { it.name == model.fileName },
                isDownloading = downloadingId == model.id,
                progress = downloadProgress,
                onDownload = {
                    downloadingId = model.id
                    downloadError = ""
                    scope.launch {
                        try {
                            val dest = File(state.modelsDir, model.fileName)
                            Repository.downloadFile(model.downloadUrl, dest) { done, total ->
                                downloadProgress = if (total > 0) done.toFloat() / total else 0f
                            }
                            localFiles = Repository.listLocalModels(state.modelsDir)
                        } catch (e: Exception) {
                            downloadError = e.toString()
                        } finally {
                            downloadingId = ""
                        }
                    }
                },
                onActivateBundled = {
                    scope.launch { state.loadBundledModel() }
                },
                onActivateFile = { file ->
                    scope.launch { state.loadModelFile(file) }
                },
                localFile = localFiles.firstOrNull { it.name == model.fileName },
            )
        }
        if (downloadError.isNotBlank()) {
            item { Text(text = downloadError) }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.models_hub_title))
        }
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.models_search_hint)) },
                )
                Button(
                    onClick = {
                        hubLoading = true
                        hubError = ""
                        scope.launch {
                            try {
                                hubResults = Repository.searchHubModels(query)
                            } catch (e: Exception) {
                                hubError = e.toString()
                            } finally {
                                hubLoading = false
                            }
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(stringResource(R.string.models_search))
                }
            }
        }
        if (hubLoading) {
            item { CircularProgressIndicator() }
        }
        if (hubError.isNotBlank()) {
            item { Text(text = hubError) }
        }
        items(hubResults) { hub ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = hub.id)
                    Text(
                        text = stringResource(
                            R.string.models_hub_stats,
                            hub.downloads,
                            hub.likes,
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ModelCard(
    model: GalleryModel,
    isActive: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    progress: Float,
    onDownload: () -> Unit,
    onActivateBundled: () -> Unit,
    onActivateFile: (File) -> Unit,
    localFile: File?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = model.name)
            Text(text = model.repo)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = model.description)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.models_stats,
                    model.vocabSize,
                    model.dModel,
                    model.examples,
                    model.epochs,
                    model.finalLoss,
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    if (model.supportsThinking) {
                        R.string.models_thinking_yes
                    } else {
                        R.string.models_thinking_no
                    }
                )
            )
            if (isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.models_active))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (model.id == "tinybili") {
                    Button(onClick = onActivateBundled) {
                        Text(stringResource(R.string.models_use_bundled))
                    }
                }
                if (isDownloaded && localFile != null) {
                    Button(onClick = { onActivateFile(localFile) }) {
                        Text(stringResource(R.string.models_activate))
                    }
                } else {
                    OutlinedButton(onClick = onDownload, enabled = !isDownloading) {
                        Text(stringResource(R.string.models_download))
                    }
                }
            }
            if (isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
