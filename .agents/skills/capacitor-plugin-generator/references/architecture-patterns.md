# Plugin Architecture Patterns

This reference covers structural patterns and organization strategies for Capacitor plugins.

## Standard Plugin Structure

```
my-capacitor-plugin/
├── package.json              # NPM package configuration
├── tsconfig.json             # TypeScript compiler config
├── capacitor.config.json     # Plugin metadata
├── README.md                 # Usage documentation
├── src/
│   ├── definitions.ts        # TypeScript interface definitions
│   ├── web.ts                # Web platform implementation
│   └── index.ts              # Plugin export and registration
├── ios/
│   └── Sources/MyPlugin/
│       ├── MyPlugin.swift             # iOS native implementation
│       └── MyPluginPlugin.swift       # iOS bridge / `CAPBridgedPlugin`
├── android/
│   └── src/main/java/com/company/plugin/
│       ├── MyPlugin.java              # Android implementation
│       └── MyPluginPlugin.java        # Android bridge / `@CapacitorPlugin`
├── dist/                     # Built JavaScript (generated)
└── tests/
    ├── web.test.ts
    ├── ios/
    └── android/
```

For the generator-invocation flow that produces this layout, see
`references/scaffolding.md`.

---

## Architecture Layers

### Layer 1: TypeScript Definitions (`src/definitions.ts`)

**Purpose**: Define the contract between web and native code.

```typescript
// Pure interface - no implementation
export interface MyPluginPlugin {
  /**
   * Method description
   * @param options - Input parameters
   * @returns Promise with result data
   */
  methodName(options: MethodOptions): Promise<MethodResult>;
}

// Define all data structures
export interface MethodOptions {
  param1: string;
  param2?: number;  // Optional parameters
}

export interface MethodResult {
  success: boolean;
  data: string;
}
```

**Key principles**:
- Only interfaces, no implementation.
- Document all parameters with JSDoc and `@since`.
- Use semantic names; group related types together.
- Use string union types instead of TypeScript enums (so wire values stay
  visible).

For the full set of contract rules, see `references/api-design.md`.

### Layer 2: Web Implementation (`src/web.ts`)

**Purpose**: Provide browser-based implementation for testing and PWA support.

```typescript
import { WebPlugin } from '@capacitor/core';
import type { MyPluginPlugin, MethodOptions, MethodResult } from './definitions';

export class MyPluginWeb extends WebPlugin implements MyPluginPlugin {
  async methodName(options: MethodOptions): Promise<MethodResult> {
    // Option 1: Use web APIs if available
    if ('relevantAPI' in window) {
      const result = await (window as any).relevantAPI.call(options.param1);
      return { success: true, data: result };
    }

    // Option 2: Provide mock implementation
    console.warn('MyPlugin web implementation: returning mock data');
    return {
      success: true,
      data: `Mock result for ${options.param1}`,
    };

    // Option 3: Throw if not supported
    throw this.unavailable('This feature is not available on web');
  }
}
```

**Web implementation strategies**:

| Strategy       | When to Use                                          | Example                                          |
| ---            | ---                                                  | ---                                              |
| **Web API**    | Browser API exists                                   | Geolocation, Battery, Notifications              |
| **Polyfill**   | Behavior can be simulated with adjacent browser APIs | Storage (IndexedDB), HTTP (fetch with retries)   |
| **Mock data**  | Useful only for testing without a device             | Device info, hardware features                   |
| **Throw**      | No web equivalent exists at all                      | NFC, Bluetooth, specific sensors                 |

Mock data should be marked clearly (`console.warn(...)` on each call) so
nobody ships it. Prefer throwing `unimplemented()` over silently returning
fake values when the platform answer is "you cannot do this on web." See
`references/web-guide.md` for `unavailable()` vs `unimplemented()` and
feature-detection patterns.

### Layer 3: Native iOS Implementation

**Purpose**: Access iOS platform APIs and bridge to JavaScript.

```swift
import Foundation
import Capacitor

@objc(MyPluginPlugin)
public class MyPluginPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "MyPluginPlugin"
    public let jsName = "MyPlugin"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "methodName", returnType: CAPPluginReturnPromise)
    ]

    private let implementation = MyPlugin()

    @objc func methodName(_ call: CAPPluginCall) {
        // 1. Extract parameters
        guard let param1 = call.getString("param1") else {
            call.reject("Missing param1", "INVALID_PARAMETER")
            return
        }
        let param2 = call.getInt("param2") ?? 0

        // 2. Perform native work
        let result = implementation.performNativeWork(param1, param2)

        // 3. Return to JavaScript
        call.resolve([
            "success": true,
            "data": result
        ])
    }
}
```

