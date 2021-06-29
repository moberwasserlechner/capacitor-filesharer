import Foundation
import Capacitor

@objc(FileSharerPlugin)
public class FileSharerPlugin: CAPPlugin {

    let PARAM_FILENAME = "filename"
    let PARAM_BASE64_DATA = "base64Data"

    let ERR_PARAM_NO_FILENAME = "ERR_PARAM_NO_FILENAME"
    let ERR_PARAM_NO_DATA = "ERR_PARAM_NO_DATA"
    let ERR_PARAM_DATA_INVALID = "ERR_PARAM_DATA_INVALID"
    let ERR_FILE_CACHING_FAILED = "ERR_FILE_CACHING_FAILED"

    @objc func share(_ call: CAPPluginCall) {
        guard let filename = call.getString(self.PARAM_FILENAME) else {
            call.reject(self.ERR_PARAM_NO_FILENAME)
            return
        }
        guard let base64Data = call.getString(self.PARAM_BASE64_DATA) else {
            call.reject(self.ERR_PARAM_NO_DATA)
            return
        }

        let tmpUrl = FileManager.default.temporaryDirectory.appendingPathComponent(filename)

        guard let dataObj = Data(base64Encoded: base64Data) else {
            call.reject(self.ERR_PARAM_DATA_INVALID)
            return
        }

        do {
            try dataObj.write(to: tmpUrl)
            
            DispatchQueue.main.async {
                let activityVC = UIActivityViewController(activityItems: [tmpUrl], applicationActivities: nil)
                // must be on the main thread
                let capacitorView = self.bridge?.viewController.view

                // On iPhones the activity is shown as modal
                // On iPads on the other side it must be a popover by providing either a (sourceView AND sourceRect) OR a barButtonItem.
                activityVC.popoverPresentationController?.sourceView = capacitorView
                activityVC.popoverPresentationController?.sourceRect =
                    CGRect(x: capacitorView?.center.x ?? 0,
                           y: capacitorView?.bounds.size.height ?? 0,
                           width: 0,
                           height: 0)

                self.bridge?.viewController.present(activityVC, animated: true, completion: {
                    call.resolve()
                })
            }
        } catch {
            call.reject(self.ERR_FILE_CACHING_FAILED)
        }
    }

}
