# API Design Best Practices

This reference covers how to design clean, intuitive TypeScript APIs for Capacitor plugins.

The TypeScript contract drives every generated platform. Design `src/definitions.ts`
before implementing web, iOS, or Android.

## Contract Rules

A short list of non-negotiables that every method on a plugin interface must follow:

- Methods return `Promise<T>` or `Promise<void>`.
- A method has at most one parameter named `options` (or follow the established
  parameter name where mirroring an existing public API).
- Name method options `<MethodName>Options`.
- Name method results `<MethodName>Result`.
- Define separate interfaces for options, results, and event payloads.
- Use string union types instead of TypeScript enums.
- Use `undefined` rather than `null` for absent optional values.
- Import type-only symbols with `import type` and avoid unused imports.
- Use stable cross-platform units and ISO 8601 strings for dates.
- Add JSDoc and `@since` to every public interface, method, property, type,
  and listener overload.
- If any method needs runtime permission, special settings access, or manual
  native setup, add `checkPermissions()` and `requestPermissions()` to the API
  unless Capacitor provides a more specific established pattern.
- If the same user-facing capability has both local and system-wide variants,
  model them explicitly instead of hiding platform differences.

The rest of this reference unpacks each rule with examples.

---

## Core Principles

### 1. Promise-Based APIs

✅ **Always use Promises** for async operations:

```typescript
// ✅ Good
async getLocation(): Promise<Location> {
  // Returns promise
}

// ❌ Bad - callbacks
getLocation(callback: (location: Location) => void): void {
  // Callback hell
}

// ❌ Bad - synchronous when it shouldn't be
getLocationSync(): Location {
  // Blocks thread
}
```

### 2. Strong Typing

✅ **Define explicit interfaces** for all data:

```typescript
// ✅ Good - Strongly typed
interface PhotoOptions {
  quality: number;          // 0-100
  source: 'camera' | 'gallery';
  resultType: 'base64' | 'uri';
}

interface Photo {
  base64String?: string;
  path?: string;
  format: 'jpeg' | 'png';
}

async getPhoto(options: PhotoOptions): Promise<Photo>;

// ❌ Bad - Weakly typed
async getPhoto(options: any): Promise<any>;
```

### 3. Semantic Method Names

Use clear, action-oriented names:

```typescript
// ✅ Good
async getCurrentPosition(): Promise<Position>
async startMonitoring(): Promise<void>
async requestPermissions(): Promise<PermissionStatus>
async capturePhoto(): Promise<Photo>

// ❌ Bad
async position(): Promise<Position>        // Not clear if get/set
async monitor(): Promise<void>             // Start or stop?
async permissions(): Promise<PermissionStatus>  // Check or request?
async photo(): Promise<Photo>              // Too vague
```

---

## Method Naming Conventions

### Action Verbs

The verb dictates the method's contract — readers should be able to predict
the return shape from the name alone.

| Verb              | Usage                                   | Example                                           |
|-------------------|-----------------------------------------|---------------------------------------------------|
| `get`             | Retrieve current state, no side effects | `getBatteryStatus()`, `getCurrentPosition()`      |
| `check`           | Test condition, no prompt               | `checkPermissions()`, `isAvailable()`             |
| `request`         | Ask the user or system for something    | `requestPermissions()`, `requestToken()`          |
| `start`           | Begin continuous operation              | `startMonitoring()`, `startScanning()`            |
| `stop`            | End continuous operation                | `stopMonitoring()`, `stopScanning()`              |
| `create`          | Make new resource                       | `createFile()`, `createNotification()`            |
| `delete`/`remove` | Remove resource                         | `deleteFile()`, `removeNotification()`            |
| `update`          | Mutate existing state                   | `updateSettings()`, `modifyRecord()`              |
| `open`            | Open / show platform UI                 | `openSettings()`, `showDialog()`                  |
| `close`           | Close / dismiss UI                      | `closeDialog()`, `dismissAlert()`                 |
| `add`             | Add to a collection                     | `addListener()`                                   |
| `remove`          | Remove from a collection                | `removeListener()`, `removeAllListeners()`        |