For the modern Swift Package Manager layout, the two-class split (bridge
vs implementation), permission flows, the Plugin Errors enum, and the SDK
adapter pattern, see `references/ios-implementation.md`.

### Layer 4: Native Android Implementation

**Purpose**: Access Android platform APIs and bridge to JavaScript.

```kotlin
package com.company.plugin

import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.JSObject

@CapacitorPlugin(name = "MyPlugin")
class MyPluginPlugin : Plugin() {
    private val implementation = MyPlugin()

    @PluginMethod
    fun methodName(call: PluginCall) {
        // 1. Extract parameters
        val param1 = call.getString("param1") ?: run {
            call.reject("Missing param1", "INVALID_PARAMETER")
            return
        }
        val param2 = call.getInt("param2", 0)

        // 2. Perform native work
        val result = implementation.performNativeWork(param1, param2)

        // 3. Return to JavaScript
        val ret = JSObject()
        ret.put("success", true)
        ret.put("data", result)
        call.resolve(ret)
    }
}
```

For activity-result plumbing, background dispatch, the Java filename rule,
and Plugin Errors constants, see `references/android-implementation.md`.

### Layer 5: Plugin Registration (`src/index.ts`)

**Purpose**: Export plugin for consumption by Capacitor apps.

```typescript
import { registerPlugin } from '@capacitor/core';
import type { MyPluginPlugin } from './definitions';

const MyPlugin = registerPlugin<MyPluginPlugin>('MyPlugin', {
  web: () => import('./web').then(m => new m.MyPluginWeb()),
});

export * from './definitions';
export { MyPlugin };
```

The first argument to `registerPlugin()` must match the iOS `jsName` and
the Android `@CapacitorPlugin(name)`. Name drift here is the most common
cause of plugins loading silently but never dispatching native calls. See
`references/scaffolding.md` "Name Parity".

---

## Architectural Patterns

### Bridge Pattern (Default)

The bridge pattern separates the Capacitor bridge from native business
logic. Treat the plugin bridge class as a controller: it parses calls,
gates permissions, delegates work, maps results, and emits events. It
should not grow into the whole implementation.

```
JavaScript API
  -> Capacitor plugin bridge class
    -> Native implementation class
      -> Platform APIs
```

Use this for simple and medium plugins:

- Small or moderate public API surfaces.
- Platform-specific work that fits behind clear method boundaries.
- Plugins where the bridge class can translate calls, enforce permissions,
  and delegate native work without coordinating many subsystems.

Benefits:

- Keeps `CAPPlugin` / `Plugin` classes thin.
- Makes native logic easier to unit test.
- Reduces mismatch between TypeScript, iOS, Android, and web implementations.

### Code Organization

Split implementation by responsibility rather than by convenience:

- `<PluginName>Plugin`: Capacitor bridge / controller only.
- `<PluginName>` or `<Feature>Manager`: platform API calls and native behavior.
- `<PermissionManager>`: runtime permissions or special settings access.
- `<Config>`: plugin configuration defaults and parsing.
- `<Mapper>` or small helpers: platform enum / string / result conversion.

Extract reusable parsing, validation, clamping, enum mapping, and
result-building helpers. Do not leave permission handling, platform API
calls, serialization, and sample-only logic in one large bridge method.

### Facade Pattern (Complex Subsystems)

Use a Facade when a plugin coordinates several native subsystems. `Facade`
is the design pattern name; generated class names may use `Facade`,
`Coordinator`, or `Manager` when that better matches the platform or
domain.

- Complex lifecycle, permission, foreground/background, external SDK, or
  long-lived event flows.
- Plugins that need multiple managers, delegates, receivers, activities,
  or lifecycle hooks.
- Plugins where a single implementation class would become a large
  coordinator.

The Capacitor plugin class should still stay thin. It should delegate to
a coordinator that owns subsystem orchestration and delegates focused
work to services such as permission, lifecycle, event, dependency, and
data managers.

### Pattern: Simple Request-Response

**Use when**: Single method call returns data.

```typescript
// TypeScript
interface DataPlugin {
  getData(options: { id: string }): Promise<{ value: string }>;
}
```

