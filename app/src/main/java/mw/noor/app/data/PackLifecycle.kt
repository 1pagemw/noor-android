package mw.noor.app.data

internal fun packUpdateAvailable(installedVersion: Int, catalogueVersion: Int): Boolean =
    catalogueVersion > installedVersion

internal fun hasDownloadSpace(availableBytes: Long?, requiredBytes: Long): Boolean =
    availableBytes == null || availableBytes >= requiredBytes
