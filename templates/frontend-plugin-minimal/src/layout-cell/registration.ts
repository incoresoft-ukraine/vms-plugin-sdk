/**
 * Slot `client_cell_registration`. A side-effect module: the host imports it and the call below
 * runs, adding an entry to the layout sidebar so an operator can drop a cell of this plugin onto a
 * layout. Nothing is exported on purpose, and the host expects no default export here.
 *
 * The registry lives on `globalThis`, shared by every bundle on the page, because Module Federation
 * would otherwise give each bundle its own copy of the module and our registration would never
 * reach the core renderer.
 */
import { modalService } from '@incoresoft/incoresoft-ui'
import { ToastsService } from '@incoresoft/incoresoft-toasts'

import { registerSidebarItem } from 'core/features/layoutCellsRegistry'
import { usePermissionStore } from 'core/stores/usePermissionStore'

import VPluginIcon from '@/components/VPluginIcon.vue'
import { PERMISSIONS, PLUGIN_ID, PLUGIN_TITLE_KEY, SELECT_CATEGORY_MODAL } from '@/nameset'

registerSidebarItem({
  /** Unique across all plugins: it doubles as the drag payload. */
  id: `${PLUGIN_ID}-category`,
  /** The generic plugin cell type; VPluginCell then renders our `client_layout_cell` component. */
  cellType: 'plugin',
  /** Only 'plugins' and 'others' are rendered; anything else silently disappears. */
  group: 'plugins',
  icon: VPluginIcon,
  labelKey: PLUGIN_TITLE_KEY,

  /**
   * Produces the content of the new cell, or null to cancel. Checking the permission here, before
   * opening anything, is what turns a forbidden action into a clear message instead of an empty
   * modal.
   */
  async acquireContent() {
    const permissions = usePermissionStore()
    if (!permissions.hasPluginPermission(PERMISSIONS.VIEW_ITEMS)) {
      ToastsService.warning('REQUIRED_PERMISSIONS')
      return null
    }

    return new Promise<string | null>((resolve) => {
      modalService.show(SELECT_CATEGORY_MODAL, {
        callbackFunction: (categoryId: number | null) => {
          // Cell content format expected by the host: `${pluginId}:${payload}`; ours is the category id.
          resolve(categoryId ? `${PLUGIN_ID}:${categoryId}` : null)
        },
      })
    })
  },
})
