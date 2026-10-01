import type { RouteRecordRaw } from 'vue-router'

// Auto-discovered by the host because component.json declares the layout with path "/".
// Paths are relative to /<module_name>/; the host mounts them as children of the layout.
// Route names are global in the host router: end them with the plugin name.
export const clientRoutes: RouteRecordRaw[] = [
  { path: '', redirect: 'items' },
  {
    path: 'items',
    name: 'ClientItemsViewSamplePlugin',
    component: () => import('@/views/client/ClientItemsView.vue'),
  },
]
