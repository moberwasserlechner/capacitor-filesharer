# Configuration and Build Setup

Complete guide for configuring Capacitor plugins including build systems, dependencies, and project structure.

## Package Configuration

### package.json

```json
{
  "name": "@company/capacitor-myplugin",
  "version": "1.0.0",
  "description": "Capacitor plugin for [feature]",
  "main": "dist/plugin.cjs.js",
  "module": "dist/esm/index.js",
  "types": "dist/esm/index.d.ts",
  "unpkg": "dist/plugin.js",
  "files": [
    "android/",
    "dist/",
    "ios/",
    "src/",
    "*.md"
  ],
  "author": "Your Name",
  "license": "MIT",
  "repository": {
    "type": "git",
    "url": "https://github.com/company/capacitor-myplugin"
  },
  "bugs": {
    "url": "https://github.com/company/capacitor-myplugin/issues"
  },
  "keywords": [
    "capacitor",
    "plugin",
    "native",
    "ios",
    "android"
  ],
  "scripts": {
    "verify": "npm run verify:ios && npm run verify:android && npm run verify:web",
    "verify:ios": "swift build -c release --package-path . && swift test --package-path .",
    "verify:android": "cd android && ./gradlew clean build test",
    "verify:web": "npm run build",
    "lint": "npm run eslint && npm run prettier -- --check && npm run swiftlint -- lint",
    "fmt": "npm run eslint -- --fix && npm run prettier -- --write && npm run swiftlint -- --fix --format",
    "eslint": "eslint . --ext ts",
    "prettier": "prettier \"**/*.{css,html,ts,js,java}\"",
    "swiftlint": "node-swiftlint",
    "docgen": "docgen --api MyPluginPlugin --output-readme README.md --output-json dist/docs.json",
    "build": "npm run clean && npm run docgen && tsc && rollup -c rollup.config.js",
    "clean": "rimraf ./dist",
    "watch": "tsc --watch",
    "prepublishOnly": "npm run build"
  },
  "devDependencies": {
    "@capacitor/android": "^6.0.0",
    "@capacitor/core": "^6.0.0",
    "@capacitor/docgen": "^0.2.0",
    "@capacitor/ios": "^6.0.0",
    "@ionic/eslint-config": "^0.3.0",
    "@ionic/prettier-config": "^1.0.1",
    "@ionic/swiftlint-config": "^1.1.2",
    "eslint": "^7.11.0",
    "prettier": "~2.3.0",
    "prettier-plugin-java": "~1.0.2",
    "rimraf": "^3.0.2",
    "rollup": "^2.32.0",
    "swiftlint": "^1.0.1",
    "typescript": "~4.1.5"
  },
  "peerDependencies": {
    "@capacitor/core": "^6.0.0"
  },
  "prettier": "@ionic/prettier-config",
  "swiftlint": "@ionic/swiftlint-config",
  "eslintConfig": {
    "extends": "@ionic/eslint-config/recommended"
  },
  "capacitor": {
    "ios": {
      "src": "ios"
    },
    "android": {
      "src": "android"
    }
  }
}
```

### Understanding the Quality Scripts

The Capacitor plugin template includes essential quality scripts:

#### `npm run verify`

Verifies all platforms build successfully. **Run this before committing!**

```bash
npm run verify
```

Runs three sub-commands:
- `verify:web` - Compiles TypeScript
- `verify:ios` - Builds Swift package and runs tests (`swift build && swift test`)
- `verify:android` - Runs Gradle build and tests (`./gradlew clean build test`)

**When to use:**
- Before committing code
- Before creating a pull request
- Before publishing to npm
- After adding native code
- In CI/CD pipelines

#### `npm run fmt`

Auto-formats all code according to Capacitor conventions. **Run before every commit!**

```bash
npm run fmt
```

Formats:
- TypeScript/JavaScript (ESLint with `--fix`, Prettier with `--write`)
- Swift (SwiftLint with `--fix`)
- Java/Kotlin (Prettier)

**When to use:**
- Before committing (make it a habit!)
- After writing new code
- To fix lint errors automatically

#### `npm run lint`

Checks code style without modifying files. **Run in CI/CD to enforce quality!**

```bash
npm run lint
```

Checks:
- TypeScript/JavaScript (ESLint, Prettier with `--check`)
- Swift (SwiftLint)
- Reports errors but doesn't fix them

**When to use:**
- In CI/CD pipelines (fail build on errors)
- Before final code review
- To verify code quality

#### Recommended Workflow

