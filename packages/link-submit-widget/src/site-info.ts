export const SITE_INFO_API = '/apis/api.link.submit.halo.run/v1alpha1/site-info';

export type SiteInfoField = 'displayName' | 'logo' | 'description';

export type SiteInfo = {
  title?: string;
  description?: string;
  logo?: string;
  detail?: string;
};

/** 官方申请表单字段名 → 站点信息字段名 */
const SITE_INFO_KEYS: Record<SiteInfoField, keyof SiteInfo> = {
  displayName: 'title',
  description: 'description',
  logo: 'logo',
};

export function normalizeSiteUrl(rawUrl: string): string {
  const trimmed = rawUrl.trim();
  if (!trimmed) return '';
  return /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
}

export function buildSiteInfoRequestUrl(rawUrl: string): string {
  const url = normalizeSiteUrl(rawUrl);
  if (!url) return '';
  return `${SITE_INFO_API}?url=${encodeURIComponent(url)}`;
}

/** 只填空白或上一次自动填入的字段，避免覆盖访客手改的内容 */
export function shouldReplaceFieldValue(
  currentValue: string,
  previouslyFilled: string | undefined
): boolean {
  const current = currentValue.trim();
  return !current || current === (previouslyFilled ?? '').trim();
}

export function toFormValues(info: SiteInfo): Record<SiteInfoField, string> {
  const values = {} as Record<SiteInfoField, string>;
  (Object.keys(SITE_INFO_KEYS) as SiteInfoField[]).forEach((field) => {
    const value = info[SITE_INFO_KEYS[field]];
    values[field] = typeof value === 'string' ? value.trim() : '';
  });
  return values;
}

export function hasAnyFormValue(values: Record<SiteInfoField, string>): boolean {
  return Object.values(values).some((value) => value.length > 0);
}

export function siteInfoErrorMessage(status: number, info: SiteInfo): string {
  if (info.detail && info.detail.trim()) return info.detail.trim();
  if (status === 403) return '管理员已关闭自动获取网站信息';
  if (status === 429) return '获取过于频繁，请稍后再试';
  if (status === 400) return '该网址无法访问，请检查后手动填写';
  return `获取网站信息失败（HTTP ${status}），请手动填写`;
}
