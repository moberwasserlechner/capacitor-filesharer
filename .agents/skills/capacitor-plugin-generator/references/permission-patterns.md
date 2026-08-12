# Permission Patterns

Complete guide for handling runtime permissions across iOS, Android, and web platforms in Capacitor plugins.

## Permission API Design

### Standard Permission Interface

```typescript
// src/definitions.ts
export interface MyPluginPlugin {
  /**
   * Check current permission status without prompting user
   */
  checkPermissions(): Promise<PermissionStatus>;

  /**
   * Request permissions from user (shows system dialog)
   */
  requestPermissions(): Promise<PermissionStatus>;
}

export interface PermissionStatus {
  /**
   * Permission state for each requested permission
   * - 'granted': User has granted permission
   * - 'denied': User has denied permission
   * - 'prompt': User has not been asked yet (or 'notDetermined' on iOS)
   */
  [permission: string]: PermissionState;
}

export type PermissionState = 'granted' | 'denied' | 'prompt';
```

---

## iOS Permission Implementation

### Checking Permissions

```swift
import AVFoundation
import Photos
import CoreLocation

@objc func checkPermissions(_ call: CAPPluginCall) {
    var permissions = [String: String]()

    // Camera
    let cameraStatus = AVCaptureDevice.authorizationStatus(for: .video)
    permissions["camera"] = getPermissionState(cameraStatus)

    // Photo Library
    if #available(iOS 14, *) {
        let photoStatus = PHPhotoLibrary.authorizationStatus(for: .readWrite)
        permissions["photos"] = getPermissionState(photoStatus)
    } else {
        let photoStatus = PHPhotoLibrary.authorizationStatus()
        permissions["photos"] = getPermissionState(photoStatus)
    }

    // Location
    let locationStatus = CLLocationManager.authorizationStatus()
    permissions["location"] = getLocationPermissionState(locationStatus)

    call.resolve(permissions)
}

private func getPermissionState(_ status: AVAuthorizationStatus) -> String {
    switch status {
    case .authorized:
        return "granted"
    case .denied, .restricted:
        return "denied"
    case .notDetermined:
        return "prompt"
    @unknown default:
        return "prompt"
    }
}

private func getPermissionState(_ status: PHAuthorizationStatus) -> String {
    switch status {
    case .authorized, .limited:
        return "granted"
    case .denied, .restricted:
        return "denied"
    case .notDetermined:
        return "prompt"
    @unknown default:
        return "prompt"
    }
}

private func getLocationPermissionState(_ status: CLAuthorizationStatus) -> String {
    switch status {
    case .authorizedAlways, .authorizedWhenInUse:
        return "granted"
    case .denied, .restricted:
        return "denied"
    case .notDetermined:
        return "prompt"
    @unknown default:
        return "prompt"
    }
}
```

### Requesting Permissions

```swift
@objc func requestPermissions(_ call: CAPPluginCall) {
    var results = [String: String]()
    let group = DispatchGroup()

    // Request camera permission
    group.enter()
    AVCaptureDevice.requestAccess(for: .video) { granted in
        results["camera"] = granted ? "granted" : "denied"
        group.leave()
    }

    // Request photo library permission
    group.enter()
    if #available(iOS 14, *) {
        PHPhotoLibrary.requestAuthorization(for: .readWrite) { status in
            results["photos"] = self.getPermissionState(status)
            group.leave()
        }
    } else {
        PHPhotoLibrary.requestAuthorization { status in
            results["photos"] = self.getPermissionState(status)
            group.leave()
        }
    }

    // Wait for all permissions
    group.notify(queue: .main) {
        call.resolve(results)
    }
}
```

### Location Permission (Special Case)

```swift
import CoreLocation

class MyPlugin: CAPPlugin, CLLocationManagerDelegate {
    private var locationManager: CLLocationManager?
    private var permissionCall: CAPPluginCall?

    @objc func requestPermissions(_ call: CAPPluginCall) {
        let locationType = call.getString("location") ?? "whenInUse"

        locationManager = CLLocationManager()
        locationManager?.delegate = self
        permissionCall = call

        if locationType == "always" {
            locationManager?.requestAlwaysAuthorization()
        } else {
            locationManager?.requestWhenInUseAuthorization()
        }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        guard let call = permissionCall else { return }
        permissionCall = nil

        let status = manager.authorizationStatus
        call.resolve([
            "location": getLocationPermissionState(status)
        ])
    }
}
```

