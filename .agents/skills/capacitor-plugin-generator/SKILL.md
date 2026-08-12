---
name: capacitor-plugin-generator
description: >-
  Generates new Capacitor plugin scaffolds and first-pass implementations from
  conversational requirements or a structured YAML input contract. Use when a
  user says "generate a Capacitor plugin", "create a Capacitor plugin scaffold",
  "build a native plugin for iOS and Android", "turn this plugin plan into
  Capacitor code", or "use this YAML contract to generate a plugin". Do not use
  for analyzing Cordova source, migrating whole apps, installing existing
  plugins, upgrading Capacitor versions, or publishing production-ready code
  without human review.
metadata:
  author: ionic
  source: https://github.com/ionic-team/capacitor-skills
---

# Capacitor Plugin Generator

Generate a reviewable Capacitor plugin candidate from either human intent or a
structured YAML contract. The output should follow the official Capacitor plugin
architecture, but it is a first pass that requires human review before release.

## When to Use This Skill

✅ **Use this skill when:**

- Creating a new Capacitor plugin from scratch.
- Adding native functionality (camera, sensors, storage, etc.) to a Capacitor app.
- Designing plugin architecture and API contracts.
- Implementing native code for iOS (Swift) or Android (Kotlin/Java).
- Bridging native APIs to JavaScript/TypeScript.
- Setting up plugin configuration and build systems.
- Generating a plugin from a structured YAML contract handed off by the
  `cordova-capacitor-plugin-migration` skill.

❌ **Do NOT use this skill for:**

- Building standard Capacitor apps (use Capacitor documentation instead).
- Web-only features that don't require native bridges.
- Modifying existing Capacitor core plugins.
- *Analyzing* Cordova plugin structure (use `cordova-capacitor-plugin-migration`
  first; that skill produces the input contract this skill consumes).
- Upgrading existing plugins to newer Capacitor versions.
- Publishing production-ready code without human review.

## Prerequisites

| Requirement | Use |
| --- | --- |
| Node.js LTS and npm | Run the Capacitor plugin generator and package scripts. |
| Xcode | Build and verify iOS output when iOS is targeted. |
| Android Studio and Android SDK | Build and verify Android output when Android is targeted. |
| CocoaPods and Gradle | Resolve native dependencies when required by generated code. |
| Capacitor plugin knowledge | Review generated native bridge code before publishing. |

## Agent Behavior

- Detect whether the user provided conversational intent or structured YAML.
- In conversational mode, ask only for missing plugin identity, methods,
  platforms, events, permissions, configuration, and native dependencies.
- In structured mode, parse `references/input-contract.md`, skip elicitation,
  and halt if the optional migration block contains blockers or tier 3 hooks.
- Treat migration metadata as implementation context only. Do not inspect or
  analyze Cordova source; that belongs to the migration skill.
- Prefer the official Capacitor plugin generator, then edit the generated
  scaffold to implement the requested API.
- Keep generated code contract-first: TypeScript definitions drive web, iOS,
  Android, docs, and sample app behavior.
- Use only Capacitor classes that exist in the installed `@capacitor/core`,
  `@capacitor/android`, and `@capacitor/ios` packages. Do not invent helper
  classes, utilities, or import paths. When uncertain whether an API exists,
  read the package source rather than infer from its name.
- When the generated TypeScript contract mirrors an existing public API
  (Capacitor core/community, Capawesome, internal libraries, or a documented
  JavaScript API the user is replacing), look up the actual string literal
  values used on the wire. Do not derive them from human-friendly names.
  Structured-mode YAML pins these values explicitly; conversational-mode
  generation must consult the source.
- Keep bridge files thin. Split native logic into implementation, manager,
  permission, config, and mapper helpers when a method would otherwise become
  a large mixed-responsibility block.
- Choose native APIs by intended product behavior, not by demo convenience. If
  the requested behavior requires permissions, special app settings, or manual
  app configuration, generate the proper `checkPermissions()` /
  `requestPermissions()` flow and document the manual setup.
- Clearly report which verification commands were run and which need local
  human/device validation.
- Never publish for real from this skill. Run publish checks and `npm publish
  --access public --dry-run` only.

## Procedures

### Phase 1: Determine the Task and Entry Mode

Read `references/input-contract.md`. If the input is YAML with `plugin`,
`platforms`, and `api`, parse it as structured mode and skip questions. If the
input is conversational, elicit the minimum missing fields needed to create the
same contract shape internally.

### Phase 2: Scaffold

Read `references/scaffolding.md`. Run the official Capacitor plugin generator
with non-interactive flags when possible. Enforce name parity:
`registerPlugin()` JavaScript name equals iOS `jsName` equals Android
`@CapacitorPlugin(name)`.

### Phase 3: Design the TypeScript API

