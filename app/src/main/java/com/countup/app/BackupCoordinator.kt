package com.countup.app

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Result of reading and validating a CountUp JSON backup stream.
 */
sealed interface BackupImportResult {
    data class Valid(val payload: CountUpBackupPayload) : BackupImportResult
    data class Damaged(val salvagedPayload: CountUpBackupPayload) : BackupImportResult
    data class UnsupportedSchema(val detectedVersion: Int) : BackupImportResult
    data object FileTooLarge : BackupImportResult
    data object InvalidOrEmpty : BackupImportResult
    data object Corrupted : BackupImportResult
    data object Failed : BackupImportResult
}

/**
 * Domain coordinator for backup JSON streaming, validation, and restoration strategy resolution.
 * Pure Kotlin without Android framework dependencies for seamless testability.
 */
object BackupCoordinator {

    const val MAX_BACKUP_BYTES: Int = 2 * 1024 * 1024 // 2 MB strict payload budget

    /**
     * Serializes repository data as JSON and writes to [outputStream].
     */
    fun exportToStream(outputStream: OutputStream, repository: CountUpRepository): Boolean {
        return try {
            val payload = repository.exportBackupPayload()
            val json = CountUpBackupPayload.encode(payload)
            outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Reads up to [MAX_BACKUP_BYTES] from [inputStream] and validates the JSON payload.
     */
    fun importFromStream(inputStream: InputStream): BackupImportResult {
        return try {
            val buffer = ByteArray(8192)
            val baos = ByteArrayOutputStream()
            var totalRead = 0
            var exceeded = false

            inputStream.use { stream ->
                while (true) {
                    val read = stream.read(buffer)
                    if (read == -1) break
                    totalRead += read
                    if (totalRead > MAX_BACKUP_BYTES) {
                        exceeded = true
                        break
                    }
                    baos.write(buffer, 0, read)
                }
            }

            if (exceeded) {
                return BackupImportResult.FileTooLarge
            }

            val raw = baos.toString(Charsets.UTF_8.name())
            when (val validation = CountUpBackupPayload.validate(raw)) {
                is BackupValidationResult.Valid -> {
                    if (validation.payload.items.isNotEmpty()) {
                        BackupImportResult.Valid(validation.payload)
                    } else {
                        BackupImportResult.InvalidOrEmpty
                    }
                }
                is BackupValidationResult.Damaged -> {
                    BackupImportResult.Damaged(validation.salvagedPayload)
                }
                is BackupValidationResult.UnsupportedSchema -> {
                    BackupImportResult.UnsupportedSchema(validation.detectedVersion)
                }
                BackupValidationResult.Corrupted -> {
                    BackupImportResult.Corrupted
                }
            }
        } catch (_: Exception) {
            BackupImportResult.Failed
        }
    }

    /**
     * Resolves the effective restore strategy, ensuring damaged payloads cannot execute REPLACE_ALL.
     */
    fun resolveRestoreStrategy(strategy: RestoreStrategy, isDamaged: Boolean): RestoreStrategy {
        return if (isDamaged && strategy == RestoreStrategy.REPLACE_ALL) {
            RestoreStrategy.MERGE_KEEP_EXISTING
        } else {
            strategy
        }
    }
}
