package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.AssistantViewModel
import com.example.ui.JarvisApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val viewModel: AssistantViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        // Permissions Launcher
        val permissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { _ ->
          viewModel.updateBridgeStatus()
        }

        Surface(modifier = Modifier.fillMaxSize()) {
          JarvisApp(
            viewModel = viewModel,
            onRequestAudioPermission = {
              permissionLauncher.launch(
                arrayOf(Manifest.permission.RECORD_AUDIO)
              )
            },
            onRequestCallPermission = {
              permissionLauncher.launch(
                arrayOf(Manifest.permission.CALL_PHONE)
              )
            },
            onRequestContactsPermission = {
              permissionLauncher.launch(
                arrayOf(Manifest.permission.READ_CONTACTS)
              )
            }
          )
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.updateBridgeStatus()
  }

  override fun onPause() {
    super.onPause()
    viewModel.interruptSpeaking()
  }

  override fun onStop() {
    super.onStop()
    viewModel.interruptSpeaking()
  }
}