### Info.plist Configuration

```xml
<!-- ios/Plugin/Info.plist -->

<!-- Camera -->
<key>NSCameraUsageDescription</key>
<string>This app needs camera access to take photos</string>

<!-- Photo Library -->
<key>NSPhotoLibraryUsageDescription</key>
<string>This app needs to access your photos</string>
<key>NSPhotoLibraryAddUsageDescription</key>
<string>This app needs to save photos to your library</string>

<!-- Location - When In Use -->
<key>NSLocationWhenInUseUsageDescription</key>
<string>This app needs your location when in use</string>

<!-- Location - Always -->
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>This app needs your location always</string>
<key>NSLocationAlwaysUsageDescription</key>
<string>This app needs your location always</string>

<!-- Microphone -->
<key>NSMicrophoneUsageDescription</key>
<string>This app needs microphone access to record audio</string>

<!-- Contacts -->
<key>NSContactsUsageDescription</key>
<string>This app needs access to your contacts</string>

<!-- Calendar -->
<key>NSCalendarsUsageDescription</key>
<string>This app needs access to your calendar</string>

<!-- Reminders -->
<key>NSRemindersUsageDescription</key>
<string>This app needs access to your reminders</string>

<!-- Motion & Fitness -->
<key>NSMotionUsageDescription</key>
<string>This app needs access to motion sensors</string>

<!-- Bluetooth -->
<key>NSBluetoothAlwaysUsageDescription</key>
<string>This app needs Bluetooth access</string>
<key>NSBluetoothPeripheralUsageDescription</key>
<string>This app needs Bluetooth peripheral access</string>

<!-- Face ID -->
<key>NSFaceIDUsageDescription</key>
<string>This app uses Face ID for authentication</string>

<!-- Speech Recognition -->
<key>NSSpeechRecognitionUsageDescription</key>
<string>This app needs speech recognition access</string>

<!-- Media Library -->
<key>NSAppleMusicUsageDescription</key>
<string>This app needs access to your media library</string>
```

---

## Android Permission Implementation

### Checking Permissions

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@PluginMethod
fun checkPermissions(call: PluginCall) {
    val permissions = JSObject()

    // Camera
    val cameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    )
    permissions.put("camera", getPermissionState(cameraPermission))

    // Location (Fine)
    val locationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    permissions.put("location", getPermissionState(locationPermission))

    // Storage (Read)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13+ uses granular media permissions
        val photosPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES
        )
        permissions.put("photos", getPermissionState(photosPermission))
    } else {
        val storagePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        permissions.put("photos", getPermissionState(storagePermission))
    }

    // Microphone
    val micPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    )
    permissions.put("microphone", getPermissionState(micPermission))

    call.resolve(permissions)
}

