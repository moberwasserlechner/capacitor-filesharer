import {WebPlugin} from '@capacitor/core';
import {FileSharerPlugin, ShareFileOptions} from "./definitions";
import * as FileSaver from 'file-saver';

export class FileSharerPluginWeb extends WebPlugin implements FileSharerPlugin {

    constructor() {
        super({
            name: 'FileSharer',
            platforms: ['web']
        });
    }

    async share(options: ShareFileOptions): Promise<void> {
        return new Promise<any>((resolve, reject) => {
            let blob = new Blob(
                [ this.toByteArray(options.base64Data) ],
                {type: options.contentType}
                );
            FileSaver.saveAs(blob, options.filename);
            resolve();
        });
    }

    toByteArray(base64Data: string): Uint8Array {
        const byteCharacters = atob(base64Data);
        const byteNumbers = new Array(byteCharacters.length);
        for (let i = 0; i < byteCharacters.length; i++) {
            byteNumbers[i] = byteCharacters.charCodeAt(i);
        }
        return new Uint8Array(byteNumbers);
    }
}

const FileSharer = new FileSharerPluginWeb();

export { FileSharer };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(FileSharer);
