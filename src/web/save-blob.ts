const OBJECT_URL_LIFETIME_MS = 40_000;

/**
 * Starts a browser download for a locally created Blob.
 *
 * The delayed revocation gives Safari enough time to consume the object URL
 * after the temporary anchor has been clicked.
 */
export function saveBlob(blob: Blob, filename: string): void {
  const objectUrl = URL.createObjectURL(blob);

  try {
    saveUrl(objectUrl, filename);
  } finally {
    setTimeout(() => URL.revokeObjectURL(objectUrl), OBJECT_URL_LIFETIME_MS);
  }
}

/** Starts a download without taking ownership of the supplied URL. */
export function saveUrl(url: string, filename: string): void {
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.rel = 'noopener';
  anchor.style.display = 'none';

  try {
    document.body.append(anchor);
    anchor.click();
  } finally {
    anchor.remove();
  }
}
