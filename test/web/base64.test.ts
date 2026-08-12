import { describe, expect, it } from 'vitest';

import { decodeBase64 } from '../../src/web/base64';

describe('decodeBase64', () => {
  it('decodes base64 into bounded chunks', () => {
    expect(decodeBase64('AAECAwQF', 4)).toEqual([
      new Uint8Array([0, 1, 2]),
      new Uint8Array([3, 4, 5]),
    ]);
  });

  it('preserves base64 groups split across source chunks', () => {
    expect(decodeBase64('AAH+/w==', 5)).toEqual([
      new Uint8Array([0, 1, 254]),
      new Uint8Array([255]),
    ]);
  });

  it('accepts base64 whitespace split across source chunks', () => {
    expect(decodeBase64('Y W\nJ j', 2)).toEqual([
      new Uint8Array([97, 98, 99]),
    ]);
  });

  it('decodes an empty value', () => {
    expect(decodeBase64('')).toEqual([]);
  });

  it.each(['%', 'YQ==YQ=='])(
    'rejects malformed base64 without accepting chunks independently',
    (base64Data) => {
      expect(() => decodeBase64(base64Data, 4)).toThrow();
    },
  );

  it.each([0, -1, 1.5])('rejects invalid chunk size %s', (chunkSize) => {
    expect(() => decodeBase64('YQ==', chunkSize)).toThrow(RangeError);
  });
});
