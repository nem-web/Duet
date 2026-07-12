package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DuetAppLayout
import com.example.ui.theme.DuetTheme
import com.example.viewmodel.DuetViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: DuetViewModel = viewModel()
            val selectedThemeId by viewModel.selectedThemeId.collectAsStateWithLifecycle()

            // Toast effect observer
            LaunchedEffect(key1 = true) {
                viewModel.toastMessage.collect { message ->
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }

            // Handle incoming notification intent navigation
            LaunchedEffect(intent) {
                val navigateTo = intent?.getStringExtra("navigate_to")
                if (navigateTo != null) {
                    viewModel.setTab(navigateTo)
                }
            }

            DuetTheme(themeId = selectedThemeId) {
                DuetAppLayout(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
