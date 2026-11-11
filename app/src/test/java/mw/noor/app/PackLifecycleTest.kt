package mw.noor.app

import mw.noor.app.data.hasDownloadSpace
import mw.noor.app.data.packUpdateAvailable
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackLifecycleTest {
    @Test
    fun updateIsAvailableOnlyWhenCatalogueVersionIsNewer() {
        assertTrue(packUpdateAvailable(installedVersion = 1, catalogueVersion = 2))
        assertFalse(packUpdateAvailable(installedVersion = 2, catalogueVersion = 2))
        assertFalse(packUpdateAvailable(installedVersion = 3, catalogueVersion = 2))
    }

    @Test
    fun storagePreflightBlocksKnownInsufficientSpaceButAllowsUnknownCapacity() {
        assertTrue(hasDownloadSpace(availableBytes = 200_000L, requiredBytes = 100_000L))
        assertFalse(hasDownloadSpace(availableBytes = 99_999L, requiredBytes = 100_000L))
        assertTrue(hasDownloadSpace(availableBytes = null, requiredBytes = 100_000L))
    }
}
