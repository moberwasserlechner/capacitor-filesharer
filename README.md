# Capacitor File Sharing plugin

<a href="https://github.com/moberwasserlechner/capacitor-filesharer/actions?query=workflow%3ACI"><img src="https://img.shields.io/github/workflow/status/moberwasserlechner/capacitor-filesharer/CI?style=flat-square" /></a>
<a href="https://www.npmjs.com/package/@byteowls/capacitor-filesharer"><img src="https://img.shields.io/npm/dw/@byteowls/capacitor-filesharer?style=flat-square" /></a>
<a href="https://www.npmjs.com/package/@byteowls/capacitor-filesharer"><img src="https://img.shields.io/npm/v/@byteowls/capacitor-filesharer?style=flat-square" /></a>
<a href="https://www.npmjs.com/package/@byteowls/capacitor-filesharer"><img src="https://img.shields.io/npm/l/@byteowls/capacitor-filesharer?style=flat-square" /></a>

## Installation

For Capacitor v4
```bash
npm i @byteowls/capacitor-filesharer
npx cap sync
```

For Capacitor v3 use `3.0.0`
```bash
npm i @byteowls/capacitor-filesharer@3.0.0
npx cap sync
```
For Capacitor v2 use `2.0.0`
```bash
npm i @byteowls/capacitor-filesharer@2.0.0
npx cap sync
```

For further details on what has changed see the [CHANGELOG](https://github.com/moberwasserlechner/capacitor-filesharer/blob/main/CHANGELOG.md).

## Versions

| Plugin | For Capacitor | Docs                                                                                      | Notes                          |
|--------|---------------|-------------------------------------------------------------------------------------------|--------------------------------|
| 4.x    | 4.x.x         | [README](./README.md)                                                                     | Breaking changes see Changelog. |
| 3.x    | 3.x.x         | [README](https://github.com/moberwasserlechner/capacitor-filesharer/blob/3.0.0/README.md) | Breaking changes see Changelog. XCode 12.0 needs this version |
| 2.x    | 2.x.x         | [README](https://github.com/moberwasserlechner/capacitor-filesharer/blob/2.0.0/README.md) | Breaking changes see Changelog. XCode 11.4 needs this version |
| 1.x    | 1.x.x         | [README](https://github.com/moberwasserlechner/capacitor-filesharer/blob/1.0.0/README.md)        |                                |

## Maintainers

| Maintainer | GitHub | Social |
| -----------| -------| -------|
| Michael Oberwasserlechner | [moberwasserlechner](https://github.com/moberwasserlechner) |  |

Actively maintained: YES

## Configuration

Starting with version 3.0.0, the plugin is registered automatically on all platforms.

### Use it

```typescript
import {
  FileSharer
} from '@byteowls/capacitor-filesharer';

@Component({
  template: '<button (click)="downloadButtonClick()">Download file</button>'
})
export class SignupComponent {
    downloadButtonClick() {
        FileSharer.share({
            filename: "test.pdf",
            contentType: "application/pdf",
            // If you want to save base64:
            base64Data: "...",
            // If you want to save a file from a path:
            path: "../../file.pdf",
        }).then(() => {
            // do sth
        }).catch(error => {
            console.error("File sharing failed", error.message);
        });
    }
}
```

### Error Codes

* ERR_PARAM_NO_FILENAME ... Filename missing or invalid.
* ERR_PARAM_NO_DATA ... Base64 data missing.
* ERR_PARAM_NO_CONTENT_TYPE ... Content type missing
* ERR_PARAM_DATA_INVALID ... Base64 data is invalid. See [this comment](https://github.com/moberwasserlechner/capacitor-filesharer/issues/5#issuecomment-502070959) for a possible error.
* ERR_FILE_CACHING_FAILED ... Caching the file in temp directory on the device failed.

## Platform: Web/PWA

### Register plugin
On Web/PWA the plugin is registered **automatically** by Capacitor.

## Platform: Android

Prerequisite: [Capacitor Android Docs](https://capacitor.ionicframework.com/docs/android/configuration)

### Configure

Add `outState.clear()` to your `MainActivity`. This fixes `android.os.TransactionTooLargeException` when sharing larger files.

```java
package com.company.project;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.clear();
  }
}
```

### Register plugin
On Android the plugin is registered **automatically** by Capacitor.

## Platform: iOS
Prerequisite: [Capacitor iOS Docs](https://capacitor.ionicframework.com/docs/ios/configuration)

### Register plugin
On iOS the plugin is registered **automatically** by Capacitor.

## Platform: Electron

- No timeline.

## Contribute

See [Contribution Guidelines](https://github.com/moberwasserlechner/capacitor-filesharer/blob/main/.github/CONTRIBUTING.md).

## Changelog
See [CHANGELOG](https://github.com/moberwasserlechner/capacitor-filesharer/blob/main/CHANGELOG.md).

## License

MIT. Please see [LICENSE](https://github.com/moberwasserlechner/capacitor-filesharer/blob/main/LICENSE).

## Disclaimer

We have no business relation to Ionic.
