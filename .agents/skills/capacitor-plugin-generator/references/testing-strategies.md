# Testing Strategies

Comprehensive guide for testing Capacitor plugins across all layers: TypeScript, web, iOS, and Android.

## Testing Pyramid

```
                    E2E Tests
                   (Full Stack)
                 /              \
            Integration Tests
           (Native + Bridge)
          /                    \
     Unit Tests              Unit Tests
   (TypeScript/Web)        (iOS/Android)
```

---

## Quick Start: Quality Commands

Before diving into detailed testing, know these essential Capacitor plugin commands:

### Verify All Platforms Build

```bash
npm run verify
```

This command is your **first line of defense**. It verifies:
- ✅ **Web**: TypeScript compiles successfully
- ✅ **iOS**: Swift builds and tests pass
- ✅ **Android**: Gradle builds and tests pass

**When to run:**
- After making any native code changes
- Before committing code
- Before creating a pull request
- Before publishing to npm
- As part of CI/CD pipeline

### Format Code

```bash
npm run fmt
```

Auto-formats all code according to Capacitor conventions:
- TypeScript/JavaScript (ESLint + Prettier)
- Swift (SwiftLint)
- Java/Kotlin (Prettier)

**When to run:**
- Before committing (make it a habit!)
- After writing new code
- To fix lint errors automatically

### Lint Code

```bash
npm run lint
```

Checks code style without modifying files. Reports issues that `fmt` couldn't auto-fix.

**When to run:**
- In CI/CD pipelines (fail build on errors)
- Before final code review
- To verify code quality

### Recommended Workflow

```bash
# 1. Write code
# ... edit files ...

# 2. Format automatically
npm run fmt

# 3. Run unit tests
npm test

# 4. Verify all platforms build
npm run verify

# 5. Final lint check
npm run lint

# 6. Commit if all pass
git add .
git commit -m "feat: add feature"
```

---

## TypeScript/Web Testing

### Setup with Jest

```bash
npm install --save-dev jest @types/jest ts-jest
```

```javascript
// jest.config.js
module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  roots: ['<rootDir>/src'],
  testMatch: ['**/__tests__/**/*.ts', '**/?(*.)+(spec|test).ts'],
  collectCoverageFrom: [
    'src/**/*.ts',
    '!src/**/*.d.ts',
    '!src/**/index.ts',
  ],
  coverageThreshold: {
    global: {
      branches: 70,
      functions: 80,
      lines: 80,
      statements: 80,
    },
  },
};
```

### Unit Tests for Definitions

```typescript
// src/__tests__/definitions.test.ts
import type { MyPluginPlugin, MethodOptions } from '../definitions';

describe('Type Definitions', () => {
  test('MethodOptions accepts valid parameters', () => {
    const options: MethodOptions = {
      param1: 'test',
      param2: 42,
    };

    expect(options.param1).toBe('test');
    expect(options.param2).toBe(42);
  });

  test('MethodOptions allows optional parameters', () => {
    const options: MethodOptions = {
      param1: 'test',
      // param2 is optional
    };

    expect(options.param1).toBe('test');
    expect(options.param2).toBeUndefined();
  });
});
```

### Unit Tests for Web Implementation

```typescript
// src/__tests__/web.test.ts
import { MyPluginWeb } from '../web';

describe('MyPluginWeb', () => {
  let plugin: MyPluginWeb;

  beforeEach(() => {
    plugin = new MyPluginWeb();
  });

  describe('methodName', () => {
    test('returns success with valid input', async () => {
      const result = await plugin.methodName({
        param1: 'test',
        param2: 42,
      });

      expect(result.success).toBe(true);
      expect(result.data).toBeDefined();
    });

    test('rejects with missing required parameter', async () => {
      await expect(
        plugin.methodName({
          param1: null as any,
        })
      ).rejects.toThrow('INVALID_PARAMETER');
    });

    test('handles errors gracefully', async () => {
      // Mock internal error
      jest.spyOn(plugin as any, 'performWebOperation').mockRejectedValue(
        new Error('Network error')
      );

      await expect(
        plugin.methodName({ param1: 'test' })
      ).rejects.toThrow('OPERATION_FAILED');
    });
  });
});
```

