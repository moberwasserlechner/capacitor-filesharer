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
}
