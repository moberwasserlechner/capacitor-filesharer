declare global {
    interface PluginRegistry {
        FileSharer?: FileSharerPlugin;
    }
}

export interface FileSharerPlugin {
    /**
     *
     * @param {ShareFileOptions} options
     * @returns {Promise<void>}
     */
    share(options: ShareFileOptions): Promise<void>;
}

export interface ShareFileOptions {

    filename: string;

    contentType: string;

    base64Data: string;

    /**
     * Custom options for the platform "web"
     */
    web?: {

    },
    /**
     * Custom options for the platform "android"
     */
    android?: {
        chooserTitle: string; // Override the default share sheet title
    },
    /**
     * Custom options for the platform "ios"
     */
    ios?: {

    }
}