```bash
# 1. Make changes
# ... edit code ...

# 2. Format
npm run fmt

# 3. Build
npm run build

# 4. Test
npm test

# 5. Verify all platforms
npm run verify

# 6. Lint check
npm run lint

# 7. Commit if all pass
git commit -m "feat: add feature"
```

---

## iOS Configuration (Swift Package Manager)

### Package.swift

**Swift Package Manager is the recommended approach for iOS plugins.**

```swift
// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "MyPlugin",
    platforms: [.iOS(.v15)],
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

### With External Dependencies

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
                "Alamofire"
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

### Legacy: Podspec (Optional Fallback)

**Note:** CocoaPods support is maintained for backward compatibility, but Swift Package Manager is preferred.

```ruby
# MyPlugin.podspec (optional, for CocoaPods users)
require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name = 'MyPlugin'
  s.version = package['version']
  s.summary = package['description']
  s.license = package['license']
  s.homepage = package['repository']['url']
  s.author = package['author']
  s.source = { :git => package['repository']['url'], :tag => s.version.to_s }
  s.source_files = 'ios/Sources/MyPlugin/**/*.swift'
  s.ios.deployment_target = '15.0'
  s.swift_version = '5.9'
  s.dependency 'Capacitor', '~> 6.0'
  s.dependency 'CapacitorCordova', '~> 6.0'
end
```

### iOS Project Structure (Modern)

```
ios/
├── Sources/
│   └── MyPlugin/
│       ├── MyPlugin.swift           # Implementation class
│       └── MyPluginPlugin.swift     # Plugin bridge class
└── Tests/
    └── MyPluginTests/
        └── MyPluginTests.swift      # Unit tests
```

**Note**: No Xcode project or Podfile needed with Swift Package Manager!

### Info.plist Permissions

**Note**: These permissions are configured in the **consuming app's** Info.plist, not in the plugin itself.

```xml
<!-- ios/Plugin/Info.plist -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- Camera -->
    <key>NSCameraUsageDescription</key>
    <string>This app needs camera access to take photos</string>

    <!-- Photo Library -->
    <key>NSPhotoLibraryUsageDescription</key>
    <string>This app needs photo library access to select photos</string>
    <key>NSPhotoLibraryAddUsageDescription</key>
    <string>This app needs photo library access to save photos</string>

    <!-- Location -->
    <key>NSLocationWhenInUseUsageDescription</key>
    <string>This app needs location access when in use</string>
    <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
    <string>This app needs location access always</string>

    <!-- Microphone -->
    <key>NSMicrophoneUsageDescription</key>
    <string>This app needs microphone access to record audio</string>

    <!-- Contacts -->
    <key>NSContactsUsageDescription</key>
    <string>This app needs contacts access</string>

    <!-- Calendar -->
    <key>NSCalendarsUsageDescription</key>
    <string>This app needs calendar access</string>

    <!-- Motion -->
    <key>NSMotionUsageDescription</key>
    <string>This app needs motion sensor access</string>

    <!-- Bluetooth -->
    <key>NSBluetoothAlwaysUsageDescription</key>
    <string>This app needs Bluetooth access</string>
</dict>
</plist>
```

---

## Android Configuration

### build.gradle

```gradle
// android/build.gradle
ext {
    javaVersion = JavaVersion.VERSION_11
    androidxAppCompatVersion = '1.6.1'
    androidxCoreVersion = '1.10.1'
}

