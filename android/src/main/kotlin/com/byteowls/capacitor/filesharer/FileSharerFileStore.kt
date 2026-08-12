package com.byteowls.capacitor.filesharer

import android.util.Base64
import java.io.File

internal fun interface Base64Decoder {
    fun decode(value: String): ByteArray
}

internal class FileSharerFileStore(
    private val cacheDirectory: File,
    private val base64Decoder: Base64Decoder = Base64Decoder { Base64.decode(it, Base64.DEFAULT) },
) {
    fun cache(options: FileSharerOptions): File {
        prepareCacheDirectory()
        val cachedFile = safeCacheFile(options.filename)
        val data = loadData(options)

        try {
            cachedFile.outputStream().use { it.write(data) }
        } catch (error: Exception) {
            throw FileSharerException(FileSharerErrors.FILE_CACHING_FAILED, error)
        }

        if (!cachedFile.isFile) {
            throw FileSharerException(FileSharerErrors.FILE_CACHING_FAILED)
        }
        return cachedFile
    }

    private fun prepareCacheDirectory() {
        if (!cacheDirectory.exists() && !cacheDirectory.mkdirs()) {
            throw FileSharerException(FileSharerErrors.FILE_CACHING_FAILED)
        }
        if (!cacheDirectory.isDirectory) {
            throw FileSharerException(FileSharerErrors.FILE_CACHING_FAILED)
        }

        cacheDirectory.listFiles()?.forEach { existing ->
            if (!existing.delete()) {
                throw FileSharerException(FileSharerErrors.FILE_CACHING_FAILED)
            }
        }
    }

    private fun safeCacheFile(filename: String): File {
        try {
            val canonicalDirectory = cacheDirectory.canonicalFile
            val candidate = File(canonicalDirectory, filename).canonicalFile
            if (candidate.parentFile != canonicalDirectory) {
                throw FileSharerException(FileSharerErrors.FILE_CACHING_FAILED)
            }
            return candidate
        } catch (error: FileSharerException) {
            throw error
        } catch (error: Exception) {
            throw FileSharerException(FileSharerErrors.FILE_CACHING_FAILED, error)
        }
    }

    private fun loadData(options: FileSharerOptions): ByteArray {
        options.base64Data?.let { encoded ->
            try {
                return base64Decoder.decode(encoded)
            } catch (error: IllegalArgumentException) {
                throw FileSharerException(FileSharerErrors.DATA_INVALID, error)
            }
        }

        val path = requireNotNull(options.path)
        val resolvedPath = CAPACITOR_FILE_URL.matchEntire(path)?.groupValues?.get(1) ?: path
        try {
            return File(resolvedPath).readBytes()
        } catch (error: Exception) {
            throw FileSharerException(FileSharerErrors.LOCAL_FILE_NOT_FOUND, error)
        }
    }

    private companion object {
        val CAPACITOR_FILE_URL = Regex(
            "^[A-Za-z][A-Za-z0-9+.-]*://[^/]+/_capacitor_file_(.*)$",
        )
    }
}
