package com.kgsoft.kait.studio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
        LanguageDropdown(
            current = language,
            onSelect = { code ->
                language = code
                LocaleHelper.setLanguage(context, code)
            },
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.more_dark_title))
        Text(text = stringResource(R.string.more_dark_body))

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.more_about_title))
        Text(text = stringResource(R.string.more_about_body))
        Text(text = stringResource(R.string.more_version, "0.1.2"))

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

/**
 * Language picker as a dropdown menu: four languages in a row of buttons
 * took too much space and will only grow if more languages are added.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdown(current: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        LocaleHelper.LANG_SYSTEM to stringResource(R.string.lang_system),
        LocaleHelper.LANG_ARABIC to stringResource(R.string.lang_arabic),
        LocaleHelper.LANG_ENGLISH to stringResource(R.string.lang_english),
        LocaleHelper.LANG_CHINESE to stringResource(R.string.lang_chinese),
    )
    val selectedLabel = options.firstOrNull { it.first == current }?.second
        ?: options.first().second
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.more_language_title)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        // NOTE: ExposedDropdownMenu was removed in Material3 1.4; the menu
        // inside an ExposedDropdownMenuBox is now a plain DropdownMenu.
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { Text(text = label) },
                    onClick = {
                        onSelect(code)
                        expanded = false
                    },
                )
            }
        }
    }
}
