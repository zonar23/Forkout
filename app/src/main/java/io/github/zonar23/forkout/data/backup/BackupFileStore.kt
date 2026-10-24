package io.github.zonar23.forkout.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads and writes backup files picked through the system file dialog (Storage Access
 * Framework), so the app needs no storage permission.
 */
@Singleton
class BackupFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** @throws IOException if the file cannot be written. */
    suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        // "wt" truncates, so overwriting an existing larger file leaves no old content behind.
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open $uri")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
    }

    /** @throws IOException if the file cannot be read. */
    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
        stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