Avoid bare nouns (`status()`, `permissions()`) — readers cannot tell whether
they read or write.

### Naming Examples

```typescript
// State retrieval
async getBatteryStatus(): Promise<BatteryInfo>
async getNetworkStatus(): Promise<NetworkInfo>

// Permission handling
async checkPermissions(): Promise<PermissionStatus>
async requestPermissions(): Promise<PermissionStatus>

// Operations
async capturePhoto(options: PhotoOptions): Promise<Photo>
async scanBarcode(): Promise<BarcodeResult>
async shareContent(options: ShareOptions): Promise<void>

// Monitoring
async startLocationUpdates(): Promise<void>
async stopLocationUpdates(): Promise<void>
async addListener(eventName: string, callback: Function): Promise<PluginListenerHandle>
async removeAllListeners(): Promise<void>

// Availability checks
async isAvailable(): Promise<{ available: boolean }>
async isSupported(): Promise<{ supported: boolean }>
```

---

## Parameter Design

### Options Objects

✅ **Use options objects** for methods with multiple parameters:

```typescript
// ✅ Good - Options object
interface WriteFileOptions {
  path: string;
  data: string;
  encoding?: 'utf8' | 'base64';  // Optional with default
  append?: boolean;               // Optional boolean
}

async writeFile(options: WriteFileOptions): Promise<void>

// Usage is clear
await Filesystem.writeFile({
  path: 'notes.txt',
  data: 'Hello world',
  encoding: 'utf8',
  append: true
});

// ❌ Bad - Many parameters
async writeFile(
  path: string,
  data: string,
  encoding?: string,
  append?: boolean
): Promise<void>

// Hard to remember parameter order
await Filesystem.writeFile('notes.txt', 'Hello', 'utf8', true);
```

The options interface is named `<MethodName>Options` (e.g., `WriteFileOptions`
for `writeFile`). The corresponding result type, if any, is named
`<MethodName>Result`. The pair gives reviewers a predictable mental model
and makes generated docs easier to scan.

### Optional vs Required

```typescript
interface RequestOptions {
  // Required - no default sensible value
  url: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';

  // Optional - has sensible defaults
  headers?: Record<string, string>;
  timeout?: number;          // Default: 30000
  followRedirects?: boolean; // Default: true
}
```

### Default Values

Document defaults in JSDoc:

```typescript
interface CameraOptions {
  /**
   * Image quality (0-100)
   * @default 90
   */
  quality?: number;

  /**
   * Maximum width in pixels
   * @default 0 (no limit)
   */
  width?: number;

  /**
   * Source for photo
   * @default 'camera'
   */
  source?: 'camera' | 'gallery';
}
```

---

## Return Type Design

### Success Results

```typescript
// Simple success - void
async deleteFile(options: { path: string }): Promise<void>

// Return data
async readFile(options: { path: string }): Promise<{ data: string }>

// Return multiple values
async getLocation(): Promise<{
  latitude: number;
  longitude: number;
  accuracy: number;
  timestamp: number;
}>
```

### Boolean Results

Wrap booleans in an object so the API can grow without breaking consumers.
Adding a field to a result object is non-breaking; changing a primitive
return type is breaking.

```typescript
// ✅ Good - Explicit naming, extensible
async isAvailable(): Promise<{ available: boolean }>
async checkConnection(): Promise<{ connected: boolean }>

// ❌ Bad - Ambiguous and brittle
async available(): Promise<boolean>  // Is this checking or setting?
```

### List Results

Same rule for arrays — wrap in an object so future fields can be added
non-breakingly:

```typescript
// ✅ Good - Named array property, extensible
async listFiles(): Promise<{ files: FileInfo[]; truncated?: boolean }>

interface FileInfo {
  name: string;
  size: number;
  modified: number;
}

// ❌ Bad - Raw array (harder to extend later)
async listFiles(): Promise<FileInfo[]>
```

### `FeatureAvailability` Shape

For "is this feature available here?" methods, the canonical shape includes
a reason field so consumers can show appropriate messaging:

