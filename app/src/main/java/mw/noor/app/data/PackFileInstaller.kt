package mw.noor.app.data

import java.io.File

/** Process-wide gate: a UI repository and each WorkManager repository must serialize file mutations. */
internal object NoorFileGate {
    val lock: Any = Any()
}

/** Stages a validated pack before replacing the current copy and recovers interrupted swaps. */
internal class PackFileInstaller(
    private val rename: (File, File) -> Boolean = { source, destination -> source.renameTo(destination) },
) {
    fun recover(destination: File) {
        synchronized(NoorFileGate.lock) {
            val backup = backupFor(destination)
            when {
                !destination.exists() && backup.exists() -> rename(backup, destination)
                destination.exists() && backup.exists() -> backup.delete()
            }
        }
    }

    fun install(staged: File, destination: File) {
        synchronized(NoorFileGate.lock) {
            recover(destination)
            val backup = backupFor(destination)
            check(!backup.exists() || destination.exists()) {
                "Unable to restore the previously saved content pack. Reopen Noor and try again."
            }
            if (backup.exists()) check(backup.delete()) { "Unable to prepare a safe content update." }

            if (!destination.exists()) {
                check(rename(staged, destination)) { "Unable to save the downloaded content pack." }
                return
            }

            check(rename(destination, backup)) { "Unable to preserve the saved content pack during update." }
            if (rename(staged, destination)) {
                backup.delete()
                return
            }

            val restored = rename(backup, destination)
            check(restored) {
                "The update could not be installed and the saved pack could not be restored. Reopen Noor and try again."
            }
            error("Unable to save the downloaded content pack. Your previous pack is still available.")
        }
    }

    /** Deletes the destination and every staged/backup artifact belonging to it. */
    fun deleteAllArtifacts(destination: File) {
        synchronized(NoorFileGate.lock) {
            val parent = destination.parentFile
            listOf(
                destination,
                File(parent, destination.name + ".bak"),
                File(parent, destination.name + ".tmp"),
                File(parent, destination.name + ".migration.tmp"),
            ).forEach { file -> if (file.exists()) check(file.delete()) { "Unable to delete ${file.name}." } }
        }
    }

    private fun backupFor(destination: File): File = File(destination.parentFile, "${destination.name}.bak")
}
