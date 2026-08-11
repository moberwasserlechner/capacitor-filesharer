package com.byteowls.capacitor.filesharer

import com.getcapacitor.JSObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FileSharerOptionsTest {
    @Test
    fun `parses base options and documented Android chooser title`() {
        val options = FileSharerOptions.from(
            JSObject(
                """
                {
                  "filename": "report.pdf",
                  "contentType": "application/pdf",
                  "base64Data": "cGRm",
                  "android": { "chooserTitle": "Send report" }
                }
                """.trimIndent(),
            ),
        )

        assertEquals("report.pdf", options.filename)
        assertEquals("application/pdf", options.contentType)
        assertEquals("cGRm", options.base64Data)
        assertNull(options.path)
        assertEquals("Send report", options.chooserTitle)
    }

    @Test
    fun `accepts a local path instead of base64 data`() {
        val options = FileSharerOptions.from(
            JSObject(
                """{"filename":"report.pdf","contentType":"application/pdf","path":"/tmp/report.pdf"}""",
            ),
        )

        assertNull(options.base64Data)
        assertEquals("/tmp/report.pdf", options.path)
    }

    @Test
    fun `rejects a missing filename`() {
        assertFailure(
            """{"contentType":"text/plain","base64Data":"dGVzdA=="}""",
            FileSharerErrors.NO_FILENAME,
        )
    }

    @Test
    fun `rejects a missing content type`() {
        assertFailure(
            """{"filename":"test.txt","base64Data":"dGVzdA=="}""",
            FileSharerErrors.NO_CONTENT_TYPE,
        )
    }

    @Test
    fun `rejects a missing data source`() {
        assertFailure(
            """{"filename":"test.txt","contentType":"text/plain"}""",
            FileSharerErrors.NO_DATA,
        )
    }

    private fun assertFailure(json: String, expectedCode: String) {
        val error = assertThrows(FileSharerException::class.java) {
            FileSharerOptions.from(JSObject(json))
        }
        assertEquals(expectedCode, error.code)
    }
}
