# 04 — Frontend

Reference implementation: `templates/frontend-plugin-minimal`. This chapter describes the VMS
**25.1** web client.

## 1. Toolchain and packages

Vue 3.4 · TypeScript · Webpack 5 with `ModuleFederationPlugin` · vue-router 4.5 · vue-i18n 9.9 ·
pinia 2.1 · SCSS. `npm run typecheck` runs `vue-tsc --noEmit` over the plugin, `.vue` files
included. No linter is preconfigured.

`package.json` pins the versions the VMS 25.1 host runs: `vue` 3.4.15, `vue-router` 4.5.1 (4.6 needs
Vue 3.5), `vue-i18n` 9.9.1, `pinia` 2.1.7. They are provided by the host at runtime (01 §5), so what
compiles also exists at runtime.

The UI packages come from the **public npm registry** and are provided by the host at runtime too:

| Package | Use it for |
|---|---|
| `@incoresoft/incoresoft-ui` | buttons, tables (`ISTable`), fields and selects, modals (`ISModal`, `modalService`), expand blocks, pagination (`ISPagination`), loaders |
| `@incoresoft/incoresoft-icons` | the icon set (`ISIcon*`) |
| `@incoresoft/incoresoft-toasts` | `ToastsService.success / warning / error(localeKey)` |

Install `^25.1.0` for VMS 25.1 (at the time of writing: `incoresoft-ui` 25.1.489, `incoresoft-icons`
25.1.94, `incoresoft-toasts` 25.1.15). Other `@incoresoft/*` packages you may see in the host are
part of the VMS product and not part of the SDK. Component contracts are in each package's
`dist/**/*.d.ts`; for example `ISTable` takes `columns: (string | { text, width })[]` and
`data: { id, <column>: { value } }[]` and offers the slots `filter`, `actions`, `<column>` and
`<column>_header`.

## 2. `webpack.config.js`

```js
new ModuleFederationPlugin({
  name: PLUGIN_NAME,                // 'sample_plugin' == backend PLUGIN_NAME, a valid JS identifier
  filename: 'app.js',               // the host loads <publicPath>app.js
  remotes: { host: 'host@/app.js', styleguide: 'styleguide@/app.js', core: 'core@/app.js' },
  exposes: { './ClientLayoutSamplePlugin': './src/layouts/ClientLayoutSamplePlugin.vue', './clientRoutes': './src/router/clientRoutes.ts', … },
  shared: getSharedPackages(),      // vue, vue-router, vue-i18n, pinia, @incoresoft/*: { singleton: true, import: false }
})
output.publicPath = '/resources/sample_plugin/'   // production
```

* `exposes` keys are the `component` values of `component.json` (without `./`).
* `shared … import: false`: Vue and the `@incoresoft/*` packages are never bundled; bundling your
  own copy breaks reactivity and injection. Every other dependency you add is bundled into your
  plugin.
* Build with `npm run build` (production mode). A development build bakes a dev-server address into
  the chunk URLs and does not load inside an installed VMS.

## 3. `public/component.json`

```json
[
  { "component": "ClientLayoutSamplePlugin", "path": "/", "type": "view", "location": null },
  { "component": "AdminLayoutSamplePlugin",  "path": "/", "type": "view", "location": "admin" },
  { "component": "VClientHeaderPlusItem",    "path": "",  "type": "client_header_plus_item", "location": null },
  { "component": "VPluginIcon",              "path": "",  "type": "plugin_icon", "location": null }
]
```

| Field | Meaning |
|---|---|
| `component` | Exposed module name. Its `default` export is used: a Vue component, or for the two util slots a function or object. |
| `type` | Slot type (§10). `view` = routed page. |
| `path` | For `view`: route path relative to `/<name>` (client) or `/admin/<name>` (admin). `"/"` marks the layout; its child routes come from the exposed `./clientRoutes` / `./adminRoutes` (`export const clientRoutes: RouteRecordRaw[]`). Empty for other slots. |
| `location` | `"admin"` → Admin Center route tree (`/admin/<name>/…`); anything else, `null` included → client. Ignored by non-view entries. |

The backend reads the file from the jar and returns the entries in `GET /api/v1/plugins/extensions`.
A change takes effect after you rebuild the jar, update the plugin and reload the page.