```typescript
export interface FeatureAvailability {
  available: boolean;
  /** Why the feature is unavailable on this device/session. */
  reason?: string;
}

interface MyPlugin {
  isAvailable(): Promise<FeatureAvailability>;
}
```

---

## Error Handling

### Canonical Error Codes

Reject with a small, consistent vocabulary of code strings across web, iOS,
and Android so consumers can write unified error handling. Add new codes
only when callers genuinely need to branch on the cause; do not invent a
one-off code per call site.

| Code                | When to use                                                                  |
| ---                 | ---                                                                          |
| `UNAVAILABLE`       | The feature is not supported on this platform/device/session.                |
| `PERMISSION_DENIED` | The user denied a runtime permission (or it was permanently denied).         |
| `INVALID_PARAMETER` | Arguments fail validation: missing, wrong type, or out of range.             |
| `OPERATION_FAILED`  | The native operation failed for a reason not covered above.                  |

Subtype `OPERATION_FAILED` only when callers need to branch on cause:
`NETWORK_ERROR`, `HARDWARE_ERROR`, `TIMEOUT`, `CANCELLED`. Keep the surface
small.

### Cross-Platform Reject Signatures

Native reject signatures take the message first and the code second:

```swift
// iOS
call.reject("Camera permission not granted", "PERMISSION_DENIED")
```

```java
// Android
call.reject("Camera permission not granted", "PERMISSION_DENIED");
```

```typescript
// Web — attach a `code` property so consumers see the same wire shape.
const error = new Error('Camera permission not granted');
(error as Error & { code: string }).code = 'PERMISSION_DENIED';
throw error;
```

### Error Objects

```typescript
interface PluginError extends Error {
  code: string;
  message: string;
  details?: any;  // Platform-specific details
}

// Usage
try {
  await Camera.getPhoto();
} catch (error) {
  if ((error as PluginError).code === 'PERMISSION_DENIED') {
    // Show permission rationale
  } else if ((error as PluginError).code === 'UNAVAILABLE') {
    // Feature not supported
  }
}
```

Centralize the codes natively — see `references/ios-implementation.md`
"Plugin Errors as a Swift Enum" and `references/android-implementation.md`
"Plugin Errors as Constants" so the codes do not drift across methods.

### When to Throw vs Return

Reject (throw) for **exceptional failures** the caller cannot recover from
in the normal flow. Return a status object for **expected conditions** the
caller should branch on:

```typescript
// ✅ Throw for invalid input — exceptional, caller has a bug
async getPhoto(options: PhotoOptions): Promise<Photo> {
  if (options.quality < 0 || options.quality > 100) {
    const err = new Error('quality must be 0-100');
    (err as Error & { code: string }).code = 'INVALID_PARAMETER';
    throw err;
  }
  // ...
}

// ✅ Return status for expected conditions — denied is a normal outcome
async checkPermissions(): Promise<PermissionStatus> {
  return { camera: 'denied' };
}
```

The litmus test: if every well-written caller has to wrap the call in
`try/catch` to handle a routine outcome, the API should return a status
instead. If only buggy callers will see the failure, throw.

---

## Event Listeners

### Event Pattern

```typescript
interface MyPlugin {
  /**
   * Start monitoring (if needed)
   */
  startMonitoring(): Promise<void>;

  /**
   * Listen for events
   */
  addListener(
    eventName: 'dataChange',
    listenerFunc: (data: DataType) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Remove specific listener
   */
  removeListener(handle: PluginListenerHandle): Promise<void>;

  /**
   * Remove all listeners for an event
   */
  removeAllListeners(): Promise<void>;

  /**
   * Stop monitoring (if needed)
   */
  stopMonitoring(): Promise<void>;
}
```

Event names must be identical strings across TypeScript / web / iOS /
Android — see `references/architecture-patterns.md` "Event Parity" and
"Event Dispatch Locality" for the cross-platform rules.

### Event Naming

