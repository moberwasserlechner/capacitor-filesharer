import { registerPlugin } from '@capacitor/core';

import type { FileSharerPlugin } from './definitions';

const FileSharer = registerPlugin<FileSharerPlugin>('FileSharer', {
  web: () => import('./web').then(({ FileSharerPluginWeb }) => new FileSharerPluginWeb()),
});

export * from './definitions';
export { FileSharer };
