package com.moneytracker.ui.screens.settings

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class SettingsUiState(
    val isChecking: Boolean = false,
    val checked: Boolean = false,
    val updateAvailable: Boolean = false,
    val latestVersion: String = "",
    val downloadUrl: String = "",
    val error: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val downloadedFilePath: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private val app = application

    fun checkUpdate() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(isChecking = true)
            try {
                val result = withContext(Dispatchers.IO) { fetchLatestRelease() }
                _uiState.value = result
            } catch (e: Exception) {
                _uiState.value = SettingsUiState(checked = true, error = "Không thể kiểm tra: ${e.message}")
            }
        }
    }

    fun downloadAndInstall() {
        val url = _uiState.value.downloadUrl
        if (url.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true, downloadProgress = 0)
            try {
                val filePath = withContext(Dispatchers.IO) {
                    downloadApk(url)
                }
                _uiState.value = _uiState.value.copy(isDownloading = false, downloadedFilePath = filePath)
                installApk(filePath)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isDownloading = false, error = "Tải thất bại: ${e.message}")
            }
        }
    }

    private fun downloadApk(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000

        val fileName = "MoneyTracker-${_uiState.value.latestVersion}.apk"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val file = File(downloadsDir, fileName)

        conn.inputStream.use { input ->
            FileOutputStream(file).use { output ->
                val totalBytes = conn.contentLength
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                }
            }
        }

        return file.absolutePath
    }

    private fun installApk(filePath: String) {
        val file = File(filePath)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
        } else {
            android.net.Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        app.startActivity(intent)
    }

    private fun fetchLatestRelease(): SettingsUiState {
        val url = URL("https://api.github.com/repos/khucnguyenthanhbinh-star/MoneyTracker/releases/latest")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        val response = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        val latestTag = json.getString("tag_name")
        val currentVersion = "v0.0.1"

        val assets = json.getJSONArray("assets")
        var downloadUrl = ""
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.getString("name").endsWith(".apk")) {
                downloadUrl = asset.getString("browser_download_url")
                break
            }
        }

        return if (latestTag != currentVersion) {
            SettingsUiState(checked = true, updateAvailable = true, latestVersion = latestTag, downloadUrl = downloadUrl)
        } else {
            SettingsUiState(checked = true, updateAvailable = false, latestVersion = latestTag)
        }
    }
}
