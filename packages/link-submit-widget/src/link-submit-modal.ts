import { LitElement, PropertyValues, css, html } from 'lit';
import { property, state } from 'lit/decorators.js';

const OFFICIAL_APPLICATION_API = '/apis/api.link.halo.run/v1alpha1/link-applications';
const CAPTCHA_API = `${OFFICIAL_APPLICATION_API}/captcha`;

interface ProblemResponse {
  detail?: string;
  title?: string;
}

interface CaptchaResponse {
  challengeId: string;
  image: string;
  expiresInSeconds: number;
}

interface ApplicationResponse {
  id: string;
  status: string;
}

export function createOfficialApplicationPayload(formData: FormData, challengeId: string) {
  const rssUrl = String(formData.get('rssUrl') || '').trim();
  return {
    url: String(formData.get('url') || '').trim(),
    displayName: String(formData.get('displayName') || '').trim(),
    logo: String(formData.get('logo') || '').trim() || null,
    description: String(formData.get('description') || '').trim() || null,
    email: String(formData.get('email') || '').trim() || null,
    backlink: String(formData.get('backlink') || '').trim() || null,
    feedUrls: rssUrl ? [rssUrl] : [],
    challengeId,
    captchaCode: String(formData.get('captchaCode') || '').trim(),
  };
}

async function parseResponse<T>(response: Response): Promise<T | null> {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text) as T;
  } catch {
    throw new Error('接口返回格式错误');
  }
}

export class LinkSubmitModal extends LitElement {
  @property({ type: Boolean, reflect: true })
  open = false;

  @state()
  private submitting = false;

  @state()
  private fetchingSite = false;

  @state()
  private sitePreviewEnabled = true;

  @state()
  private captchaLoading = false;

  @state()
  private captcha?: CaptchaResponse;

  @state()
  private toastMessage = '';

  @state()
  private toastType: 'success' | 'error' = 'success';

  private siteInfoRequest = 0;
  private captchaRequest = 0;
  private toastTimer?: number;
  private captchaExpiryTimer?: number;
  private previousBodyOverflow = '';
  private focusOrigin: HTMLElement | null = null;
  private lastFetchedSiteInfo = { title: '', logo: '', description: '' };

  constructor() {
    super();
    void this.fetchConfiguration();
  }

  override willUpdate(changedProperties: PropertyValues) {
    if (!changedProperties.has('open')) return;
    if (this.open) {
      const activeElement = document.activeElement;
      this.focusOrigin = activeElement instanceof HTMLElement ? activeElement : null;
      this.previousBodyOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      if (!this.captcha) void this.fetchCaptcha();
    } else {
      document.body.style.overflow = this.previousBodyOverflow;
      const focusOrigin = this.focusOrigin;
      this.focusOrigin = null;
      if (focusOrigin?.isConnected) {
        requestAnimationFrame(() => focusOrigin.focus());
      }
    }
  }

  override updated(changedProperties: PropertyValues) {
    if (changedProperties.has('open') && this.open) {
      requestAnimationFrame(() =>
        this.shadowRoot?.querySelector<HTMLInputElement>('#input-url')?.focus()
      );
    }
  }

  override disconnectedCallback() {
    this.siteInfoRequest += 1;
    this.captchaRequest += 1;
    window.clearTimeout(this.toastTimer);
    window.clearTimeout(this.captchaExpiryTimer);
    document.body.style.overflow = this.previousBodyOverflow;
    super.disconnectedCallback();
  }

  private showToast(message: string, type: 'success' | 'error' = 'success') {
    window.clearTimeout(this.toastTimer);
    this.toastMessage = message;
    this.toastType = type;
    this.toastTimer = window.setTimeout(() => {
      this.toastMessage = '';
    }, 3600);
  }

  private async fetchConfiguration() {
    try {
      const response = await fetch('/apis/api.link.submit.halo.run/v1alpha1/configuration', {
        credentials: 'same-origin',
        headers: { Accept: 'application/json' },
      });
      if (!response.ok) return;
      const data = await parseResponse<{ linkPreviewEnabled?: boolean }>(response);
      this.sitePreviewEnabled = data?.linkPreviewEnabled !== false;
    } catch (error) {
      console.error('Failed to load link enhancement configuration:', error);
    }
  }

