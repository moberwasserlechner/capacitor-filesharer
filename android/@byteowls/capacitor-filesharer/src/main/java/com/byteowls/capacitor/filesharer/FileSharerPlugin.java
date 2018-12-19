package com.byteowls.capacitor.filesharer;

import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@NativePlugin(requestCodes = { FileSharerPlugin.SEND_REQUEST_CODE }, name = "FileSharer")
public class FileSharerPlugin extends Plugin {

    static final int SEND_REQUEST_CODE = 2545;
    private static final String PARAM_FILENAME = "filename";
    private static final String PARAM_CONTENT_TYPE = "contentType";
    private static final String PARAM_BASE64_DATA = "base64Data";
    private static final String PARAM_ANDROID_CHOOSER = "android.titlechooser";
    private static final String CAP_FILESHARER_TEMP = "cap_filesharer_temp";
    private static final String FILE_PROVIDER_NAME = "com.byteowls.capacitor.filesharer.fileprovider";

    public FileSharerPlugin() {}

    @PluginMethod()
    public void share(final PluginCall call) {

        String filename = ConfigUtils.getCallParam(String.class, call, PARAM_FILENAME);
        if (filename == null || filename.length() == 0) {
            call.reject("Option '" + PARAM_FILENAME + "' is required!");
            return;
        }

        String contentType = ConfigUtils.getCallParam(String.class, call, PARAM_CONTENT_TYPE);
        if (contentType == null || contentType.length() == 0) {
            call.reject("Option '" + PARAM_CONTENT_TYPE + "' is required!");
            return;
        }

        String base64Data = ConfigUtils.getCallParam(String.class, call, PARAM_BASE64_DATA);
        if (base64Data == null || base64Data.length() == 0) {
            call.reject("Option '" + PARAM_BASE64_DATA + "' is required!");
            return;
        }

        String chooserTitle = ConfigUtils.getCallParam(String.class, call, PARAM_ANDROID_CHOOSER);

        saveCall(call);

        // save cachedFile to cache dir
        try {
            File cachedFile = File.createTempFile(filename, null, getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(cachedFile)) {
                byte[] decodedData = Base64.decode(base64Data, Base64.DEFAULT);
                fos.write(decodedData);
                fos.flush();
            }

            // create a send intent

            final Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

            Uri fileUri = FileProvider.getUriForFile(getContext(),
                FILE_PROVIDER_NAME,
                cachedFile);
            sendIntent.setDataAndType(fileUri, contentType);
//            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);

            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivityForResult(call, Intent.createChooser(sendIntent, chooserTitle), SEND_REQUEST_CODE);
//            if (sendIntent.resolveActivity(getContext().getPackageManager()) != null) {
//            } else {
//                Log.e(getLogTag(), "@byteowls/capacitor-filesharer: No app for " + sendIntent.getAction());
//                call.reject("NO_FILE_APP_FOUND");
//            }

        } catch (IOException e) {
            // Error while creating cachedFile
            call.reject("ERR_CACHED_FILE_NOT_CREATED");
        }
    }

    private File getCacheDir() {
        File cacheDir = new File(getContext().getCacheDir(), CAP_FILESHARER_TEMP);
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

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleOnActivityResult(requestCode, resultCode, data);
        if (requestCode == SEND_REQUEST_CODE) {
            PluginCall call = getSavedCall();
            call.resolve();
        }
    }

}
