import Foundation

enum FileSharerError: String, Error {
    case noFilename = "ERR_PARAM_NO_FILENAME"
    case noData = "ERR_PARAM_NO_DATA"
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

        let destination = try safeDestination(for: filename)
        do {
            try data.write(to: destination)
            return destination
        } catch {
            throw FileSharerError.cachingFailed
        }
    }

    private func safeDestination(for filename: String) throws -> URL {
        let resolvedDirectory = directory.resolvingSymlinksInPath().standardizedFileURL
        let destination = resolvedDirectory
            .appendingPathComponent(filename)
            .resolvingSymlinksInPath()
            .standardizedFileURL

        guard destination.deletingLastPathComponent() == resolvedDirectory else {
            throw FileSharerError.cachingFailed
        }
        return destination
    }
}
