import Foundation
import Capacitor

@objc(FileSharerPlugin)
public class FileSharerPlugin: CAPPlugin {

    let PARAM_FILENAME = "filename"
    let PARAM_BASE64_DATA = "base64Data"
    let PARAM_PATH = "path"

    let ERR_PARAM_NO_FILENAME = "ERR_PARAM_NO_FILENAME"
    let ERR_PARAM_NO_DATA = "ERR_PARAM_NO_DATA"
    let ERR_PARAM_DATA_INVALID = "ERR_PARAM_DATA_INVALID"
    let ERR_FILE_CACHING_FAILED = "ERR_FILE_CACHING_FAILED"
    let ERR_FILE_NOT_FOUND = "ERR_FILE_NOT_FOUND"

    @objc func share(_ call: CAPPluginCall) {
        guard let filename = call.getString(self.PARAM_FILENAME) else {
            call.reject(self.ERR_PARAM_NO_FILENAME)
            return
        }
        let base64Data = call.getString(self.PARAM_BASE64_DATA)
        let filePath = call.getString(self.PARAM_PATH)

        if let base64Data = base64Data, !base64Data.isEmpty {
            let tmpUrl = FileManager.default.temporaryDirectory.appendingPathComponent(filename)

            guard let dataObj = Data(base64Encoded: base64Data) else {
                call.reject(self.ERR_PARAM_DATA_INVALID)
                return
            }

            do {
                try dataObj.write(to: tmpUrl)
                presentShareSheet(for: tmpUrl, call: call)
                return
            } catch {
                call.reject(self.ERR_FILE_CACHING_FAILED)
            }
        }

        if let filePath = filePath, !filePath.isEmpty {
            let actualFilePath = extractCapacitorFilePath(from: filePath)
            let originalFileUrl = URL(fileURLWithPath: actualFilePath).standardizedFileURL

            if FileManager.default.fileExists(atPath: originalFileUrl.path) {
                do {
                    let tmpUrl = FileManager.default.temporaryDirectory.appendingPathComponent(originalFileUrl.lastPathComponent)
                    try FileManager.default.copyItem(at: originalFileUrl, to: tmpUrl)
                    presentShareSheet(for: tmpUrl, call: call)
                    return
                } catch {
                    call.reject(self.ERR_FILE_CACHING_FAILED)
                    return
                }
            } else {
                call.reject(self.ERR_FILE_NOT_FOUND)
                return
            }
        }

        call.reject(self.ERR_PARAM_NO_DATA)
    }

    private func extractCapacitorFilePath(from path: String) -> String {
        if let range = path.range(of: "_capacitor_file_") {
            return String(path[range.upperBound...])
        }
        return path
    }

    private func presentShareSheet(for fileUrl: URL, call: CAPPluginCall) {
        DispatchQueue.main.async {
            let activityVC = UIActivityViewController(activityItems: [fileUrl], applicationActivities: nil)
            let capacitorView = self.bridge?.viewController?.view

            activityVC.popoverPresentationController?.sourceView = capacitorView
            activityVC.popoverPresentationController?.sourceRect =
                CGRect(x: capacitorView?.center.x ?? 0,
                       y: capacitorView?.bounds.size.height ?? 0,
                       width: 0,
                       height: 0)

            self.bridge?.viewController?.present(activityVC, animated: true, completion: {
                call.resolve()
            })
        }
    }
}
