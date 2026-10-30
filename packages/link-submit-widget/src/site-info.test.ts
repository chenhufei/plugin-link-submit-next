import { describe, expect, it } from 'vitest';

import {
  buildSiteInfoRequestUrl,
  hasAnyFormValue,
  normalizeSiteUrl,
  shouldReplaceFieldValue,
  siteInfoErrorMessage,
  toFormValues,
} from './site-info';

describe('normalizeSiteUrl', () => {
  it('adds an https scheme when the visitor omitted it', () => {
    expect(normalizeSiteUrl(' example.com ')).toBe('https://example.com');
  });

  it('keeps an explicit scheme', () => {
    expect(normalizeSiteUrl('http://example.com/a')).toBe('http://example.com/a');
  });

  it('returns an empty string without input', () => {
    expect(normalizeSiteUrl('   ')).toBe('');
  });
});

describe('buildSiteInfoRequestUrl', () => {
  it('encodes the target url as a query parameter', () => {
    expect(buildSiteInfoRequestUrl('example.com/a?b=1&c=2')).toBe(
      '/apis/api.link.submit.halo.run/v1alpha1/site-info?url=https%3A%2F%2Fexample.com%2Fa%3Fb%3D1%26c%3D2'
    );
  });

  it('normalizes the scheme before encoding', () => {
    expect(buildSiteInfoRequestUrl('example.com')).toBe(
      '/apis/api.link.submit.halo.run/v1alpha1/site-info?url=https%3A%2F%2Fexample.com'
    );
  });

  it('returns an empty string for a blank url', () => {
    expect(buildSiteInfoRequestUrl('')).toBe('');
  });
});

describe('shouldReplaceFieldValue', () => {
  it('fills an untouched field', () => {
    expect(shouldReplaceFieldValue('', 'previous')).toBe(true);
  });

  it('replaces the value fetched on the previous run', () => {
    expect(shouldReplaceFieldValue('旧标题', '旧标题')).toBe(true);
  });

  it('never overwrites what the visitor typed', () => {
    expect(shouldReplaceFieldValue('我自己写的', '旧标题')).toBe(false);
  });
});

describe('toFormValues', () => {
  it('maps the official form fields and trims whitespace', () => {
    expect(
      toFormValues({ title: '  小站 ', description: '  ', logo: ' https://example.com/i.png ' })
    ).toEqual({
      displayName: '小站',
      description: '',
      logo: 'https://example.com/i.png',
    });
  });

  it(' tolerates a response without any metadata', () => {
    expect(toFormValues({})).toEqual({ displayName: '', description: '', logo: '' });
  });
});

describe('hasAnyFormValue', () => {
  it('detects an all-empty payload', () => {
    expect(hasAnyFormValue({ displayName: '', description: '', logo: '' })).toBe(false);
    expect(hasAnyFormValue({ displayName: '', description: '', logo: 'x' })).toBe(true);
  });
});

describe('siteInfoErrorMessage', () => {
  it('prefers the server problem detail', () => {
    expect(siteInfoErrorMessage(429, { detail: '获取过于频繁' })).toBe('获取过于频繁');
  });

  it('falls back to a status specific hint', () => {
    expect(siteInfoErrorMessage(403, {})).toBe('管理员已关闭自动获取网站信息');
    expect(siteInfoErrorMessage(429, {})).toBe('获取过于频繁，请稍后再试');
    expect(siteInfoErrorMessage(502, {})).toBe('获取网站信息失败（HTTP 502），请手动填写');
  });
});
