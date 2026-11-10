package com.kgsoft.kait.studio

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kgsoft.kait.studio.ui.theme.KAITStudioTheme
import java.io.File

/**
 * Error boundary screen: shown when an unexpected error escapes every
 * other handler. It explains what happened in plain words and offers a
 * restart instead of a crash.
 */
class ErrorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MESSAGE = "error_message"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
        val log = runCatching { File(filesDir, "crash.log").readText() }.getOrNull() ?: ""
        setContent {
            KAITStudioTheme {
                ErrorScreen(message = message, log = log,
                    onRestart = {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(intent)
                        finish()
                    },
                    onClearLog = {
                        runCatching { File(filesDir, "crash.log").delete() }
                        finish()
                    })
            }
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    log: String,
    onRestart: () -> Unit,
    onClearLog: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(text = stringResource(R.string.error_title))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(R.string.error_body))
        Spacer(modifier = Modifier.height(12.dp))
        if (message.isNotBlank()) {
            Text(text = message)
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (log.isNotBlank()) {
            Text(text = log.take(2000))
            Spacer(modifier = Modifier.height(12.dp))
        }
        Button(onClick = onRestart) {
            Text(text = stringResource(R.string.error_restart))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onClearLog) {
            Text(text = stringResource(R.string.error_clear))
        }
    }
}
