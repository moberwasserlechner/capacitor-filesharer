import { describe, expect, it } from 'vitest';

import { decodeBase64 } from '../../src/web/base64';

describe('decodeBase64', () => {
  it('decodes base64 into bytes', () => {
    expect(decodeBase64('AAH+/w==')).toEqual(new Uint8Array([0, 1, 254, 255]));
  });

  it('decodes an empty value', () => {
    expect(decodeBase64('')).toEqual(new Uint8Array());
  });

  it('rejects malformed base64', () => {
    expect(() => decodeBase64('%')).toThrow();
  });
});