## 4. `public/settings.json`

```json
{
  "title": "Sample Plugin",
  "dev_path": "http://localhost:8030/resources/sample_plugin/app.js",
  "alarms_available": true,
  "routes_permissions": { "/sample_plugin": "ViewSampleItems" },
  "client_permissions": { "search": ["ViewSampleItems"], "plugin_page": ["ViewSampleItems"] }
}
```

| Key | Effect |
|---|---|
| `title` | The plugin's name as the host shows it: the Roles tab, the layout-cell header, the Search block, the alarm type filter. The client tab title is separate: your header "+" item passes it as a locale key. |
| `dev_path` | Used only by Incoresoft's internal development build of the web client. On an installed VMS the bundle is always loaded from the jar. Keep the key; it is harmless. |
| `alarms_available` | Adds your plugin to the alarm type filter of the Alarms page and of the notifications bar. |
| `routes_permissions` | `route prefix → permission id`. Guards the plugin's **client** routes: a route is allowed when it starts with a key whose permission the user holds; otherwise the host redirects to the first allowed key, or to `/403`. Key the plugin root (`/sample_plugin`) so that pages you add later are covered. Admin Center routes are not guarded by it. |
| `client_permissions.plugin_page` | **Any-of** list gating the header "+" item. Empty or missing = everyone. |
| `client_permissions.search` | **All-of** list gating your block on the Search page. `[]` or missing = everyone. |

Write both `client_permissions` lists as JSON arrays. Permission ids must match the backend's
`Permission.getId()`.

## 5. Project layout and conventions

```
src/
  nameset.ts                  PLUGIN_ID (= module name), PLUGIN_TITLE_KEY, TESTING_ID, PERMISSIONS, modal names prefixed with PLUGIN_ID
  layouts/                    ClientLayoutSamplePlugin.vue (client tab), AdminLayoutSamplePlugin.vue (Admin Center section with header tabs)
  router/                     clientRoutes.ts, adminRoutes.ts (+ adminNavigation): RouteRecordRaw[] with relative paths
  views/admin/AdminCategoriesView.vue    categories table: search, create / edit (name + camera), delete
  views/client/ClientItemsView.vue       items table: pages, category filter, create, delete, video, live updates
  components/admin/           VCategoryModal, VAdminHeaderNav, the two rule-wizard blocks
  components/alarms/          type column, message column, details message, details preview (video)
  components/search/          VSearchFilterBlock, VSearchCard, VSearchInfo, VSearchAlarmInfo
  components/notifications/   VNotificationItem
  components/cell/            VClientLayoutCell
  components/modals/          VItemModal, VItemVideoModal, VSelectCategoryModal
  components/                 VClientHeaderPlusItem, VAdminSidebarMenuItem, VPluginIcon
  composables/useItemsLive.ts the live WebSocket feed, shared by pages and cells
  layout-cell/registration.ts side-effect module for the `client_cell_registration` slot
  api/                        makeRequest.ts (wrapper of core/api), categories.ts, items.ts
  locales/                    index.ts registers messages/{en,uk,es}.json with the host
  models/                     alarms.ts, rules.ts, navigation.ts, search.ts — the shapes the host passes in
  types/vms-host.d.ts         types of the host modules the plugin imports (§7)
```

Rules the template follows:

* **Keep-alive name.** The client layout's component `name` is `'ClientLayout' + PascalCase(module
  name)` (`sample_plugin → ClientLayoutSamplePlugin`); the host caches the tab by that name, and
  without it your pages remount on every tab switch.
* **Inner keep-alive with an own-route guard.** Both layouts render their pages through
  `<keep-alive :max="1">` and `<component :is="isOwnRoute ? Component : null" />`. While another
  tab is active the host keeps your layout alive and its `<router-view>` resolves the other tab's
  page; the guard keeps your own page cached instead of mounting a foreign one. Use the ternary on
  `:is`, not `v-if`. Cached pages load their data in `onActivated`, which also runs on the first
  mount.
* Route names, Pinia store ids and modal names are global in the host: suffix or prefix them with
  the plugin name. The template has no Pinia store; if you add one, `defineStore(`${PLUGIN_ID}-…`)`.
* Add `data-el-name` attributes (`${TESTING_ID}-…`) for UI tests.

## 6. Talking to the backend

```ts
import coreApi from 'core/api'
const response = await coreApi.makeRequest({ url: '/api/v2/sample_plugin/items', method: 'GET', params })
```

`makeRequest` prefixes the API base URL, sends the session cookie, toasts `response.data[0].type`
for 400 (without a field), 404, 409, 422 and 498, and logs the user out on 401. The template wraps
it in `api/makeRequest.ts`, which unwraps `.data`.

Two things to know about `params`:

* arrays are serialised as `ids=[1,2]` (the backend template parses this form);
* **every key is written**, `undefined` and `null` included (`category_id=undefined`). Build the
  object with the parameters that have a value only, as `api/items.ts` does.

WebSockets: `${coreApi.config.WEBSOCKET_API}/api/v2/ws/sample_plugin/…` through
`styleguide/services/WebSocketService`, which reconnects and parses JSON. The template's
`composables/useItemsLive.ts` opens one socket for all subscribers, closes it with the last one, and
has every subscriber reload after a reconnect, because changes made while the socket was down were
never delivered.

The host's confirmation dialog is a modal named `confirmModal`, always mounted:
`modalService.show('confirmModal', { title, message, confirm })`. It calls `confirm` and closes
without waiting for a returned promise; handle a failed request inside `confirm`.

## 7. Host modules you can import

The host exposes its modules to plugins through Module Federation; they exist only at runtime, and
TypeScript learns about them from `src/types/vms-host.d.ts`, which the template ships. The typed,
supported surface for 25.1:

| Module | What it gives |
|---|---|
| `core/api` | `coreApi.config` (`API`, `WEBSOCKET_API`, `STORAGE_URL`), `coreApi.makeRequest(config)` |
| `core/stores/useUserStore` | the signed-in user, `locale`, date and time formats |
| `core/stores/usePermissionStore` | `hasPermission`, `hasPluginPermission`, per-camera and per-layout checks, `isAdmin` |
| `core/stores/useDevicesStore` | `camerasList` (cameras the user may see), `getCameras()` |
| `core/components/AppPlayer` | the host player: live, or archive with `isPlayback`, `camera`, `currentTime`, `event {start, end}` |
| `core/components/VCameraThumbnail` | a frame from the archive: `cameraId`, `timestamp` |
| `core/formatters/time` | `formatAsUserDate`, `formatAsUserTime` in the user's format and time zone |
| `core/features/layoutCellsRegistry` | `registerSidebarItem`, `registerPluginCellEditor` (§10 B) |
| `host/stores/useSystemStore` | `setExtensionsLocales`, the list of loaded extensions |
| `styleguide/services/modal-service` | `modalService.show / hide(name, params)` |
| `styleguide/services/WebSocketService` | reconnecting WebSocket client |
| `styleguide/services/notification-service` | in-app notifications |
| `styleguide/stores/useValidationErrorStore` | maps a 400 body onto form fields (`getErrorForField`, `setErrors`) |

Every other host module resolves as `any`. Prefer the modules above; they are what the sample uses
and what Incoresoft keeps stable within a release line. Replace the declaration file when you move
to a new release line.

## 8. Permissions on the client

```ts
usePermissionStore().hasPluginPermission('ManageSampleItems')   // true for root or any role holding it
```

Plus the declarative gates in `settings.json` (§4). The permissions themselves are created by the
backend; the frontend only references ids and translates the `PERMISSION_*` keys. Where the host
does not filter for you, check yourself: the layout-sidebar entry of `client_cell_registration` is
shown to every user, so the template checks `ViewSampleItems` in `acquireContent` and again inside
the cell (a layout can be shared with users who lack it). UI checks are a convenience; the backend
enforces every permission on its routes.

## 9. Locales

`useSystemStore().setExtensionsLocales({ en, uk, es })` with flat `KEY: text` maps; the allowed
locales are `en`, `uk` and `es`. The host applies the registered maps **once, after all plugins are
loaded**, so register them from a module the host loads at start (the template imports `@/locales`
in both layouts). Host keys win on conflict, so **prefix your keys** (`SAMPLE_…`). Include: UI
strings, `PERMISSION_*` for your permissions, the error `type` keys your API returns, the rule
trigger names (`SAMPLE_ANY_ITEM`, `SAMPLE_CONTAINS_WORD`: the host translates a trigger as
`t(trigger.toUpperCase())`), and the title of your client tab (`SAMPLE_PLUGIN_TITLE`). Host keys
such as `CAMERA`, `VIDEO`, `DELETE`, `SAVE`, `ALL`, `NONE`, `FIELD_REQUIRED` can be used as they are.

Styling: use the UI kit and the host's CSS variables (`--text-color-0…60`,
`--background-color-…`); scoped SCSS; no global resets. The plugin icon is an SVG component; some
host places recolour it (the layout-cell name pill forces its `fill`).

## 10. Slots

A slot is a `type` value in `component.json`. The host stores the exposed module's default export
in a map keyed by **module name** and renders `map[<name>]` in the matching place. For alarm and
notification slots the key is the alarm `type` (= `RuleType.getTypeId()`, by convention the module
name). The template implements every slot below except the maps and the roles tab, so the exact
props and events can be read from working code.

### A. Pages and navigation

| Slot type | Where | Contract |
|---|---|---|
| `view` | Vue Router | `location: "admin"` → `/admin/<name>/<path>`; otherwise `/<name>/<path>`. The layout entry (`path: "/"`) takes its children from the exposed `./clientRoutes` / `./adminRoutes`. |
| `client_header_plus_item` | the header "+" popover | No props. Emit `tabClicked` with `{ id: <name>, type: 'view', title: <locale key>, path: '/<name>/…', icon: <name> }` (template: `models/navigation.ts`). Shown only to users allowed by `client_permissions.plugin_page`. |
| `plugin_icon` | tabs, the layout-cell pill, the Search block list, admin tables, the rule "Where" step | No props. An SVG with a tight viewBox. |
| `admin_sidebar_menu_item` | Admin Center sidebar, **PLUGINS** group | No props. Render a `<router-link :to="/admin/<name>/…" class="app-sidebar__menu--item">` as the template does. Shown to every Admin Center user. |

### B. Layout cells

| Slot type | Where | Contract |
|---|---|---|
| `client_layout_cell` | a layout cell of type `plugin` | Prop `payload: string`, the part of the cell content after `<name>:`. The host draws the frame: name pill with `settings.title` and your icon, controls, padding. Fill the content area only. |
| `client_cell_registration` | executed on load | A side-effect module: `registerSidebarItem({ id, cellType: 'plugin', group: 'plugins', icon, labelKey, acquireContent })` from `core/features/layoutCellsRegistry`. `acquireContent` returns the cell content `` `${name}:${payload}` `` or `null` to cancel (the template opens its `plugin_select_modal` and resolves with the chosen category). `registerPluginCellEditor(name, editor)` adds an edit button to placed cells. The sidebar entry is shown to every user: check the permission in `acquireContent`. |

### C. Search page

The host owns the page: period, camera tree, sort order, result list, details panel. Your block does
the searching and hands the rows to the host. `models/search.ts` in the template types the whole
contract.

| Slot type | Where | Contract |
|---|---|---|
| `client_search_filter_block` | the filter sidebar; shown to users allowed by `client_permissions.search` | Props: `blockKey` (= your name), `blockName` (= `settings.title`), `activeExpanded` (v-model), `startTime`, `endTime`, `cameras` (ticked in the host tree), `searchOrder` (`'asc'` \| `'desc'`), `savedBlockFilters` (what you last sent in `onChangeFilters`, or `null`), `reachEndTrigger` (flips: load the next page), `refreshTrigger` (grows: start over). Emits: `update:activeExpanded`, `onCloseBlock(blockKey)`, `onChangeFilters(blockKey, filters)` (**the host clears the list** — send it before every new first page), `onSetSearchData(blockKey, rows, total)` (**the host appends** the rows), `onSetShowLoader(bool)`. |
| `client_search_card` | over the result card | Prop `item`: your row plus `key` and `searchedElementType`. The host draws the card with a frame from the camera archive when the row has `camera_id`, `timestamp` and `__SHOW_CAMERA_THUMBNAIL: true`. |
| `client_search_info` | the details panel | Props `item`, `setSearchPlayerSettings({ camera, startTime, endTime } \| null)`, `setSearchDetailsHeaderInfo(title, text)`. |
| `client_search_alarm_info` | details of one of your alarms in Search → Alarms | Same props; `item` is the alarm. Needs `alarms_available`. |

### D. Alarms and notifications (keyed by alarm type)

| Slot type | Where | Contract |
|---|---|---|
| `alarms_type_column` | alarms table "Type" cell; alarm modal | prop `alarm` |
| `alarms_message_column` | alarms table "Message" cell | prop `alarm` |
| `alarms_details_message` | "Message" block of the alarm and notification modals | prop `event` (the alarm or notification); emit `closeModalEvent` before navigating away |
| `alarms_details_preview` | video block of the alarm and notification modals | prop `event`. The host adds `event.camera_id` from the camera of the alarm's source (03 §12.1); the template renders `VCameraThumbnail` and `AppPlayer` around `event.timestamp` |
| `plugin_notification_item` | body of a notification card in the notifications bar | prop `item`; emits `removeNotification(id)` and `setSystemMessage({ is_desktop, type, title, body, image })`, which asks the host for the desktop notification. The host draws the card and its close button; fill the body only |

The alarm as your components see it: `id`, `type`, `source_id`, `source_name`, `camera_id`,
`server_name`, `rule_id`, `rule_name`, `options` (the rule options), `message` (your rule message),
`state`, `priority_level`, `timestamp` (ms), `owner_id`.

### E. Admin Center — rule wizard

| Slot type | Where | Contract |
|---|---|---|
| `admin_events_and_rules_options_block` | the "What" step, for rules of your type | `modelValue` (v-model; becomes `rule.options`), `viewMode` (`'create'` \| `'edit'` \| `'details'`, details is read-only). Emits `update:modelValue`, `showWhereStep` / `hideWhereStep` (the host also listens for the what, when, for and action steps), `resetSources` (clears the "Where" selection when the trigger changes). The template shows `validationStore.getErrorForField('options.word')` under its input. |
| `admin_events_and_rules_where_block` | the "Where" step, replacing the camera tree | `modelValue` (v-model, the source UUIDs), `options` (current rule options), `viewMode`. The template lists the categories by `source_id`. |
| `admin_roles_permissions_tab` | Roles → role details, an extra tab named `settings.title` | prop `role`. Declare the static component option `vmsRole: true` to receive the VMS role; the host does not listen to events of this tab, so save through your own API. Not in the sample; Incoresoft's access-control plugin uses it for per-door permissions. |

### F. Modals mounted globally

| Slot type | Where | Contract |
|---|---|---|
| `plugin_select_modal` | mounted once by the host, always present | No props. Put `ISModal`s here and open them with `modalService.show('<name>_…', payload)` from anywhere; the payload arrives in the modal's `beforeOpen` event, not as props. The template's category picker for the layout cell lives here. |

### G. Maps and plans

Not in the sample: the contract below comes from the host code and from Incoresoft's access-control
plugin, which can be shared on request.

| Slot type | Where | Contract |
|---|---|---|
| `map_plan_config_block` | editor side panel, a tab per plugin of the selected node | `config` (v-model:config); optionally expose `initDefaultConfig()`, which the host calls when the node has no settings for your plugin yet. Saved as the node's settings for your plugin and handed to your backend `MapPlanEventFilter` / `MapPlanEventFactory`. |
| `map_plan_notification` | live overlay on a node | props `notification` (your `MapPlanEvent` JSON), `config` (the node's settings for your plugin) |
| `map_plan_notification_default_config` | applied when a node is placed | default export: a function returning the default config object |
| `map_plan_object` | your own placeable objects (backend `MapPlanEventEmitter`) | default export: `{ icon: Component, hasFieldOfView: boolean, contextMenu?, nodeOverlay? }` |

### H. Availability

A plugin's components are rendered only while the extension is available: an extension that
requires Incoresoft's analytics integration is hidden while that integration is off. The header
"+" item additionally needs `client_permissions.plugin_page`, the Search block
`client_permissions.search`. The admin sidebar item and the layout-sidebar entry are shown to
everyone.
