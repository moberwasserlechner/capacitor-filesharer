const DEFAULT_CHUNK_SIZE = 4 * 1024 * 1024;
const BASE64_WHITESPACE = /[\t\n\f\r ]/g;

/**
 * Decodes Base64 into bounded byte chunks for use as Blob parts.
 *
 * Keeping each `atob` result bounded avoids holding a second decoded copy of
 * the complete file in memory. The optional chunk size is the number of
 * encoded source characters processed per iteration.
 */
export function decodeBase64(
  base64Data: string,
  chunkSize = DEFAULT_CHUNK_SIZE,
): Uint8Array<ArrayBuffer>[] {
  if (!Number.isInteger(chunkSize) || chunkSize <= 0) {
    throw new RangeError('Base64 chunk size must be a positive integer');
  }

  const chunks: Uint8Array<ArrayBuffer>[] = [];
  let pending = '';

  for (let offset = 0; offset < base64Data.length; offset += chunkSize) {
    pending += base64Data
      .slice(offset, offset + chunkSize)
      .replace(BASE64_WHITESPACE, '');

    // Keep the padded group pending until the end so `atob` can reject any
    // data that incorrectly appears after the padding.
    const paddingIndex = pending.indexOf('=');
    const decodableLength =
      paddingIndex === -1
        ? pending.length - (pending.length % 4)
        : paddingIndex - (paddingIndex % 4);

    if (decodableLength > 0) {
      chunks.push(decodeChunk(pending.slice(0, decodableLength)));
      pending = pending.slice(decodableLength);
    }
  }

  if (pending.length > 0) {
    chunks.push(decodeChunk(pending));
  }

  return chunks;
}

/** Converts one complete Base64 group sequence into its binary bytes. */
function decodeChunk(base64Data: string): Uint8Array<ArrayBuffer> {
  const characters = atob(base64Data);
  const bytes = new Uint8Array(characters.length);

  for (let index = 0; index < characters.length; index += 1) {
    bytes[index] = characters.charCodeAt(index);
  }

  return bytes;
}
