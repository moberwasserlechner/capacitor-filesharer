package com.byteowls.capacitor.filesharer;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import org.json.JSONObject;

@NativePlugin(name = "FileSharer")
public class FileSharerPlugin extends Plugin {

    public FileSharerPlugin() {}

    @PluginMethod()
    public void share(final PluginCall call) {
            String appId = getCallParam(String.class,call, PARAM_APP_ID);
            String androidAppId = getCallParam(String.class, call, PARAM_ANDROID_APP_ID);
            if (androidAppId != null && !androidAppId.isEmpty()) {
                appId = androidAppId;
            }

            if (appId == null || appId.length() == 0) {
                call.reject("Option '"+PARAM_APP_ID+"' or '"+PARAM_ANDROID_APP_ID+"' is required!");
                return;
            }

            String baseUrl = getCallParam(String.class, call, PARAM_AUTHORIZATION_BASE_URL);
            if (baseUrl == null || baseUrl.length() == 0) {
                call.reject("Option '"+PARAM_AUTHORIZATION_BASE_URL+"' is required!");
                return;
            }
            String accessTokenEndpoint = getCallParam(String.class, call, PARAM_ACCESS_TOKEN_ENDPOINT); // placeholder
            if (accessTokenEndpoint == null || accessTokenEndpoint.length() == 0) {
                call.reject("Option '"+PARAM_ACCESS_TOKEN_ENDPOINT+"' is required!");
                return;
            }
            String customScheme = getCallParam(String.class, call, PARAM_ANDROID_CUSTOM_SCHEME);
            if (customScheme == null || customScheme.length() == 0) {
                call.reject("Option '"+ PARAM_ANDROID_CUSTOM_SCHEME +"' is required!");
                return;
            }

            String responseType = getCallParam(String.class, call, PARAM_RESPONSE_TYPE);
            String androidResponseType = getCallParam(String.class, call, PARAM_ANDROID_RESPONSE_TYPE);
            if (androidResponseType != null && !androidResponseType.isEmpty()) {
                responseType = androidResponseType;
            }

    }

    private <T> T getCallParam(Class<T> clazz, PluginCall call, String key) {
        return getCallParam(clazz, call, key, null);
    }

    private <T> T getCallParam(Class<T> clazz, PluginCall call, String key, T defaultValue) {
        String k = getDeepestKey(key);
        try {
            JSONObject o = getDeepestObject(call.getData(), key);

            Object value = null;
            if (clazz.isAssignableFrom(String.class)) {
                value = o.getString(k);
            } else if (clazz.isAssignableFrom(Boolean.class)) {
                value = o.getBoolean(k);
            } else if (clazz.isAssignableFrom(Double.class)) {
                value = o.getDouble(k);
            } else if (clazz.isAssignableFrom(Integer.class)) {
                value = o.getInt(k);
            } else if (clazz.isAssignableFrom(Float.class)) {
                Double doubleValue = o.getDouble(k);
                value = doubleValue.floatValue();
            } else if (clazz.isAssignableFrom(Integer.class)) {
                value = o.getInt(k);
            }
            if (value == null) {
                return defaultValue;
            }
            return (T) value;
        } catch (Exception ignore) {}
        return defaultValue;
    }

    private String getDeepestKey(String key) {
        String[] parts = key.split("\\.");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return null;
    }

    private JSObject getDeepestObject(JSObject o, String key) {
        // Split on periods
        String[] parts = key.split("\\.");
        // Search until the second to last part of the key
        for (int i = 0; i < parts.length-1; i++) {
            String k = parts[i];
            o = o.getJSObject(k);
        }
        return o;
    }

    public void discardAuthState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getContext().deleteSharedPreferences(getLogTag());
        }
    }

}