### Mocking Browser APIs

```typescript
// src/__tests__/geolocation-web.test.ts
import { GeolocationPluginWeb } from '../web';

describe('GeolocationPluginWeb', () => {
  let plugin: GeolocationPluginWeb;
  let mockGeolocation: any;

  beforeEach(() => {
    plugin = new GeolocationPluginWeb();

    // Mock navigator.geolocation
    mockGeolocation = {
      getCurrentPosition: jest.fn(),
      watchPosition: jest.fn(),
      clearWatch: jest.fn(),
    };

    Object.defineProperty(global.navigator, 'geolocation', {
      value: mockGeolocation,
      configurable: true,
    });
  });

  test('getCurrentPosition returns location', async () => {
    const mockPosition = {
      coords: {
        latitude: 37.7749,
        longitude: -122.4194,
        accuracy: 10,
        altitude: null,
        altitudeAccuracy: null,
        heading: null,
        speed: null,
      },
      timestamp: Date.now(),
    };

    mockGeolocation.getCurrentPosition.mockImplementation((success: any) => {
      success(mockPosition);
    });

    const result = await plugin.getCurrentPosition();

    expect(result.latitude).toBe(37.7749);
    expect(result.longitude).toBe(-122.4194);
    expect(result.accuracy).toBe(10);
  });

  test('getCurrentPosition handles permission denial', async () => {
    mockGeolocation.getCurrentPosition.mockImplementation((_: any, error: any) => {
      error({ code: 1, message: 'User denied geolocation' });
    });

    await expect(plugin.getCurrentPosition()).rejects.toThrow();
  });

  test('throws when geolocation API unavailable', async () => {
    Object.defineProperty(global.navigator, 'geolocation', {
      value: undefined,
      configurable: true,
    });

    await expect(plugin.getCurrentPosition()).rejects.toThrow('not available');
  });
});
```

### Testing Event Listeners

```typescript
// src/__tests__/battery-web.test.ts
import { BatteryPluginWeb } from '../web';

describe('BatteryPluginWeb - Events', () => {
  let plugin: BatteryPluginWeb;
  let mockBattery: any;

  beforeEach(() => {
    plugin = new BatteryPluginWeb();

    mockBattery = {
      level: 0.85,
      charging: false,
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
    };

    (global.navigator as any).getBattery = jest.fn().mockResolvedValue(mockBattery);
  });

  test('addListener registers event handler', async () => {
    const callback = jest.fn();

    await plugin.addListener('batteryChange', callback);

    expect(mockBattery.addEventListener).toHaveBeenCalledWith(
      'levelchange',
      expect.any(Function)
    );
  });

  test('listener receives battery updates', async () => {
    const callback = jest.fn();

    await plugin.addListener('batteryChange', callback);

    // Simulate battery change
    mockBattery.level = 0.50;
    const handler = mockBattery.addEventListener.mock.calls[0][1];
    await handler();

    expect(callback).toHaveBeenCalledWith(
      expect.objectContaining({
        level: 50,
        isCharging: false,
      })
    );
  });

  test('removeAllListeners cleans up', async () => {
    const callback = jest.fn();

    await plugin.addListener('batteryChange', callback);
    await plugin.removeAllListeners();

    expect(mockBattery.removeEventListener).toHaveBeenCalled();
  });
});
```

---

## iOS Testing (XCTest)

### Basic XCTest Setup

```swift
// ios/PluginTests/PluginTests.swift
import XCTest
@testable import Plugin

class MyPluginTests: XCTestCase {

    var plugin: MyPlugin!

    override func setUp() {
        super.setUp()
        plugin = MyPlugin()
    }

    override func tearDown() {
        plugin = nil
        super.tearDown()
    }

    func testPerformWork() {
        let result = plugin.performWork("test", 42)
        XCTAssertEqual(result, "iOS result: test 42")
    }

    func testPerformWorkWithEmptyString() {
        let result = plugin.performWork("", 0)
        XCTAssertEqual(result, "iOS result:  0")
    }
}
```