private fun getPermissionState(permission: Int): String {
    return when (permission) {
        PackageManager.PERMISSION_GRANTED -> "granted"
        PackageManager.PERMISSION_DENIED -> {
            // Check if we should show rationale (user previously denied)
            // For simplicity, return "denied"
            "denied"
        }
        else -> "prompt"
    }
}
```

### Requesting Permissions

```kotlin
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class MyPlugin : Plugin() {
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var savedCall: PluginCall? = null

    override fun load() {
        super.load()

        // Register permission launcher
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val call = savedCall ?: return@registerForActivityResult
            savedCall = null

            val results = JSObject()
            permissions.forEach { (permission, granted) ->
                val key = mapPermissionToKey(permission)
                results.put(key, if (granted) "granted" else "denied")
            }

            call.resolve(results)
        }
    }

    @PluginMethod
    fun requestPermissions(call: PluginCall) {
        savedCall = call

        val permissionsToRequest = mutableListOf<String>()

        // Build list of permissions to request
        permissionsToRequest.add(Manifest.permission.CAMERA)
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)

        // Request all permissions
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private fun mapPermissionToKey(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "camera"
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> "location"
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES -> "photos"
            Manifest.permission.RECORD_AUDIO -> "microphone"
            else -> permission
        }
    }
}
```

### Android 13+ Media Permissions

```kotlin
@PluginMethod
fun requestPhotoPermission(call: PluginCall) {
    savedCall = call

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13+: Granular media permissions
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        // Pre-Android 13: Storage permission
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    permissionLauncher.launch(arrayOf(permission))
}
```

### AndroidManifest.xml Configuration

```xml
<!-- android/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.company.plugin">

    <!-- Camera -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />

    <!-- Location -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-feature android:name="android.hardware.location.gps" android:required="false" />

    <!-- Storage/Photos -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
                     android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
                     android:maxSdkVersion="28" />

    <!-- Android 13+ Media permissions -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

    <!-- Microphone -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <!-- Bluetooth -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

    <!-- Contacts -->
    <uses-permission android:name="android.permission.READ_CONTACTS" />
    <uses-permission android:name="android.permission.WRITE_CONTACTS" />

    <!-- Calendar -->
    <uses-permission android:name="android.permission.READ_CALENDAR" />
    <uses-permission android:name="android.permission.WRITE_CALENDAR" />

    <!-- Phone -->
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.CALL_PHONE" />

    <!-- SMS -->
    <uses-permission android:name="android.permission.READ_SMS" />
    <uses-permission android:name="android.permission.SEND_SMS" />

    <!-- Network -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Vibration -->
    <uses-permission android:name="android.permission.VIBRATE" />

</manifest>
```

---

## Web Permission Implementation

### Browser Permission APIs

```typescript
// src/web.ts
import { WebPlugin } from '@capacitor/core';
import type { MyPluginPlugin, PermissionStatus } from './definitions';

export class MyPluginWeb extends WebPlugin implements MyPluginPlugin {
  async checkPermissions(): Promise<PermissionStatus> {
    const permissions: PermissionStatus = {};

    // Camera - use Permissions API if available
    if ('permissions' in navigator) {
      try {
        const cameraResult = await navigator.permissions.query({ name: 'camera' as PermissionName });
        permissions.camera = this.mapWebPermissionState(cameraResult.state);
      } catch {
        permissions.camera = 'prompt';
      }

      try {
        const micResult = await navigator.permissions.query({ name: 'microphone' as PermissionName });
        permissions.microphone = this.mapWebPermissionState(micResult.state);
      } catch {
        permissions.microphone = 'prompt';
      }

      try {
        const geoResult = await navigator.permissions.query({ name: 'geolocation' as PermissionName });
        permissions.location = this.mapWebPermissionState(geoResult.state);
      } catch {
        permissions.location = 'prompt';
      }
    } else {
      // Permissions API not available
      permissions.camera = 'prompt';
      permissions.microphone = 'prompt';
      permissions.location = 'prompt';
    }

    return permissions;
  }

