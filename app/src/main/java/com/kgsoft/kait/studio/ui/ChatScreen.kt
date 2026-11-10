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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kgsoft.kait.studio.R
import com.kgsoft.kait.studio.data.ChatMessage
import kotlinx.coroutines.launch

/**
 * Chat with a TAIT model. The model's thinking (<think>) is shown in its
 * own collapsible card, never mixed into the answer text.
 */
@Composable
fun ChatScreen(state: StudioState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Model header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val model = state.model
            Text(
                text = if (model != null) {
                    state.modelName + "  ·  vocab " + model.vocabSize +
                        "  ·  d " + model.dModel
                } else {
                    stringResource(R.string.chat_no_model)
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (state.model == null) {
            Button(
                onClick = { scope.launch { state.loadBundledModel() } },
                enabled = !state.modelLoading,
            ) {
                Text(stringResource(R.string.chat_load_tinybili))
            }
            if (state.modelLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator()
            }
            if (state.modelError.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = state.modelError)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(onClick = { showSettings = !showSettings }) {
            Text(stringResource(R.string.chat_settings))
        }
        if (showSettings) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.chat_temperature, state.temperature))
            Slider(
                value = state.temperature,
                onValueChange = { state.temperature = it },
                valueRange = 0.1f..1.5f,
            )
            Text(stringResource(R.string.chat_top_k, state.topK))
            Slider(
                value = state.topK.toFloat(),
                onValueChange = { state.topK = it.toInt() },
                valueRange = 1f..20f,
            )
            Text(stringResource(R.string.chat_max_tokens, state.maxTokens))
            Slider(
                value = state.maxTokens.toFloat(),
                onValueChange = { state.maxTokens = it.toInt() },
                valueRange = 10f..120f,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages) { message ->
                MessageCard(message = message)
            }
            if (state.isGenerating) {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
        LaunchedEffect(state.messages.size) {
            if (state.messages.isNotEmpty()) {
                listState.animateScrollToItem(state.messages.size - 1)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.chat_hint)) },
                enabled = state.model != null && !state.isGenerating,
            )
            Button(
                onClick = {
                    val prompt = input.trim()
                    if (prompt.isEmpty()) return@Button
                    input = ""
                    scope.launch { state.sendPrompt(prompt) }
                },
                enabled = state.model != null && !state.isGenerating,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(stringResource(R.string.chat_send))
            }
        }
    }
}

@Composable
fun MessageCard(message: ChatMessage) {
    var showReasoning by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (message.isError) {
            CardDefaults.cardColors()
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (message.role == "user") {
                    stringResource(R.string.chat_you)
                } else {
                    stringResource(R.string.chat_model)
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (message.reasoning.isNotBlank()) {
                OutlinedButton(onClick = { showReasoning = !showReasoning }) {
                    Text(stringResource(R.string.chat_show_thinking))
                }
                if (showReasoning) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = message.reasoning)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(text = message.text)
            if (message.role == "model" && !message.isError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.chat_stats,
                        message.newTokens,
                        message.millis,
                    )
                )
            }
        }
    }
}
