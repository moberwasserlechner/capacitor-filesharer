<p align="center"><br><img src="https://user-images.githubusercontent.com/236501/85893648-1c92e880-b7a8-11ea-926d-95355b8175c7.png" width="128" height="128" /></p>
<h3 align="center">File Sharing</h3>
<p align="center"><strong><code>@byteowls/capacitor-filesharer</code></strong></p>
<p align="center">
    Capacitor File Sharing plugin
</p>

<p align="center">
    <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" />
    <a href="https://github.com/moberwasserlechner/capacitor-filesharer/actions?query=workflow%3ACI"><img src="https://img.shields.io/github/actions/workflow/status/moberwasserlechner/capacitor-filesharer/ci.yml?style=flat-square" /></a>
    <a href="https://www.npmjs.com/package/@byteowls/capacitor-filesharer"><img src="https://img.shields.io/npm/l/@byteowls/capacitor-filesharer?style=flat-square" /></a>
<br>
  <a href="https://www.npmjs.com/package/@byteowls/capacitor-filesharer"><img src="https://img.shields.io/npm/dw/@byteowls/capacitor-filesharer?style=flat-square" /></a>
  <a href="https://www.npmjs.com/package/@byteowls/capacitor-filesharer"><img src="https://img.shields.io/npm/v/@byteowls/capacitor-filesharer?style=flat-square" /></a>
</p>

## Introduction

Capacitor plugin to share files on Android and iOS using the native share dialog and to download files on the Web.
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

## Usage

```typescript
import {
  FileSharer,
  FileSharerErrorCode,
} from '@byteowls/capacitor-filesharer';

export async function shareReport(base64Data: string): Promise<void> {
  try {
    await FileSharer.share({
      filename: 'report.pdf',
      contentType: 'application/pdf',
      base64Data,
    });
  } catch (error: unknown) {
    if (
      error instanceof Error &&
      error.message === FileSharerErrorCode.UserCancelled
    ) {
      return;
    }
    throw error;
  }
}
```

Provide either `base64Data` or a platform-supported `path`. If Android receives both, `base64Data` takes precedence. Web and iOS currently require `base64Data`.

### Input support

| Input | Web | Android | iOS |
| --- | --- | --- | --- |
| Base64 data | Supported | Supported | Supported |
| Raw local path | Not supported | Supported when app-accessible | Not supported |
| Capacitor `_capacitor_file_` URL | Not supported | Supported | Not supported |
| `file://` or `content://` URI | Not supported | Not supported | Not supported |

Base64 crosses the JavaScript/native bridge and requires additional encoded and decoded memory. Keep payload sizes practical. Android paths avoid Base64 bridge overhead, but the current implementation still reads the complete source into memory while caching it. Cross-platform streaming path and URI support is tracked in [#66](https://github.com/moberwasserlechner/capacitor-filesharer/issues/66).

### Errors

Errors expose one of the exported `FileSharerErrorCode` values through `Error.message`.

| Export | Value | Platforms |
| --- | --- | --- |
| `NoFilename` | `ERR_PARAM_NO_FILENAME` | Web, Android, iOS |
| `NoData` | `ERR_PARAM_NO_DATA` | Web, Android, iOS |
| `NoContentType` | `ERR_PARAM_NO_CONTENT_TYPE` | Web, Android |
| `InvalidData` | `ERR_PARAM_DATA_INVALID` | Web, Android, iOS |
| `FileCachingFailed` | `ERR_FILE_CACHING_FAILED` | Android, iOS |
| `LocalFileNotFound` | `ERR_LOCAL_FILE_NOT_FOUND` | Android |
| `UserCancelled` | `USER_CANCELLED` | Android |

### Completion behavior

| Platform | The promise resolves when |
| --- | --- |
| Web | The browser download has been started. |
| Android | The share activity returns without a cancellation result. |
| iOS | The share sheet has been presented. |

## Platform: Web

Web downloads use Blob URLs and the anchor `download` attribute. Version 8 does not include legacy-browser download fallbacks.

## Platform: iOS

Version 8 is distributed exclusively through Swift Package Manager. Applications upgrading from plugin 7 must migrate their Capacitor iOS project from CocoaPods to Swift Package Manager before installing this release.

## Platform: Android

Android can share an app-accessible local path without first converting the file to Base64:

```typescript
await FileSharer.share({
  filename: 'report.pdf',
  contentType: 'application/pdf',
  path: '/data/user/0/com.example.app/files/report.pdf',
  android: {
    chooserTitle: 'Share report',
  },
});
```

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