```typescript
// ✅ Good - Descriptive event names
addListener('batteryChange', callback)
addListener('networkStatusChange', callback)
addListener('locationUpdate', callback)

// ❌ Bad - Vague names
addListener('change', callback)
addListener('update', callback)
addListener('data', callback)
```

### Multiple Event Types

```typescript
interface SensorPlugin {
  addListener(
    eventName: 'accelerometer',
    listenerFunc: (data: AccelerometerData) => void,
  ): Promise<PluginListenerHandle>;

  addListener(
    eventName: 'gyroscope',
    listenerFunc: (data: GyroscopeData) => void,
  ): Promise<PluginListenerHandle>;

  addListener(
    eventName: 'magnetometer',
    listenerFunc: (data: MagnetometerData) => void,
  ): Promise<PluginListenerHandle>;
}

// Usage
const handle = await Sensor.addListener('accelerometer', (data) => {
  console.log(data.x, data.y, data.z);
});
```

---

## Permission APIs

### Standard Permission Pattern

```typescript
export interface PermissionStatus {
  [key: string]: 'granted' | 'denied' | 'prompt' | 'prompt-with-rationale';
}

interface MyPlugin {
  /**
   * Check current permission status without prompting
   */
  checkPermissions(): Promise<PermissionStatus>;

  /**
   * Request permissions from user (shows system dialog)
   */
  requestPermissions(): Promise<PermissionStatus>;
}
```

Standardize permission state strings on `granted` / `denied` / `prompt` /
`prompt-with-rationale`. Import `PermissionState` from `@capacitor/core`
unless the plugin needs custom states (e.g., iOS limited photo library
adds `'limited'`).

### Example: Camera Plugin

```typescript
interface CameraPermissionStatus {
  camera: 'granted' | 'denied' | 'prompt';
  photos: 'granted' | 'denied' | 'prompt';  // iOS photo library
}

// Check without prompting
const status = await Camera.checkPermissions();
if (status.camera === 'granted') {
  // Can use camera
}

// Request if needed
if (status.camera !== 'granted') {
  const result = await Camera.requestPermissions();
  if (result.camera === 'granted') {
    // Permission granted
  } else {
    // Permission denied - show rationale
  }
}
```

For deep-dive on multi-permission handling, location-delegate pattern, and
the consumer-side flow patterns (Check Before Use / Just-In-Time / Deferred),
see `references/permission-patterns.md`.

---

## Platform-Specific APIs

### Conditional Features

```typescript
interface FeatureAvailability {
  available: boolean;
  reason?: string;  // Why not available (if !available)
}

interface MyPlugin {
  /**
   * Check if feature is available on current platform
   */
  isAvailable(): Promise<FeatureAvailability>;
}

// Usage
const { available, reason } = await NFC.isAvailable();
if (!available) {
  console.log(`NFC not available: ${reason}`);
  // e.g., "Not supported on web", "Requires iOS 13+", etc.
}
```

### Platform-Specific Options

When iOS and Android need substantively different inputs for the same
logical operation, surface the divergence with namespaced sub-objects
rather than flattening platform-specific keys into the top level:

```typescript
interface NotificationOptions {
  /** Common across platforms. */
  title: string;
  body: string;

  /** iOS-only fields. */
  ios?: {
    sound?: string;
    badge?: number;
    threadId?: string;
  };

  /** Android-only fields. */
  android?: {
    channelId: string;
    priority?: 'high' | 'low';
    smallIcon?: string;
  };
}
```

Document which keys are platform-specific in JSDoc. Platform-specific
options should be optional from the contract's perspective; the native
side falls back to sensible defaults if the consumer omits them.

---

## When Mirroring an Existing API

If the requested plugin mirrors an existing public API — a Capacitor
core/community plugin, a Capawesome plugin, an internal library, or a
documented JavaScript API the user is replacing — look up the actual
source `definitions.ts` (or equivalent) before generating. Match the
wire-format string literal values exactly. Do not derive them from
human-friendly names or TypeScript enum key names.