```swift
// iOS
@objc func getData(_ call: CAPPluginCall) {
    let id = call.getString("id") ?? ""
    let value = fetchData(id)
    call.resolve(["value": value])
}
```

```kotlin
// Android
@PluginMethod
fun getData(call: PluginCall) {
    val id = call.getString("id") ?: ""
    val value = fetchData(id)
    call.resolve(JSObject().put("value", value))
}
```

### Pattern: Event Streaming

**Use when**: Native code pushes data continuously (GPS, sensors).

```typescript
// TypeScript
interface SensorPlugin {
  startMonitoring(): Promise<void>;
  stopMonitoring(): Promise<void>;
  addListener(
    eventName: 'sensorData',
    listenerFunc: (data: SensorData) => void,
  ): Promise<PluginListenerHandle>;
}
```

```swift
// iOS
@objc func startMonitoring(_ call: CAPPluginCall) {
    startSensorUpdates { data in
        self.notifyListeners("sensorData", data: data)
    }
    call.resolve()
}
```

```kotlin
// Android
@PluginMethod
fun startMonitoring(call: PluginCall) {
    startSensorUpdates { data ->
        notifyListeners("sensorData", data)
    }
    call.resolve()
}
```

#### Event Parity

Every event name is a contract string. Keep the exact same value in:

- `src/definitions.ts` listener overload.
- `src/web.ts` `notifyListeners()`.
- iOS `notifyListeners(_:data:)`.
- Android `notifyListeners(name, data)`.
- Sample app listener registration.

Do not translate event names per platform.

#### Event Dispatch Locality

Capacitor's `notifyListeners(...)` is intentionally scoped to the plugin
class on both platforms — `protected` on Android and reached through
`self` on iOS. Generated code must respect this:

- Implementation classes, managers, services, broadcast receivers, and
  observers must not call `plugin.notifyListeners(...)` through a captured
  plugin reference. The Android compiler rejects it; iOS allows it but
  it is fragile and breaks when the plugin is not yet loaded.
- Either return event data to the plugin class and dispatch there, or
  expose a `public` wrapper method on the plugin class that calls
  `notifyListeners(...)` internally.
- Background contexts that may run before the plugin is loaded (FCM
  service, APNs receipt, deep-link intent, broadcast receiver) must
  dispatch through a static accessor on the plugin class, not through a
  captured plugin instance, because the plugin may not exist yet.

Treat this as a non-negotiable rule. Violations surface as access-modifier
compile errors on Android and silent no-ops or crashes on iOS.

### Pattern: Permission-Gated Access

**Use when**: Feature requires runtime permissions.

For methods that require runtime permissions, gate the action on the
current authorization state and reject with a consistent code if access
isn't granted. Do not auto-prompt inside an unrelated method — let the
caller invoke `requestPermissions()` first.

```typescript
// TypeScript
interface CameraPlugin {
  checkPermissions(): Promise<PermissionStatus>;
  requestPermissions(): Promise<PermissionStatus>;
  takePhoto(): Promise<Photo>;
}
```

```swift
// iOS
@objc func capturePhoto(_ call: CAPPluginCall) {
    let status = AVCaptureDevice.authorizationStatus(for: .video)
    switch status {
    case .authorized:
        performCapture(call)
    case .notDetermined:
        call.reject("Permission not requested. Call requestPermissions() first.",
                    "PERMISSION_DENIED")
    case .denied, .restricted:
        call.reject("Camera permission denied. Direct user to Settings.",
                    "PERMISSION_DENIED")
    @unknown default:
        call.reject("Unknown permission status.", "OPERATION_FAILED")
    }
}
```

The same shape applies on Android via `getPermissionState(alias)`. The
contract: `checkPermissions()` reports state, `requestPermissions()` may
prompt, gated methods reject without prompting. For the consumer-side
flow patterns (Check Before Use / Just-In-Time / Deferred), see
`references/permission-patterns.md`.

### Pattern: Background Task

**Use when**: Long-running operations (downloads, uploads, transcoding).

Long-running operations should not block a single `Promise<Result>`.
Model them as a task that returns a `taskId` immediately, emits
`progress` events, and can be cancelled.

