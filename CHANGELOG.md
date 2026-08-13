# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Cross-platform, memory-safe local source sharing through `path`: Web accepts caller-owned Blob URLs, Android accepts filesystem and content URIs, and iOS accepts filesystem URLs; Android and iOS also accept Capacitor file URLs from the configured WebView origin [#66](https://github.com/moberwasserlechner/capacitor-filesharer/issues/66).
- Exported `ERR_PARAM_PATH_INVALID` for malformed and unsupported path or URI sources.

### Changed
- Base64 data now consistently takes precedence when both `base64Data` and `path` are supplied.
- Android path and URI sources are copied with bounded streams instead of being loaded completely into memory.

### Fixed
- Android packaging now loads Gradle plugins compatibly when included in a Capacitor host and supplies its own test dependency fallback.
- The published Swift package no longer references or ships repository tests; native package verification now covers Swift target paths and Android host integration.

## [8.0.0] - 2026-08-15

### Added
- ESM, CommonJS, and bundled declaration package entry points with consumer verification.
- Focused native tests for Android file handling and iOS plugin metadata, Base64 decoding, and temporary-file failures.
- Exported stable error-code constants, including consistent invalid Base64 reporting on Web.

### Changed
- Migrated development workflows from npm and Jest to pnpm and Vitest, with modernized TypeScript and package build tooling.
- Local npm publishing now runs full cross-platform verification before upload.
- Restricted the npm artifact to runtime files and added exact package-content verification.
- Reimplemented the Android bridge and file-sharing logic in Kotlin.
- Replaced FileSaver with browser Blob APIs and bounded Base64 decoding to reduce peak memory usage for large files [#56](https://github.com/moberwasserlechner/capacitor-filesharer/issues/56).

### Fixed
- Web validation failures no longer continue into a file download attempt.
- Android raw paths containing `_capacitor_file_` in a filename are no longer mistaken for Capacitor file URLs.
- Android now reads the documented `android.chooserTitle` option.
- Android local paths without Capacitor's internal URL marker no longer crash during parsing.
- Android and iOS reject filenames that would escape the plugin cache directory.

### Breaking
- Capacitor 8 is the new minimum peer dependency.
- iOS distribution now uses the renamed `CapacitorFileSharer` Swift package exclusively; CocoaPods support has been removed [#60](https://github.com/moberwasserlechner/capacitor-filesharer/issues/60).
- Web downloads now require Blob URL and anchor download support; legacy-browser fallbacks are no longer included.

## [7.0.0] - 2025-07-22

### Breaking
- Capacitor 7 is new minimum peer dependency. [#57](https://github.com/moberwasserlechner/capacitor-filesharer/issues/57)

## [6.0.0] - 2024-07-25

### Breaking
- Capacitor 6.x is new minimum peer dependency. [#53](https://github.com/moberwasserlechner/capacitor-filesharer/issues/53)

## [5.0.0] - 2023-09-04

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

[Unreleased]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/8.0.0...main
[8.0.0]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/7.0.0...8.0.0
[7.0.0]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/6.0.0...7.0.0
[6.0.0]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/5.0.0...6.0.0
[5.0.0]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/4.0.1...5.0.0
[4.0.1]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/4.0.0...4.0.1
[4.0.0]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/3.0.0...4.0.0
[3.0.0]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/2.0.0...3.0.0
[2.0.0]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/1.0.1...2.0.0
[1.0.1]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/moberwasserlechner/capacitor-filesharer/compare/1.0.0-beta.1...1.0.0
[1.0.0-beta.1]: https://github.com/moberwasserlechner/capacitor-filesharer/releases/tag/1.0.0-beta.1
