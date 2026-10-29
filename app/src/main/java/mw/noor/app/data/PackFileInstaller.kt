package mw.noor.app.data

import java.io.File

/** Stages a validated pack before replacing the current copy and recovers interrupted swaps. */
internal class PackFileInstaller(
    private val rename: (File, File) -> Boolean = { source, destination -> source.renameTo(destination) },
) {
    @Synchronized
    fun recover(destination: File) {
        val backup = backupFor(destination)
        when {
            !destination.exists() && backup.exists() -> rename(backup, destination)
            destination.exists() && backup.exists() -> backup.delete()
        }
    }

    @Synchronized
    fun install(staged: File, destination: File) {
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

    private fun backupFor(destination: File): File = File(destination.parentFile, "${destination.name}.bak")
}
