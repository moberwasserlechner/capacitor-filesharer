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

    private static let noFilenameError = "ERR_PARAM_NO_FILENAME"
    private static let noDataError = "ERR_PARAM_NO_DATA"
    private let fileStore = FileSharerFileStore()

    @objc func share(_ call: CAPPluginCall) {
        guard let filename = call.getString("filename") else {
            call.reject(Self.noFilenameError)
            return
        }
        guard let base64Data = call.getString("base64Data") else {
            call.reject(Self.noDataError)
            return
        }
        do {
            let temporaryURL = try fileStore.cache(filename: filename, base64Data: base64Data)

            DispatchQueue.main.async { [weak self] in
                guard let self else {
                    call.reject(FileSharerError.cachingFailed.rawValue)
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
        } catch let error as FileSharerError {
            call.reject(error.rawValue)
        } catch {
            call.reject(FileSharerError.cachingFailed.rawValue)
        }
    }
}
