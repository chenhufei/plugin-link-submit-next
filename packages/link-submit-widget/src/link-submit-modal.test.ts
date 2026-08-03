import { afterEach, describe, expect, it, vi } from 'vitest';
import { LinkSubmitModal } from './link-submit-modal';

interface ConfigurationTestModal {
  fetchConfiguration: () => Promise<void>;
  sitePreviewEnabled: boolean;
}

function createModalForConfigurationTest() {
  const modal = Object.create(LinkSubmitModal.prototype) as unknown as ConfigurationTestModal;
  Object.defineProperty(modal, 'sitePreviewEnabled', {
    configurable: true,
    value: true,
    writable: true,
  });
  return modal;
}

describe('LinkSubmitModal configuration', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('disables site preview when the public configuration turns it off', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ linkPreviewEnabled: false }),
    }));
    const modal = createModalForConfigurationTest();

    await modal.fetchConfiguration();

    expect(modal.sitePreviewEnabled).toBe(false);
  });

  it('keeps the compatible default when configuration cannot be loaded', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')));
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const modal = createModalForConfigurationTest();

    await modal.fetchConfiguration();

    expect(modal.sitePreviewEnabled).toBe(true);
  });
});
