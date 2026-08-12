package com.byteowls.capacitor.filesharer

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.InputStream
import java.net.URI

internal fun interface Base64Decoder {
    fun decode(value: String): ByteArray
}

internal fun interface SourceOpener {
    fun open(value: String): InputStream
}

internal class AndroidSourceOpener(
    private val contentResolver: ContentResolver?,
    private val capacitorOrigin: URI?,
) : SourceOpener {
    override fun open(value: String): InputStream {
        if (!URI_SCHEME.containsMatchIn(value)) {
            val file = File(value)
            if (!file.isAbsolute) {
                throw FileSharerException(FileSharerErrors.PATH_INVALID)
            }
            return try {
                file.inputStream()
            } catch (error: Exception) {
                throw FileSharerException(FileSharerErrors.LOCAL_FILE_NOT_FOUND, error)
            }
        }

        val uri = parseUri(value)
        return try {
            when {
                uri.scheme.equals("file", ignoreCase = true) -> openFileUri(uri)
                uri.scheme.equals("content", ignoreCase = true) -> openContentUri(value)
                isCapacitorFileUrl(uri) -> File(capacitorPath(uri)).inputStream()
                else -> throw FileSharerException(FileSharerErrors.PATH_INVALID)
            }
        } catch (error: FileSharerException) {
            throw error
        } catch (error: Exception) {
            throw FileSharerException(FileSharerErrors.LOCAL_FILE_NOT_FOUND, error)
        }
    }

    private fun parseUri(value: String): URI = try {
        URI(value)
    } catch (error: Exception) {
        throw FileSharerException(FileSharerErrors.PATH_INVALID, error)
    }

    private fun openFileUri(uri: URI): InputStream {
        if (uri.host != null || uri.rawAuthority?.isNotEmpty() == true) {
            throw FileSharerException(FileSharerErrors.PATH_INVALID)
        }
        return File(uri).inputStream()
    }

    private fun openContentUri(value: String): InputStream {
        val resolver = contentResolver
            ?: throw FileSharerException(FileSharerErrors.PATH_INVALID)
        return resolver.openInputStream(Uri.parse(value))
            ?: throw FileSharerException(FileSharerErrors.LOCAL_FILE_NOT_FOUND)
    }

    private fun isCapacitorFileUrl(uri: URI): Boolean {
        val origin = capacitorOrigin ?: return false
        return uri.rawUserInfo == null &&
            uri.rawQuery == null &&
            uri.rawFragment == null &&
            uri.scheme.equals(origin.scheme, ignoreCase = true) &&
            uri.host.equals(origin.host, ignoreCase = true) &&
            effectivePort(uri) == effectivePort(origin) &&
            uri.path.startsWith(CAPACITOR_FILE_PREFIX)
    }

    private fun capacitorPath(uri: URI): String =
        uri.path.removePrefix(CAPACITOR_FILE_PREFIX).takeIf { it.startsWith('/') }
            ?: throw FileSharerException(FileSharerErrors.PATH_INVALID)

    private fun effectivePort(uri: URI): Int = when {
        uri.port != -1 -> uri.port
        uri.scheme.equals("http", ignoreCase = true) -> 80
        uri.scheme.equals("https", ignoreCase = true) -> 443
        else -> -1
    }

    private companion object {
        const val CAPACITOR_FILE_PREFIX = "/_capacitor_file_"
        val URI_SCHEME = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
    }
}

internal class FileSharerFileStore(
    private val cacheDirectory: File,
    private val base64Decoder: Base64Decoder = Base64Decoder { Base64.decode(it, Base64.DEFAULT) },
    private val sourceOpener: SourceOpener = AndroidSourceOpener(null, null),
) {
    fun cache(options: FileSharerOptions): File {
        val source = if (options.base64Data == null) {
            sourceOpener.open(requireNotNull(options.path))
        } else {
            null
        }

        try {
            prepareCacheDirectory()
            val cachedFile = safeCacheFile(options.filename)

            options.base64Data?.let { encoded ->
                val data = try {
                    base64Decoder.decode(encoded)
                } catch (error: IllegalArgumentException) {
                    throw FileSharerException(FileSharerErrors.DATA_INVALID, error)
                }
                writeBytes(cachedFile, data)
            } ?: copySource(cachedFile, requireNotNull(source))

            if (!cachedFile.isFile) {
                throw FileSharerException(FileSharerErrors.FILE_CACHING_FAILED)
            }
            return cachedFile
        } finally {
            // Closing a source must not replace a stable public error from caching.
            runCatching { source?.close() }
        }
    }

    private fun writeBytes(destination: File, data: ByteArray) {
        try {
            destination.outputStream().use { it.write(data) }
        } catch (error: Exception) {
            throw FileSharerException(FileSharerErrors.FILE_CACHING_FAILED, error)
        }
    }

    private fun copySource(destination: File, source: InputStream) {
        try {
            destination.outputStream().buffered().use { destinationStream ->
                source.copyTo(destinationStream, STREAM_BUFFER_SIZE)
            }
        } catch (error: Exception) {
            throw FileSharerException(FileSharerErrors.FILE_CACHING_FAILED, error)
        }
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

    private companion object {
        const val STREAM_BUFFER_SIZE = 16 * 1024
    }
}
