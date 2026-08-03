// @vitest-environment jsdom

import { afterEach, describe, expect, it, vi } from 'vitest'
import { installLinksManagementEntry } from '../links-management-entry'

function renderLinksHeader(title = '链接') {
  document.body.innerHTML = `
    <main class="main-content">
      <div class="page-header">
        <h2 class="page-header__title">
          <span class="page-header__title-text">${title}</span>
        </h2>
        <div class="page-header__actions"></div>
      </div>
    </main>
  `
}

async function flushMutations() {
  await Promise.resolve()
  await Promise.resolve()
}

afterEach(() => {
  window.__linkSubmitNextLinksEntryCleanup?.()
  document.body.innerHTML = ''
  window.history.replaceState({}, '', '/')
  vi.restoreAllMocks()
})

describe('installLinksManagementEntry', () => {
  it('mounts one management entry on the official links page', async () => {
    window.history.replaceState({}, '', '/console/links')
    renderLinksHeader()

    installLinksManagementEntry({ hasPermission: () => true })
    document.body.append(document.createElement('div'))
    await flushMutations()

    const entries = document.querySelectorAll('[data-link-submit-next-entry]')
    expect(entries).toHaveLength(1)
    expect(entries[0]?.getAttribute('href')).toBe('/console/tools/link-submit-next')
    expect(entries[0]?.textContent).toContain('自助提交管理')
  })

  it('stays hidden without permission or an exact official page match', async () => {
    window.history.replaceState({}, '', '/console/links')
    renderLinksHeader('成员管理')
    installLinksManagementEntry({ hasPermission: () => true })
    await flushMutations()
    expect(document.querySelector('[data-link-submit-next-entry]')).toBeNull()

    window.__linkSubmitNextLinksEntryCleanup?.()
    renderLinksHeader()
    installLinksManagementEntry({ hasPermission: () => false })
    await flushMutations()
    expect(document.querySelector('[data-link-submit-next-entry]')).toBeNull()
  })

  it('removes the entry after leaving the links route', async () => {
    window.history.replaceState({}, '', '/console/links')
    renderLinksHeader()
    installLinksManagementEntry({ hasPermission: () => true })
    expect(document.querySelector('[data-link-submit-next-entry]')).not.toBeNull()

    window.history.replaceState({}, '', '/console/overview')
    document.body.append(document.createElement('div'))
    await flushMutations()

    expect(document.querySelector('[data-link-submit-next-entry]')).toBeNull()
  })
})
