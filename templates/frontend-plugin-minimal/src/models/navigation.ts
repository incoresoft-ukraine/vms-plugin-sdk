// Contract of the `client_header_plus_item` slot: emit 'tabClicked' with this object.
export interface INavigationTab {
  id: string        // module name → the host uses it to de-duplicate opened tabs
  type: 'view'
  title: string
  path: string      // absolute client route, e.g. '/sample_plugin/items'
  icon?: string     // module name → the host renders the `plugin_icon` slot component
}