```typescript
export interface DownloadPlugin {
  /** Start a download. Resolves with a task id immediately. */
  startDownload(options: { url: string }): Promise<{ taskId: string }>;

  /** Cancel a previously started task. */
  cancelDownload(options: { taskId: string }): Promise<void>;

  /** Emitted as the task makes progress (0-100). */
  addListener(
    eventName: 'downloadProgress',
    listenerFunc: (event: { taskId: string; percent: number }) => void,
  ): Promise<PluginListenerHandle>;

  /** Emitted once when the task completes (or fails). */
  addListener(
    eventName: 'downloadComplete',
    listenerFunc: (event: { taskId: string; uri?: string; error?: string }) => void,
  ): Promise<PluginListenerHandle>;
}
```

The native side keeps a `taskId -> task` map, dispatches progress through
the plugin's `notifyListeners` (per the Event Dispatch Locality rule), and
removes the entry on completion or cancel. Do not return a single
`Promise<Result>` that resolves only when the task finishes — consumers
need progress visibility and cancellation.

---

## Data Flow Patterns

### Synchronous Flow (Simple)

```
JavaScript Call
     ↓
Plugin Bridge
     ↓
Native Method
     ↓
Return Result
     ↓
Promise Resolves
```

### Asynchronous Flow (with Callbacks)

```
JavaScript Call
     ↓
Plugin Bridge
     ↓
Native Method (starts async work)
     ↓
Callback / Completion Handler
     ↓
notifyListeners() or resolve()
     ↓
JavaScript Receives Event / Result
```

### Bidirectional Flow (Events)

```
JavaScript addListener()
     ↓
Plugin Registers Listener
     ↓
Native Code Monitors
     ↓
Event Occurs
     ↓
notifyListeners()
     ↓
JavaScript Callback Fires
     ↓
(repeat until removeListener())
```

---

## Error Handling Architecture

### Error Propagation Strategy

```typescript
// TypeScript - canonical 4-code taxonomy
type PluginErrorCode = 'UNAVAILABLE' | 'PERMISSION_DENIED' | 'INVALID_PARAMETER' | 'OPERATION_FAILED';

// Use consistent error format
try {
  await plugin.method();
} catch (error) {
  // (error as { code?: string }).code = 'PERMISSION_DENIED'
  // error.message = 'Camera permission not granted'
}
```

```swift
// iOS - message first, code second
call.reject("Camera permission not granted", "PERMISSION_DENIED")
```

```kotlin
// Android - message first, code second
call.reject("Camera permission not granted", "PERMISSION_DENIED")
```

### Error Hierarchy

```
Plugin Errors
├── UNAVAILABLE - Feature not supported on platform
├── PERMISSION_DENIED - User denied permission
├── INVALID_PARAMETER - Bad input from JavaScript
└── OPERATION_FAILED - Native operation failed
    ├── NETWORK_ERROR (subtype)
    ├── HARDWARE_ERROR (subtype)
    ├── TIMEOUT (subtype)
    └── CANCELLED (subtype)
```

For the canonical taxonomy and cross-platform reject signatures, see
`references/api-design.md` "Error Handling". For native enum / constants
patterns that keep the codes from drifting between methods, see
`references/ios-implementation.md` and
`references/android-implementation.md`.

---

## Plugin Size and Scope

### Single Responsibility Principle

✅ **Good - Focused plugins**:
- `@capacitor/camera` - Only camera/photo functionality
- `@capacitor/geolocation` - Only location services
- `@capacitor/filesystem` - Only file operations

❌ **Bad - Kitchen sink plugins**:
- `@company/utilities` - Camera, GPS, files, sensors, etc.

### When to Split a Plugin

Generate one plugin per cohesive capability. Split when any of these are
true:

- More than ~10 unrelated public methods.
- Different methods need different runtime permissions (e.g., camera vs
  contacts vs location in one plugin).
- Different methods have different platform support (some iOS-only, some
  Android-only).
- Versioning would benefit from independence (e.g., a stable core +
  experimental adjacent feature).

### Plugin Composition

When splitting, prefer composition over inheritance: ship two small
focused plugins and let consumer apps import both, rather than a single
plugin with internal partitions.

```typescript
// Avoid: one plugin doing everything.
import { DeviceUtils } from '@company/device-utils';
const photo = await DeviceUtils.getPhoto();
const pos = await DeviceUtils.getCurrentPosition();
await DeviceUtils.writeFile({ path: 'p.jpg', data: photo.base64String });

// Prefer: small plugins composed at the call site.
import { Camera } from '@capacitor/camera';
import { Geolocation } from '@capacitor/geolocation';
import { Filesystem } from '@capacitor/filesystem';

const photo = await Camera.getPhoto();
const position = await Geolocation.getCurrentPosition();
await Filesystem.writeFile({ path: 'photo.jpg', data: photo.base64String });
```

