import type { RouteRecordRaw } from 'vue-router'

import { PLUGIN_ID } from '@/nameset'

// Mounted under /admin/<module_name>/. Admin Center is where the plugin is set up; the client is
// where operators work with it.
export const adminRoutes: RouteRecordRaw[] = [
  { path: '', redirect: 'categories' },
  {
    path: 'categories',
    name: 'AdminCategoriesViewSamplePlugin',
    component: () => import('@/views/admin/AdminCategoriesView.vue'),
  },
]

/** Tabs of the admin header navigation; add an entry for every new admin page. */
export const adminNavigation = [
  { key: 'categories', name: 'SAMPLE_CATEGORIES', to: `/admin/${PLUGIN_ID}/categories` },
]
