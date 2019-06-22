import Foundation
import Capacitor

@objc(FileSharerPlugin)
public class FileSharerPlugin: CAPPlugin {

    @objc func share(_ call: CAPPluginCall) {
        guard let filename = call.getString("filename") else {
            call.reject("ERR_PARAM_NO_FILENAME")
            return
        }
        guard let base64Data = call.getString("base64Data") else {
            call.reject("ERR_PARAM_NO_DATA")
            return
        }
        
        let tmpUrl = FileManager.default.temporaryDirectory.appendingPathComponent(filename)
        
        guard let dataObj = Data(base64Encoded: base64Data) else {
            call.reject("ERR_PARAM_DATA_INVALID")
            return
        }
        do {
            try dataObj.write(to: tmpUrl)
            
            let activityVC = UIActivityViewController(activityItems: [tmpUrl], applicationActivities: nil)
            DispatchQueue.main.async {
                if let popOver = activityVC.popoverPresentationController {
                    popOver.sourceView = self.bridge.viewController.view
                    //popOver.sourceRect =
                    //popOver.barButtonItem
                }
                self.bridge.viewController.present(activityVC, animated: true, completion: {
                    call.resolve()
                })
            }
        } catch {
            call.reject("ERR_FILE_CACHING_FAILED")
        }
    }
    
}
