package com.byteowls.capacitor.filesharer

import android.app.Activity
import androidx.activity.result.ActivityResult
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "FileSharer")
class FileSharerPlugin : Plugin() {
    private lateinit var implementation: FileSharer

    override fun load() {
        super.load()
        implementation = FileSharer(context, bridge.localUrl)
    }

    @PluginMethod
    fun share(call: PluginCall) {
        try {
            val options = FileSharerOptions.from(call.data)
            val chooser = implementation.createChooserIntent(options)
            startActivityForResult(call, chooser, "callbackComplete")
        } catch (error: FileSharerException) {
            call.reject(error.code)
        }
    }

    @ActivityCallback
    private fun callbackComplete(call: PluginCall, result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_CANCELED) {
            call.reject(FileSharerErrors.USER_CANCELLED)
        } else {
            call.resolve()
        }
    }
}