---

## Performance Considerations

Each call across the JavaScript ↔ native bridge has serialization and
context-switch overhead. Generated APIs should default to shapes that
minimize bridge traffic.

### Minimize Bridge Crossings

Expose a batch method when the caller is likely to invoke the same
operation many times in succession.

```typescript
// ❌ Bad - 100 bridge crossings.
for (let i = 0; i < 100; i++) {
  await plugin.processItem(i);
}

// ✅ Good - 1 bridge crossing.
await plugin.processBatch([0, 1, 2, /* ... */ 99]);
```

### Handle Large Data Efficiently

Photos, audio recordings, and downloaded files should round-trip as
filesystem paths or `content://` URIs. Base64 inflates payload size by
~33% and forces a full UTF-16 traversal in V8 / JSC.

```typescript
// ❌ Bad - Pass large data through bridge
await plugin.processImage({ data: base64EncodedMegabytes });

// ✅ Good - Use file paths
await plugin.processImage({ uri: 'file:///path/to/image.jpg' });
```

### Use Events for Streams

When the native side produces data continuously (sensors, location,
download progress), expose `addListener(...)` and `notifyListeners(...)`;
do not require the caller to poll a `getCurrent()` method on a timer.

```typescript
// ❌ Bad - Polling
setInterval(async () => {
  const data = await plugin.getSensorData();
}, 100);

// ✅ Good - Event listener
plugin.addListener('sensorData', (data) => {
  // Receives data as it's available
});
```

These are defaults, not hard rules. Small payloads, one-shot calls, and
debug-only methods can ignore them.

---

## Testing Architecture

### Layer Testing Strategy

Pragmatic targets a candidate plugin can aim for. Higher is better, but
the numbers below reflect what's realistic given the bridge constraints:

| Layer                  | Tooling             | Coverage target |
| ---                    | ---                 | ---             |
| TypeScript API         | Jest                | 80%+            |
| Web implementation     | Jest + JSDOM        | 70%+            |
| iOS native             | XCTest              | 60%+            |
| Android native         | JUnit / Robolectric | 60%+            |
| End-to-end integration | Sample app + Detox  | Key flows only  |

Native targets are lower because UI framework code (UIKit, Activity
lifecycle) is hard to cover in unit tests. Push business logic into plain
classes (per the Testability Guidelines below) so the non-UI portion can
clear the 80%+ bar.

### Testability Guidelines

Two rules that keep the bridge unit-testable in isolation from Capacitor:

1. **Inject platform dependencies.** Don't hard-code references to
   `CLLocationManager`, `AVCaptureDevice`, etc. inside the implementation
   class. Take a protocol / interface in the constructor with a default
   implementation that uses the real platform API; tests can pass a fake.

   ```swift
   // ✅ Good - testable
   protocol LocationProvider {
       func currentLocation() throws -> CLLocation
   }
   class LocationImpl {
       private let provider: LocationProvider
       init(provider: LocationProvider = SystemLocationProvider()) {
           self.provider = provider
       }
   }
   ```

2. **Separate business logic from the bridge.** Plugin bridge methods
   should be small: parse `CAPPluginCall` / `PluginCall` options,
   delegate to a plain class, map the result. Put validation, format
   conversion, and computation in classes that have no Capacitor types
   in their public surface — those classes can be tested with vanilla
   XCTest / JUnit / Jest without mocking Capacitor.

   ```typescript
   // ✅ Good
   class DataProcessor {
       static process(input: string): string { /* logic */ }
   }
   class MyPluginWeb extends WebPlugin {
       async method(options: Options): Promise<Result> {
           const result = DataProcessor.process(options.input);
           return { result };
       }
   }
   ```

For framework-specific test scaffolding (Mock `CAPPluginCall`, Mockito,
Robolectric, Jest navigator mocking), see `references/testing-strategies.md`.

---

## Versioning Strategy

### Semantic Versioning for Plugins

- **MAJOR** (1.0.0 → 2.0.0)
  - Breaking API changes
  - Removed methods
  - Changed method signatures
  - Minimum Capacitor version bump

