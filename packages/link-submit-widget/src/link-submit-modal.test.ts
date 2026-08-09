import { afterEach, describe, expect, it, vi } from 'vitest';
import { LinkSubmitModal } from './link-submit-modal';

interface ConfigurationTestModal {
  fetchConfiguration: () => Promise<void>;
  sitePreviewEnabled: boolean;
}

interface SiteInfoTestModal {
  fillSiteInfo: (title?: string, logo?: string, description?: string) => void;
  lastFetchedSiteInfo: {
    title: string;
    logo: string;
    description: string;
  };
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

function createModalForSiteInfoTest() {
  const fields = {
    '#input-name': { value: '' },
    '#input-logo': { value: '' },
    '#textarea-description': { value: '' },
  };
  const modal = Object.create(LinkSubmitModal.prototype) as unknown as SiteInfoTestModal;
  Object.defineProperty(modal, 'shadowRoot', {
    configurable: true,
    value: {
      querySelector: (selector: keyof typeof fields) => fields[selector],
    },
  });
  Object.defineProperty(modal, 'lastFetchedSiteInfo', {
    configurable: true,
    value: { title: '', logo: '', description: '' },
    writable: true,
  });
  return { fields, modal };
}

describe('LinkSubmitModal configuration', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('disables site preview when the public configuration turns it off', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ linkPreviewEnabled: false }),
      })
    );
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

describe('LinkSubmitModal site information', () => {
  it('updates values that came from the previous website lookup', () => {
    const { fields, modal } = createModalForSiteInfoTest();

    modal.fillSiteInfo('Old title', 'https://old.test/logo.png', 'Old description');
    modal.fillSiteInfo('New title', 'https://new.test/logo.png', 'New description');

    expect(fields['#input-name'].value).toBe('New title');
    expect(fields['#input-logo'].value).toBe('https://new.test/logo.png');
    expect(fields['#textarea-description'].value).toBe('New description');
  });

  it('keeps values that the user changed manually', () => {
    const { fields, modal } = createModalForSiteInfoTest();

    modal.fillSiteInfo('Old title', 'https://old.test/logo.png', 'Old description');
    fields['#input-name'].value = 'Custom title';
    modal.fillSiteInfo('New title', 'https://new.test/logo.png', 'New description');

    expect(fields['#input-name'].value).toBe('Custom title');
    expect(fields['#input-logo'].value).toBe('https://new.test/logo.png');
    expect(fields['#textarea-description'].value).toBe('New description');
  });

  it('clears stale automatic values when the new website has no metadata', () => {
    const { fields, modal } = createModalForSiteInfoTest();

    modal.fillSiteInfo('Old title', 'https://old.test/logo.png', 'Old description');
    modal.fillSiteInfo();

    expect(fields['#input-name'].value).toBe('');
    expect(fields['#input-logo'].value).toBe('');
    expect(fields['#textarea-description'].value).toBe('');
  });
});
