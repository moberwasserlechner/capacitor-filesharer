# iOS Implementation Guide (Swift Package Manager)

Complete guide for implementing Capacitor plugins in Swift using **Swift Package Manager** (the modern, recommended approach).

## Modern iOS Plugin Architecture

Capacitor plugins on iOS now use **Swift Package Manager (SPM)** instead of CocoaPods. The plugin follows a **two-class pattern**:

1. **Plugin class** - Bridges Capacitor to native code (extends `CAPPlugin` + `CAPBridgedPlugin`)
2. **Implementation class** - Contains business logic (plain Swift class)

---

## Project Structure

```
my-plugin/
├── Package.swift                    # Swift Package Manager manifest
├── ios/
│   ├── Sources/
│   │   └── MyPlugin/
│   │       ├── MyPlugin.swift            # Implementation class
│   │       └── MyPluginPlugin.swift      # Plugin bridge class
│   └── Tests/
│       └── MyPluginTests/
│           └── MyPluginTests.swift       # Unit tests
└── README.md
```

**Key differences from legacy approach:**
- ✅ Uses Package.swift (not Podspec or Xcode project)
- ✅ Sources in `ios/Sources/PluginName/`
- ✅ No Plugin.m Objective-C bridge file needed
- ✅ Two-file pattern separates concerns

---

## 1. Package.swift Configuration

### Basic Setup

```swift
// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "MyPlugin",
    platforms: [.iOS(.v15)],  // Minimum iOS 15
    products: [
        .library(
            name: "MyPlugin",
            targets: ["MyPlugin"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "6.0.0")
    ],
    targets: [
        .target(
            name: "MyPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/MyPlugin"
        ),
        .testTarget(
            name: "MyPluginTests",
            dependencies: ["MyPlugin"],
            path: "ios/Tests/MyPluginTests"
        )
    ]
)
```

### With Additional Dependencies

```swift
let package = Package(
    name: "MyPlugin",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "MyPlugin", targets: ["MyPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "6.0.0"),
        // Add external dependencies
        .package(url: "https://github.com/Alamofire/Alamofire.git", from: "5.8.0")
    ],
    targets: [
        .target(
            name: "MyPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm"),
                "Alamofire"  // External dependency
            ],
            path: "ios/Sources/MyPlugin"
        ),
        .testTarget(
            name: "MyPluginTests",
            dependencies: ["MyPlugin"],
            path: "ios/Tests/MyPluginTests"
        )
    ]
)
```

---

## 2. Plugin Bridge Class

### Basic Plugin Bridge

```swift
// ios/Sources/MyPlugin/MyPluginPlugin.swift
import Foundation
import Capacitor

@objc(MyPluginPlugin)
public class MyPluginPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "MyPluginPlugin"
    public let jsName = "MyPlugin"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "echo", returnType: CAPPluginReturnPromise),
    ]

    private let implementation = MyPlugin()

    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        let result = implementation.echo(value)
        call.resolve(["value": result])
    }
}
```

**Key components:**
- `CAPBridgedPlugin` protocol - Modern plugin interface
- `identifier` - Unique plugin identifier (class name)
- `jsName` - JavaScript-facing plugin name
- `pluginMethods` - Array defining callable methods
- `implementation` - Separate class with business logic

### Plugin with Multiple Methods

```swift
// ios/Sources/CameraPlugin/CameraPluginPlugin.swift
import Foundation
import Capacitor

@objc(CameraPluginPlugin)
public class CameraPluginPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "CameraPluginPlugin"
    public let jsName = "Camera"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getPhoto", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "checkPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissions", returnType: CAPPluginReturnPromise),
    ]

    private let implementation = CameraPlugin()

    @objc func getPhoto(_ call: CAPPluginCall) {
        let quality = call.getInt("quality") ?? 90
        let source = call.getString("source") ?? "camera"

        implementation.getPhoto(quality: quality, source: source) { result in
            switch result {
            case .success(let photoData):
                call.resolve(photoData)
            case .failure(let error):
                call.reject(error.localizedDescription)
            }
        }
    }

    @objc func checkPermissions(_ call: CAPPluginCall) {
        let status = implementation.checkPermissions()
        call.resolve(status)
    }

    @objc func requestPermissions(_ call: CAPPluginCall) {
        implementation.requestPermissions { status in
            call.resolve(status)
        }
    }
}
```

---

## 3. Implementation Class

### Basic Implementation

