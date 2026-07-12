package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.UpdateInfo
import com.example.util.UpdateManager
import com.example.util.UpdateState
import com.example.viewmodel.DuetViewModel

@Composable
fun AppUpdateDialog(viewModel: DuetViewModel) {
    val context = LocalContext.current
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    
    // Check if install permission is granted
    var isInstallPermissionGranted by remember {
        mutableStateOf(UpdateManager.canRequestPackageInstalls(context))
    }
    
    // Track activity resume to recheck permissions
    DisposableEffect(updateState) {
        isInstallPermissionGranted = UpdateManager.canRequestPackageInstalls(context)
        onDispose {}
    }

    when (val state = updateState) {
        is UpdateState.Idle, is UpdateState.Checking -> {
            // No UI overlay is required for idle or automatic check states.
        }
        
        is UpdateState.UpdateAvailable -> {
            val info = state.info
            val isMandatory = info.isMandatory(UpdateManager.getInstalledVersionCode(context))
            LaunchedEffect(info) {
                android.util.Log.d("UpdateScreens", "APP_UPDATE: Showing dialog")
            }
            Dialog(
                onDismissRequest = {
                    if (!isMandatory) {
                        viewModel.dismissUpdate()
                    }
                },
                properties = DialogProperties(
                    dismissOnBackPress = !isMandatory,
                    dismissOnClickOutside = !isMandatory
                )
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("update_available_dialog")
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update Available",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "New Update Available!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Version v${info.versionName} (${info.versionCode})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Release Notes section
                        if (info.releaseNotes.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "What's New:",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = info.releaseNotes,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        if (isMandatory) {
                            Text(
                                text = "This is a critical update. You must update to continue using Duet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                        ) {
                            if (!isMandatory) {
                                OutlinedButton(
                                    onClick = { viewModel.dismissUpdate() },
                                    modifier = Modifier.weight(1f).testTag("update_cancel_btn")
                                ) {
                                    Text("Not Now")
                                }
                            }
                            
                            Button(
                                onClick = {
                                    android.util.Log.d("UpdateScreens", "APP_UPDATE: User clicked Update")
                                    viewModel.downloadAndPrepareUpdate(info)
                                },
                                modifier = Modifier.weight(1f).testTag("update_now_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Update")
                                }
                            }
                        }
                    }
                }
            }
        }
        
        is UpdateState.Downloading -> {
            val progress = state.progress
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("update_downloading_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Downloading Update...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val progressPercent = (progress * 100).toInt().coerceIn(0, 100)
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LinearProgressIndicator(
                            progress = { progress },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Please keep the app open during download",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        is UpdateState.ReadyToInstall -> {
            val file = state.file
            val info = state.info
            val isMandatory = info.isMandatory(UpdateManager.getInstalledVersionCode(context))
            
            // Recheck permission status
            val hasInstallPermission = UpdateManager.canRequestPackageInstalls(context)
            
            Dialog(
                onDismissRequest = {
                    if (!isMandatory) {
                        viewModel.dismissUpdate()
                    }
                },
                properties = DialogProperties(
                    dismissOnBackPress = !isMandatory,
                    dismissOnClickOutside = !isMandatory
                )
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("update_ready_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Install Permission Needed",
                            tint = if (hasInstallPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = if (hasInstallPermission) "Install Update" else "Permission Required",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = if (hasInstallPermission) {
                                "The update has been downloaded. Press Install to upgrade now."
                            } else {
                                "To install this update, Android requires the 'Install unknown apps' permission. Please enable this permission in the settings page."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                        ) {
                            if (!isMandatory) {
                                OutlinedButton(
                                    onClick = { viewModel.dismissUpdate() },
                                    modifier = Modifier.weight(1f).testTag("install_dismiss_btn")
                                ) {
                                    Text("Dismiss")
                                }
                            }
                            
                            if (hasInstallPermission) {
                                Button(
                                    onClick = { viewModel.installDownloadedUpdate(file) },
                                    modifier = Modifier.weight(1f).testTag("install_confirm_btn")
                                ) {
                                    Text("Install")
                                }
                            } else {
                                Button(
                                    onClick = { 
                                        UpdateManager.openInstallSettings(context)
                                        Toast.makeText(context, "Please enable 'Allow from this source' for Duet", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f).testTag("install_settings_btn")
                                ) {
                                    Text("Grant Permission")
                                }
                            }
                        }
                    }
                }
            }
        }
        
        is UpdateState.Error -> {
            Dialog(
                onDismissRequest = { viewModel.dismissUpdate() }
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("update_error_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Update Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Update Failed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { viewModel.dismissUpdate() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().testTag("error_dismiss_btn")
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}
