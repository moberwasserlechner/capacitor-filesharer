import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { saveBlob } from '../../src/web/save-blob';

describe('saveBlob', () => {
  const click = vi.fn();
  const remove = vi.fn();
  const append = vi.fn();
  const createObjectURL = vi.fn(() => 'blob:test');
  const revokeObjectURL = vi.fn();
  const anchor = {
    href: '',
    download: '',
    rel: '',
    style: { display: '' },
    click,
    remove,
  };

  beforeEach(() => {
    vi.useFakeTimers();
    click.mockReset();
    remove.mockReset();
    append.mockReset();
    createObjectURL.mockClear();
    revokeObjectURL.mockClear();
    anchor.href = '';
    anchor.download = '';
    anchor.rel = '';
    anchor.style.display = '';

    vi.stubGlobal('URL', { createObjectURL, revokeObjectURL });
    vi.stubGlobal('document', {
      createElement: vi.fn(() => anchor),
      body: { append },
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('clicks a temporary download anchor and later revokes its object URL', () => {
    const blob = new Blob(['test'], { type: 'text/plain' });

    saveBlob(blob, 'test.txt');

    expect(createObjectURL).toHaveBeenCalledWith(blob);
    expect(anchor).toMatchObject({
      href: 'blob:test',
      download: 'test.txt',
      rel: 'noopener',
      style: { display: 'none' },
    });
    expect(append).toHaveBeenCalledWith(anchor);
    expect(click).toHaveBeenCalledOnce();
    expect(remove).toHaveBeenCalledOnce();
    expect(revokeObjectURL).not.toHaveBeenCalled();

    vi.advanceTimersByTime(40_000);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:test');
  });

  it('still removes the anchor and schedules cleanup when clicking fails', () => {
    click.mockImplementationOnce(() => {
      throw new Error('click failed');
    });

    expect(() => saveBlob(new Blob(), 'test.txt')).toThrow('click failed');
    expect(remove).toHaveBeenCalledOnce();

    vi.runAllTimers();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:test');
  });
});
