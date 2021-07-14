package com.byteowls.capacitor.filesharer;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@CapacitorPlugin(name = "FileSharer")
public class FileSharerPlugin extends Plugin {

    private static final String CAP_FILESHARER_TEMP = "capfilesharer";

    private static final String PARAM_FILENAME = "filename";
    private static final String PARAM_CONTENT_TYPE = "contentType";
    private static final String PARAM_BASE64_DATA = "base64Data";
    private static final String PARAM_ANDROID_CHOOSER = "android.titlechooser";

    private static final String ERR_PARAM_NO_FILENAME = "ERR_PARAM_NO_FILENAME";
    private static final String ERR_PARAM_NO_CONTENT_TYPE = "ERR_PARAM_NO_CONTENT_TYPE";
    private static final String ERR_PARAM_NO_DATA = "ERR_PARAM_NO_DATA";
    private static final String ERR_FILE_CACHING_FAILED = "ERR_FILE_CACHING_FAILED";
    private static final String ERR_PARAM_DATA_INVALID = "ERR_PARAM_DATA_INVALID";

    public FileSharerPlugin() {}

    @SuppressWarnings("Duplicates")
    @PluginMethod()
    public void share(final PluginCall call) {
        String fileProviderName = getContext().getPackageName() + ".filesharer.fileprovider";

        String filename = ConfigUtils.getCallParam(String.class, call, PARAM_FILENAME);
        if (filename == null || filename.length() == 0) {
            call.reject(ERR_PARAM_NO_FILENAME);
            return;
        }

        String contentType = ConfigUtils.getCallParam(String.class, call, PARAM_CONTENT_TYPE);
        if (contentType == null || contentType.length() == 0) {
            call.reject(ERR_PARAM_NO_CONTENT_TYPE);
            return;
        }

        String base64Data = ConfigUtils.getCallParam(String.class, call, PARAM_BASE64_DATA);
        if (base64Data == null || base64Data.length() == 0) {
            call.reject(ERR_PARAM_NO_DATA);
            return;
        }

        String chooserTitle = ConfigUtils.getCallParam(String.class, call, PARAM_ANDROID_CHOOSER);
        saveCall(call);

        // save cachedFile to cache dir
        File cachedFile = new File(getCacheDir(), filename);
        try (FileOutputStream fos = new FileOutputStream(cachedFile)) {
            byte[] decodedData = Base64.decode(base64Data, Base64.DEFAULT);
            fos.write(decodedData);
            fos.flush();
        } catch (IOException e) {
            Log.e(getLogTag(), e.getMessage());
            call.reject(ERR_FILE_CACHING_FAILED);
            return;
        } catch (IllegalArgumentException e) {
            call.reject(ERR_PARAM_DATA_INVALID);
            return;
        }

        // create a send intent

        final Intent sendIntent = new Intent(Intent.ACTION_SEND);

        Uri contentUri = FileSharerProvider.getUriForFile(getContext(), fileProviderName, cachedFile);
        sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        sendIntent.setTypeAndNormalize(contentType);
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(call, Intent.createChooser(sendIntent, chooserTitle), "callbackComplete");
    }

    private File getCacheDir() {
        File cacheDir = new File(getContext().getFilesDir(), CAP_FILESHARER_TEMP);
        if (!cacheDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cacheDir.mkdirs();
        } else {
            for (File f : cacheDir.listFiles()) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
        return cacheDir;
    }

    @ActivityCallback
    private void callbackComplete(PluginCall call, Instrumentation.ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_CANCELED) {
            call.reject("Activity canceled");
        } else {
            call = getSavedCall();
            call.resolve();
        }
    }
}
