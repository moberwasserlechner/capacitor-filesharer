# Capacitor plugin for file sharing

[![npm](https://img.shields.io/npm/v/@byteowls/capacitor-filesharer.svg)](https://www.npmjs.com/package/@byteowls/capacitor-filesharer)
[![npm](https://img.shields.io/npm/dt/@byteowls/capacitor-filesharer.svg?label=npm%20downloads)](https://www.npmjs.com/package/@byteowls/capacitor-filesharer)
[![Twitter Follow](https://img.shields.io/twitter/follow/michaelowl_web.svg?style=social&label=Follow&style=flat-square)](https://twitter.com/michaelowl_web)
[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.me/moberwasserlechner)


## Installation

`npm i @byteowls/capacitor-filesharer`

Minimum Capacitor version is **2.0.0**

## Configuration

This example shows the common process of configuring this plugin.

Although it was taken from a Angular 6 application, it should work in other frameworks as well.



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
            base64Data: "...",
            contentType: "application/pdf",
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

No further config is needed.

## Platform: Android

**Register the plugin** in `com.companyname.appname.MainActivity#onCreate`

```
import com.byteowls.capacitor.filesharer.FileSharerPlugin;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<Class<? extends Plugin>> additionalPlugins = new ArrayList<>();
        // Additional plugins you've installed go here
        // Ex: additionalPlugins.add(TotallyAwesomePlugin.class);
        additionalPlugins.add(FileSharerPlugin.class);

        // Initializes the Bridge
        this.init(savedInstanceState, additionalPlugins);
    }
```

Override the onSaveInstanceState on the main activity to avoid an issue that results in !!! FAILED BINDER TRANSACTION !!! errors when dealing with larger files ([Related issue](https://github.com/moberwasserlechner/capacitor-filesharer/issues/13))

```
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.clear();
  }
```

## Platform: iOS

No further config is needed. On iOS the plugin is registered automatically by Capacitor.

## Platform: Electron

- No timeline.

## Contribute

See [Contribution Guidelines](https://github.com/moberwasserlechner/capacitor-filesharer/blob/master/.github/CONTRIBUTING.md).

## Changelog
See [CHANGELOG](https://github.com/moberwasserlechner/capacitor-filesharer/blob/master/CHANGELOG.md).

## License

MIT. Please see [LICENSE](https://github.com/moberwasserlechner/capacitor-filesharer/blob/master/LICENSE).

## BYTEOWLS Software & Consulting

This plugin is powered by [BYTEOWLS Software & Consulting](https://byteowls.com) and was build for [Team Conductor](https://team-conductor.com/en/) - Next generation club management platform.
