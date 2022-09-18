import {WebPlugin} from '@capacitor/core';
import type {FileSharerPlugin, ShareFileOptions} from "./definitions";
import * as FileSaver from 'file-saver';
import {WebUtils} from "./web-utils";

export class FileSharerPluginWeb extends WebPlugin implements FileSharerPlugin {

    async share(options: ShareFileOptions): Promise<void> {
        return new Promise((resolve, reject) => {
            if (!options.base64Data || options.base64Data.length == 0) {
                reject(new Error("ERR_PARAM_NO_DATA"));
            } else if (!options.filename || options.filename.length == 0) {
                reject(new Error("ERR_PARAM_NO_FILENAME"));
            } else if (!options.contentType || options.contentType.length == 0) {
                reject(new Error("ERR_PARAM_NO_CONTENT_TYPE"));
            }

            let blob = new Blob(
                [
                    WebUtils.toByteArray(options.base64Data!)
                ],
                {
                    type: options.contentType
                }
            );
            FileSaver.saveAs(blob, options.filename);
            resolve();
        });
    }

}
