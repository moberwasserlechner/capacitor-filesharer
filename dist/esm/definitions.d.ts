declare global {
    interface PluginRegistry {
        FileSharer?: FileSharerPlugin;
    }
}
export interface FileSharerPlugin {
    /**
     * Share a file using the native share dialog on Android and iOS and download the file on Web.
     * @param {ShareFileOptions} options
     * @returns {Promise<void>}
     */
    share(options: ShareFileOptions): Promise<void>;
}
export interface ShareFileOptions {
    /**
     * The filename with a extension.
     */
    filename: string;
    /**
     * The base64 encoded data.
     */
    base64Data: string;
    /**
     * The content type of the provided data.
     */
    contentType: string;
    /**
     * Custom options for the platform "android"
     */
    android?: {
        /**
         * Override the default share sheet title
         */
        chooserTitle: string;
    };
}
