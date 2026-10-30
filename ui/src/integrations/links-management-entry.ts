import { utils } from '@halo-dev/ui-shared'

const ENTRY_ATTRIBUTE = 'data-link-submit-next-entry'
const LINKS_CONSOLE_PATH = '/console/links'
const ENHANCEMENT_SETTINGS_PATH = '/console/plugins/link-submit-next?tab=basic'

type EntryOptions = {
  hasPermission?: () => boolean
}

declare global {
  interface Window {
    __linkSubmitNextLinksEntryCleanup?: () => void
  }
}

function isLinksConsolePage() {
  return window.location.pathname.replace(/\/$/, '') === LINKS_CONSOLE_PATH
}

function findLinksHeaderActions() {
  return document.querySelector<HTMLElement>('.page-header .page-header__actions')
}

function createEntry() {
  const entry = document.createElement('a')
  entry.href = ENHANCEMENT_SETTINGS_PATH
  entry.className = 'btn-sm btn-secondary btn'
  entry.setAttribute(ENTRY_ATTRIBUTE, '')
  entry.setAttribute('aria-label', '打开友链申请增强设置')

  const content = document.createElement('span')
  content.className = 'btn-content'
  content.textContent = '增强设置'
  entry.append(content)
  return entry
}

function defaultPermissionGuard() {
  try {
    return utils.permission.has(['plugin:link:submit-next:view'])
  } catch {
    return false
  }
}

export function installLinksManagementEntry(options: EntryOptions = {}) {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return () => undefined
  }

  window.__linkSubmitNextLinksEntryCleanup?.()

  const hasPermission = options.hasPermission ?? defaultPermissionGuard
  let observer: MutationObserver | undefined

  const syncEntry = () => {
    const existing = document.querySelector<HTMLElement>(`[${ENTRY_ATTRIBUTE}]`)
    if (!isLinksConsolePage() || !hasPermission()) {
      existing?.remove()
      return
    }

    const actions = findLinksHeaderActions()
    if (!actions) {
      existing?.remove()
      return
    }

    if (!existing) {
      actions.prepend(createEntry())
    } else if (existing.parentElement !== actions) {
      actions.prepend(existing)
    }
  }

  const start = () => {
    if (!document.body || observer) return
    syncEntry()
    observer = new MutationObserver(syncEntry)
    observer.observe(document.body, { childList: true, subtree: true })
  }

  const cleanup = () => {
    observer?.disconnect()
    observer = undefined
    document.querySelector<HTMLElement>(`[${ENTRY_ATTRIBUTE}]`)?.remove()
    document.removeEventListener('DOMContentLoaded', start)
    window.removeEventListener('pagehide', cleanup)
    if (window.__linkSubmitNextLinksEntryCleanup === cleanup) {
      delete window.__linkSubmitNextLinksEntryCleanup
    }
  }

  window.__linkSubmitNextLinksEntryCleanup = cleanup
  window.addEventListener('pagehide', cleanup, { once: true })

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start, { once: true })
  } else {
    start()
  }

  return cleanup
}