A common failure mode: a known API exposes a TypeScript enum like
`enum Style { Heavy = 'HEAVY', Medium = 'MEDIUM', Light = 'LIGHT' }`. If
the contract is generated from the human description ("style options
Heavy, Medium, Light") instead of the source, the generator may produce
`'Heavy' | 'Medium' | 'Light'` as the union — which is not the wire
format and will not interoperate with apps already using the official
plugin.

The structured YAML mode pins these values in `api.types[].values` so the
generator does not need to guess. Conversational mode must consult the
source when a target API exists; otherwise, document the chosen wire
format explicitly so reviewers can see what was decided.

### Native Dependency Detection

Mirroring an existing API means matching its architecture too. Before
generating native code, inspect the official plugin's dependency
declarations:

- **iOS** — read the `.podspec` for `s.dependency '<Library>'` and
  `Package.swift` for `dependencies: [.package(url: ...)]`.
- **Android** — read `android/build.gradle` for
  `implementation '<group>:<artifact>:...'` entries (excluding
  `:capacitor-android` itself).

If the official plugin depends on a native library that wraps the
underlying platform API, the candidate plugin **must declare the same
dependency and delegate to it** — do not reimplement from scratch. The
skill's job is wire-compatibility *and* architectural compatibility;
reimplementing under the same TypeScript surface produces a divergent
fork that loses upstream bug fixes, behavior parity, and platform-quirk
handling.

When the SDK is wrapped, the bridge class becomes a thin adapter — see
`references/ios-implementation.md` and `references/android-implementation.md`
for the SDK adapter pattern.

When the official plugin's bridge code is available locally, **read its
actual SDK call sites and mirror them**. The official plugin is the
canonical example of how to call this SDK from a Capacitor bridge; do not
invent alternative API surfaces based on the SDK's name alone (e.g.,
inferring class names like `XCameraLib.takePhoto(request:completion:)`
from the package title). Match the official's import statements, type
names, delegate conformances, and method signatures exactly.

If the official source is not reachable and the SDK headers cannot be
read, the candidate must still compile. Generate a local
protocol/interface stub named `<SDKName>Bridge` with the operations the
plugin needs, and inject a placeholder implementation that rejects with
`unimplemented()`. Mark every call site with a `TODO(SDK): wire up
<method> via <real SDK class>` comment so a human reviewer can complete
the integration. Never import speculative type names that the agent has
not verified exist.

---

## Versioning and Deprecation

### Deprecating Methods

Annotate evolution explicitly. Every public symbol already needs `@since`;
methods, types, or properties scheduled for removal also need
`@deprecated`.

```typescript
interface MyPlugin {
  /**
   * @deprecated Use getDataV2() instead. Will be removed in v3.0.0
   * @see getDataV2
   */
  getData(): Promise<OldData>;

  /**
   * Improved data fetching with additional fields
   * @since 2.1.0
   */
  getDataV2(): Promise<NewData>;
}
```

A deprecated method must still work — keep it forwarding to its
replacement and log once at runtime so consumers see the migration
notice in development:

```typescript
async getData(): Promise<OldData> {
  console.warn('[ExamplePlugin] getData is deprecated; use getDataV2');
  return this.getDataV2() as unknown as OldData;
}
```

### Version-Specific Features

```typescript
interface MyPlugin {
  /**
   * Advanced feature
   * @since 2.0.0
   * @requires iOS 14+, Android 11+
   */
  advancedFeature(): Promise<void>;
}
```

`@requires` documents minimum platform/OS versions. Pair with a runtime
guard (`unavailable()` on the native side) so calls on older OS versions
reject cleanly rather than crash at the API boundary.

Bump the npm `version` field per semver: MAJOR removes deprecated APIs,
MINOR adds new ones, PATCH fixes bugs without contract changes.

---

## Documentation Standards

### JSDoc Comments

```typescript
interface MyPlugin {
  /**
   * Capture a photo using the device camera
   *
   * @param options - Configuration for photo capture
   * @returns Promise with photo data
   * @throws {PluginError} PERMISSION_DENIED if camera permission not granted
   * @throws {PluginError} UNAVAILABLE if camera not available
   *
   * @example
   * ```typescript
   * const photo = await Camera.getPhoto({
   *   quality: 90,
   *   source: 'camera',
   *   resultType: 'base64'
   * });
   * console.log(photo.base64String);
   * ```
   *
   * @see requestPermissions
   * @since 1.0.0
   */
  getPhoto(options: PhotoOptions): Promise<Photo>;
}
```

