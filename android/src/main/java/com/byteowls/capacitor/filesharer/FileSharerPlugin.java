package com.byteowls.capacitor.filesharer;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import android.webkit.MimeTypeMap;
import android.os.Build;
import android.app.PendingIntent;



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

    @PluginMethod()
    public void shareMultiple(PluginCall call) {
        JSArray files = call.getArray("files");
        String dialogTitle = call.getString("dialogTitle", "Share");

        Intent intent = new Intent(files != null && files.length() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND);

        shareMultipleFiles(files, intent, call);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pi = PendingIntent.getBroadcast(getContext(), 0, new Intent(Intent.EXTRA_CHOSEN_COMPONENT), flags);
        Intent chooser = Intent.createChooser(intent, dialogTitle, pi.getIntentSender());
        chooser.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(call, chooser, "shareMultipleCallback");
    }

    @ActivityCallback
    public void shareMultipleCallback(PluginCall call, Object result) {
        call.resolve();
    }

    private void shareMultipleFiles(JSArray files, Intent intent, PluginCall call) {
        List<Object> filesList;
        ArrayList<Uri> fileUris = new ArrayList<>();
        try {
            filesList = files.toList();
            for (int i = 0; i < filesList.size(); i++) {
                String file = (String) filesList.get(i);

                String type = getMimeType(file);
                if (type == null || filesList.size() > 1) {
                    type = "*/*";
                }
                intent.setType(type);

                Uri fileUrl = FileProvider.getUriForFile(
                    getContext(),
                    getContext().getPackageName() + ".fileprovider",
                    new File(Uri.parse(file).getPath())
                );
                fileUris.add(fileUrl);
            }
            if (fileUris.size() > 1) {
                intent.putExtra(Intent.EXTRA_STREAM, fileUris);
            } else if (fileUris.size() == 1) {
                intent.putExtra(Intent.EXTRA_STREAM, fileUris.get(0));
            }
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ex) {
            call.reject(ex.getLocalizedMessage());
            return;
        }
    }
    private String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

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
