import Foundation

enum FileSharerError: String, Error {
    case invalidData = "ERR_PARAM_DATA_INVALID"
    case cachingFailed = "ERR_FILE_CACHING_FAILED"
}

struct FileSharerFileStore {
    private let directory: URL

    init(directory: URL = FileManager.default.temporaryDirectory) {
        self.directory = directory
    }

    func cache(filename: String, base64Data: String) throws -> URL {
        guard let data = Data(base64Encoded: base64Data) else {
            throw FileSharerError.invalidData
        }

        let destination = directory.appendingPathComponent(filename)
        do {
            try data.write(to: destination)
            return destination
        } catch {
            throw FileSharerError.cachingFailed
        }
    }
}
