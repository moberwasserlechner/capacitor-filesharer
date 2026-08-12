import Foundation
import XCTest
@testable import CapacitorFileSharer

final class FileSharerFileStoreTests: XCTestCase {
    private var temporaryDirectory: URL!

    override func setUpWithError() throws {
        temporaryDirectory = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(
            at: temporaryDirectory,
            withIntermediateDirectories: true
        )
    }

    override func tearDownWithError() throws {
        try FileManager.default.removeItem(at: temporaryDirectory)
        temporaryDirectory = nil
    }

    func testCachesDecodedBase64Data() throws {
        let store = FileSharerFileStore(directory: temporaryDirectory)

        let cachedURL = try store.cache(filename: "hello.txt", base64Data: "aGVsbG8=")

        XCTAssertEqual(cachedURL, temporaryDirectory.appendingPathComponent("hello.txt"))
        XCTAssertEqual(try Data(contentsOf: cachedURL), Data("hello".utf8))
    }

    func testCopiesAnAbsolutePathWithoutLoadingItAsData() throws {
        let source = temporaryDirectory.appendingPathComponent("source.txt")
        try Data("content".utf8).write(to: source)
        let cacheDirectory = temporaryDirectory.appendingPathComponent("cache", isDirectory: true)
        try FileManager.default.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
        let store = FileSharerFileStore(directory: cacheDirectory)

        let cachedURL = try store.cache(
            filename: "shared.txt",
            base64Data: nil,
            path: source.path,
            capacitorOrigin: nil
        )

        XCTAssertEqual(cachedURL.lastPathComponent, "shared.txt")
        XCTAssertEqual(try String(contentsOf: cachedURL, encoding: .utf8), "content")
    }

    func testCopiesAFileURL() throws {
        let source = temporaryDirectory.appendingPathComponent("source file.txt")
        try Data("content".utf8).write(to: source)
        let cacheDirectory = temporaryDirectory.appendingPathComponent("cache", isDirectory: true)
        try FileManager.default.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
        let store = FileSharerFileStore(directory: cacheDirectory)

        let cachedURL = try store.cache(
            filename: "shared.txt",
            base64Data: nil,
            path: source.absoluteString,
            capacitorOrigin: nil
        )

        XCTAssertEqual(try String(contentsOf: cachedURL, encoding: .utf8), "content")
    }

    func testCopiesACapacitorFileURLFromTheConfiguredOrigin() throws {
        let source = temporaryDirectory.appendingPathComponent("source file.txt")
        try Data("content".utf8).write(to: source)
        let cacheDirectory = temporaryDirectory.appendingPathComponent("cache", isDirectory: true)
        try FileManager.default.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
        let store = FileSharerFileStore(directory: cacheDirectory)
        let encodedPath = source.path.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed)!
        let path = "capacitor://app/_capacitor_file_\(encodedPath)"

        let cachedURL = try store.cache(
            filename: "shared.txt",
            base64Data: nil,
            path: path,
            capacitorOrigin: URL(string: "capacitor://app")
        )

        XCTAssertEqual(try String(contentsOf: cachedURL, encoding: .utf8), "content")
    }

    func testRejectsNetworkAndSpoofedCapacitorURLs() {
        let store = FileSharerFileStore(directory: temporaryDirectory)
        for path in [
            "https://example.test/file",
            "https://example.test/_capacitor_file_/tmp/file",
            "blob:https://example.test/id"
        ] {
            XCTAssertThrowsError(
                try store.cache(
                    filename: "file.txt",
                    base64Data: nil,
                    path: path,
                    capacitorOrigin: URL(string: "capacitor://localhost")
                )
            ) { error in
                XCTAssertEqual(error as? FileSharerError, .invalidPath)
            }
        }
    }

    func testMapsMissingLocalFileToPublicError() {
        let store = FileSharerFileStore(directory: temporaryDirectory)

        XCTAssertThrowsError(
            try store.cache(
                filename: "file.txt",
                base64Data: nil,
                path: "/missing/file.txt",
                capacitorOrigin: nil
            )
        ) { error in
            XCTAssertEqual(error as? FileSharerError, .localFileNotFound)
        }
    }

    func testKeepsBase64Precedence() throws {
        let store = FileSharerFileStore(directory: temporaryDirectory)

        let cachedURL = try store.cache(
            filename: "file.txt",
            base64Data: "aGVsbG8=",
            path: "https://example.test/file",
            capacitorOrigin: nil
        )

        XCTAssertEqual(try String(contentsOf: cachedURL, encoding: .utf8), "hello")
    }

    func testRejectsInvalidBase64Data() {
        let store = FileSharerFileStore(directory: temporaryDirectory)

        XCTAssertThrowsError(try store.cache(filename: "invalid.txt", base64Data: "%%%")) { error in
            XCTAssertEqual(error as? FileSharerError, .invalidData)
        }
    }

    func testMapsWriteFailureToCachingError() throws {
        let unavailableDirectory = temporaryDirectory.appendingPathComponent("missing", isDirectory: true)
        let store = FileSharerFileStore(directory: unavailableDirectory)

        XCTAssertThrowsError(try store.cache(filename: "hello.txt", base64Data: "aGVsbG8=")) { error in
            XCTAssertEqual(error as? FileSharerError, .cachingFailed)
        }
    }

    func testKeepsCachedFilesInsideConfiguredDirectory() throws {
        let cacheDirectory = temporaryDirectory.appendingPathComponent("cache", isDirectory: true)
        try FileManager.default.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
        let store = FileSharerFileStore(directory: cacheDirectory)

        XCTAssertThrowsError(try store.cache(filename: "../escaped.txt", base64Data: "aGVsbG8=")) { error in
            XCTAssertEqual(error as? FileSharerError, .cachingFailed)
        }
        XCTAssertFalse(
            FileManager.default.fileExists(
                atPath: temporaryDirectory.appendingPathComponent("escaped.txt").path
            )
        )
    }
}
