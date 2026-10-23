package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.SavedInsightsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.util.TtsManager
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var ttsManager: TtsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: App Check wiring mandated by Firebase AI Logic before setContent or ViewModels
        configureAppCheck(this, intent)

        // Initialize Android Text-To-Speech for Tamil & English read-aloud
        ttsManager = TtsManager(this)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf("chat") }

                    when (currentScreen) {
                        "saved_insights" -> {
                            SavedInsightsScreen(
                                viewModel = chatViewModel,
                                ttsManager = ttsManager,
                                onBack = { currentScreen = "chat" }
                            )
                        }
                        else -> {
                            ChatScreen(
                                viewModel = chatViewModel,
                                ttsManager = ttsManager,
                                onNavigateToSaved = { currentScreen = "saved_insights" }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}