```swift
// ios/Sources/MyPlugin/MyPlugin.swift
import Foundation

@objc public class MyPlugin: NSObject {
    @objc public func echo(_ value: String) -> String {
        print("Echo: \(value)")
        return value
    }
}
```

**Key points:**
- Inherits from `NSObject` for Objective-C compatibility
- Methods marked `@objc public` for bridge access
- Pure Swift business logic
- No Capacitor dependencies

### Implementation with Async Operations

```swift
// ios/Sources/GeolocationPlugin/GeolocationPlugin.swift
import Foundation
import CoreLocation

@objc public class GeolocationPlugin: NSObject {

    private let locationManager = CLLocationManager()
    private var completion: (([String: Any]) -> Void)?

    @objc public func getCurrentPosition(completion: @escaping ([String: Any]) -> Void) {
        self.completion = completion

        locationManager.delegate = self
        locationManager.requestWhenInUseAuthorization()
        locationManager.requestLocation()
    }

    @objc public func checkPermissions() -> [String: String] {
        let status = locationManager.authorizationStatus
        return ["location": mapAuthStatus(status)]
    }

    private func mapAuthStatus(_ status: CLAuthorizationStatus) -> String {
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
}

extension GeolocationPlugin: CLLocationManagerDelegate {
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }

        completion?([
            "latitude": location.coordinate.latitude,
            "longitude": location.coordinate.longitude,
            "accuracy": location.horizontalAccuracy,
            "timestamp": location.timestamp.timeIntervalSince1970 * 1000
        ])
        completion = nil
    }

    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        completion?(["error": error.localizedDescription])
        completion = nil
    }
}
```

### Implementation with Result Type

```swift
// ios/Sources/CameraPlugin/CameraPlugin.swift
import Foundation
import AVFoundation

@objc public class CameraPlugin: NSObject {

    public enum CameraError: Error {
        case permissionDenied
        case cameraUnavailable
        case captureFailed

        var localizedDescription: String {
            switch self {
            case .permissionDenied: return "Camera permission denied"
            case .cameraUnavailable: return "Camera not available"
            case .captureFailed: return "Photo capture failed"
            }
        }
    }

    @objc public func getPhoto(
        quality: Int,
        source: String,
        completion: @escaping (Result<[String: Any], CameraError>) -> Void
    ) {
        // Check permission
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        guard status == .authorized else {
            completion(.failure(.permissionDenied))
            return
        }

        // Check camera availability
        guard UIImagePickerController.isSourceTypeAvailable(.camera) else {
            completion(.failure(.cameraUnavailable))
            return
        }

        // Simulate photo capture
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            let photoData: [String: Any] = [
                "base64String": "mock_base64_data",
                "format": "jpeg",
                "path": "/tmp/photo.jpg"
            ]
            completion(.success(photoData))
        }
    }

    @objc public func checkPermissions() -> [String: String] {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        return ["camera": mapPermissionStatus(status)]
    }

    @objc public func requestPermissions(completion: @escaping ([String: String]) -> Void) {
        AVCaptureDevice.requestAccess(for: .video) { granted in
            DispatchQueue.main.async {
                completion(["camera": granted ? "granted" : "denied"])
            }
        }
    }

    private func mapPermissionStatus(_ status: AVAuthorizationStatus) -> String {
        switch status {
        case .authorized: return "granted"
        case .denied, .restricted: return "denied"
        case .notDetermined: return "prompt"
        @unknown default: return "prompt"
        }
    }
}
```

---

## 4. Parameter Extraction

### From Plugin Bridge

```swift
@objc func methodName(_ call: CAPPluginCall) {
    // String
    let stringValue = call.getString("key") ?? ""
    guard let requiredString = call.getString("required") else {
        call.reject("Missing required parameter")
        return
    }

    // Int
    let intValue = call.getInt("count") ?? 0

    // Double
    let doubleValue = call.getDouble("price") ?? 0.0

    // Bool
    let boolValue = call.getBool("enabled") ?? false

    // Array
    if let items = call.getArray("items") {
        // Process array
    }

    // Object
    if let config = call.getObject("config") {
        let name = config["name"] as? String
        let age = config["age"] as? Int
    }

    // Pass to implementation
    let result = implementation.doWork(
        requiredString,
        count: intValue,
        enabled: boolValue
    )
    call.resolve(result)
}
```

---

## 5. Returning Results

### Simple Values

```swift
@objc func getValue(_ call: CAPPluginCall) {
    let value = implementation.getValue()
    call.resolve(["value": value])
}
```

### Complex Objects

