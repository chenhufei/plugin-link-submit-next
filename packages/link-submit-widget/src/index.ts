import {
  buildSiteInfoRequestUrl,
  hasAnyFormValue,
  normalizeSiteUrl,
  shouldReplaceFieldValue,
  siteInfoErrorMessage,
  toFormValues,
  type SiteInfo,
  type SiteInfoField,
} from './site-info';

const ENHANCE_SCRIPT_SELECTOR = 'script[data-link-enhance]';
// 官方主题与插件的表单 action 可能写成相对路径，也可能写成绝对地址。
// 用 jQuery 风格的 [action*=] 选择器去匹配时，[action*="/links/apply/submit"] 与
// [action*="links/apply/submit"] 必须二选一，总有一半场景会漏掉，这里直接按路径判断。
// PluginLinks 2.5 起访客申请改走 REST 接口，action 不再固定，
// 因此再补一条「按字段特征识别」的兜底：含 url + displayName + logo 的表单即申请表单。
// 本插件不提交申请、不复制官方表单，所以不出现任何申请接口地址。
const APPLY_FORM_ACTION_PATHS = ['/links/apply/submit'];
const APPLY_FORM_REQUIRED_FIELDS = ['url', 'displayName', 'logo'];
const TOOLS_CLASS = 'link-enhance-tools';
const FIELD_CLASS = 'link-enhance-url-wrap';
const BUTTON_CLASS = 'link-enhance-fetch';
const STATUS_CLASS = 'link-enhance-status';
const ENHANCED_ATTRIBUTE = 'data-link-enhanced';

/** 与官方友链后台编辑弹窗一致：mdi:web-refresh 地球图标 */
const FETCH_ICON =
  '<svg class="link-enhance-icon" viewBox="0 0 24 24" width="16" height="16" fill="currentColor" aria-hidden="true">' +
  '<path d="M13.82 14H9.66C9.56 13.34 9.5 12.68 9.5 12S9.56 10.65 9.66 10H14.34C14.43 10.65 14.5 11.32 14.5 12C14.5 12.5 14.46 13 14.4 13.46C15 12.96 15.72 12.57 16.5 12.32C16.5 12.21 16.5 12.11 16.5 12C16.5 11.32 16.44 10.66 16.36 10H19.74C19.9 10.64 20 11.31 20 12C20 12.06 20 12.12 20 12.18C20.7 12.35 21.35 12.63 21.95 13C22 12.68 22 12.34 22 12C22 6.5 17.5 2 12 2C6.47 2 2 6.5 2 12C2 17.5 6.5 22 12 22C12.34 22 12.68 22 13 21.95C12.59 21.29 12.29 20.55 12.13 19.75C12.09 19.82 12.05 19.89 12 19.96C11.17 18.76 10.5 17.43 10.09 16H12.5C12.81 15.25 13.26 14.58 13.82 14M18.92 8H15.97C15.65 6.75 15.19 5.55 14.59 4.44C16.43 5.07 17.96 6.34 18.92 8M12 4.03C12.83 5.23 13.5 6.57 13.91 8H10.09C10.5 6.57 11.17 5.23 12 4.03M4.26 14C4.1 13.36 4 12.69 4 12S4.1 10.64 4.26 10H7.64C7.56 10.66 7.5 11.32 7.5 12S7.56 13.34 7.64 14H4.26M5.08 16H8C8.35 17.25 8.8 18.45 9.4 19.56C7.57 18.93 6.03 17.65 5.08 16M8 8H5.08C6.03 6.34 7.57 5.06 9.4 4.44C8.8 5.55 8.35 6.75 8 8M20.83 15.67L22 14.5V18.5H18L19.77 16.73C19.32 16.28 18.69 16 18 16C16.62 16 15.5 17.12 15.5 18.5S16.62 21 18 21C18.82 21 19.54 20.61 20 20H21.71C21.12 21.47 19.68 22.5 18 22.5C15.79 22.5 14 20.71 14 18.5S15.79 14.5 18 14.5C19.11 14.5 20.11 14.95 20.83 15.67Z"/>' +
  '</svg>';
const FETCH_SPINNER = '<span class="link-enhance-spinner" aria-hidden="true"></span>';

const FORM_FIELDS: SiteInfoField[] = ['displayName', 'logo', 'description'];

type StatusKind = 'idle' | 'pending' | 'success' | 'error';

type FormState = {
  requestId: number;
  filled: Partial<Record<SiteInfoField, string>>;
  button: HTMLButtonElement;
  status: HTMLElement;
};

const formStates = new WeakMap<HTMLFormElement, FormState>();

function isLinkPreviewEnabled(): boolean {
  const script = document.querySelector<HTMLScriptElement>(ENHANCE_SCRIPT_SELECTOR);
  return !script || script.dataset.linkPreviewEnabled !== 'false';
}

function setStatus(state: FormState, message: string, kind: StatusKind): void {
  const pending = kind === 'pending';
  state.status.textContent = message;
  state.status.dataset.status = kind;
  state.button.disabled = pending;
  state.button.setAttribute('aria-busy', pending ? 'true' : 'false');
  // 与官方后台一致：请求中把图标换成转圈
  state.button.innerHTML = pending ? FETCH_SPINNER : FETCH_ICON;
}

function findField<T extends HTMLInputElement | HTMLTextAreaElement>(
  form: HTMLFormElement,
  name: SiteInfoField
): T | null {
  return form.querySelector<T>(`[name="${name}"]`);
}

