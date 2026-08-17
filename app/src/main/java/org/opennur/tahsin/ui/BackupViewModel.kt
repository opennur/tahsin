package org.opennur.tahsin.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opennur.tahsin.util.BackupManager
import org.opennur.tahsin.util.BackupResult

data class BackupUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val success: Boolean = false,
    val importCompleted: Boolean = false,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val app: Context,
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun exportTo(uri: Uri) {
        _state.value = BackupUiState(busy = true)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val json = backupManager.export()
                    app.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Tidak dapat membuka file untuk menulis")
                }
            }
            _state.value = BackupUiState(
                message = if (result.isSuccess) "Ekspor berhasil!" else "Gagal: ${result.exceptionOrNull()?.message}",
                success = result.isSuccess,
            )
        }
    }

    fun importFrom(uri: Uri) {
        _state.value = BackupUiState(busy = true)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val json = app.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error("Tidak dapat membaca file backup")
                    backupManager.import(json)
                }
            }
            _state.value = when {
                result.isFailure -> BackupUiState(
                    message = "Gagal: ${result.exceptionOrNull()?.message}",
                    success = false,
                )
                result.getOrDefault(BackupResult()).success -> BackupUiState(
                    message = "Impor berhasil! ${result.getOrDefault(BackupResult()).importedStores} file dipulihkan.",
                    success = true,
                    importCompleted = true,
                )
                else -> BackupUiState(
                    message = "Impor selesai: ${result.getOrDefault(
                        org.opennur.tahsin.util.BackupResult(),
                    ).errors.joinToString("; ")}",
                    success = true,
                    importCompleted = true,
                )
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