### Testing with Mock CAPPluginCall

```swift
import XCTest
import Capacitor
@testable import Plugin

class MockPluginCall: CAPPluginCall {
    var resolvedData: [String: Any]?
    var rejectedMessage: String?

    override func resolve(_ data: [String: Any]) {
        resolvedData = data
    }

    override func reject(_ message: String) {
        rejectedMessage = message
    }
}

class MyPluginTests: XCTestCase {

    func testMethodNameSuccess() {
        let plugin = MyPlugin()

        let call = MockPluginCall(
            callbackId: "test",
            options: ["param1": "test", "param2": 42],
            success: { _, _ in },
            error: { _ in }
        )

        plugin.methodName(call)

        XCTAssertNotNil(call.resolvedData)
        XCTAssertEqual(call.resolvedData?["success"] as? Bool, true)
    }

    func testMethodNameMissingParameter() {
        let plugin = MyPlugin()

        let call = MockPluginCall(
            callbackId: "test",
            options: [:],  // Missing param1
            success: { _, _ in },
            error: { _ in }
        )

        plugin.methodName(call)

        XCTAssertNotNil(call.rejectedMessage)
        XCTAssertEqual(call.rejectedMessage, "Missing param1")
    }
}
```

### Testing Async Operations

```swift
func testAsyncOperation() {
    let expectation = self.expectation(description: "Async operation completes")

    let plugin = MyPlugin()
    let call = MockPluginCall(
        callbackId: "test",
        options: ["url": "https://example.com"],
        success: { _, _ in
            expectation.fulfill()
        },
        error: { _ in
            XCTFail("Should not error")
        }
    )

    plugin.downloadFile(call)

    waitForExpectations(timeout: 5.0) { error in
        XCTAssertNil(error)
        XCTAssertNotNil(call.resolvedData)
    }
}
```

### Testing Permissions

```swift
import AVFoundation

func testCheckCameraPermission() {
    let plugin = MyPlugin()

    // Note: Actual permission testing requires UI tests on device
    // Unit tests can only verify logic flow

    let call = MockPluginCall(
        callbackId: "test",
        options: [:],
        success: { _, _ in },
        error: { _ in }
    )

    plugin.checkPermissions(call)

    XCTAssertNotNil(call.resolvedData)
    XCTAssertNotNil(call.resolvedData?["camera"])
}
```

---

## Android Testing (JUnit)

### Basic JUnit Setup

```kotlin
// android/src/test/java/com/company/plugin/MyPluginTest.kt
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import com.company.plugin.MyPlugin

class MyPluginTest {

    private lateinit var plugin: MyPlugin

    @Before
    fun setUp() {
        plugin = MyPlugin()
    }

    @Test
    fun testPerformWork() {
        val result = plugin.performWork("test", 42)
        assertEquals("Android result: test 42", result)
    }

    @Test
    fun testPerformWorkWithEmptyString() {
        val result = plugin.performWork("", 0)
        assertEquals("Android result:  0", result)
    }
}
```

### Testing with Mock PluginCall

```kotlin
import org.junit.Test
import org.mockito.Mockito.*
import com.getcapacitor.PluginCall
import com.getcapacitor.JSObject

class MyPluginTest {

    @Test
    fun testMethodNameSuccess() {
        val plugin = MyPlugin()

        val call = mock(PluginCall::class.java)
        `when`(call.getString("param1")).thenReturn("test")
        `when`(call.getInt("param2", 0)).thenReturn(42)

        plugin.methodName(call)

        verify(call).resolve(argThat { result ->
            result.getBoolean("success") == true
        })
    }

    @Test
    fun testMethodNameMissingParameter() {
        val plugin = MyPlugin()

        val call = mock(PluginCall::class.java)
        `when`(call.getString("param1")).thenReturn(null)

        plugin.methodName(call)

        verify(call).reject("Missing param1")
    }
}
```

