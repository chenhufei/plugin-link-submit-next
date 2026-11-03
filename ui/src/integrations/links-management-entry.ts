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

/**
 * 入口按钮跳的是插件设置页，而设置页要 manage 才能打开。
 * 只按 view 判断会让「能看见菜单、点进去一堵无权限」的账号反复踩坑，
 * 所以这里直接要求 manage；没有权限的账号不显示这个入口。
 */
const REQUIRED_PERMISSIONS = ['plugin:link:submit-next:manage']

function defaultPermissionGuard() {
  try {
    return utils.permission.has(REQUIRED_PERMISSIONS)
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

  // 选择器依赖 Halo 控制台的页头结构。结构变了只会表现为「按钮不见了」，
  // 没有任何报错，所以这里按路径去重后 warn 一次，便于下次排查。
  let warnedPathname = ''

  const syncEntry = () => {
    const existing = document.querySelector<HTMLElement>(`[${ENTRY_ATTRIBUTE}]`)
    if (!isLinksConsolePage() || !hasPermission()) {
      existing?.remove()
      return
    }

    const actions = findLinksHeaderActions()
    if (!actions) {
      if (warnedPathname !== window.location.pathname) {
        warnedPathname = window.location.pathname
        console.warn(
          '[link-submit-next] 未找到友链控制台的页头操作区（.page-header .page-header__actions），「增强设置」入口未注入。Halo 升级若改了页头结构，需要同步更新这个选择器。'
        )
      }
      existing?.remove()
      return
    }
    warnedPathname = ''

    if (!existing) {
      actions.prepend(createEntry())
    } else if (existing.parentElement !== actions) {
      actions.prepend(existing)
    }
  }

  // 控制台每次渲染都会产生成百上千条 DOM 变动记录。
  // 直接把 syncEntry 挂上去会在一帧内被调用几十次，每次都要走一遍 querySelector；
  // 这里用 rAF 把同一帧内的变动合并成一次同步，syncEntry 本身也是幂等的。
  let syncScheduled = false
  const scheduleSync = () => {
    if (syncScheduled) return
    syncScheduled = true
    requestAnimationFrame(() => {
      syncScheduled = false
      syncEntry()
    })
  }

  const start = () => {
    if (!document.body || observer) return
    syncEntry()
    observer = new MutationObserver(scheduleSync)
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
