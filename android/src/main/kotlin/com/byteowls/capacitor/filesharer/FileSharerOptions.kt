package com.byteowls.capacitor.filesharer

import com.getcapacitor.JSObject

internal data class FileSharerOptions(
    val filename: String,
    val contentType: String,
    val base64Data: String?,
    val path: String?,
    val chooserTitle: String?,
) {
    companion object {
        fun from(data: JSObject): FileSharerOptions {
            val filename = data.getString("filename").takeUnless { it.isNullOrEmpty() }
                ?: throw FileSharerException(FileSharerErrors.NO_FILENAME)
            val contentType = data.getString("contentType").takeUnless { it.isNullOrEmpty() }
                ?: throw FileSharerException(FileSharerErrors.NO_CONTENT_TYPE)
            val base64Data = data.getString("base64Data").takeUnless { it.isNullOrEmpty() }
            val path = data.getString("path").takeUnless { it.isNullOrEmpty() }
            if (base64Data == null && path == null) {
                throw FileSharerException(FileSharerErrors.NO_DATA)
            }

            return FileSharerOptions(
                filename = filename,
                contentType = contentType,
                base64Data = base64Data,
                path = path,
                chooserTitle = data.getJSObject("android")?.getString("chooserTitle"),
            )
        }
    }
}

internal object FileSharerErrors {
    const val NO_FILENAME = "ERR_PARAM_NO_FILENAME"
    const val NO_CONTENT_TYPE = "ERR_PARAM_NO_CONTENT_TYPE"
    const val NO_DATA = "ERR_PARAM_NO_DATA"
    const val FILE_CACHING_FAILED = "ERR_FILE_CACHING_FAILED"
    const val DATA_INVALID = "ERR_PARAM_DATA_INVALID"
    const val PATH_INVALID = "ERR_PARAM_PATH_INVALID"
    const val LOCAL_FILE_NOT_FOUND = "ERR_LOCAL_FILE_NOT_FOUND"
    const val USER_CANCELLED = "USER_CANCELLED"
}

internal class FileSharerException(
    val code: String,
    cause: Throwable? = null,
) : Exception(code, cause)