  private async fetchCaptcha() {
    const requestId = ++this.captchaRequest;
    this.captchaLoading = true;
    window.clearTimeout(this.captchaExpiryTimer);
    try {
      const response = await fetch(CAPTCHA_API, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { Accept: 'application/json' },
        cache: 'no-store',
      });
      const data = await parseResponse<CaptchaResponse & ProblemResponse>(response);
      if (requestId !== this.captchaRequest) return;
      if (!response.ok || !data?.challengeId || !data.image) {
        throw new Error(data?.detail || data?.title || '验证码加载失败');
      }
      this.captcha = data;
      const refreshAfter = Math.max(15, data.expiresInSeconds - 10) * 1000;
      this.captchaExpiryTimer = window.setTimeout(() => {
        this.captcha = undefined;
        if (this.open) void this.fetchCaptcha();
      }, refreshAfter);
    } catch (error) {
      if (requestId === this.captchaRequest) {
        this.captcha = undefined;
        this.showToast(error instanceof Error ? error.message : '验证码加载失败', 'error');
      }
    } finally {
      if (requestId === this.captchaRequest) this.captchaLoading = false;
    }
  }

  private async fetchSiteInfo() {
    const urlInput = this.shadowRoot?.querySelector<HTMLInputElement>('#input-url');
    if (!urlInput?.value.trim()) {
      this.showToast('请先填写网址', 'error');
      return;
    }

    let url = urlInput.value.trim();
    if (!/^https?:\/\//i.test(url)) {
      url = `https://${url}`;
      urlInput.value = url;
    }

    const requestId = ++this.siteInfoRequest;
    this.fetchingSite = true;
    try {
      const response = await fetch(
        `/apis/api.link.submit.halo.run/v1alpha1/site-info?url=${encodeURIComponent(url)}`,
        { credentials: 'same-origin', headers: { Accept: 'application/json' }, cache: 'no-store' }
      );
      const data = await parseResponse<{
        title?: string;
        description?: string;
        logo?: string;
        detail?: string;
      }>(response);
      if (requestId !== this.siteInfoRequest) return;
      if (!response.ok) {
        this.showToast(data?.detail || '获取网站信息失败，请手动填写', 'error');
        return;
      }
      this.fillSiteInfo(data?.title, data?.logo, data?.description);
      this.showToast(
        data?.title || data?.description || data?.logo
          ? '已自动填充网站信息'
          : '未获取到网站信息，请手动填写',
        data?.title || data?.description || data?.logo ? 'success' : 'error'
      );
    } catch (error) {
      if (requestId === this.siteInfoRequest) {
        this.showToast(error instanceof Error ? error.message : '获取网站信息失败', 'error');
      }
    } finally {
      if (requestId === this.siteInfoRequest) this.fetchingSite = false;
    }
  }

  private fillSiteInfo(title?: string, logo?: string, description?: string) {
    const nameInput = this.shadowRoot?.querySelector<HTMLInputElement>('#input-name');
    const logoInput = this.shadowRoot?.querySelector<HTMLInputElement>('#input-logo');
    const descriptionInput =
      this.shadowRoot?.querySelector<HTMLTextAreaElement>('#textarea-description');
    if (nameInput) this.replaceFetchedValue(nameInput, title, this.lastFetchedSiteInfo.title);
    if (logoInput) this.replaceFetchedValue(logoInput, logo, this.lastFetchedSiteInfo.logo);
    if (descriptionInput) {
      this.replaceFetchedValue(descriptionInput, description, this.lastFetchedSiteInfo.description);
    }
    this.lastFetchedSiteInfo = {
      title: title?.trim() || '',
      logo: logo?.trim() || '',
      description: description?.trim() || '',
    };
  }

  private replaceFetchedValue(
    field: HTMLInputElement | HTMLTextAreaElement,
    nextValue: string | undefined,
    previousFetchedValue: string
  ) {
    const currentValue = field.value.trim();
    if (!currentValue || currentValue === previousFetchedValue)
      field.value = nextValue?.trim() || '';
  }

  private handleClose() {
    this.siteInfoRequest += 1;
    this.fetchingSite = false;
    this.open = false;
  }

  private handleKeydown(event: KeyboardEvent) {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.handleClose();
      return;
    }
    if (event.key !== 'Tab') return;

    const modalContent = this.shadowRoot?.querySelector<HTMLElement>('.modal-content');
    const focusable = Array.from(
      modalContent?.querySelectorAll<HTMLElement>(
        'button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [href]'
      ) ?? []
    ).filter((element) => !element.hidden && element.getAttribute('aria-hidden') !== 'true');
    if (!focusable.length) return;

    const activeElement = this.shadowRoot?.activeElement as HTMLElement | null;
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  private async handleSubmit(event: SubmitEvent) {
    event.preventDefault();
    if (!this.captcha?.challengeId) {
      this.showToast('验证码尚未加载，请刷新后重试', 'error');
      return;
    }

    this.submitting = true;
    const form = event.currentTarget as HTMLFormElement;
    try {
      const response = await fetch(OFFICIAL_APPLICATION_API, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json, application/problem+json',
        },
        body: JSON.stringify(
          createOfficialApplicationPayload(new FormData(form), this.captcha.challengeId)
        ),
      });
      const result = await parseResponse<ApplicationResponse & ProblemResponse>(response);
      if (!response.ok) {
        this.showToast(result?.detail || result?.title || '提交失败，请检查表单', 'error');
        await this.fetchCaptcha();
        return;
      }
      this.showToast('申请已提交到官方友链审核，请等待处理');
      form.reset();
      this.captcha = undefined;
      window.setTimeout(() => this.handleClose(), 1200);
    } catch (error) {
      this.showToast(error instanceof Error ? error.message : '提交失败，请稍后重试', 'error');
      await this.fetchCaptcha();
    } finally {
      this.submitting = false;
    }
  }

  private renderForm() {
    return html`
      <header class="modal-header">
        <div>
          <h2 id="link-submit-modal-title">申请友链</h2>
          <p>申请将进入 Halo 官方链接插件审核，本插件仅提供表单与信息提取增强。</p>
        </div>
        <button
          type="button"
          class="icon-button"
          aria-label="关闭申请窗口"
          @click=${this.handleClose}
        >
          <svg aria-hidden="true" viewBox="0 0 24 24"><path d="M6 6l12 12M18 6 6 18" /></svg>
        </button>
      </header>
      <form @submit=${this.handleSubmit}>
        <div class="field field-wide">
          <label for="input-url">网站地址 <span>*</span></label>
          <div class="url-row">
            <input
              id="input-url"
              name="url"
              type="url"
              placeholder="https://example.com"
              required
            />
            ${
              this.sitePreviewEnabled
                ? html`<button
                    type="button"
                    class="secondary-button"
                    ?disabled=${this.fetchingSite}
                    @click=${this.fetchSiteInfo}
                  >
                    ${this.fetchingSite ? '获取中...' : '获取信息'}
                  </button>`
                : ''
            }
          </div>
          <small>修改网址后可再次获取，已自动填入的标题、Logo 和描述会同步更新。</small>
        </div>

        <div class="field-grid">
          <div class="field">
            <label for="input-name">网站名称 <span>*</span></label>
            <input id="input-name" name="displayName" type="text" required />
          </div>
          <div class="field">
            <label for="input-email">联系邮箱</label>
            <input id="input-email" name="email" type="email" autocomplete="email" />
          </div>
          <div class="field">
            <label for="input-logo">Logo 地址</label>
            <input id="input-logo" name="logo" type="url" placeholder="https://..." />
          </div>
          <div class="field">
            <label for="input-rss">RSS / Atom</label>
            <input id="input-rss" name="rssUrl" type="url" placeholder="https://.../feed.xml" />
          </div>
        </div>

        <div class="field">
          <label for="input-backlink">本站友链页面</label>
          <input
            id="input-backlink"
            name="backlink"
            type="url"
            placeholder="已添加本站链接的页面地址"
          />
        </div>

        <div class="field">
          <label for="textarea-description">网站描述</label>
          <textarea id="textarea-description" name="description" rows="3"></textarea>
        </div>

        <div class="captcha-row">
          <div class="field captcha-input">
            <label for="input-captcha">验证码 <span>*</span></label>
            <input id="input-captcha" name="captchaCode" type="text" autocomplete="off" required />
          </div>
          <button
            type="button"
            class="captcha-image"
            aria-label="刷新验证码"
            ?disabled=${this.captchaLoading}
            @click=${this.fetchCaptcha}
          >
            ${
              this.captcha?.image
                ? html`<img src=${this.captcha.image} alt="友链申请验证码" />`
                : html`<span>${this.captchaLoading ? '加载中' : '点击刷新'}</span>`
            }
          </button>
        </div>

        <footer class="modal-footer">
          <button type="button" class="secondary-button" @click=${this.handleClose}>取消</button>
          <button
            type="submit"
            class="primary-button"
            ?disabled=${this.submitting || this.captchaLoading || !this.captcha}
          >
            ${this.submitting ? '提交中...' : '提交申请'}
          </button>
        </footer>
      </form>
    `;
  }

  override render() {
    const ariaHidden: 'true' | 'false' = this.open ? 'false' : 'true';
    return html`
      <div
        class="modal-wrapper ${this.open ? 'is-open' : ''}"
        aria-hidden=${ariaHidden}
        @keydown=${this.handleKeydown}
      >
        <button
          class="modal-layer"
          type="button"
          aria-label="关闭申请窗口"
          @click=${this.handleClose}
        ></button>
        <section
          class="modal-content"
          role="dialog"
          aria-modal="true"
          aria-labelledby="link-submit-modal-title"
        >
          ${this.open ? this.renderForm() : ''}
        </section>
        ${
          this.toastMessage
            ? html`<div
                class="toast ${this.toastType}"
                role=${this.toastType === 'error' ? 'alert' : 'status'}
                aria-live="polite"
              >
                <strong>${this.toastType === 'error' ? '提交提示' : '操作成功'}</strong>
                <span>${this.toastMessage}</span>
              </div>`
            : ''
        }
      </div>
    `;
  }

  static override styles = css`
    :host {
      --accent: var(--link-submit-widget-form-button-bg-color, #d13e43);
      --accent-hover: var(--link-submit-widget-form-button-hover-bg-color, #b92f35);
      --surface: var(--link-submit-widget-base-bg-color, #ffffff);
      --surface-muted: color-mix(in srgb, var(--surface) 94%, #64748b);
      --text: var(--link-submit-widget-form-text-color, #18202b);
      --text-muted: var(--link-submit-widget-form-label-color, #5d6878);
      --border: var(--link-submit-widget-form-border-color, #d9dee7);
      --radius: var(--link-submit-widget-base-rounded, 8px);
      font:
        400 16px/1.55 ui-sans-serif,
        system-ui,
        -apple-system,
        BlinkMacSystemFont,
        'Segoe UI',
        sans-serif;
      color: var(--text);
    }
    * {
      box-sizing: border-box;
    }
    button,
    input,
    textarea {
      font: inherit;
      letter-spacing: 0;
    }
    .modal-wrapper {
      position: fixed;
      inset: 0;
      z-index: 999;
      display: grid;
      place-items: center;
      padding: 24px;
      visibility: hidden;
      pointer-events: none;
    }
    .modal-wrapper.is-open {
      visibility: visible;
      pointer-events: auto;
    }
    .modal-layer {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      border: 0;
      background: var(--link-submit-widget-modal-layer-color, rgb(15 23 42 / 0.64));
      opacity: 0;
      transition: opacity 180ms ease;
    }
    .is-open .modal-layer {
      opacity: 1;
    }
    .modal-content {
      position: relative;
      width: min(680px, 100%);
      max-height: min(820px, calc(100vh - 48px));
      overflow: auto;
      overscroll-behavior: contain;
      scrollbar-gutter: stable;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      box-shadow: 0 24px 70px rgb(15 23 42 / 0.24);
      opacity: 0;
      translate: 0 12px;
      transition:
        opacity 180ms ease,
        translate 220ms cubic-bezier(0.22, 1, 0.36, 1);
    }
    .is-open .modal-content {
      opacity: 1;
      translate: 0 0;
    }
    .modal-header {
      position: sticky;
      top: 0;
      z-index: 2;
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 20px;
      padding: 24px 28px 18px;
      background: color-mix(in srgb, var(--surface) 94%, transparent);
      border-bottom: 1px solid var(--border);
      backdrop-filter: blur(10px);
    }
    h2 {
      margin: 0;
      font-size: 1.25rem;
      line-height: 1.3;
    }
    .modal-header p {
      margin: 6px 0 0;
      max-width: 520px;
      color: var(--text-muted);
      font-size: 0.86rem;
    }
    .icon-button {
      flex: 0 0 40px;
      width: 40px;
      height: 40px;
      display: grid;
      place-items: center;
      padding: 0;
      border: 1px solid transparent;
      border-radius: 50%;
      background: transparent;
      color: var(--text-muted);
      cursor: pointer;
    }
    .icon-button:hover {
      background: var(--surface-muted);
      color: var(--text);
    }
    .icon-button svg {
      width: 21px;
      height: 21px;
      fill: none;
      stroke: currentColor;
      stroke-width: 1.8;
      stroke-linecap: round;
    }
    form {
      display: grid;
      gap: 20px;
      padding: 24px 28px 28px;
    }
    .field-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 18px;
    }
    .field {
      display: grid;
      gap: 7px;
      min-width: 0;
    }
    label {
      color: var(--text);
      font-size: 0.9rem;
      font-weight: 600;
    }
    label span {
      color: var(--accent);
    }
    small {
      color: var(--text-muted);
      font-size: 0.78rem;
    }
    input,
    textarea {
      width: 100%;
      min-width: 0;
      border: 1px solid var(--border);
      border-radius: calc(var(--radius) - 2px);
      background: var(--surface);
      color: var(--text);
      outline: none;
      transition:
        border-color 150ms ease,
        box-shadow 150ms ease;
    }
    input {
      min-height: 44px;
      padding: 0 12px;
    }
    textarea {
      resize: vertical;
      min-height: 88px;
      padding: 10px 12px;
    }
    input:focus,
    textarea:focus {
      border-color: var(--accent);
      box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent) 18%, transparent);
    }
    .url-row {
      display: grid;
      grid-template-columns: minmax(0, 1fr) auto;
      gap: 10px;
    }
    .captcha-row {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 180px;
      gap: 14px;
      align-items: end;
    }
    .captcha-image {
      min-height: 72px;
      display: grid;
      place-items: center;
      overflow: hidden;
      padding: 4px;
      border: 1px solid var(--border);
      border-radius: calc(var(--radius) - 2px);
      background: var(--surface-muted);
      color: var(--text-muted);
      cursor: pointer;
    }
    .captcha-image img {
      display: block;
      width: 100%;
      height: 62px;
      object-fit: contain;
    }
    .primary-button,
    .secondary-button {
      min-height: 42px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 0 18px;
      border-radius: calc(var(--radius) - 2px);
      font-weight: 600;
      cursor: pointer;
      transition:
        background-color 150ms ease,
        border-color 150ms ease,
        color 150ms ease,
        transform 100ms ease;
    }
    .primary-button {
      border: 1px solid var(--accent);
      background: var(--accent);
      color: #fff;
    }
    .primary-button:hover:not(:disabled) {
      background: var(--accent-hover);
      border-color: var(--accent-hover);
    }
    .secondary-button {
      border: 1px solid var(--border);
      background: var(--surface);
      color: var(--text);
    }
    .secondary-button:hover:not(:disabled) {
      border-color: color-mix(in srgb, var(--accent) 45%, var(--border));
      color: var(--accent);
    }
    button:active:not(:disabled) {
      transform: translateY(1px);
    }
    button:disabled {
      opacity: 0.55;
      cursor: not-allowed;
    }
    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      padding-top: 4px;
    }
    .toast {
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 3;
      width: min(360px, calc(100vw - 40px));
      display: grid;
      gap: 3px;
      padding: 14px 16px;
      border: 1px solid #a7d7bd;
      border-radius: var(--radius);
      background: #effaf4;
      color: #14532d;
      box-shadow: 0 14px 36px rgb(15 23 42 / 0.18);
    }
    .toast.error {
      border-color: #efb0b0;
      background: #fff1f1;
      color: #8a1c1c;
    }
    .toast span {
      font-size: 0.84rem;
    }
    @media (max-width: 620px) {
      .modal-wrapper {
        align-items: end;
        padding: 0;
      }
      .modal-content {
        width: 100%;
        max-height: calc(100dvh - 12px);
        border-radius: var(--radius) var(--radius) 0 0;
      }
      .modal-header {
        padding: 20px 18px 15px;
      }
      form {
        padding: 20px 18px 24px;
      }
      .field-grid,
      .captcha-row,
      .url-row {
        grid-template-columns: minmax(0, 1fr);
      }
      .captcha-image {
        min-height: 68px;
      }
      .modal-footer {
        position: sticky;
        bottom: 0;
        margin: 0 -18px -24px;
        padding: 14px 18px calc(14px + env(safe-area-inset-bottom));
        background: var(--surface);
        border-top: 1px solid var(--border);
      }
      .modal-footer button {
        flex: 1;
      }
    }
    @media (prefers-reduced-motion: reduce) {
      *,
      *::before,
      *::after {
        scroll-behavior: auto !important;
        transition-duration: 0.01ms !important;
        animation-duration: 0.01ms !important;
      }
    }
  `;
}

customElements.get('link-submit-modal') ||
  customElements.define('link-submit-modal', LinkSubmitModal);

declare global {
  interface HTMLElementTagNameMap {
    'link-submit-modal': LinkSubmitModal;
  }
}
