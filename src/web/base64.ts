export function decodeBase64(base64Data: string): Uint8Array<ArrayBuffer> {
  const characters = atob(base64Data);
  const bytes = new Uint8Array(characters.length);

  for (let index = 0; index < characters.length; index += 1) {
    bytes[index] = characters.charCodeAt(index);
  }

  return bytes;
}