### Testing with Robolectric (Android Context)

```kotlin
// build.gradle
testImplementation 'org.robolectric:robolectric:4.10'

// Test class
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MyPluginContextTest {

    @Test
    fun testFileOperations() {
        val context = RuntimeEnvironment.getApplication()
        val plugin = MyPlugin()
        // Inject context if needed

        // Test file operations that require Android context
        val file = File(context.filesDir, "test.txt")
        file.writeText("test content")

        assertTrue(file.exists())
        assertEquals("test content", file.readText())

        file.delete()
    }
}
```

### Testing Permissions

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
class PermissionTest {

    @Test
    fun testCheckPermissionGranted() {
        val app = RuntimeEnvironment.getApplication()
        val shadowApp = shadowOf(app)

        // Grant permission
        shadowApp.grantPermissions(Manifest.permission.CAMERA)

        val plugin = MyPlugin()
        val call = mock(PluginCall::class.java)

        plugin.checkPermissions(call)

        verify(call).resolve(argThat { result ->
            result.getString("camera") == "granted"
        })
    }
}
```

---

## Integration Testing

### End-to-End Flow Testing

```typescript
// e2e/plugin.test.ts
import { MyPlugin } from '@company/capacitor-myplugin';

describe('MyPlugin Integration', () => {
  test('full workflow', async () => {
    // 1. Check availability
    const available = await MyPlugin.isAvailable();
    expect(available.available).toBe(true);

    // 2. Check permissions
    let permissions = await MyPlugin.checkPermissions();
    if (permissions.camera !== 'granted') {
      permissions = await MyPlugin.requestPermissions();
    }
    expect(permissions.camera).toBe('granted');

    // 3. Use feature
    const result = await MyPlugin.methodName({
      param1: 'integration test',
      param2: 99,
    });

    expect(result.success).toBe(true);
    expect(result.data).toBeDefined();
  });
});
```

### Testing with the Example App

The `create-capacitor-plugin` template includes an **example-app** directory - a real Capacitor application for testing your plugin in a realistic environment.

#### Setup Example App

```bash
# 1. Build your plugin
npm run build

# 2. Set up example app
cd example-app
npm install
npx cap sync
```

#### Implement Example Usage

Create realistic usage examples in the example app:

```typescript
// example-app/src/js/example.js (or similar entry file)
import { MyPlugin } from '@company/capacitor-myplugin';

// Example 1: Basic usage
async function testBasicFeature() {
  console.log('Testing basic feature...');

  try {
    const result = await MyPlugin.methodName({ param: 'test' });
    console.log('✅ Success:', result);

    // Update UI to show result
    document.getElementById('result').textContent = JSON.stringify(result, null, 2);
  } catch (error) {
    console.error('❌ Error:', error);
    document.getElementById('error').textContent = error.message;
  }
}

// Example 2: Permission flow
async function testPermissions() {
  console.log('Testing permissions...');

  // Check current status
  let status = await MyPlugin.checkPermissions();
  console.log('Current permission status:', status);

  // Request if needed
  if (status.camera !== 'granted') {
    console.log('Requesting permission...');
    status = await MyPlugin.requestPermissions();
  }

  if (status.camera === 'granted') {
    console.log('✅ Permission granted, calling feature...');
    await testBasicFeature();
  } else {
    console.log('❌ Permission denied');
  }
}

// Example 3: Event listeners
async function testEvents() {
  console.log('Testing event listeners...');

  const listener = await MyPlugin.addListener('dataUpdate', (data) => {
    console.log('📡 Event received:', data);
    document.getElementById('events').textContent += JSON.stringify(data) + '\n';
  });

  // Start monitoring
  await MyPlugin.startMonitoring();

  // Clean up after 10 seconds
  setTimeout(async () => {
    await MyPlugin.stopMonitoring();
    await listener.remove();
    console.log('🛑 Stopped monitoring');
  }, 10000);
}

