package com.byteowls.capacitor.filesharer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CapacitorPlugin(name = "FileSharer")
public class FileSharerPlugin extends Plugin {

    private static final String CAP_FILESHARER_TEMP = "capfilesharer";

    private static final String PARAM_FILENAME = "filename";
    private static final String PARAM_CONTENT_TYPE = "contentType";
    private static final String PARAM_BASE64_DATA = "base64Data";
    private static final String PARAM_LOCAL_PATH = "path";
    private static final String PARAM_ANDROID_CHOOSER = "android.titlechooser";

    private static final String ERR_PARAM_NO_FILENAME = "ERR_PARAM_NO_FILENAME";
    private static final String ERR_PARAM_NO_CONTENT_TYPE = "ERR_PARAM_NO_CONTENT_TYPE";
    private static final String ERR_PARAM_NO_DATA = "ERR_PARAM_NO_DATA";
    private static final String ERR_FILE_CACHING_FAILED = "ERR_FILE_CACHING_FAILED";
    private static final String ERR_PARAM_DATA_INVALID = "ERR_PARAM_DATA_INVALID";
    private static final String ERR_LOCAL_FILE_NOT_FOUND = "ERR_LOCAL_FILE_NOT_FOUND";
    private static final String USER_CANCELLED = "USER_CANCELLED";

    private String callbackId;

    public FileSharerPlugin() {}

    @PluginMethod()
    public void share(final PluginCall call) {
        final String fileProviderName = getContext().getPackageName() + ".filesharer.fileprovider";
        this.callbackId = call.getCallbackId();

        JSObject callData = call.getData();
        String filename = ConfigUtils.getParamString(callData, PARAM_FILENAME);
        if (filename == null || filename.length() == 0) {
            call.reject(ERR_PARAM_NO_FILENAME);
            return;
        }

        String contentType = ConfigUtils.getParamString(callData, PARAM_CONTENT_TYPE);
        if (contentType == null || contentType.length() == 0) {
            call.reject(ERR_PARAM_NO_CONTENT_TYPE);
            return;
        }

        String base64Data = ConfigUtils.getParamString(callData, PARAM_BASE64_DATA);
        if (base64Data == null || base64Data.length() == 0) {
            String path = ConfigUtils.getParamString(callData, PARAM_LOCAL_PATH);
            if (path != null && path.length() > 0) {
                String[] parts = path.split("_capacitor_file_");
                if (parts[1] != null) {
                    path = parts[1];
                }
                try {
                    InputStream is = new FileInputStream(path);
                    base64Data = readFileAsBase64EncodedData(is);
                } catch (IOException e) {
                    Log.e(getLogTag(), e.getMessage());
                    call.reject(ERR_LOCAL_FILE_NOT_FOUND);
                    return;
                }
            } else {
                call.reject(ERR_PARAM_NO_DATA);
                return;
            }
        }

        String chooserTitle = ConfigUtils.getParamString(callData, PARAM_ANDROID_CHOOSER);
        this.bridge.saveCall(call);

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
            Log.e(getLogTag(), e.getMessage());
            call.reject(ERR_PARAM_DATA_INVALID);
            return;
        }

        if (cachedFile.exists()) {
            Uri contentUri = FileSharerProvider.getUriForFile(getContext(), fileProviderName, cachedFile);

            // create a send intent
            final Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            sendIntent.setTypeAndNormalize(contentType);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            sendIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            List<LabeledIntent> intentList = new ArrayList<>();

            PackageManager packageManager = getContext().getPackageManager();
            // find activities that can handle the file shared
            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(sendIntent, 0);
            for (ResolveInfo resolveInfo : resolveInfos) {
                String packageName = resolveInfo.activityInfo.packageName;
                final Intent intent = new Intent();
                intent.setComponent(new ComponentName(packageName, resolveInfo.activityInfo.name));
                intent.setPackage(packageName);
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                intent.setTypeAndNormalize(contentType);
                intentList.add(new LabeledIntent(
                    intent, packageName, resolveInfo.loadLabel(packageManager), resolveInfo.getIconResource())
                );
            }
            Intent chooser = Intent.createChooser(sendIntent, chooserTitle);
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toArray(new LabeledIntent[0]));

            startActivityForResult(call, chooser, "callbackComplete");
        } else {
            Log.e(getLogTag(), "Cached file does not exist!");
            call.reject(ERR_FILE_CACHING_FAILED);
        }
    }

    public String readFileAsBase64EncodedData(InputStream is) throws IOException {
        FileInputStream fileInputStreamReader = (FileInputStream) is;
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];

        int c;
        while ((c = fileInputStreamReader.read(buffer)) != -1) {
            byteStream.write(buffer, 0, c);
        }
        fileInputStreamReader.close();

        return Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP);
    }

    private File getCacheDir() {
        File cacheDir = new File(getContext().getFilesDir(), CAP_FILESHARER_TEMP);
        if (!cacheDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cacheDir.mkdirs();
        } else {
            if (cacheDir.listFiles() != null) {
                for (File f : Objects.requireNonNull(cacheDir.listFiles())) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
        }
        return cacheDir;
    }

    @ActivityCallback
    private void callbackComplete(PluginCall call, ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_CANCELED) {
            call.reject(USER_CANCELLED);
        } else {
            call = this.bridge.getSavedCall(this.callbackId);
            call.resolve();
        }
    }
}