`@since` is mandatory on every public symbol — `npm run docgen` reads it
and emits a generated API table into the README between the `docgen`
markers.

### Interface Documentation

```typescript
/**
 * Configuration options for photo capture
 */
export interface PhotoOptions {
  /**
   * Image quality (0-100)
   * Lower values = smaller file size
   * @default 90
   */
  quality?: number;

  /**
   * Where to get the photo from
   * - 'camera': Open camera to take new photo
   * - 'gallery': Select from photo library
   * @default 'camera'
   */
  source?: 'camera' | 'gallery';

  /**
   * Format of returned data
   * - 'base64': Base64-encoded string
   * - 'uri': File path URI
   * @default 'base64'
   */
  resultType?: 'base64' | 'uri';
}
```

---

## API Design Checklist

When designing a new plugin API:

- [ ] **Methods return Promises** for async operations (`Promise<T>` or `Promise<void>`).
- [ ] **All data types have explicit interfaces**, named `<MethodName>Options` and `<MethodName>Result`.
- [ ] **Method names are semantic and action-oriented** — pick a verb from the naming table.
- [ ] **Parameters use options objects** (for methods with 2+ params, or where named fields aid readability).
- [ ] **Optional parameters have documented `@default` values**.
- [ ] **Error codes drawn from the canonical taxonomy** (`UNAVAILABLE`, `PERMISSION_DENIED`, `INVALID_PARAMETER`, `OPERATION_FAILED`).
- [ ] **Events have descriptive names** and identical strings across TS / web / iOS / Android.
- [ ] **Permission methods follow the standard pattern** (`checkPermissions()` / `requestPermissions()`) with typed `PermissionStatus`.
- [ ] **Platform differences are documented** in JSDoc; surfaced through `ios?` / `android?` namespaces when options diverge.
- [ ] **All public APIs have JSDoc comments** with `@since`.
- [ ] **Examples provided** for complex methods (via `@example`).
- [ ] **Return types are wrapped in objects** (no bare `Promise<boolean>` or `Promise<T[]>`) for future extensibility.
- [ ] **Native SDK dependencies declared** in `Package.swift` / podspec / `build.gradle` if mirroring an existing plugin that wraps an SDK.

---

## Common Patterns

### Pattern: Resource Management

For plugins that manage long-lived native resources (file handles, BLE
connections, audio sessions, database transactions), expose them through
a handle that the caller passes back on each operation. This avoids
hiding state inside the plugin and makes lifecycles explicit:

```typescript
interface ResourcePlugin {
  /** Open a resource and return an opaque handle. */
  open(options: { id: string }): Promise<{ handle: string }>;

  /** Operate on the resource. */
  read(options: { handle: string }): Promise<{ data: string }>;
  write(options: { handle: string; data: string }): Promise<void>;

  /** Always require the caller to close. */
  close(options: { handle: string }): Promise<void>;
}
```

The native side keeps a `handle -> resource` map and rejects with
`OPERATION_FAILED` when an unknown handle is passed. Document that
consumers must `close()` to avoid leaks; for resources that must survive
plugin unload, document the recovery semantics explicitly.

### Pattern: Batch Operations

```typescript
interface BatchPlugin {
  // Single operation
  processItem(options: { id: string }): Promise<{ result: string }>;

  // Batch operation (more efficient)
  processBatch(options: { ids: string[] }): Promise<{ results: string[] }>;
}
```

Each call across the JavaScript ↔ native bridge has serialization and
context-switch overhead. Expose a batch method when the caller is likely
to invoke the same operation many times in succession — see
`references/architecture-patterns.md` "Performance Considerations" for
the underlying rationale.

### Pattern: Configuration

