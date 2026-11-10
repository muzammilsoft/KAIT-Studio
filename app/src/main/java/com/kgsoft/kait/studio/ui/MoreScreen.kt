package com.kgsoft.kait.studio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kgsoft.kait.studio.LocaleHelper
import com.kgsoft.kait.studio.R
import java.io.File

/**
 * More: profile (username only, never email), language, dark mode note,
 * about, and the local crash log from the error boundary.
 */
@Composable
fun MoreScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var displayName by remember { mutableStateOf(LocaleHelper.getDisplayName(context)) }
    var language by remember { mutableStateOf(LocaleHelper.getLanguage(context)) }
    var crashLog by remember {
        mutableStateOf(
            runCatching { File(context.filesDir, "crash.log").readText() }.getOrNull() ?: ""
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.more_profile_title))
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.more_username)) },
            singleLine = true,
        )
        Button(onClick = { LocaleHelper.setDisplayName(context, displayName.trim()) }) {
            Text(stringResource(R.string.more_save_name))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.more_language_title))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LanguageButton(
                label = stringResource(R.string.lang_system),
                selected = language == LocaleHelper.LANG_SYSTEM,
                onClick = {
                    language = LocaleHelper.LANG_SYSTEM
                    LocaleHelper.setLanguage(context, LocaleHelper.LANG_SYSTEM)
                },
            )
            LanguageButton(
                label = stringResource(R.string.lang_arabic),
                selected = language == LocaleHelper.LANG_ARABIC,
                onClick = {
                    language = LocaleHelper.LANG_ARABIC
                    LocaleHelper.setLanguage(context, LocaleHelper.LANG_ARABIC)
                },
            )
            LanguageButton(
                label = stringResource(R.string.lang_english),
                selected = language == LocaleHelper.LANG_ENGLISH,
                onClick = {
                    language = LocaleHelper.LANG_ENGLISH
                    LocaleHelper.setLanguage(context, LocaleHelper.LANG_ENGLISH)
                },
            )
            LanguageButton(
                label = stringResource(R.string.lang_chinese),
                selected = language == LocaleHelper.LANG_CHINESE,
                onClick = {
                    language = LocaleHelper.LANG_CHINESE
                    LocaleHelper.setLanguage(context, LocaleHelper.LANG_CHINESE)
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.more_dark_title))
        Text(text = stringResource(R.string.more_dark_body))

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.more_about_title))
        Text(text = stringResource(R.string.more_about_body))
        Text(text = stringResource(R.string.more_version, "0.1.0"))

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.more_crash_title))
        if (crashLog.isNotBlank()) {
            Text(text = crashLog.take(2000))
            OutlinedButton(onClick = {
                runCatching { File(context.filesDir, "crash.log").delete() }
                crashLog = ""
            }) {
                Text(stringResource(R.string.error_clear))
            }
        } else {
            Text(text = stringResource(R.string.more_crash_empty))
        }
    }
}

@Composable
fun LanguageButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) {
            Text(text = label)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(text = label)
        }
    }
}
