# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [5.0.0] - TBD

### Breaking
- Capacitor 5.x is new minimum peer dependency.

## [4.0.1] - 2023-07-10

### Fixed

- Add Android config to readme. [#42](https://github.com/moberwasserlechner/capacitor-filesharer/issues/42)

## [4.0.0] - 2022-09-18

### Added
- Support local (capacitor) file paths, in addition to base64 strings [#41](https://github.com/moberwasserlechner/capacitor-filesharer/pull/41)

### Breaking
- Capacitor 4.x is new minimum peer dependency.

## [3.0.0] - 2021-09-23

### Breaking
- Capacitor 3.x is new minimum peer dependency. [#28](https://github.com/moberwasserlechner/capacitor-filesharer/issues/28)

## [2.0.0] - 2020-04-10

### Fixed
- iPad on iOS 13.3+ does not work. closes #16. thx [@nikosdouvlis](https://github.com/nikosdouvlis)

### Breaking
- Capacitor 2.x is new minimum peer dependency. closes #19

## [1.0.1] - 2020-03-04

### Added
- Changelog file added

### Changed
- Dev dependencies upgraded

### Fixed
- #17 Avoid provider name collisions with multiple apps. thx @jpxd and @Raerten

### Docs
- Add import statement to MainActivity.java in readme example. thx @corysmc
- #13 Add instructions for resolving the FAILED BINDER TRANSACTION error when the app goes to background. thx @FREEZX

## [1.0.0] - 2019-06-23

### Changed
- Minimum Capacitor version is now **1.0.0**
- Minimum Java version is 1.8
- Plugin follows Capacitor's new Android structure and uses a project dependency instead of the Capacitor maven dependency which will solve versioning problems and compile errors

### Fixed
- Sharing files on iPad now works

## [1.0.0-beta.1] - 2019-03-02

### Added
- Share files using the native share dialog on Android and iOS
- Download files on the Web

[Unreleased]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/4.0.0...main
[4.0.0]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/3.0.0...4.0.0
[3.0.0]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/2.0.0...3.0.0
[2.0.0]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/1.0.1...2.0.0
[1.0.1]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/1.0.0-beta.1...1.0.0
[1.0.0-beta.1]: https://github.com/moberwasserlechner/capacitor-filesharer/releases/tag/1.0.0-beta.1
