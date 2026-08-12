import { WebPlugin } from '@capacitor/core';

import {
  FileSharerErrorCode,
  type FileSharerPlugin,
  type ShareFileOptions,
} from './definitions';
import { decodeBase64 } from './web/base64';
import { saveBlob, saveUrl } from './web/save-blob';

export class FileSharerPluginWeb extends WebPlugin implements FileSharerPlugin {
  async share(options: ShareFileOptions): Promise<void> {
    if (!options.filename) {
      throw new Error(FileSharerErrorCode.NoFilename);
    }
    if (!options.contentType) {
      throw new Error(FileSharerErrorCode.NoContentType);
    }

    if (options.base64Data) {
      let blobParts: Uint8Array<ArrayBuffer>[];
      try {
        blobParts = decodeBase64(options.base64Data);
      } catch {
        throw new Error(FileSharerErrorCode.InvalidData);
      }

      const blob = new Blob(blobParts, {
        type: options.contentType,
      });
      saveBlob(blob, options.filename);
      return;
    }

    if (!options.path) {
      throw new Error(FileSharerErrorCode.NoData);
    }
    if (!isBlobUrl(options.path)) {
      throw new Error(FileSharerErrorCode.InvalidPath);
    }
    saveUrl(options.path, options.filename);
  }
}

function isBlobUrl(value: string): boolean {
  try {
    return new URL(value).protocol === 'blob:';
  } catch {
    return false;
  }
}