// Add buttons to trigger tests
document.getElementById('btnBasic').addEventListener('click', testBasicFeature);
document.getElementById('btnPermissions').addEventListener('click', testPermissions);
document.getElementById('btnEvents').addEventListener('click', testEvents);
```

#### Update Example App HTML

```html
<!-- example-app/index.html -->
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <title>My Plugin Example</title>
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
</head>
<body>
  <h1>My Plugin Test App</h1>

  <section>
    <h2>Basic Tests</h2>
    <button id="btnBasic">Test Basic Feature</button>
    <button id="btnPermissions">Test Permissions</button>
    <button id="btnEvents">Test Events</button>
  </section>

  <section>
    <h3>Result:</h3>
    <pre id="result"></pre>
  </section>

  <section>
    <h3>Errors:</h3>
    <pre id="error"></pre>
  </section>

  <section>
    <h3>Events:</h3>
    <pre id="events"></pre>
  </section>

  <script type="module" src="src/js/example.js"></script>
</body>
</html>
```

#### Test on Platforms

```bash
# iOS
cd example-app
npx cap open ios
# In Xcode:
# 1. Select a device/simulator
# 2. Click Run
# 3. Open Safari DevTools → Develop → [Device] → [App] to see console logs
# 4. Test all buttons/features
# 5. Check Xcode console for native logs

# Android
npx cap open android
# In Android Studio:
# 1. Select a device/emulator
# 2. Click Run
# 3. Open Chrome DevTools → chrome://inspect to see console logs
# 4. Test all buttons/features
# 5. Check Logcat for native logs
```

#### Example App Workflow

```bash
# Complete test cycle
npm run build              # Build plugin
cd example-app
npm install                # Install dependencies
npx cap sync               # Sync plugin to native projects
npx cap open ios           # Test on iOS
npx cap open android       # Test on Android
```

#### What to Test in Example App

**Functional Testing:**
- ✅ All public plugin methods work
- ✅ Correct return values and data types
- ✅ Error handling (invalid params, permission denied)
- ✅ Event listeners fire correctly
- ✅ Permissions flow works on both platforms
- ✅ Platform-specific behavior

**Real-World Scenarios:**
- ✅ App backgrounding/foregrounding
- ✅ Network connectivity changes
- ✅ Low memory conditions
- ✅ Multiple rapid calls
- ✅ Concurrent operations

**Platform Differences:**
- ✅ iOS-specific behavior
- ✅ Android-specific behavior
- ✅ Different OS versions

#### Debugging in Example App

**iOS (Safari DevTools):**
```bash
# 1. Enable Web Inspector in iOS Settings → Safari → Advanced
# 2. Connect device and run app
# 3. Safari → Develop → [Device Name] → [App]
# 4. Console shows JavaScript logs
# 5. Xcode Console shows native Swift logs
```

**Android (Chrome DevTools):**
```bash
# 1. Enable USB Debugging on device
# 2. Connect device and run app
# 3. Chrome → chrome://inspect
# 4. Click "inspect" under your app
# 5. Console shows JavaScript logs
# 6. Android Studio Logcat shows native Kotlin logs
```

#### Keeping Example App Updated

```bash
# After modifying plugin
npm run build              # Build plugin changes
cd example-app
npx cap sync               # Sync updated plugin
npx cap copy               # Copy web assets
# Re-run on devices to test changes
```

**Best Practices:**
- 📝 Document expected behavior in example app
- 🔄 Update examples when adding new features
- 🐛 Add examples for bug reproductions
- 📸 Include screenshots of expected UI
- ✅ Use example app for manual testing before releases

### Testing with Appium (Mobile)

```typescript
// e2e/mobile.test.ts
import { remote } from 'webdriverio';

