# Structured Input Contract

Use this contract when the user provides YAML directly or when the Cordova
migration skill invokes the generator. Conversational mode should collect the
same information internally, then proceed as if this shape had been provided.

## Required Base Contract

```yaml
plugin:
  name: screen-orientation
  package_id: com.example.screenorientation
  class_name: ScreenOrientation
  description: Lock and unlock device screen orientation
  repo_url: https://github.com/example/capacitor-screen-orientation
  author: Ionic Team <hi@ionic.io>
  license: MIT

platforms: [ios, android, web]

api:
  methods:
    - name: orientation
      returns: Promise<OrientationResult>
      jsdoc: Returns the device's current orientation.
      since: "1.0.0"
    - name: lock
      options: OrientationLockOptions
      returns: Promise<void>
      jsdoc: Locks the screen to the given orientation.
      since: "1.0.0"

  types:
    - name: OrientationType
      kind: union
      values: [portrait-primary, portrait-secondary, landscape-primary, landscape-secondary]
    - name: OrientationResult
      kind: interface
      fields:
        - { name: type, type: OrientationType }
    - name: OrientationLockOptions
      kind: interface
      fields:
        - { name: orientation, type: OrientationType }

  events:
    - name: screenOrientationChange
      payload: OrientationResult
      jsdoc: Fired when the device orientation changes.

permissions:
  ios: []
  android: []

dependencies:
  ios:
    cocoapods: []
    spm: []
    system_frameworks: [UIKit]
  android:
    gradle: []
    maven_repos: []
```

## Field Rules

- `plugin.name`: npm directory/package suffix in kebab case.
- `plugin.package_id`: Android package identifier.
- `plugin.class_name`: PascalCase JavaScript/native plugin name. Do not add a
  trailing `Plugin` suffix unless the real plugin name includes it.
- `plugin.author`: Package author string. The current Capacitor generator
  requires this in non-interactive mode; ask the user or use a clearly marked
  POC placeholder if structured input omits it.
- `platforms`: any subset of `ios`, `android`, and `web`.
- `api.methods[].name`: JavaScript method name.
- `api.methods[].options`: optional TypeScript options interface name.
- `api.methods[].returns`: `Promise<void>`, `Promise<ResultType>`, or
  `Promise<CallbackID>` for native callback/watch methods. Listener APIs use
  `Promise<PluginListenerHandle>` on `addListener()`.
- `api.types`: define every options, result, event payload, and union type used
  by methods or listeners.
- `api.events`: optional; event names must be exact strings shared by all
  platform implementations.
- `permissions`: optional per platform; empty arrays mean no runtime permission
  flow is required.
- `dependencies`: optional per platform; include only dependencies required by
  generated code.

## Optional Migration Block

The migration skill may append:

```yaml
migration:
  source: cordova
  complexity: simple
  output_mode: side_by_side
  blockers: []
  warnings: []
  language_modernization:
    ios: { from: objective_c, to: swift }
    android: { from: java, to: kotlin }
  source_files:
    ios: [src/ios/Example.m]
    android: [src/android/Example.java]
    js: [www/example.js]
  hooks:
    tier_1: []
    tier_2: []
    tier_3: []
  cordova_to_capacitor_map:
    - cordova: "Example.echo(value, success, error)"
      capacitor: "Example.echo({ value })"
  notes:
    - "Mirrors @capacitor/example v6.x definitions.ts (Foo enum casing)."
    - "Weak-linked ImageIO declared in plugin.xml — emit s.weak_framework in podspec."
```

Migration block field rules:

- `blockers`, `warnings`, `notes` are arrays of free-form strings. `blockers`
  stops generator handoff; `warnings` surface in the Phase 10 checkpoint but
  do not stop; `notes` are advisory breadcrumbs for the reviewer (e.g.
  "mirrors @capacitor/<name> wire format", "weak iOS framework requires
  `s.weak_framework` in podspec", "vendored xcframework at
  `migration.source_files.ios[0]`").
- `cordova_to_capacitor_map` entries must be quoted strings. Bare JS syntax
  like `Foo.bar(value, success, error)` is not valid YAML — it parses as a
  mapping and fails the contract validator.

If `migration.blockers` or `migration.hooks.tier_3` is non-empty, stop and ask
for those issues to be resolved by the migration skill or a human. Do not
reanalyze Cordova source in this generator skill.
