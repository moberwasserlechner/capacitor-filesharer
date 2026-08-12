export const FileSharerErrorCode = {
  NoFilename: 'ERR_PARAM_NO_FILENAME',
  NoData: 'ERR_PARAM_NO_DATA',
  NoContentType: 'ERR_PARAM_NO_CONTENT_TYPE',
  InvalidData: 'ERR_PARAM_DATA_INVALID',
  InvalidPath: 'ERR_PARAM_PATH_INVALID',
  FileCachingFailed: 'ERR_FILE_CACHING_FAILED',
  LocalFileNotFound: 'ERR_LOCAL_FILE_NOT_FOUND',
  UserCancelled: 'USER_CANCELLED',
} as const;

export type FileSharerErrorCode =
  (typeof FileSharerErrorCode)[keyof typeof FileSharerErrorCode];

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
   * The filename with an extension.
   */
  filename: string;
  /**
   * The Base64-encoded file data. Takes precedence when `path` is also supplied.
   */
  base64Data?: string;
  /**
   * A platform-supported local source. Web accepts `blob:` URLs; Android accepts
   * raw paths, `file://`, `content://`, and Capacitor file URLs; iOS accepts raw
   * absolute paths, `file://`, and Capacitor file URLs. Network URLs are rejected.
   */
  path?: string;
  /**
   * The media type of the provided data.
   */
  contentType: string;
  /**
   * Android-specific share options.
   */
  android?: {
    /**
     * Override the default share-sheet title.
     */
    chooserTitle: string;
  };
}
