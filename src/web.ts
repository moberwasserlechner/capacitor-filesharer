import { WebPlugin } from '@capacitor/core';
import { saveAs } from 'file-saver';

import type { FileSharerPlugin, ShareFileOptions } from './definitions';
import { decodeBase64 } from './web/base64';

export class FileSharerPluginWeb extends WebPlugin implements FileSharerPlugin {
  async share(options: ShareFileOptions): Promise<void> {
    if (!options.base64Data) {
      throw new Error('ERR_PARAM_NO_DATA');
    }
    if (!options.filename) {
      throw new Error('ERR_PARAM_NO_FILENAME');
    }
    if (!options.contentType) {
      throw new Error('ERR_PARAM_NO_CONTENT_TYPE');
    }

    const blob = new Blob([decodeBase64(options.base64Data)], {
      type: options.contentType,
    });
    saveAs(blob, options.filename);
  }
}