describe('Mobile Plugin Tests', () => {
  let driver: any;

  beforeAll(async () => {
    driver = await remote({
      capabilities: {
        platformName: 'iOS',
        platformVersion: '15.0',
        deviceName: 'iPhone 13',
        app: '/path/to/app.app',
        automationName: 'XCUITest',
      },
    });
  });

  afterAll(async () => {
    await driver.deleteSession();
  });

  test('camera permission flow', async () => {
    // Tap button to trigger camera
    const button = await driver.$('~cameraButton');
    await button.click();

    // Handle system permission alert
    const alert = await driver.getAlertText();
    expect(alert).toContain('access the camera');

    await driver.acceptAlert();

    // Verify camera opened
    const camera = await driver.$('~cameraView');
    expect(await camera.isDisplayed()).toBe(true);
  });
});
```

---

## Test Coverage

### Generate Coverage Reports

```json
// package.json
{
  "scripts": {
    "test": "jest",
    "test:coverage": "jest --coverage",
    "test:watch": "jest --watch"
  }
}
```

```bash
# Run with coverage
npm run test:coverage

# Output:
# ----------------------|---------|----------|---------|---------|
# File                  | % Stmts | % Branch | % Funcs | % Lines |
# ----------------------|---------|----------|---------|---------|
# All files             |   85.71 |    83.33 |   87.50 |   85.71 |
#  src                  |   90.00 |    85.00 |   92.31 |   90.00 |
#   definitions.ts      |  100.00 |   100.00 |  100.00 |  100.00 |
#   index.ts            |  100.00 |   100.00 |  100.00 |  100.00 |
#   web.ts              |   87.50 |    80.00 |   90.00 |   87.50 |
# ----------------------|---------|----------|---------|---------|
```

---

## Testing Checklist

### TypeScript/Web Layer
- [ ] Unit tests for all public methods
- [ ] Test parameter validation
- [ ] Test error handling
- [ ] Mock browser APIs
- [ ] Test event listeners
- [ ] Achieve 80%+ code coverage

### iOS Layer
- [ ] XCTest for plugin logic
- [ ] Test parameter extraction
- [ ] Test success/error flows
- [ ] Test permissions (on device)
- [ ] Test UI interactions (UITests)

### Android Layer
- [ ] JUnit for plugin logic
- [ ] Test with Mockito for PluginCall
- [ ] Test with Robolectric for Android APIs
- [ ] Test permissions (on device)
- [ ] Test activity results

### Integration
- [ ] Test full web-to-native flow
- [ ] Test on real iOS device
- [ ] Test on real Android device
- [ ] Test permission workflows
- [ ] Test error recovery
- [ ] Test platform-specific behavior

---

## Continuous Integration

### GitHub Actions Example (Recommended)

**Use Capacitor's built-in commands for CI:**

```yaml
# .github/workflows/ci.yml
name: CI

on: [push, pull_request]

jobs:
  quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '20'
      - run: npm ci

      # Lint check (fail on style violations)
      - name: Lint
        run: npm run lint

      # Run unit tests with coverage
      - name: Test
        run: npm test

      # Verify all platforms build
      - name: Verify
        run: npm run verify

      # Upload coverage
      - uses: codecov/codecov-action@v3
        if: success()
```

### Separate Platform Jobs (Alternative)

If you need more control over platform-specific builds:

```yaml
# .github/workflows/test.yml
name: Test

on: [push, pull_request]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '20'
      - run: npm ci
      - run: npm run lint

  test-web:
    runs-on: ubuntu-latest
    needs: lint
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '20'
      - run: npm ci
      - run: npm run build
      - run: npm test
      - uses: codecov/codecov-action@v3

  verify-ios:
    runs-on: macos-latest
    needs: lint
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '20'
      - run: npm ci
      # Uses Swift Package Manager (no pod install needed!)
      - run: swift build -c release
      - run: swift test

  verify-android:
    runs-on: ubuntu-latest
    needs: lint
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '20'
      - uses: actions/setup-java@v3
        with:
          distribution: 'zulu'
          java-version: '17'
      - run: npm ci
      - run: cd android && ./gradlew clean build test
```

### Pre-commit Hook (Local)

Add to `.husky/pre-commit` or use `lint-staged`:

```bash
#!/bin/sh
# .husky/pre-commit

# Format code automatically
npm run fmt

