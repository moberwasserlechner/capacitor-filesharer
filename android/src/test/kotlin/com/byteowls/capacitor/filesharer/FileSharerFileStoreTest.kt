package com.byteowls.capacitor.filesharer

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FileSharerFileStoreTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `decodes base64 into the cache and removes stale files`() {
        val cacheDirectory = temporaryDirectory.resolve("cache").toFile().apply { mkdirs() }
        val staleFile = cacheDirectory.resolve("stale.txt").apply { writeText("stale") }
        val store = FileSharerFileStore(cacheDirectory, Base64Decoder { encoded ->
            assertEquals("encoded", encoded)
            byteArrayOf(1, 2, 3)
        })

        val cached = store.cache(options(base64Data = "encoded"))

        assertEquals("test.txt", cached.name)
        assertArrayEquals(byteArrayOf(1, 2, 3), cached.readBytes())
        assertFalse(staleFile.exists())
    }

    @Test
    fun `reads a local file path directly`() {
        val source = temporaryDirectory.resolve("source.txt").toFile().apply {
            writeBytes(byteArrayOf(4, 5, 6))
        }
        val store = FileSharerFileStore(temporaryDirectory.resolve("cache").toFile())

        val cached = store.cache(options(base64Data = null, path = source.absolutePath))

        assertArrayEquals(byteArrayOf(4, 5, 6), cached.readBytes())
    }

    @Test
    fun `extracts a path from a Capacitor file URL`() {
        val source = temporaryDirectory.resolve("source.txt").toFile().apply { writeText("content") }
        val store = FileSharerFileStore(temporaryDirectory.resolve("cache").toFile())

        val cached = store.cache(
            options(base64Data = null, path = "https://localhost/_capacitor_file_${source.absolutePath}"),
        )

        assertEquals("content", cached.readText())
    }

    @Test
    fun `does not alter a raw path containing the Capacitor file marker`() {
        val source = temporaryDirectory.resolve("_capacitor_file_report.txt").toFile().apply {
            writeText("content")
        }
        val store = FileSharerFileStore(temporaryDirectory.resolve("cache").toFile())

        val cached = store.cache(options(base64Data = null, path = source.absolutePath))

        assertEquals("content", cached.readText())
    }

    @Test
    fun `maps invalid base64 to the public error code`() {
        val store = FileSharerFileStore(
            temporaryDirectory.resolve("cache").toFile(),
            Base64Decoder { throw IllegalArgumentException("invalid") },
        )

        assertFailure(FileSharerErrors.DATA_INVALID) {
            store.cache(options(base64Data = "invalid"))
        }
    }

    @Test
    fun `maps a missing local file to the public error code`() {
        val store = FileSharerFileStore(temporaryDirectory.resolve("cache").toFile())

        assertFailure(FileSharerErrors.LOCAL_FILE_NOT_FOUND) {
            store.cache(options(base64Data = null, path = "/missing/file.txt"))
        }
    }

    @Test
    fun `keeps cached files inside the configured directory`() {
        val store = FileSharerFileStore(temporaryDirectory.resolve("cache").toFile())

        assertFailure(FileSharerErrors.FILE_CACHING_FAILED) {
            store.cache(options(filename = "../escaped.txt"))
        }
        assertFalse(temporaryDirectory.resolve("escaped.txt").toFile().exists())
    }

    private fun options(
        filename: String = "test.txt",
        base64Data: String? = "encoded",
        path: String? = null,
    ) = FileSharerOptions(
        filename = filename,
        contentType = "text/plain",
        base64Data = base64Data,
        path = path,
        chooserTitle = null,
    )

    private fun assertFailure(expectedCode: String, action: () -> Unit) {
        val error = assertThrows(FileSharerException::class.java, action)
        assertEquals(expectedCode, error.code)
    }
}
