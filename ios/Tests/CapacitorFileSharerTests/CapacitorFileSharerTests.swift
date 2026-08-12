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

    func testRequiredParameterErrorCodesMatchPublicApi() {
        XCTAssertEqual(FileSharerError.noFilename.rawValue, "ERR_PARAM_NO_FILENAME")
        XCTAssertEqual(FileSharerError.noData.rawValue, "ERR_PARAM_NO_DATA")
    }
}