# Verify builds before committing
npm run verify

# Lint check
npm run lint
```

---

## Best Practices

### DO:
- ✅ **Run `npm run fmt` before every commit** - Auto-format code
- ✅ **Run `npm run verify` before pushing** - Ensure all platforms build
- ✅ **Run `npm run lint` in CI/CD** - Enforce code quality
- ✅ Write tests before fixing bugs (TDD)
- ✅ Test happy paths and error cases
- ✅ Mock external dependencies
- ✅ Test on real devices for permissions and hardware
- ✅ Maintain high code coverage (80%+)
- ✅ Use descriptive test names
- ✅ Keep tests fast and isolated
- ✅ Run tests in CI/CD pipeline
- ✅ Set up pre-commit hooks for `fmt` and `lint`

### DON'T:
- ❌ **Skip `verify` before committing** - Catch build errors early
- ❌ **Commit without running `fmt`** - Inconsistent style causes review friction
- ❌ **Ignore lint warnings** - They catch real issues
- ❌ Skip testing error handling
- ❌ Test only on simulators/emulators
- ❌ Write flaky tests
- ❌ Test implementation details
- ❌ Ignore failing tests
- ❌ Skip integration tests
- ❌ Forget to test platform differences

---

## Complete Development Workflow

Here's the **recommended end-to-end workflow** for plugin development, integrating all quality checks:

### Day-to-Day Development

```bash
# 1. Start working on a feature
git checkout -b feature/new-capability

# 2. Write code (TypeScript, Swift, Kotlin)
# ... make changes ...

# 3. Format code automatically
npm run fmt

# 4. Run unit tests
npm test

# 5. Verify all platforms build
npm run verify
# This runs:
#   - verify:web (TypeScript compilation)
#   - verify:ios (Swift build + tests)
#   - verify:android (Gradle build + tests)

# 6. Lint check for any remaining issues
npm run lint

# 7. If all pass, commit
git add .
git commit -m "feat: add new capability"

# 8. Push and create PR
git push origin feature/new-capability
```

### Before Creating a Pull Request

```bash
# Run full verification suite
npm run verify

# Ensure code is formatted
npm run fmt

# Double-check linting
npm run lint

# Run tests with coverage
npm test -- --coverage

# Test with example-app on real devices
npm run build
cd example-app
npx cap sync
npx cap open ios      # Test on iOS
npx cap open android  # Test on Android
cd ..
```

### Before Publishing to npm

```bash
# 1. Ensure everything is committed
git status

# 2. Run full quality check
npm run verify && npm run lint

# 3. Bump version
npm version patch  # or minor, or major

# 4. Build for distribution
npm run build

# 5. Publish
npm publish

# 6. Tag and push
git push && git push --tags
```

### Fixing Lint/Build Errors

```bash
# If lint fails, try auto-fixing first
npm run fmt

# Re-run verify to see if it fixed the issue
npm run verify

# Check what specific errors remain
npm run lint

# If iOS build fails
swift build  # See detailed error
swift test   # Run tests

# If Android build fails
cd android && ./gradlew build --info

# If TypeScript fails
npm run build
```

### Setting Up Pre-commit Hooks

Install Husky for automatic checks:

```bash
npm install --save-dev husky
npx husky install

# Add pre-commit hook
npx husky add .husky/pre-commit "npm run fmt && npm run verify && npm run lint"
```

Now `fmt`, `verify`, and `lint` run automatically before every commit!

---

## Summary

**Testing Strategy**:

1. **Quality Commands** (use these daily!)
   - `npm run fmt` - Auto-format code
   - `npm run verify` - Build all platforms
   - `npm run lint` - Check code style

2. **Unit Tests** (80% coverage)
   - TypeScript: Jest
   - iOS: XCTest
   - Android: JUnit

3. **Example App Testing**
   - Use included `example-app` for realistic integration testing
   - Implement working examples of all plugin features
   - Test on real devices (iOS/Android)
   - Verify permissions, events, and error handling

4. **Integration Tests**
   - Web-to-native bridge
   - Permission flows
   - Error handling

5. **E2E Tests**
   - Real device testing with example-app
   - User workflows
   - Platform-specific behavior

6. **CI/CD**
   - Run `lint` to fail fast
   - Run `verify` to build all platforms
   - Coverage reporting
   - Platform-specific builds

**Essential Commands**:
```bash
npm run fmt      # Format → Run before commit
npm run verify   # Build all → Run before push
npm run lint     # Check → Run in CI/CD
```

**Remember**: Use `fmt` + `verify` + `lint` religiously. Good tests catch bugs early and give confidence when refactoring!

---

## Local Linking

To test the generated plugin from a sample app:

```bash
cd <plugin-root>
npm install
npm run build

