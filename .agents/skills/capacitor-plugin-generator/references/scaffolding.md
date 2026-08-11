# Scaffolding

Use the official Capacitor plugin generator as the starting point unless the
user is updating an already-scaffolded plugin.

## Command

Prefer non-interactive flags:

```bash
npm init @capacitor/plugin@latest <plugin-directory> -- \
  --name "<npm-package-name>" \
  --package-id "<android.package.id>" \
  --class-name "<PluginClassName>" \
  --repo "<repository-url>" \
  --author "<author-name-or-email>" \
  --license "<SPDX-license>" \
  --description "<one-line description>"
```

Equivalent direct form:

```bash
npx @capacitor/create-plugin@latest <plugin-directory> \
  --name "<npm-package-name>" \
  --package-id "<android.package.id>" \
  --class-name "<PluginClassName>" \
  --repo "<repository-url>" \
  --author "<author-name-or-email>" \
  --license "<SPDX-license>" \
  --description "<one-line description>"
```

## Required Inputs

| Input | Rule |
| --- | --- |
| npm package name | Scoped or unscoped package name, such as `@ionic-enterprise/capacitor-example`. |
| package ID | Reverse-DNS Android package ID, such as `com.ionic.example`. |
| class name | PascalCase plugin name, such as `Example`; avoid `ExamplePlugin` to prevent duplicate suffixes. |
| repo URL | Use the intended repository URL or a clearly marked placeholder for POC output. |
| author | Required by the generator in non-interactive mode; ask if missing or use a clearly marked POC placeholder. |
| license | SPDX value, usually `MIT` unless the user provides another license. |
| description | Short package description, not marketing copy. |

## Native Dependency Detection (When Mirroring)

When generating a plugin that mirrors an existing official or community
plugin, inspect the official's dependency declarations *before* scaffolding
so the generated `Package.swift` / `.podspec` / `build.gradle` includes the
right native libraries from the start:

- **iOS** — read the official `*.podspec` for `s.dependency '<Library>'`
  and `Package.swift` for `dependencies: [.package(url: ...)]`.
- **Android** — read `android/build.gradle` for
  `implementation '<group>:<artifact>:...'` entries (excluding
  `:capacitor-android` itself).

Pass the detected dependencies into the generation flow alongside the API
contract. If the official wraps a native SDK, the candidate's bridge
becomes a thin adapter — see `references/ios-implementation.md` "SDK Adapter
Pattern" and `references/android-implementation.md` "SDK Adapter Pattern". See
`references/api-design.md` "Native Dependency Detection" for the rule.

## `--class-name` Anti-Pattern

The Capacitor scaffolder appends `Plugin` to the class name during file
generation. Pass the bare PascalCase name without that suffix or you will
get duplicated `*PluginPlugin` filenames.

| Flag value                       | Generated files                                  | Result        |
| ---                              | ---                                              | ---           |
| `--class-name "Example"`         | `ExamplePlugin.swift`, `ExamplePlugin.java`      | ✅ correct    |
| `--class-name "ExamplePlugin"`   | `ExamplePluginPlugin.swift`, `ExamplePluginPlugin.java` | ❌ stuttering |

The same rule applies to the structured YAML `plugin.class_name` field —
strip a trailing `Plugin` from any value the user provides before invoking
the scaffolder.

## Name Parity

The JavaScript plugin name is the class name without a generated suffix. Keep it
identical across the project:

```typescript
const Example = registerPlugin<ExamplePlugin>('Example', {
  web: () => import('./web').then((m) => new m.ExampleWeb()),
});
```

```swift
public let jsName = "Example"
```

```java
@CapacitorPlugin(name = "Example")
```

Name drift is the most common cause of plugins loading silently but never
dispatching native calls.

## Post-Scaffold Checks

Run:

```bash
cd <plugin-directory>
npm install
npm run verify
```

If a native toolchain is unavailable, run the platform-specific checks that are
available and report the rest as local follow-up:

```bash
npm run verify:web
npm run verify:ios
npm run verify:android
```