For runtime-configurable values that app developers set in their
Capacitor config rather than passing per-call, see
`references/configuration.md` "Runtime Plugin Configuration". The runtime
config interface lives alongside the plugin interface in
`src/definitions.ts` (or a sibling `src/config.ts`) and augments
`@capacitor/cli`'s `PluginsConfig` type.

---

## Anti-Patterns to Avoid

### ❌ Callback Hell

```typescript
// Don't do this
plugin.getData((data) => {
  plugin.processData(data, (result) => {
    plugin.saveResult(result, (success) => {
      console.log('Done');
    });
  });
});

// Use Promises
const data = await plugin.getData();
const result = await plugin.processData(data);
await plugin.saveResult(result);
```

### ❌ Unclear Boolean Returns

```typescript
// Don't do this
async camera(): Promise<boolean>  // What does true/false mean?

// Be explicit
async isCameraAvailable(): Promise<{ available: boolean }>
```

### ❌ Stringly-Typed APIs

```typescript
// Don't do this — collapses every operation into one method, defeats type checking.
async doAction(action: string, data: any): Promise<any>

// Use specific methods and types
async capturePhoto(options: PhotoOptions): Promise<Photo>
async recordVideo(options: VideoOptions): Promise<Video>
```

### ❌ Mutable Options

```typescript
// Don't do this
const options = { quality: 90 };
await plugin.process(options);
// Plugin modifies options internally
console.log(options.quality);  // Changed to 100?

// Keep options immutable
```

The options object passed across the bridge belongs to the caller.
Native code receives a JSON copy anyway, so any "mutation" only affects
a local clone — surface that clearly by treating options as read-only
inputs and returning new result objects.

### ❌ Boolean Parameters That Change Behavior

```typescript
// Avoid: caller has to remember what `true` means at every call site.
loadFile(path: string, sync: boolean): Promise<string>;

// Prefer named modes or distinct methods.
loadFile(options: { path: string; mode: 'sync' | 'async' }): Promise<string>;
```

These shapes show up most often when a contract is generated from a
verbal description that did not break operations into typed shapes. When
in doubt, err toward more specific methods with `<MethodName>Options` /
`<MethodName>Result` interfaces.

---

## Registration

`src/index.ts` should register the plugin and lazily load the web
implementation:

```typescript
import { registerPlugin } from '@capacitor/core';

import type { ExamplePlugin } from './definitions';

const Example = registerPlugin<ExamplePlugin>('Example', {
  web: () => import('./web').then((m) => new m.ExampleWeb()),
});

export * from './definitions';
export { Example };
```

The first argument to `registerPlugin()` is the **JavaScript plugin name**.
This must match the `jsName` property in the iOS `CAPBridgedPlugin`
conformance and the `name` property in the Android `@CapacitorPlugin`
annotation. See `references/scaffolding.md` "Name Parity" for details.

---

## Method Signature Mapping

| API behavior              | TypeScript                                       | iOS return type            | Android annotation                                          |
| ---                       | ---                                              | ---                        | ---                                                         |
| Returns a value           | `Promise<Result>`                                | `CAPPluginReturnPromise`   | `@PluginMethod()`                                           |
| Returns no value          | `Promise<void>`                                  | `CAPPluginReturnNone`      | `@PluginMethod(returnType = PluginMethod.RETURN_NONE)`     |
| Native callback / watch   | `Promise<CallbackID>` with a callback parameter  | `CAPPluginReturnCallback`  | `@PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)` |

Use callback return types only for native streams or watchers that save
and reuse a `PluginCall`. Events should usually be modeled with
`addListener()` and `notifyListeners()`.

---

## Summary

**Good API Design**:
- Clear, predictable method names from the naming verb table
- Strong typing with explicit `<MethodName>Options` and `<MethodName>Result` interfaces
- Consistent error handling using the canonical 4-code taxonomy
- Well-documented with JSDoc + `@since` and `@example`
- Extensible through return-shape wrapping
- Platform differences handled gracefully via `ios?` / `android?` namespaces
- Mirrors existing public APIs faithfully — wire format and native dependencies

**Remember**: The API is the user's first impression of your plugin. Make
it intuitive!
