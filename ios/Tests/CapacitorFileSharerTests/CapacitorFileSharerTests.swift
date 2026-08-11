import XCTest
@testable import CapacitorFileSharer

final class CapacitorFileSharerTests: XCTestCase {
    func testPluginMetadataMatchesJavaScriptRegistration() {
        let plugin = FileSharerPlugin()

        XCTAssertEqual(plugin.identifier, "FileSharerPlugin")
        XCTAssertEqual(plugin.jsName, "FileSharer")
        XCTAssertEqual(plugin.pluginMethods.count, 1)
        XCTAssertEqual(plugin.pluginMethods.first?.name, "share")
    }
}
