import Foundation
import Capacitor

typealias JSObject = [String:Any]

@objc(FileSharerPlugin)
public class FileSharerPlugin: CAPPlugin {

    @objc func share(_ call: CAPPluginCall) {
        guard let filename = getString(call, "filename") else {
            call.reject("Option 'filename' is required!")
            return
        }
        guard let contentType = getString(call, "contentType") else {
            call.reject("Option 'contentType' is required!")
            return
        }
        guard let base64Data = getString(call, "base64Data") else {
            call.reject("Option 'base64Data' is required!")
            return
        }

    }

    private func getConfigObjectDeepest(_ options: [AnyHashable: Any?]!, key: String) -> [AnyHashable:Any?]? {
        let parts = key.split(separator: ".")
        
        var o = options
        for (_, k) in parts[0..<parts.count-1].enumerated() {
            if (o != nil) {
                o = o?[String(k)] as? [String:Any?] ?? nil
            }
        }
        return o
    }
    
    private func getConfigKey(_ key: String) -> String {
        let parts = key.split(separator: ".")
        if parts.last != nil {
            return String(parts.last!)
        }
        return ""
    }
    
    private func getOverwritableString(_ call: CAPPluginCall, _ key: String) -> String? {
        var base = getString(call, key)
        let ios = getString(call, "ios." + key)
        if ios != nil {
            base = ios
        }
        return base;
    }
    
    private func getValue(_ call: CAPPluginCall, _ key: String) -> Any? {
        let k = getConfigKey(key)
        let o = getConfigObjectDeepest(call.options, key: key)
        return o?[k] ?? nil
    }
    
    private func getString(_ call: CAPPluginCall, _ key: String) -> String? {
        let value = getValue(call, key)
        if value == nil {
            return nil
        }
        return value as? String
    }

}
