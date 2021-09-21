import { WebPlugin } from '@capacitor/core';
import { FileSharerPlugin, ShareFileOptions } from "./definitions";
export declare class FileSharerPluginWeb extends WebPlugin implements FileSharerPlugin {
    constructor();
    share(options: ShareFileOptions): Promise<void>;
    toByteArray(base64Data: string): Uint8Array;
}
declare const FileSharer: FileSharerPluginWeb;
export { FileSharer };
