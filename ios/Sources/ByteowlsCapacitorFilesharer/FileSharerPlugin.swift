import Capacitor
import Foundation
import UIKit

@objc(FileSharerPlugin)
public class FileSharerPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "FileSharerPlugin"
    public let jsName = "FileSharer"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "share", returnType: CAPPluginReturnPromise)
    ]

    private static let filenameParameter = "filename"
    private static let base64DataParameter = "base64Data"

    private static let noFilenameError = "ERR_PARAM_NO_FILENAME"
    private static let noDataError = "ERR_PARAM_NO_DATA"
    private static let invalidDataError = "ERR_PARAM_DATA_INVALID"
    private static let cachingFailedError = "ERR_FILE_CACHING_FAILED"

    @objc func share(_ call: CAPPluginCall) {
        guard let filename = call.getString(Self.filenameParameter) else {
            call.reject(Self.noFilenameError)
            return
        }
        guard let base64Data = call.getString(Self.base64DataParameter) else {
            call.reject(Self.noDataError)
            return
        }
        guard let data = Data(base64Encoded: base64Data) else {
            call.reject(Self.invalidDataError)
            return
        }

        let temporaryURL = FileManager.default.temporaryDirectory.appendingPathComponent(filename)

        do {
            try data.write(to: temporaryURL)

            DispatchQueue.main.async { [weak self] in
                guard let self else {
                    call.reject(Self.cachingFailedError)
                    return
                }

                let activity = UIActivityViewController(
                    activityItems: [temporaryURL],
                    applicationActivities: nil
                )
                let capacitorView = self.bridge?.viewController?.view

                // iPad requires a popover anchor while iPhone presents the same
                // controller modally.
                activity.popoverPresentationController?.sourceView = capacitorView
                activity.popoverPresentationController?.sourceRect = CGRect(
                    x: capacitorView?.center.x ?? 0,
                    y: capacitorView?.bounds.size.height ?? 0,
                    width: 0,
                    height: 0
                )

                self.bridge?.viewController?.present(activity, animated: true) {
                    call.resolve()
                }
            }
        } catch {
            call.reject(Self.cachingFailedError)
        }
    }
}
