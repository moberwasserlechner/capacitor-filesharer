<p align="center"><br><img src="https://user-images.githubusercontent.com/236501/85893648-1c92e880-b7a8-11ea-926d-95355b8175c7.png" width="128" height="128" /></p>
<h3 align="center">File Sharing</h3>
<p align="center"><strong><code>@byteowls/capacitor-filesharer</code></strong></p>
<p align="center">
    Capacitor File Sharing plugin
</p>

<p align="center">
    <img src="https://img.shields.io/maintenance/yes/2025?style=flat-square" />
    <a href="https://github.com/moberwasserlechner/capacitor-filesharer/actions?query=workflow%3ACI"><img src="https://img.shields.io/github/actions/workflow/status/moberwasserlechner/capacitor-filesharer/ci.yml?style=flat-square" /></a>
    <a href="https://www.npmjs.com/package/@byteowls/capacitor-filesharer"><img src="https://img.shields.io/npm/l/@byteowls/capacitor-filesharer?style=flat-square" /></a>
<br>
  <a href="https://www.npmjs.com/package/@byteowls/capacitor-filesharer"><img src="https://img.shields.io/npm/dw/@byteowls/capacitor-filesharer?style=flat-square" /></a>
  <a href="https://www.npmjs.com/package/@byteowls/capacitor-filesharer"><img src="https://img.shields.io/npm/v/@byteowls/capacitor-filesharer?style=flat-square" /></a>
</p>

## Introduction

Capacitor plugin to share files on Android and iOS using the native share dialog and on Web using the FileSaver lib.
## Installation

```bash
npm i @byteowls/capacitor-filesharer
npx cap sync
```

For further details on what has changed see the [CHANGELOG](https://github.com/moberwasserlechner/capacitor-filesharer/blob/main/CHANGELOG.md).

## Versions

| Plugin | For Capacitor | Docs                                                                                      | Notes                           |
|--------|---------------|-------------------------------------------------------------------------------------------|---------------------------------|
| 8.x    | 8.x.x         | [README](./README.md)                                                                     | Swift Package Manager on iOS.   |
| 7.x    | 7.x.x         | [README](https://github.com/moberwasserlechner/capacitor-filesharer/blob/7.0.0/README.md) | CocoaPods; see the changelog.   |
| 6.x    | 6.x.x         | [README](https://github.com/moberwasserlechner/capacitor-filesharer/blob/6.0.0/README.md) | CocoaPods; see the changelog.   |

## Configuration

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

## Platform: iOS

Version 8 is distributed exclusively through Swift Package Manager. Applications upgrading from plugin 7 must migrate their Capacitor iOS project from CocoaPods to Swift Package Manager before installing this release.

## Platform: Android

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

## Contribute

See [Contribution Guidelines](https://github.com/moberwasserlechner/capacitor-filesharer/blob/main/.github/CONTRIBUTING.md).

## Changelog
See [CHANGELOG](https://github.com/moberwasserlechner/capacitor-filesharer/blob/main/CHANGELOG.md).

## License

[MIT](https://opensource.org/licenses/MIT)

## Disclaimer

We have no business relation to Ionic.