```swift
@objc func getDeviceInfo(_ call: CAPPluginCall) {
    let info = implementation.getDeviceInfo()
    call.resolve([
        "model": info.model,
        "version": info.version,
        "isSimulator": info.isSimulator,
        "batteryLevel": info.batteryLevel
    ])
}
```

### Arrays

```swift
@objc func listFiles(_ call: CAPPluginCall) {
    let files = implementation.listFiles()

    let fileList = files.map { file in
        return [
            "name": file.name,
            "path": file.path,
            "size": file.size
        ]
    }

    call.resolve(["files": fileList])
}
```

---

## 6. Error Handling

### Reject Calls

```swift
@objc func riskyOperation(_ call: CAPPluginCall) {
    guard let param = call.getString("param") else {
        call.reject("INVALID_PARAMETER", "param is required")
        return
    }

    do {
        let result = try implementation.performOperation(param)
        call.resolve(["result": result])
    } catch {
        call.reject("OPERATION_FAILED", error.localizedDescription, error)
    }
}
```

### Custom Error Enum

```swift
enum PluginError: String, Error {
    case unavailable = "UNAVAILABLE"
    case permissionDenied = "PERMISSION_DENIED"
    case invalidParameter = "INVALID_PARAMETER"
    case operationFailed = "OPERATION_FAILED"

    func reject(_ call: CAPPluginCall, message: String) {
        call.reject(self.rawValue, message)
    }
}

// Usage
PluginError.permissionDenied.reject(call, message: "Camera permission not granted")
```

---

## 7. Event Listeners

### Sending Events from Plugin

```swift
// In Plugin bridge class
@objc func startMonitoring(_ call: CAPPluginCall) {
    implementation.startMonitoring { [weak self] data in
        self?.notifyListeners("dataUpdate", data: data)
    }
    call.resolve()
}

@objc func stopMonitoring(_ call: CAPPluginCall) {
    implementation.stopMonitoring()
    call.resolve()
}
```

### Implementation with Events

```swift
// ios/Sources/SensorPlugin/SensorPlugin.swift
import Foundation
import CoreMotion

@objc public class SensorPlugin: NSObject {
    private let motionManager = CMMotionManager()
    private var dataCallback: (([String: Any]) -> Void)?

    @objc public func startMonitoring(callback: @escaping ([String: Any]) -> Void) {
        self.dataCallback = callback

        guard motionManager.isAccelerometerAvailable else { return }

        motionManager.accelerometerUpdateInterval = 0.1
        motionManager.startAccelerometerUpdates(to: .main) { [weak self] data, error in
            guard let data = data else { return }

            self?.dataCallback?([
                "x": data.acceleration.x,
                "y": data.acceleration.y,
                "z": data.acceleration.z,
                "timestamp": Date().timeIntervalSince1970 * 1000
            ])
        }
    }

    @objc public func stopMonitoring() {
        motionManager.stopAccelerometerUpdates()
        dataCallback = nil
    }
}
```

---

## 8. UI Operations

### Present View Controller

```swift
@objc func openCamera(_ call: CAPPluginCall) {
    DispatchQueue.main.async {
        guard let viewController = self.bridge?.viewController else {
            call.reject("View controller not available")
            return
        }

        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = self

        // Save call for later
        self.bridge?.saveCall(call)

        viewController.present(picker, animated: true)
    }
}
```

### Show Alert

```swift
@objc func showConfirm(_ call: CAPPluginCall) {
    let title = call.getString("title") ?? "Confirm"
    let message = call.getString("message") ?? ""

    DispatchQueue.main.async {
        guard let viewController = self.bridge?.viewController else {
            call.reject("View controller not available")
            return
        }

        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)

        alert.addAction(UIAlertAction(title: "OK", style: .default) { _ in
            call.resolve(["value": true])
        })

        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel) { _ in
            call.resolve(["value": false])
        })

        viewController.present(alert, animated: true)
    }
}
```

---

## 9. Testing

### XCTest Setup

```swift
// ios/Tests/MyPluginTests/MyPluginTests.swift
import XCTest
@testable import MyPlugin

final class MyPluginTests: XCTestCase {
    var plugin: MyPlugin!

    override func setUp() {
        super.setUp()
        plugin = MyPlugin()
    }

    override func tearDown() {
        plugin = nil
        super.tearDown()
    }

    func testEcho() {
        let result = plugin.echo("Hello")
        XCTAssertEqual(result, "Hello")
    }

    func testEchoEmpty() {
        let result = plugin.echo("")
        XCTAssertEqual(result, "")
    }
}
```

### Async Testing

