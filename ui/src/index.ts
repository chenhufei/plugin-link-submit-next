import { definePlugin } from '@halo-dev/ui-shared'
import { markRaw } from 'vue'
import { installLinksManagementEntry } from '@/integrations/links-management-entry'
import LinkVariantPlus from '~icons/mdi/link-variant-plus'
import 'uno.css'

installLinksManagementEntry()

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: 'ToolsRoot',
      route: {
        path: 'link-submit-next',
        name: 'LinkSubmitNext',
        component: () => import('@/views/HomeView.vue'),
        meta: {
          title: '友链申请增强',
          permissions: ['plugin:link:submit-next:view'],
          searchable: true,
          menu: {
            name: '友链申请增强',
            group: 'tool',
            icon: markRaw(LinkVariantPlus),
            priority: 0,
          },
        },
      },
    },
  ],
  extensionPoints: {},
})
