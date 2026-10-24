package io.github.zonar23.forkout.ui.settings

import io.github.zonar23.forkout.data.backup.BackupFile
import io.github.zonar23.forkout.data.backup.BackupSummary
import io.github.zonar23.forkout.data.repository.UserSettings

data class SettingsUiState(
    val amoledBlack: Boolean = false,
    val defaultRestSeconds: Int = UserSettings.DEFAULT_REST_SECONDS,
    /** True while a backup is written or read; buttons are disabled meanwhile. */
    val isBusy: Boolean = false,
    /** A parsed backup waiting for the user to confirm the replacement. */
    val pendingImport: PendingImport? = null,
    val message: SettingsMessage? = null,
)

data class PendingImport(
    val file: BackupFile,
    val summary: BackupSummary,
)

enum class SettingsMessage {
    EXPORT_DONE,
    IMPORT_DONE,
    INVALID_FILE,
    UNSUPPORTED_VERSION,
    IO_ERROR,
}