```swift
func testAsyncOperation() async throws {
    let expectation = XCTestExpectation(description: "Operation completes")

    plugin.asyncOperation { result in
        XCTAssertNotNil(result)
        expectation.fulfill()
    }

    await fulfillment(of: [expectation], timeout: 5.0)
}
```

---

## 10. Best Practices

### DO:
- ✅ Use Swift Package Manager (not CocoaPods)
- ✅ Separate plugin bridge from implementation
- ✅ Implement `CAPBridgedPlugin` protocol
- ✅ Mark implementation methods `@objc public`
- ✅ Run UI operations on main thread
- ✅ Use Result types for async operations
- ✅ Handle all error cases explicitly
- ✅ Write unit tests for implementation class

### DON'T:
- ❌ Use CocoaPods as primary approach
- ❌ Put business logic in Plugin bridge class
- ❌ Create Plugin.m Objective-C bridge files
- ❌ Force unwrap optionals
- ❌ Block main thread with long operations
- ❌ Log sensitive data
- ❌ Assume permissions are granted

---

## Summary

**Modern iOS Plugin Checklist**:

- [ ] Create Package.swift with Capacitor dependencies
- [ ] Create Plugin bridge class (extends CAPPlugin + CAPBridgedPlugin)
- [ ] Create Implementation class (extends NSObject)
- [ ] Define pluginMethods array with method names
- [ ] Extract parameters in bridge, pass to implementation
- [ ] Return results with call.resolve()
- [ ] Handle errors with call.reject()
- [ ] Use notifyListeners() for events
- [ ] Run UI code on main thread
- [ ] Write XCTests for implementation class

**Remember**: Swift Package Manager is the modern standard. The two-class pattern keeps your code clean and testable!

---

## Where `notifyListeners()` Is Callable

`notifyListeners(_:data:)` is an instance method on `CAPPlugin`. It is callable
from within the plugin class itself or any context where `self: CAPPlugin` is in
scope. If a separate Swift implementation class, delegate, or notification
observer needs to emit an event, dispatch through the plugin via a closure or a
`weak` reference rather than passing the plugin object around.

Background and lifecycle contexts (APNs forwarding from `AppDelegate`, push
receipt handlers, observer methods on system frameworks, deep-link handlers)
should reach the plugin through a static accessor on the plugin class. The
plugin may not be loaded when the event arrives; the background class must not
assume a live plugin reference. This mirrors the Android pattern in
`android-implementation.md`.

## Opening App Settings After Permanent Denial

Once a user has denied a permission and chosen "Don't Ask Again", iOS will
never re-prompt. The plugin can only deep-link to the system settings page so
the user can change the choice manually.

```swift
@objc func openSettings(_ call: CAPPluginCall) {
    guard let url = URL(string: UIApplication.openSettingsURLString) else {
        call.reject("Cannot construct settings URL")
        return
    }
    DispatchQueue.main.async {
        UIApplication.shared.open(url) { success in
            call.resolve(["opened": success])
        }
    }
}
```

Expose this as `openSettings()` on the plugin contract whenever the API has a
permission flow. The user-facing prompt for "permission denied" should offer
this as a recovery path.

## SDK Adapter Pattern

When the official plugin (or the generation contract) declares a native SDK
dependency — for example `IONCameraLib`, Stripe, Firebase, ML Kit, Auth0,
RevenueCat — the bridge class is a thin adapter, not an implementation:

- Parse `CAPPluginCall` options into the SDK's input types.
- Call the SDK's async / completion API.
- Map the SDK's result types back into JSON for `call.resolve(...)`.
- Forward SDK errors through the standard `PluginError` enum.

```swift
import Capacitor
import IONCameraLib   // or whichever SDK the official wraps

@objc(ExamplePlugin)
public class ExamplePlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "ExamplePlugin"
    public let jsName = "Example"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "takePhoto", returnType: CAPPluginReturnPromise)
    ]

    @objc func takePhoto(_ call: CAPPluginCall) {
        let options = parseTakePhotoOptions(call)
        IONCameraLib.takePhoto(options) { [weak self] result in
            switch result {
            case .success(let photo):
                call.resolve(self?.encode(photo) ?? [:])
            case .failure(let error):
                PluginError.operationFailed.reject(call, message: error.localizedDescription)
            }
        }
    }
}
```

The implementation file collapses to option / result mappers; the SDK owns
the platform logic. Declare the SDK as a `Package.swift` dependency *and* a
`.podspec` `s.dependency` line so consumers transitively install it through
whichever distribution channel they use.