cd <sample-app>
npm install
npm install ../<plugin-root>
npx cap sync
```

For web-only sample validation:

```bash
npm start
```

For native validation:

```bash
npx cap run ios
npx cap run android
```

## Capacitor CLI Workflow

Use these commands intentionally:

| Command | When to use |
| --- | --- |
| `npx cap add ios` / `npx cap add android` | Add a native platform to a sample app that does not already have it. |
| `npx cap sync` | After installing the local plugin, changing native plugin code, changing dependencies, editing Capacitor config, or rebuilding web assets for native testing. |
| `npx cap copy` | After web-only sample app changes when native dependencies/config did not change. |
| `npx cap open ios` / `npx cap open android` | Open the native IDE for simulator/device selection, signing, Gradle sync, or manual platform inspection. |
| `npx cap run ios` / `npx cap run android` | Build and run the sample app on a simulator, emulator, or connected device. |

If generated native code, plugin metadata, permissions, dependencies, or config
changed, prefer `npx cap sync` over `npx cap copy`.

## Hooks

Capacitor 6.1+ lets plugins hook into CLI commands by adding scripts to
`package.json`:

```json
{
  "scripts": {
    "capacitor:copy:before": "node scripts/before-copy.js",
    "capacitor:copy:after": "node scripts/after-copy.js",
    "capacitor:update:before": "node scripts/before-update.js",
    "capacitor:update:after": "node scripts/after-update.js",
    "capacitor:sync:before": "node scripts/before-sync.js",
    "capacitor:sync:after": "node scripts/after-sync.js"
  }
}
```

Available hook events:

| Event | Triggered |
| --- | --- |
| `capacitor:copy:before` | Before `npx cap copy` copies web assets. |
| `capacitor:copy:after` | After `npx cap copy` completes. |
| `capacitor:update:before` | Before `npx cap update` updates native dependencies. |
| `capacitor:update:after` | After `npx cap update` completes. |
| `capacitor:sync:before` | Before `npx cap sync` (which runs copy + update). |
| `capacitor:sync:after` | After `npx cap sync` completes. |

The `$CAPACITOR_PLATFORM_NAME` environment variable is set in hook scripts and
contains the platform being processed (`android` or `ios`). Prefer npm scripts
over Cordova-style plugin hooks.

## Coverage Targets by Layer

Pragmatic targets a candidate plugin can aim for. Higher is better, but the
numbers below reflect what's realistic given the bridge constraints:

| Layer                  | Tooling             | Coverage target |
| ---                    | ---                 | ---             |
| TypeScript API         | Jest                | 80%+            |
| Web implementation     | Jest + JSDOM        | 70%+            |
| iOS native             | XCTest              | 60%+            |
| Android native         | JUnit / Robolectric | 60%+            |
| End-to-end integration | Sample app + Detox  | Key flows only  |

Native targets are lower because UI framework code (UIKit, Activity lifecycle)
is hard to cover in unit tests. Push business logic into plain classes (per the
Testability Guidelines in `architecture-patterns.md`) so the non-UI portion can
clear the 80%+ bar.

## Manual Validation Notes

Some plugin categories require real devices, credentials, or store-facing
configuration. Mark those as manual review items rather than claiming full
runtime correctness — see the Candidate Output Rule in `architecture-patterns.md`.
