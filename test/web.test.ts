import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  FileSharerErrorCode,
  type ShareFileOptions,
} from '../src/definitions';
import { FileSharerPluginWeb } from '../src/web';
import { saveBlob, saveUrl } from '../src/web/save-blob';

vi.mock('../src/web/save-blob', () => ({
  saveBlob: vi.fn(),
  saveUrl: vi.fn(),
}));

const mockedSaveBlob = vi.mocked(saveBlob);
const mockedSaveUrl = vi.mocked(saveUrl);

describe('FileSharerPluginWeb', () => {
  beforeEach(() => {
    mockedSaveBlob.mockClear();
    mockedSaveUrl.mockClear();
  });

  it('downloads decoded data with its filename and content type', async () => {
    const plugin = new FileSharerPluginWeb();

    await plugin.share({
      filename: 'test.txt',
      contentType: 'text/plain',
      base64Data: 'dGVzdA==',
    });

    expect(mockedSaveBlob).toHaveBeenCalledOnce();
    const [blob, filename] = mockedSaveBlob.mock.calls[0] ?? [];
    expect(filename).toBe('test.txt');
    expect(blob).toBeInstanceOf(Blob);
    expect((blob as Blob).type).toBe('text/plain');
    await expect((blob as Blob).text()).resolves.toBe('test');
  });

  it('downloads a caller-owned Blob URL without converting it', async () => {
    const plugin = new FileSharerPluginWeb();

    await plugin.share({
      filename: 'large.bin',
      contentType: 'application/octet-stream',
      path: 'blob:https://example.test/id',
    });

    expect(mockedSaveUrl).toHaveBeenCalledWith(
      'blob:https://example.test/id',
      'large.bin',
    );
    expect(mockedSaveBlob).not.toHaveBeenCalled();
  });

  it('keeps Base64 precedence when both sources are supplied', async () => {
    const plugin = new FileSharerPluginWeb();

    await plugin.share({
      filename: 'test.txt',
      contentType: 'text/plain',
      base64Data: 'dGVzdA==',
      path: 'https://example.test/file',
    });

    expect(mockedSaveBlob).toHaveBeenCalledOnce();
    expect(mockedSaveUrl).not.toHaveBeenCalled();
  });

  it.each([
    'https://example.test/file',
    'file:///tmp/file',
    'content://provider/file',
    '/tmp/file',
    'relative/file',
    'not a URL',
  ])('rejects unsupported Web path %s', async (path) => {
    const plugin = new FileSharerPluginWeb();

    await expect(
      plugin.share({ filename: 'test.txt', contentType: 'text/plain', path }),
    ).rejects.toThrow(FileSharerErrorCode.InvalidPath);
    expect(mockedSaveUrl).not.toHaveBeenCalled();
  });

  it.each([
    [
      { filename: 'test.txt', contentType: 'text/plain' },
      FileSharerErrorCode.NoData,
    ],
    [
      { base64Data: 'dGVzdA==', contentType: 'text/plain' },
      FileSharerErrorCode.NoFilename,
    ],
    [
      { base64Data: 'dGVzdA==', filename: 'test.txt' },
      FileSharerErrorCode.NoContentType,
    ],
    [
      {
        base64Data: 'not valid Base64!',
        filename: 'test.txt',
        contentType: 'text/plain',
      },
      FileSharerErrorCode.InvalidData,
    ],
  ])('rejects invalid options without starting a download', async (options, error) => {
    const plugin = new FileSharerPluginWeb();

    await expect(plugin.share(options as ShareFileOptions)).rejects.toThrow(error);
    expect(mockedSaveBlob).not.toHaveBeenCalled();
    expect(mockedSaveUrl).not.toHaveBeenCalled();
  });
});