- **MINOR** (1.0.0 → 1.1.0)
  - New methods added
  - New features (backward compatible)
  - Deprecations (with warnings)

- **PATCH** (1.0.0 → 1.0.1)
  - Bug fixes
  - Documentation updates
  - Internal refactoring

### Deprecation Pattern

```typescript
/**
 * @deprecated Use newMethod() instead. Will be removed in v3.0.0
 */
async oldMethod(): Promise<void> {
  console.warn('oldMethod is deprecated, use newMethod');
  return this.newMethod();
}

async newMethod(): Promise<void> {
  // New implementation
}
```

For the full deprecation / `@since` / `@requires` annotation pattern, see
`references/api-design.md` "Versioning and Deprecation".

---

## Security Considerations

Two cross-platform rules that are easy to miss in generated code:

### Input Validation

The bridge is a trust boundary between app code and the native runtime.
Before passing user-supplied strings to privileged APIs (file paths,
URLs, intent extras, shell-like inputs), validate the shape on the side
that constructs the call.

```typescript
// TypeScript - Validate before passing to native
async openLink(options: { url: string }): Promise<void> {
  if (!/^https?:\/\//.test(options.url)) {
    const err = new Error('http(s) URL required');
    (err as Error & { code: string }).code = 'INVALID_PARAMETER';
    throw err;
  }
  // pass to native
}
```

Apply the same rule to file paths (reject absolute paths or `..`
traversal unless intentional) and to any input that becomes a system
intent extra.

### Sensitive Data Handling

Tokens, passwords, biometric outputs, API keys, and credentials must not
appear in `print()` / `Log.d()` / `console.log()` even at debug level.
Generated code should log the *attempt*, not the payload:

```swift
// ❌ leaks secret to device console
@objc func authenticate(_ call: CAPPluginCall) {
    let password = call.getString("password")
    print("Password: \(password ?? "")")
}

// ✅ visibility without exposure
@objc func authenticate(_ call: CAPPluginCall) {
    Logger.info("Authentication attempt")
    // ...
}
```

Generated `Logger` / `Log.d` / `console.log` calls in the candidate plugin
must be reviewed before publish — see the Candidate Output Rule below.

### Permission Best Practices

- Request minimum permissions needed.
- Request permissions just-in-time (not at app launch).
- Provide clear rationale in permission dialogs.
- Handle denial gracefully.

For multi-permission DispatchGroup patterns and consumer-side flow
patterns, see `references/permission-patterns.md`.

---

## Plugin Lifecycle

```
Development → Testing → Publishing → Maintenance
     ↓            ↓          ↓            ↓
  Design API   Unit tests  npm publish  Bug fixes
  Implement    E2E tests   Tag release  Updates
  Document     Device test Documentation Versioning
```

### Maintenance Checklist

- [ ] Monitor for Capacitor updates.
- [ ] Test on new iOS / Android versions.
- [ ] Update dependencies regularly.
- [ ] Respond to issues / PRs.
- [ ] Deprecate old APIs gracefully.
- [ ] Keep documentation current.

---

## Candidate Output Rule

Generated output is a reviewable starting point, not production-ready
code. Flag native areas that require credentials, entitlement setup,
real-device testing, app store policy review, or manual SDK configuration.
The skill produces candidates; humans complete the final integration
work.

---

## Summary

**Key Architectural Principles**:

1. **Separation of concerns** - Each layer has a clear responsibility.
2. **Type safety** - Use TypeScript interfaces as contracts.
3. **Consistency** - Same patterns across iOS and Android.
4. **Testability** - Design for unit and integration testing; inject
   dependencies, separate business logic from bridge.
5. **Performance** - Minimize bridge crossings, batch operations, prefer
   URIs over base64, events over polling.
6. **Security** - Validate inputs, protect sensitive data.
7. **Maintainability** - Keep plugins focused and well-documented;
   compose smaller plugins instead of building monoliths.

**Bridge vs Facade**:

- **Bridge** is the default. Use for simple to medium plugins.
- **Facade** when the plugin coordinates multiple native subsystems
  (lifecycle, permissions, foreground/background, external SDK,
  long-lived events).

**Event Dispatch Locality**: never call `plugin.notifyListeners(...)`
through a captured plugin reference. Dispatch from inside the plugin
class or through a static accessor for background contexts.

**Remember**: Good architecture makes plugins easy to test, maintain, and
extend.