function applyFieldValue(
  form: HTMLFormElement,
  state: FormState,
  field: SiteInfoField,
  value: string
): boolean {
  const input = findField<HTMLInputElement | HTMLTextAreaElement>(form, field);
  if (!input) return false;
  if (!shouldReplaceFieldValue(input.value, state.filled[field])) return false;
  input.value = value;
  input.dispatchEvent(new Event('input', { bubbles: true }));
  state.filled[field] = value;
  return value.length > 0;
}

async function fetchSiteInfo(form: HTMLFormElement, state: FormState, urlInput: HTMLInputElement) {
  const requestId = ++state.requestId;
  const rawUrl = urlInput.value;
  const normalizedUrl = normalizeSiteUrl(rawUrl);

  if (!normalizedUrl) {
    setStatus(state, '请先填写网址', 'error');
    return;
  }
  urlInput.value = normalizedUrl;

  setStatus(state, '正在获取网站信息…', 'pending');
  try {
    const response = await fetch(buildSiteInfoRequestUrl(normalizedUrl), {
      credentials: 'same-origin',
      headers: { Accept: 'application/json' },
      cache: 'no-store',
    });
    const info = (await response.json().catch(() => ({}))) as SiteInfo;
    if (requestId !== state.requestId) return;

    if (!response.ok) {
      setStatus(state, siteInfoErrorMessage(response.status, info), 'error');
      return;
    }

    const values = toFormValues(info);
    const applied = FORM_FIELDS.filter((field) =>
      applyFieldValue(form, state, field, values[field])
    ).length;

    if (!hasAnyFormValue(values)) {
      setStatus(state, '未获取到网站信息，请手动填写', 'error');
    } else if (applied === 0) {
      setStatus(state, '字段已有内容，未覆盖', 'success');
    } else {
      setStatus(state, `已获取网站信息，填充 ${applied} 项`, 'success');
    }
  } catch (error) {
    if (requestId !== state.requestId) return;
    setStatus(
      state,
      error instanceof Error && error.message ? error.message : '获取网站信息失败，请手动填写',
      'error'
    );
  } finally {
    if (requestId === state.requestId && state.status.dataset.status === 'pending') {
      setStatus(state, '', 'idle');
    }
  }
}

function enhanceForm(form: HTMLFormElement): void {
  if (formStates.has(form) || form.hasAttribute(ENHANCED_ATTRIBUTE)) return;

  const urlInput = form.querySelector<HTMLInputElement>('input[name="url"]');
  if (!urlInput) return;

  // 按钮贴在网址输入框右侧，和官方后台的字段后缀按钮一致
  const field = document.createElement('span');
  field.className = FIELD_CLASS;
  urlInput.parentNode?.insertBefore(field, urlInput);
  field.appendChild(urlInput);

  const button = document.createElement('button');
  button.type = 'button';
  button.className = BUTTON_CLASS;
  button.setAttribute('data-link-enhance-fetch', 'true');
  button.setAttribute('aria-label', '获取网站信息');
  button.title = '获取网站信息';
  button.innerHTML = FETCH_ICON;
  field.appendChild(button);

  const status = document.createElement('span');
  status.className = STATUS_CLASS;
  status.setAttribute('role', 'status');
  status.setAttribute('aria-live', 'polite');

  const tools = document.createElement('span');
  tools.className = TOOLS_CLASS;
  tools.append(status);

  const label = urlInput.closest('label');
  (label ?? field).insertAdjacentElement('afterend', tools);

  const state: FormState = { requestId: 0, filled: {}, button, status };
  formStates.set(form, state);
  form.setAttribute(ENHANCED_ATTRIBUTE, 'true');

  button.addEventListener('click', () => {
    void fetchSiteInfo(form, state, urlInput);
  });
}

function hasApplyFormFields(form: HTMLFormElement): boolean {
  return APPLY_FORM_REQUIRED_FIELDS.every(
    (name) => form.querySelector(`[name="${name}"]`) !== null
  );
}

function matchesApplyForm(form: HTMLFormElement): boolean {
  const action = form.getAttribute('action') ?? '';
  const hit = (value: string) => APPLY_FORM_ACTION_PATHS.some((path) => value.includes(path));
  if (hit(action)) return true;
  try {
    if (hit(new URL(action, window.location.href).pathname)) return true;
  } catch {
    // action 为空或非法时继续走字段特征判断
  }
  return hasApplyFormFields(form);
}

export function enhanceLinkApplyForms(): void {
  if (!isLinkPreviewEnabled()) return;
  Array.from(document.querySelectorAll<HTMLFormElement>('form[action]'))
    .filter(matchesApplyForm)
    .forEach((form) => enhanceForm(form));
}

let enhanceScheduled = false;

function scheduleEnhance(): void {
  if (enhanceScheduled) return;
  enhanceScheduled = true;
  requestAnimationFrame(() => {
    enhanceScheduled = false;
    enhanceLinkApplyForms();
  });
}

/**
 * 表单可能由主题异步渲染，也可能在弹窗打开后才插入 DOM。
 * 一次扫描之外再挂一个只在「新增了节点」时才触发的观察者，
 * 并用 rAF 合并同一帧的多次变动，避免主题每次输入都触发一遍表单扫描。
 */
function startEnhancing(): void {
  enhanceLinkApplyForms();
  if (typeof MutationObserver === 'undefined' || !document.body) return;
  const observer = new MutationObserver((records) => {
    if (records.some((record) => record.addedNodes.length > 0)) {
      scheduleEnhance();
    }
  });
  observer.observe(document.body, { childList: true, subtree: true });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', startEnhancing, { once: true });
} else {
  startEnhancing();
}