  async requestPermissions(): Promise<PermissionStatus> {
    // On web, permissions are requested implicitly when using the API
    // We can't show permission dialogs directly

    const permissions: PermissionStatus = {};

    // Try to access camera
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true });
      stream.getTracks().forEach(track => track.stop());
      permissions.camera = 'granted';
    } catch {
      permissions.camera = 'denied';
    }

    // Try to access microphone
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      stream.getTracks().forEach(track => track.stop());
      permissions.microphone = 'granted';
    } catch {
      permissions.microphone = 'denied';
    }

    // Try to access geolocation
    try {
      await new Promise<void>((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(() => resolve(), () => reject());
      });
      permissions.location = 'granted';
    } catch {
      permissions.location = 'denied';
    }

    return permissions;
  }

  private mapWebPermissionState(state: PermissionState): 'granted' | 'denied' | 'prompt' {
    switch (state) {
      case 'granted':
        return 'granted';
      case 'denied':
        return 'denied';
      case 'prompt':
      default:
        return 'prompt';
    }
  }
}
```

---

## Permission Flow Patterns

### Pattern 1: Check Before Use

```typescript
// In app code
async function takePhoto() {
  // 1. Check permission first
  const status = await Camera.checkPermissions();

  if (status.camera === 'granted') {
    // 2. Permission already granted - proceed
    return await Camera.getPhoto();
  } else if (status.camera === 'prompt') {
    // 3. Permission not determined - request it
    const result = await Camera.requestPermissions();

    if (result.camera === 'granted') {
      return await Camera.getPhoto();
    } else {
      throw new Error('Camera permission denied');
    }
  } else {
    // 4. Permission denied - show settings prompt
    showSettingsPrompt();
    throw new Error('Camera permission denied');
  }
}
```

### Pattern 2: Request Just-In-Time

```typescript
async function accessLocation() {
  try {
    // Try to use feature directly
    return await Geolocation.getCurrentPosition();
  } catch (error) {
    if (error.code === 'PERMISSION_DENIED') {
      // Request permission
      const result = await Geolocation.requestPermissions();

      if (result.location === 'granted') {
        // Retry
        return await Geolocation.getCurrentPosition();
      } else {
        throw new Error('Location permission required');
      }
    }
    throw error;
  }
}
```

### Pattern 3: Deferred Permission Request

```swift
// iOS - Check first, request later
@objc func takePhoto(_ call: CAPPluginCall) {
    let status = AVCaptureDevice.authorizationStatus(for: .video)

    switch status {
    case .authorized:
        // Already granted
        openCamera(call)

    case .notDetermined:
        // Not asked yet - request now
        AVCaptureDevice.requestAccess(for: .video) { granted in
            DispatchQueue.main.async {
                if granted {
                    self.openCamera(call)
                } else {
                    call.reject("PERMISSION_DENIED", "Camera permission denied")
                }
            }
        }

    case .denied, .restricted:
        // Previously denied - prompt to settings
        call.reject(
            "PERMISSION_DENIED",
            "Camera permission denied. Please enable in Settings.",
            nil,
            ["openSettings": true]
        )

    @unknown default:
        call.reject("PERMISSION_ERROR", "Unknown permission status")
    }
}
```

---

## Opening System Settings

### iOS - Open Settings

```swift
@objc func openSettings(_ call: CAPPluginCall) {
    guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else {
        call.reject("Cannot open settings")
        return
    }

    DispatchQueue.main.async {
        if UIApplication.shared.canOpenURL(settingsUrl) {
            UIApplication.shared.open(settingsUrl) { success in
                call.resolve(["opened": success])
            }
        } else {
            call.reject("Cannot open settings")
        }
    }
}
```

### Android - Open Settings

```kotlin
@PluginMethod
fun openSettings(call: PluginCall) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
        call.resolve(JSObject().put("opened", true))
    } catch (e: Exception) {
        call.reject("Cannot open settings", e)
    }
}
```

---

## Best Practices

### DO:
- ✅ Always provide clear rationale before requesting permissions
- ✅ Request permissions just-in-time (when feature is needed)
- ✅ Check permissions before every use
- ✅ Handle all permission states (granted, denied, prompt)
- ✅ Provide fallback when permissions denied
- ✅ Guide users to settings when appropriate
- ✅ Test permission flows on real devices

### DON'T:
- ❌ Request all permissions at app launch
- ❌ Assume permissions remain granted
- ❌ Show permission dialog without context
- ❌ Ignore "denied" state
- ❌ Request unnecessary permissions
- ❌ Forget to update Info.plist / AndroidManifest.xml
- ❌ Test only on simulators/emulators

---

## Summary

**Permission Implementation Checklist**:

- [ ] Define `checkPermissions()` and `requestPermissions()` methods
- [ ] Implement iOS permission checking and requesting
- [ ] Add Info.plist usage descriptions for iOS
- [ ] Implement Android permission checking and requesting
- [ ] Declare permissions in AndroidManifest.xml
- [ ] Implement web permission handling (where applicable)
- [ ] Handle all permission states (granted/denied/prompt)
- [ ] Provide way to open system settings
- [ ] Test permission flows on real devices
- [ ] Document required permissions in README

**Remember**: Permissions are critical for user trust. Always be transparent and respectful!
