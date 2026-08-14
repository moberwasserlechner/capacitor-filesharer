package com.byteowls.capacitor.filesharer

import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LabeledIntent
import androidx.core.content.FileProvider
import java.io.File
import java.net.URI

internal class FileSharer(
    private val context: Context,
    capacitorOrigin: String,
    private val fileStore: FileSharerFileStore = FileSharerFileStore(
        File(context.filesDir, CACHE_DIRECTORY_NAME),
        sourceOpener = AndroidSourceOpener(
            context.contentResolver,
            try {
                URI(capacitorOrigin)
            } catch (_: Exception) {
                null
            },
        ),
    ),
) {
    fun createChooserIntent(options: FileSharerOptions): Intent {
        val cachedFile = fileStore.cache(options)
        val authority = "${context.packageName}.filesharer.fileprovider"
        val contentUri = try {
            FileProvider.getUriForFile(context, authority, cachedFile)
        } catch (error: Exception) {
            throw FileSharerException(FileSharerErrors.FILE_CACHING_FAILED, error)
        }

        val sendIntent = createSendIntent(options.contentType, contentUri)
        val packageManager = context.packageManager
        val targetedIntents = packageManager.queryIntentActivities(sendIntent, 0).map { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val intent = createSendIntent(options.contentType, contentUri).apply {
                component = ComponentName(packageName, resolveInfo.activityInfo.name)
                setPackage(packageName)
            }
            LabeledIntent(
                intent,
                packageName,
                resolveInfo.loadLabel(packageManager),
                resolveInfo.iconResource,
            )
        }

        return Intent.createChooser(sendIntent, options.chooserTitle).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntents.toTypedArray())
        }
    }

    private fun createSendIntent(contentType: String, contentUri: android.net.Uri): Intent =
        Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, contentUri)
            clipData = ClipData.newRawUri("shared file", contentUri)
            setTypeAndNormalize(contentType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private companion object {
        const val CACHE_DIRECTORY_NAME = "capfilesharer"
    }
}
