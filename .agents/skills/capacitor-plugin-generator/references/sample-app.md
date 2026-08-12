# Sample App

Generate or update a sample app that exercises the entire plugin API. The sample
app is part of validation, not marketing.

## Requirements

- Install the generated plugin from the local plugin path.
- Call every public method at least once.
- Register every event listener and show received payloads.
- Exercise permission methods when present.
- Exercise configuration values when present.
- Include expected-error paths for unavailable or unimplemented platforms.
- If an operation requires special settings access, include a visible
  check/request flow before calling the protected method.
- Keep the UI simple and direct so the demo can show the plugin behavior fast.

## Minimal Flow

For a simple plugin with `echo()` and `getStatus()`:

1. Render one control per method.
2. Show the last result or error.
3. Register listeners on startup.
4. Provide a "Remove listeners" control when the API exposes
   `removeAllListeners()`.

## Web Demo Commands

```bash
cd <sample-app>
npm install
npm install ../<plugin-root>
npx cap sync
npm start
```

Use `npx cap sync` after installing the local plugin or changing plugin native
code, dependencies, permissions, or Capacitor config. Use `npx cap copy` only
for web-only sample app changes after the native platforms are already synced.

## Native Demo Commands

```bash
# Add platforms only if the sample app does not already include them.
npx cap add ios
npx cap add android
npx cap sync
npx cap run ios
npx cap run android
```

## Credentialed or Device-Only Plugins

For plugins that need external credentials, entitlements, provisioning profiles,
third-party service setup, or real devices, generate the sample app wiring but
clearly mark credential and device validation as manual follow-up.
