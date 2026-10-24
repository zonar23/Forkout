package io.github.zonar23.forkout.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zonar23.forkout.data.backup.BackupException
import io.github.zonar23.forkout.data.backup.BackupFileStore
import io.github.zonar23.forkout.data.backup.BackupManager
import io.github.zonar23.forkout.data.backup.summary
import io.github.zonar23.forkout.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val fileStore: BackupFileStore,
    private val clock: Clock,
) : ViewModel() {

    private val local = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(settingsRepository.settings, local) { settings, local ->
        local.copy(amoledBlack = settings.amoledBlack, defaultRestSeconds = settings.defaultRestSeconds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setAmoledBlack(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAmoledBlack(enabled) }
    }

    fun stepDefaultRest(deltaSeconds: Int) {
        viewModelScope.launch { settingsRepository.adjustDefaultRestSeconds(deltaSeconds, MAX_REST_SECONDS) }
    }

    /** Date part of the suggested backup file name, e.g. "2026-10-24". */
    fun backupDate(): String = LocalDate.now(clock).toString()

    fun export(uri: Uri) = runBusy {
        fileStore.write(uri, backupManager.encode(backupManager.export()))
        SettingsMessage.EXPORT_DONE
    }

    /** Reads and checks the file, then asks for confirmation before replacing anything. */
    fun readImport(uri: Uri) = runBusy {
        val file = backupManager.decode(fileStore.read(uri))
        local.update { it.copy(pendingImport = PendingImport(file, file.summary())) }
        null
    }

    fun confirmImport() {
        val pending = local.value.pendingImport ?: return
        local.update { it.copy(pendingImport = null) }
        runBusy {
            backupManager.import(pending.file)
            SettingsMessage.IMPORT_DONE
        }
    }

    fun dismissImport() = local.update { it.copy(pendingImport = null) }

    fun dismissMessage() = local.update { it.copy(message = null) }

    /** Runs [block] with the busy flag set and shows its result or the matching error. */
    private fun runBusy(block: suspend () -> SettingsMessage?) {
        if (local.value.isBusy) return
        local.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val message = try {
                block()
            } catch (e: BackupException.UnsupportedVersion) {
                SettingsMessage.UNSUPPORTED_VERSION
            } catch (e: BackupException.InvalidFile) {
                SettingsMessage.INVALID_FILE
            } catch (e: IOException) {
                SettingsMessage.IO_ERROR
            } catch (e: SecurityException) {
                // The file provider revoked access to the picked document.
                SettingsMessage.IO_ERROR
            }
            local.update { it.copy(isBusy = false, message = message ?: it.message) }
        }
    }

    companion object {
        const val MAX_REST_SECONDS = 600
        const val REST_STEP_SECONDS = 15
    }
}
