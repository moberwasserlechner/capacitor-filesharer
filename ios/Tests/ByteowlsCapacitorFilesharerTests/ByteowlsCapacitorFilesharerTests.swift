import XCTest
@testable import ByteowlsCapacitorFilesharer

final class ByteowlsCapacitorFilesharerTests: XCTestCase {
    func testPluginCanBeCreated() {
        XCTAssertNotNil(FileSharerPlugin())
    }
}
