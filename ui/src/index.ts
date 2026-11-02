import { definePlugin } from '@halo-dev/ui-shared'
import { installLinksManagementEntry } from '@/integrations/links-management-entry'
import 'uno.css'

installLinksManagementEntry()

export default definePlugin({
  components: {},
  routes: [],
  extensionPoints: {},
})
