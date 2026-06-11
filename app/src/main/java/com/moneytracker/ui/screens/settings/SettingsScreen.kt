package com.moneytracker.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Ứng dụng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Phiên bản", style = MaterialTheme.typography.bodySmall)
                            Text("v0.0.1", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Text("Cập nhật", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Kiểm tra bản cập nhật", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                when {
                                    uiState.isChecking -> "Đang kiểm tra..."
                                    uiState.isDownloading -> "Đang tải... ${uiState.downloadProgress}%"
                                    uiState.updateAvailable -> "Phiên bản mới: ${uiState.latestVersion}"
                                    uiState.error != null -> uiState.error!!
                                    uiState.checked -> "Bạn đang dùng phiên bản mới nhất"
                                    else -> "Nhấn để kiểm tra"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    uiState.updateAvailable -> MaterialTheme.colorScheme.primary
                                    uiState.error != null -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        if (!uiState.isChecking && !uiState.isDownloading) {
                            Button(onClick = { viewModel.checkUpdate() }) { Text("Kiểm tra") }
                        } else {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    }

                    if (uiState.updateAvailable) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.downloadAndInstall() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isDownloading
                        ) {
                            if (uiState.isDownloading) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Tải xuống và cài đặt")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Text("MoneyTracker v0.0.1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
