# Web Guide

The web implementation should either wrap an actual Web API or clearly report
that the capability is unavailable or unimplemented.

## Pattern

```typescript
import { WebPlugin } from '@capacitor/core';

import type {
  ExamplePlugin,
  GetSignalOptions,
  GetSignalResult,
} from './definitions';

export class ExampleWeb extends WebPlugin implements ExamplePlugin {
  async getSignal(options: GetSignalOptions): Promise<GetSignalResult> {
    if (!globalThis.navigator) {
      throw this.unavailable('Navigator is not available in this browser.');
    }

    if (!('connection' in globalThis.navigator)) {
      throw this.unimplemented('The Network Information API has no supported web equivalent here.');
    }

    return {
      level: options.source === 'wifi' ? 100 : 50,
    };
  }
}
```

## `unavailable()` vs `unimplemented()`

| Use | Meaning |
| --- | --- |
| `this.unavailable(message)` | A relevant Web API exists, but this browser/platform/session cannot use it. |
| `this.unimplemented(message)` | There is no meaningful web implementation for this native feature. |

Feature-detect before touching optional browser APIs. Avoid user-agent checks
unless the API itself gives no reliable capability signal.

If `src/definitions.ts` includes `checkPermissions()` or
`requestPermissions()`, implement both in `src/web.ts`. Use the Web Permissions
API only after feature detection; throw `unavailable()` when the browser lacks
the needed API and `unimplemented()` when web cannot request that permission.

## Extending Built-in Lib Types

Modern TypeScript lib types include most stable Web APIs (`Navigator`,
`Document`, `Window`, `Permissions`, `Screen`, `Storage`, etc.). When the web
layer needs to feature-detect or use one of these APIs, prefer the lib type
directly rather than redeclaring its members.

- **Do not** declare an extending interface that redeclares a lib member with
  a different optionality or signature. TypeScript will reject it with
  `Interface 'X' incorrectly extends interface 'Y'. Property 'Z' is optional
  in type 'X' but required in type 'Y'` (or the symmetric error). Lib types
  encode optionality from the spec; do not override it.
- **For genuinely new members** (a non-standard or experimental API not yet
  in the lib), use module-merging declaration so the lib member set is
  augmented rather than replaced:
  ```typescript
  declare global {
    interface Navigator {
      // only declare members the lib does not already provide
      experimentalFooApi?: () => Promise<void>;
    }
  }
  ```
- **For optional capability checks**, use `'memberName' in target` runtime
  guards. Do not redeclare the lib type just to make a member appear optional
  at compile time — the lib already encodes optionality where the spec marks
  it.

## Web Implementation Strategies

Pick the right strategy per method. Same TypeScript surface, different web
behavior depending on whether a Web API exists:

| Strategy       | When to use                                          | Example                                          |
| ---            | ---                                                  | ---                                              |
| **Web API**    | Browser has a real API for this capability           | Geolocation, Battery Status, Web Notifications   |
| **Polyfill**   | Behavior can be simulated with adjacent browser APIs | Storage (IndexedDB), HTTP (fetch with retries)   |
| **Mock data** | Useful only for testing without a device             | Device info, hardware features                   |
| **Throw**      | No web equivalent exists at all                     | NFC, Bluetooth, specific sensors                 |

Mock data should be marked clearly (`console.warn(...)` on each call) so
nobody ships it. Prefer throwing `unimplemented()` over silently returning
fake values when the platform answer is "you cannot do this on web."

## Events

If the plugin emits events:

```typescript
this.notifyListeners('signalChange', { level: 72 });
```

The event string must exactly match the TypeScript listener overload and native
platform `notifyListeners()` calls.

## Dynamic Import

Register web through `src/index.ts`:

```typescript
const Example = registerPlugin<ExamplePlugin>('Example', {
  web: () => import('./web').then((m) => new m.ExampleWeb()),
});
```

This keeps the web implementation lazy-loaded and matches Capacitor generator
conventions.