Read `references/api-design.md`. Define `src/definitions.ts` before native
implementation. Use options/result interfaces per method, string unions instead
of enums, listener signatures for events, and JSDoc with `@since` everywhere.

### Phase 4: Implement the Web Layer

Read `references/web-guide.md`. Extend `WebPlugin`, feature-detect browser APIs
before use, throw `unavailable()` when an API exists but is unavailable in the
current browser, and throw `unimplemented()` when no web equivalent exists.
Register the web layer through a dynamic import.

### Phase 5: Define Method Signatures

Read `references/api-design.md`. For every method, choose one bridge return
type: value, void, or callback. Use callback return types only for streams or
long-lived watchers. Keep event names identical across TypeScript, web, iOS, and
Android.

### Phase 6: Implement iOS

Read `references/architecture-patterns.md` and `references/ios-implementation.md`. Use the
Bridge pattern by default: a thin Capacitor plugin class delegates to an
implementation class. Use a Facade only for complex plugins with multiple native
subsystems, permission flows, or lifecycle concerns.

### Phase 7: Implement Android

Read `references/architecture-patterns.md` and `references/android-implementation.md`. Use the
Bridge pattern by default: a thin `Plugin` class delegates to an implementation
class. Use a Facade only for complex plugins with multiple managers, permission
flows, services, activities, or lifecycle hooks.

### Phase 8: Generate a Sample App

Read `references/sample-app.md`. Create or update a sample app that exercises
the entire public plugin API, including success paths, expected errors,
permissions, configuration, and listeners.

### Phase 9: Docgen and Verify

Read `references/testing-strategies.md` and `references/publishing.md`.
Generate API docs from JSDoc with `npm run docgen`; do not hand-write API docs.
Run the relevant verify commands for targeted platforms and record any
environment-limited checks.

### Phase 10: Publish Checks

Read `references/publishing.md`. Run the pre-publish checklist and dry run:
`npm publish --access public --dry-run`. Do not publish the generated plugin
without explicit human review outside this skill.

## Best Practices

### DO

- ✅ Use command-line flags with `npm init @capacitor/plugin` so the
  scaffolder runs non-interactively.
- ✅ Detect entry mode (conversational vs structured YAML) before asking
  questions. Skip elicitation entirely in structured mode.
- ✅ Design the TypeScript API first (contract-first), then implement
  web, iOS, and Android against that contract.
- ✅ Implement the web layer for testing without devices, even when
  most methods throw `unimplemented()`.
- ✅ Inspect the official plugin's native dependencies when mirroring an
  existing API; declare the same SDKs and write a thin adapter rather
  than reimplementing.
- ✅ Document every public symbol with JSDoc and `@since`.
- ✅ Run `npm run fmt` before committing and `npm run verify` before
  reporting completion.
- ✅ Use the two-class pattern (bridge + implementation) on iOS and
  Android for testability.
- ✅ Match wire-format string and numeric values exactly when mirroring
  an existing API. Look up the official `definitions.ts` rather than
  guessing from human-friendly names.
- ✅ Keep event names identical across TypeScript / web / iOS / Android.

### DON'T

- ❌ Mix concerns — keep the plugin focused on one capability.
- ❌ Skip error handling. Reject with codes from the canonical 4-code
  taxonomy (`UNAVAILABLE`, `PERMISSION_DENIED`, `INVALID_PARAMETER`,
  `OPERATION_FAILED`).
- ❌ Use callbacks instead of promises in the TypeScript surface.
- ❌ Forget the web implementation, even for iOS/Android-only features.
- ❌ Hard-code values that should be configurable. Use
  `references/configuration.md` runtime plugin configuration.
- ❌ Invent Capacitor classes, helpers, or import paths. Verify against
  the installed `@capacitor/core`, `@capacitor/android`, and
  `@capacitor/ios` packages.
- ❌ Call `notifyListeners(...)` from outside the `Plugin` subclass —
  see `references/architecture-patterns.md` "Event Dispatch Locality".
- ❌ Publish from this skill. Run dry-run only with
  `npm publish --access public --dry-run`.
- ❌ Inspect or analyze Cordova source — that belongs to the sibling
  migration skill.

## Error Handling

