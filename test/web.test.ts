import { saveAs } from 'file-saver';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { ShareFileOptions } from '../src/definitions';
import { FileSharerPluginWeb } from '../src/web';

vi.mock('file-saver', () => ({
  saveAs: vi.fn(),
}));

const mockedSaveAs = vi.mocked(saveAs);

describe('FileSharerPluginWeb', () => {
  beforeEach(() => {
    mockedSaveAs.mockClear();
  });

  it('downloads decoded data with its filename and content type', async () => {
    const plugin = new FileSharerPluginWeb();

    await plugin.share({
      filename: 'test.txt',
      contentType: 'text/plain',
      base64Data: 'dGVzdA==',
    });

    expect(mockedSaveAs).toHaveBeenCalledOnce();
    const [blob, filename] = mockedSaveAs.mock.calls[0] ?? [];
    expect(filename).toBe('test.txt');
    expect(blob).toBeInstanceOf(Blob);
    expect((blob as Blob).type).toBe('text/plain');
    await expect((blob as Blob).text()).resolves.toBe('test');
  });

  it.each([
    [{ filename: 'test.txt', contentType: 'text/plain' }, 'ERR_PARAM_NO_DATA'],
    [{ base64Data: 'dGVzdA==', contentType: 'text/plain' }, 'ERR_PARAM_NO_FILENAME'],
    [{ base64Data: 'dGVzdA==', filename: 'test.txt' }, 'ERR_PARAM_NO_CONTENT_TYPE'],
  ])('rejects invalid options without starting a download', async (options, error) => {
    const plugin = new FileSharerPluginWeb();

    await expect(plugin.share(options as ShareFileOptions)).rejects.toThrow(error);
    expect(mockedSaveAs).not.toHaveBeenCalled();
  });
});
