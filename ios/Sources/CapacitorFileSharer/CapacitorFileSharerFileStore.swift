import Foundation

enum FileSharerError: String, Error {
    case noFilename = "ERR_PARAM_NO_FILENAME"
    case noData = "ERR_PARAM_NO_DATA"
    case invalidData = "ERR_PARAM_DATA_INVALID"
    case invalidPath = "ERR_PARAM_PATH_INVALID"
    case cachingFailed = "ERR_FILE_CACHING_FAILED"
    case localFileNotFound = "ERR_LOCAL_FILE_NOT_FOUND"
}

struct FileSharerFileStore {
    private static let capacitorFilePrefix = "/_capacitor_file_"
    private let directory: URL
    private let fileManager: FileManager

    init(
        directory: URL = FileManager.default.temporaryDirectory,
        fileManager: FileManager = .default
    ) {
        self.directory = directory
        self.fileManager = fileManager
    }

    func cache(
        filename: String,
        base64Data: String?,
        path: String?,
        capacitorOrigin: URL?
    ) throws -> URL {
        if let base64Data {
            return try cacheBase64(filename: filename, base64Data: base64Data)
        }
        guard let path, !path.isEmpty else {
            throw FileSharerError.noData
        }
        return try cacheFile(
            filename: filename,
            source: try resolve(path: path, capacitorOrigin: capacitorOrigin)
        )
    }

    func cache(filename: String, base64Data: String) throws -> URL {
        try cache(filename: filename, base64Data: base64Data, path: nil, capacitorOrigin: nil)
    }

    private func cacheBase64(filename: String, base64Data: String) throws -> URL {
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

    private func cacheFile(filename: String, source: URL) throws -> URL {
        var isDirectory: ObjCBool = false
        guard fileManager.fileExists(atPath: source.path, isDirectory: &isDirectory),
              !isDirectory.boolValue,
              fileManager.isReadableFile(atPath: source.path) else {
            throw FileSharerError.localFileNotFound
        }

        let destination = try safeDestination(for: filename)
        if source.standardizedFileURL == destination.standardizedFileURL {
            return source
        }

        do {
            if fileManager.fileExists(atPath: destination.path) {
                try fileManager.removeItem(at: destination)
            }
            try fileManager.copyItem(at: source, to: destination)
            return destination
        } catch {
            throw FileSharerError.cachingFailed
        }
    }

    private func resolve(path: String, capacitorOrigin: URL?) throws -> URL {
        if path.hasPrefix("/") {
            return URL(fileURLWithPath: path).standardizedFileURL
        }

        guard let url = URL(string: path), let scheme = url.scheme?.lowercased() else {
            throw FileSharerError.invalidPath
        }

        if scheme == "file" {
            guard url.isFileURL, url.host == nil || url.host == "" || url.host == "localhost" else {
                throw FileSharerError.invalidPath
            }
            return url.standardizedFileURL
        }

        guard let origin = capacitorOrigin,
              url.user == nil,
              url.password == nil,
              url.query == nil,
              url.fragment == nil,
              sameOrigin(url, origin),
              url.path.hasPrefix(Self.capacitorFilePrefix) else {
            throw FileSharerError.invalidPath
        }

        let nativePath = String(url.path.dropFirst(Self.capacitorFilePrefix.count))
        guard nativePath.hasPrefix("/") else {
            throw FileSharerError.invalidPath
        }
        return URL(fileURLWithPath: nativePath).standardizedFileURL
    }

    private func sameOrigin(_ lhs: URL, _ rhs: URL) -> Bool {
        lhs.scheme?.lowercased() == rhs.scheme?.lowercased() &&
            lhs.host?.lowercased() == rhs.host?.lowercased() &&
            effectivePort(lhs) == effectivePort(rhs)
    }

    private func effectivePort(_ url: URL) -> Int? {
        if let port = url.port { return port }
        switch url.scheme?.lowercased() {
        case "http": return 80
        case "https": return 443
        default: return nil
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