buildscript {
    ext.kotlin_version = '1.8.20'
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.0.0'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

apply plugin: 'com.android.library'
apply plugin: 'kotlin-android'

android {
    namespace "com.company.plugin"
    compileSdkVersion 33

    defaultConfig {
        minSdkVersion 22
        targetSdkVersion 33
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = '11'
    }

    lintOptions {
        abortOnError false
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation project(':capacitor-android')

    // AndroidX
    implementation "androidx.appcompat:appcompat:$androidxAppCompatVersion"
    implementation "androidx.core:core-ktx:$androidxCoreVersion"

    // Kotlin
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"

    // Add your dependencies
    // implementation 'com.some:library:1.0.0'

    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

### AndroidManifest.xml

```xml
<!-- android/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.company.plugin">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
                     android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <!-- Features (optional) -->
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-feature android:name="android.hardware.location.gps" android:required="false" />
    <uses-feature android:name="android.hardware.microphone" android:required="false" />

</manifest>
```

### ProGuard Rules

```proguard
# android/proguard-rules.pro

# Keep plugin classes
-keep class com.company.plugin.** { *; }

# Keep Capacitor classes
-keep class com.getcapacitor.** { *; }

# Keep JSObject
-keepclassmembers class com.getcapacitor.JSObject { *; }

# Keep plugin methods annotated with @PluginMethod
-keepclassmembers class * extends com.getcapacitor.Plugin {
    @com.getcapacitor.annotation.PluginMethod <methods>;
}
```

---

## TypeScript Configuration

### tsconfig.json

```json
{
  "compilerOptions": {
    "module": "ES2020",
    "moduleResolution": "node",
    "target": "ES2017",
    "lib": ["ES2020", "DOM"],
    "outDir": "dist/esm",
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true,
    "strict": true,
    "noImplicitAny": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "**/*.spec.ts", "**/__tests__/**"]
}
```

### Rollup Configuration

```javascript
// rollup.config.js
import nodeResolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';

export default {
  input: 'dist/esm/index.js',
  output: [
    {
      file: 'dist/plugin.js',
      format: 'iife',
      name: 'capacitorMyPlugin',
      globals: {
        '@capacitor/core': 'capacitorExports',
      },
      sourcemap: true,
      inlineDynamicImports: true,
    },
    {
      file: 'dist/plugin.cjs.js',
      format: 'cjs',
      sourcemap: true,
      inlineDynamicImports: true,
    },
  ],
  external: ['@capacitor/core'],
  plugins: [
    nodeResolve({
      browser: true,
    }),
    commonjs(),
  ],
};
```

---

## Plugin Registration in Apps

### Install Plugin

```bash
# From npm
npm install @company/capacitor-myplugin

# Or locally during development
npm install /path/to/plugin

# Sync with native projects
npx cap sync
```

### Import and Use

```typescript
// src/main.ts or similar
import { MyPlugin } from '@company/capacitor-myplugin';

// Use plugin
const result = await MyPlugin.methodName({ param1: 'value' });
```

### iOS Project Integration

After installation, the plugin is automatically added via CocoaPods:

```bash
cd ios/App
pod install
```

### Android Project Integration

After installation, the plugin is automatically added to Gradle:

```gradle
// android/app/build.gradle (automatically added by Capacitor)
dependencies {
    implementation project(':company-capacitor-myplugin')
}
```

---

## Documentation Generation

### Using @capacitor/docgen

```bash
npm install --save-dev @capacitor/docgen

# Add to package.json scripts
"docgen": "docgen --api MyPluginPlugin --output-readme README.md"

# Run
npm run docgen
```

### API Documentation Format

```typescript
/**
 * MyPlugin provides native functionality for [feature]
 *
 * @since 1.0.0
 */
export interface MyPluginPlugin {
  /**
   * Perform operation
   *
   * @param options - Configuration options
   * @returns Promise resolving to result
   * @throws {Error} PERMISSION_DENIED if permission not granted
   * @throws {Error} UNAVAILABLE if feature not available
   *
   * @since 1.0.0
   *
   * @example
   * ```typescript
   * const result = await MyPlugin.methodName({
   *   param1: 'value'
   * });
   * ```
   */
  methodName(options: MethodOptions): Promise<MethodResult>;
}
```

---

## Environment-Specific Configuration

### Development vs Production

```typescript
// src/web.ts
export class MyPluginWeb extends WebPlugin {
  private isDevelopment = process.env.NODE_ENV === 'development';

  async methodName(options: MethodOptions): Promise<MethodResult> {
    if (this.isDevelopment) {
      console.log('[MyPlugin] methodName called:', options);
    }

    // Implementation
  }
}
```

### Platform Detection

```typescript
import { Capacitor } from '@capacitor/core';

// Check platform
const platform = Capacitor.getPlatform(); // 'ios', 'android', or 'web'
const isNative = Capacitor.isNativePlatform();

// Platform-specific code
if (platform === 'ios') {
  // iOS-specific
} else if (platform === 'android') {
  // Android-specific
} else {
  // Web-specific
}
```

---

## Versioning and Publishing

### Semantic Versioning

- **MAJOR** (1.0.0 → 2.0.0): Breaking changes
- **MINOR** (1.0.0 → 1.1.0): New features
- **PATCH** (1.0.0 → 1.0.1): Bug fixes

### Version Update Checklist

```bash
# 1. Update version in package.json
npm version patch  # or minor, or major

# 2. Update CHANGELOG.md with changes

# 3. Build and test
npm run build
npm run verify

# 4. Commit changes
git commit -am "chore: release v1.0.1"
git tag v1.0.1

# 5. Publish to npm
npm publish

# 6. Push to GitHub
git push && git push --tags
```

### .npmignore

```
# .npmignore
*.log
*.orig
.DS_Store
.idea/
.vscode/
coverage/
node_modules/
src/
test/
tests/
.editorconfig
.gitignore
tsconfig.json
rollup.config.js
```

---

## Troubleshooting

### Common Build Issues

| Issue | Solution |
|-------|----------|
| iOS build fails | Run `swift build` or check Package.swift |
| Android build fails | Verify Gradle version compatibility |
| TypeScript errors | Check tsconfig.json and dependencies |
| Plugin not found in app | Run `npx cap sync` |
| Methods not available | Check CAPBridgedPlugin pluginMethods array / @CapacitorPlugin |

### Clean Build

```bash
# TypeScript
npm run clean && npm run build

# iOS (Swift Package Manager)
swift build -c release
swift test

# Android
cd android && ./gradlew clean build

# Capacitor sync
npx cap sync
```

---

## Summary

**Configuration Checklist**:

- [ ] Configure package.json with correct metadata
- [ ] Set up TypeScript compilation (tsconfig.json)
- [ ] Configure iOS (Package.swift for SPM, optional Podspec for backward compat)
- [ ] Configure Android (build.gradle, AndroidManifest.xml)
- [ ] Add build scripts (verify, lint, test)
- [ ] Configure documentation generation
- [ ] Set up version management
- [ ] Test installation in sample app
- [ ] Verify all platforms build successfully

**Remember**: Use Swift Package Manager for iOS! Proper configuration ensures smooth development and easy integration for users!

---

## Runtime Plugin Configuration

Beyond the build/package configuration above, plugins can expose **runtime
configuration values** that app developers set in their Capacitor config
file (`capacitor.config.ts` or `capacitor.config.json`). These are read-only
values available at plugin load time.

### App Developer Configuration

App developers configure plugin values under `plugins.<PluginJSName>` in their
Capacitor config:

```json
{
  "plugins": {
    "Example": {
      "style": "dark",
      "maxRetries": 3,
      "iconColor": "#FF0000"
    }
  }
}
```

Or in TypeScript:

```typescript
import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    Example: {
      style: 'dark',
      maxRetries: 3,
      iconColor: '#FF0000',
    },
  },
};

export default config;
```

### Type Definitions for Plugin Config

Extend the `PluginsConfig` interface from `@capacitor/cli` so app developers
get autocomplete and type checking:

```typescript
/// <reference types="@capacitor/cli" />

declare module '@capacitor/cli' {
  export interface PluginsConfig {
    Example?: {
      /**
       * Enables verbose native logging.
       *
       * @default false
       * @since 1.0.0
       */
      debug?: boolean;
    };
  }
}
```

Add this declaration to `src/definitions.ts` or a separate `src/config.ts`
file that is re-exported from `src/index.ts`.

### Type Resolution

Module augmentation only resolves at build time when the augmented module is
actually installed. When the generated TypeScript declares
`declare module '<name>'` or uses a `/// <reference types="<name>" />`
triple-slash directive:

- Add the augmented package to `devDependencies` in `package.json`. For
  Capacitor plugin configuration types this is `@capacitor/cli`.
- For triple-slash references, ensure the same package is reachable via
  `tsconfig.json` `compilerOptions.types` or `typeRoots`. Most templates do
  not need an explicit `types` array because TypeScript discovers
  `node_modules/@types` automatically — the install is what matters.
- TypeScript will fail with `Cannot find type definition file for '<name>'`
  or `Invalid module name in augmentation, module '<name>' cannot be found`
  when the augmented module is not installed at build time.

This rule applies to any module augmentation, not just `@capacitor/cli`.

### Reading Configuration Values

iOS:

```swift
let style = getConfig().getString("style") ?? "light"
let maxRetries = getConfig().getInt("maxRetries") ?? 3
let iconColor = getConfig().getString("iconColor") ?? "#000000"
```

Android:

```java
String style = getConfig().getString("style", "light");
int maxRetries = getConfig().getInt("maxRetries", 3);
String iconColor = getConfig().getString("iconColor", "#000000");
```

### Rules

- Configuration values are **optional**. Plugin consumers may not provide
  any configuration. Always supply default values.
- Configuration values are **not validated** by Capacitor. Plugin consumers
  can pass invalid data. Handle gracefully.
- Document all configuration options, their types, defaults, and valid
  values in the plugin README.
- Do not use config for per-call options; use method options interfaces
  instead (per `api-design.md`).
- Keep defaults identical across web, iOS, and Android.
- If a config key affects only one platform, document platform availability.