| Symptom | Fix |
| --- | --- |
| `npm init @capacitor/plugin` fails with `Refusing to prompt in non-TTY environment` | Pass all required flags non-interactively: `npm init @capacitor/plugin <folder> -- --name "<npm-name>" --package-id "<reverse-dns>" --class-name "<PascalCase>" --description "<one-line>" --author "<name <email>>" --license "<SPDX>" --repo "<url>" --android-lang "<kotlin\|java>"`. See `references/scaffolding.md`. |
| `npm init @capacitor/plugin` fails with `invalid option: --android-lang undefined: Must be either 'kotlin' or 'java'` | The `--android-lang` flag is required when running non-interactively. Add `--android-lang "kotlin"` (recommended for new plugins) or `--android-lang "java"` to the command. |
| Plugin silently fails to load | Make `registerPlugin()` name match iOS `jsName` and Android `@CapacitorPlugin(name)`. |
| Event not received in JS | Make the event name string identical across TypeScript, web, iOS, and Android. |
| iOS method not callable from JS | Ensure the method is marked `@objc` and listed in `pluginMethods`. |
| Android method not callable from JS | Ensure the method is public and annotated with `@PluginMethod()`. |
| `npm run verify:ios` fails | Run `pod install --repo-update`; then rerun the iOS verify command. |
| `npm run verify:android` fails | Sync Gradle and check Android SDK, compile SDK, and dependency versions. |
| Android compile error: `class X is public, should be declared in a file named X.java` | Java requires a public class to live in a file matching its name. When generating multiple Java classes per plugin, place each public class in its own file. Kotlin does not impose this rule. |
| Android compile error: `notifyListeners(...) has protected access in Plugin` | `notifyListeners()` is `protected` on `Plugin`. Call it only from inside a class that extends `Plugin`. If another class needs to emit events, return the data to the plugin and dispatch there, or expose a public wrapper on the plugin that calls `notifyListeners()` internally. |
| Android compile error: `cannot find symbol: class …` for a `com.getcapacitor.*` import | The import does not exist on the installed `@capacitor/android` surface. Verify imports against the package source before generating; do not infer Capacitor classes from their names. |
| Android compile error: `<method> in <Subclass> cannot override <method> in Plugin; attempting to assign weaker access privileges; was public` | A helper on the `Plugin` subclass collides with a `public` method that `com.getcapacitor.Plugin` already defines (e.g., `hasPermission`, `getPermissionState`, `notifyListeners`). Either rename the helper or match the parent's `public` visibility. |
| TypeScript build error: `Cannot find type definition file for '<name>'` or `Invalid module name in augmentation, module '<name>' cannot be found` | The augmented module is not installed. Add it to `devDependencies` (and to `tsconfig.json` `compilerOptions.types` if a triple-slash reference is used). Applies to any module augmentation, not just `@capacitor/cli`. |
| TypeScript build error: `Interface 'X' incorrectly extends interface 'Y'. Property 'Z' is optional in type 'X' but required in type 'Y'` (or the symmetric error) | Do not redeclare members the built-in lib type already provides. Use the lib type directly, or augment via `declare global { interface Y { newMember?: ... } }` for genuinely new members only. Use `'name' in target` runtime guards for capability checks. |
| `npm run docgen` produces empty output | Add JSDoc to `src/definitions.ts`; docgen reads the TypeScript contract. |
| Web API absent in target browser | Use `unavailable()` when the API exists but is missing here; use `unimplemented()` when no web equivalent exists. |
| Structured YAML is rejected | Validate against `references/input-contract.md`; ensure required base fields are present and blockers are empty. |
| Generated output looks too broad | Split unrelated capabilities into separate plugins and regenerate with a smaller API surface. |
| Generated native code reimplements logic the official plugin delegates to a native SDK | Inspect the official plugin's `.podspec` / `Package.swift` / `android/build.gradle` for native SDK dependencies. If present, declare the same SDK and write a thin adapter — see `references/api-design.md` "Native Dependency Detection". |

## Related Skills

- `cordova-capacitor-plugin-migration`: Analyze Cordova plugins and produce a
  structured migration plan for this generator.

## References

- `references/input-contract.md`: Structured YAML contract for generator input.
- `references/scaffolding.md`: Generator invocation, name parity, and scaffold verification.
- `references/api-design.md`: TypeScript API design, JSDoc, event signatures, and method return types.
- `references/web-guide.md`: WebPlugin patterns, dynamic import, feature detection, and errors.
- `references/architecture-patterns.md`: Bridge and Facade patterns plus event parity.
- `references/ios-implementation.md`: iOS bridge, implementation class, permissions, dependencies, and Podspec.
- `references/android-implementation.md`: Android bridge, implementation class, permissions, dependencies, and Gradle.
- `references/configuration.md`: Capacitor config keys under `plugins.<PluginJSName>`.
- `references/testing-strategies.md`: Local linking, verify commands, hooks, and review workflow.
- `references/publishing.md`: Package fields, docgen, checklist, and dry-run publishing.
- `references/sample-app.md`: Sample app requirements that exercise the full API.
- `references/permission-patterns.md`: Deep-dive on permission flows — multi-permission DispatchGroup on iOS, location delegate, check-before-use / just-in-time / deferred consumer patterns, opening system settings.
- `references/typescript-implementation.md`: Deep-dive on TypeScript layer — singleton plugin pattern, typed error classes, full event listener handle bookkeeping, helper utilities, Jest scaffolding.
