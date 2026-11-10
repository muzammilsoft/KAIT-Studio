package com.kgsoft.kait.studio

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.kgsoft.kait.studio.ui.KAITStudioApp
import com.kgsoft.kait.studio.ui.theme.KAITStudioTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KAITStudioTheme {
                KAITStudioApp()
            }
        }
    }
}
