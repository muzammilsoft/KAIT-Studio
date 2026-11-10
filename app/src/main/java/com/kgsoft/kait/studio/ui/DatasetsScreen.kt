package com.kgsoft.kait.studio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.kgsoft.kait.studio.data.GalleryDataset
import com.kgsoft.kait.studio.data.Repository
import kotlinx.coroutines.launch
import java.io.File

/**
 * Datasets: the curated gallery (the dataset TinyBili trained on) with
 * real samples, plus download of the published JSONL file.
 */
@Composable
fun DatasetsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val datasets = remember { Repository.loadGalleryDatasets(context) }
    var downloaded by remember { mutableStateOf(setOf<String>()) }
    var error by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = stringResource(R.string.datasets_title))
        }
        items(datasets) { dataset ->
            DatasetCard(
                dataset = dataset,
                isDownloaded = dataset.id in downloaded,
                onDownload = {
                    scope.launch {
                        try {
                            val dir = File(context.filesDir, "datasets")
                            Repository.downloadFile(
                                dataset.downloadUrl,
                                File(dir, dataset.id + ".jsonl"),
                            ) { _, _ -> }
                            downloaded = downloaded + dataset.id
                        } catch (e: Exception) {
                            error = e.toString()
                        }
                    }
                },
            )
        }
        if (error.isNotBlank()) {
            item { Text(text = error) }
        }
    }
}

@Composable
fun DatasetCard(
    dataset: GalleryDataset,
    isDownloaded: Boolean,
    onDownload: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = dataset.name)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = dataset.description)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.datasets_stats,
                    dataset.lineCount,
                    dataset.fields.joinToString(", "),
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            for (sample in dataset.sample) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.datasets_sample_prompt, sample["prompt"] ?: ""))
                Text(text = stringResource(R.string.datasets_sample_response, sample["response"] ?: ""))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onDownload, enabled = !isDownloaded) {
                Text(
                    if (isDownloaded) {
                        stringResource(R.string.datasets_downloaded)
                    } else {
                        stringResource(R.string.datasets_download)
                    }
                )
            }
        }
    }
}
