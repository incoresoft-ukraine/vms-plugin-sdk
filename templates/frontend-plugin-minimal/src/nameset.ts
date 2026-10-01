/** Same value as webpack `name` and backend PluginExtension.PLUGIN_NAME. */
export const PLUGIN_ID = 'sample_plugin'

/** Locale key of the plugin name; the host translates tab titles itself. */
export const PLUGIN_TITLE_KEY = 'SAMPLE_PLUGIN_TITLE'

/** Prefix of `data-el-name` attributes, used by UI tests. */
export const TESTING_ID = 'sample-plugin'

/** Plugin permissions (registered by the backend, SamplePermissions.java). */
export const PERMISSIONS = {
  VIEW_ITEMS: 'ViewSampleItems',
  MANAGE_ITEMS: 'ManageSampleItems',
} as const

/*
 * Modal names are global across the whole page: prefix them with the plugin id, or two plugins
 * using the same name open both modals at once.
 */
export const SELECT_CATEGORY_MODAL = `${PLUGIN_ID}_select_category_modal`
export const ITEM_MODAL = `${PLUGIN_ID}_item_modal`
export const CATEGORY_MODAL = `${PLUGIN_ID}_category_modal`
export const ITEM_VIDEO_MODAL = `${PLUGIN_ID}_item_video_modal`
