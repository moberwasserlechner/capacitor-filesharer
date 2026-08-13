# Decisions

Architectural decisions for `@byteowls/capacitor-filesharer`. Planned work belongs in GitHub issues, consumer documentation in `README.md`, and release history in `CHANGELOG.md`.

## Production compatibility

This is a released production plugin. Public API and documented behaviour remain compatible within a major version. Intentional breaking changes require a major release, migration documentation, and a changelog entry.

## Capacitor 8 is the only supported major in version 8

Version 8 targets Capacitor 8 and uses the platform floors required by that ecosystem. Supporting older Capacitor majors in the same release would make native packaging and peer resolution ambiguous; older plugin majors remain available for those applications.

## pnpm is the development package manager

The repository uses pnpm with a committed frozen lockfile. npm remains relevant only as the registry and tarball format checked by package verification.

## TypeScript source is organised by responsibility and tests live separately

`src/index.ts` is registration glue, `src/definitions.ts` is public API, and platform or domain implementation code has its own module. `test/` mirrors `src/` so published source is not mixed with test code.

## Published JavaScript and declarations are bundled

TypeScript output is intermediate build input. Rollup creates ESM, CommonJS, IIFE, and bundled declaration artifacts so consumers resolve the package consistently under Node, bundlers, and modern TypeScript module-resolution modes. Capacitor remains external because the consuming application supplies the peer dependency.

## iOS is distributed through Swift Package Manager only

Capacitor 8 is the major-version migration boundary for removing CocoaPods. The package ships a production-only `Package.swift` and sources under `ios/Sources`; the podspec, Podfile, CocoaPods Xcode project, and tests are not published. XCTest uses a separate repository-only package under `ios/Tests`. Existing consumers must migrate their Capacitor iOS project to Swift Package Manager before upgrading to plugin version 8.

## Web tests use Vitest

Vitest replaces Jest and tests live under `test/`. Coverage includes implementation modules and excludes type-only declarations and registration glue. Coverage thresholds must represent meaningful tests rather than commented placeholders.

## Android uses Kotlin with a thin bridge

`FileSharerPlugin` parses the Capacitor call, delegates to a plain Kotlin implementation, and maps activity results back to the call. Option parsing, cache-file preparation, source loading, and intent construction are separated so file and validation behavior can be tested without mocking the Capacitor bridge. Verification builds Android both standalone and from the packed npm artifact as a subproject of a Capacitor-style host, covering the publish allowlist, plugin classpath, and property integration.

Cached filenames must resolve directly beneath the plugin-owned cache directory. Android grants recipients access through a `FileProvider` URI and `ClipData`; raw filesystem paths are never placed in the share intent.

## Web downloads use browser platform APIs directly

The web implementation creates a Blob URL, clicks a temporary anchor carrying the `download` attribute, removes the anchor, and revokes the URL after a delay. The delay avoids cancelling downloads in Safari before it has consumed the URL.

Capacitor 8 is the compatibility boundary, so the plugin does not reproduce FileSaver's legacy-browser fallbacks. This removes the only production dependency while keeping the existing `FileSharer.share()` API and browser user-activation behavior synchronous.
