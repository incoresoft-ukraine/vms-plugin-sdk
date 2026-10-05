# vms-plugin-frontend-template

The frontend of the **Sample Plugin** for Incoresoft VMS, release line **25.1**: a Vue 3 Module
Federation remote that the VMS web client loads at runtime. It adds a "Sample Plugin" tab to the
client, a section to Admin Center, a header "+" entry, an admin sidebar entry and an icon, and it
fills the rule-wizard, alarm, notification, Search and layout-cell slots of the host.

Companion repositories:

- [vms-plugin-backend-template](https://github.com/incoresoft-ukraine/vms-plugin-backend-template) — the Java backend this UI calls, and the jar that ships this bundle.
- [vms-plugin-sdk](https://github.com/incoresoft-ukraine/vms-plugin-sdk) — the documentation; chapter 04 explains this template and lists every UI slot.

## What the sample does

- **Admin Center:** the plugin's categories — a table with search, create and edit (name and VMS
  camera) in a modal, delete after the host's confirmation dialog.
- **Client:** the items operators work with — a paged table filtered by category, create and delete,
  a frame and the archive video of the moment an item was created. The table follows a live
  WebSocket feed and reloads after a reconnect.
- **Rules and alarms:** the plugin's rule type in the rule wizard ("Any item" in chosen categories,
  "Item contains a word"), and how its alarms and notifications look in the alarms table, the
  details modal (with video), Search → Alarms and the notifications bar.
- **Search:** a block on the Search page — items by period, category, text and camera — with a
  frame per result and the archive video in the details.
- **Layout cell:** a sidebar entry that asks for a category and adds a cell with its newest items.

| Path | Purpose |
|---|---|
| `webpack.config.js` | Module Federation container `sample_plugin`; host-provided singletons; production `publicPath` `/resources/sample_plugin/` |
| `public/component.json`, `public/settings.json` | Which exposed components go into which slots; title, `alarms_available`, permissions of the pages, the "+" entry and the Search block |
| `src/layouts/`, `src/router/` | Client tab (`/sample_plugin/…`) and Admin Center section (`/admin/sample_plugin/…`) with cached pages |
| `src/views/`, `src/components/` | The pages and one component per slot: admin, alarms, search, notifications, cell, modals |
| `src/composables/useItemsLive.ts` | One shared WebSocket connection feeding every page and cell |
| `src/api/` | `core/api` wrapper and the plugin's REST calls |
| `src/locales/` | Translations registered with the host (`en`, `uk`, `es`) |
| `src/models/` | The shapes the host passes in: alarms, rule options, navigation, Search |
| `src/nameset.ts` | `PLUGIN_ID` (= container name = backend `PLUGIN_NAME`), title key, permission ids, modal names |
| `src/types/vms-host.d.ts` | Types of the host modules the plugin imports; there is no published typings package |

## Requirements

- Node.js 18+. The packages `@incoresoft/incoresoft-ui`, `-icons` and `-toasts` come from the
  public npm registry; nothing to configure.
- To see the bundle running: a VMS 25.1 installation and `vms-plugin-backend-template`.

Vue, vue-router, vue-i18n, pinia and the `@incoresoft/*` packages are singletons provided by the host
at runtime; `package.json` pins the versions VMS 25.1 runs (vue 3.4.15, vue-router 4.5.1, vue-i18n
9.9.1, pinia 2.1.7, `@incoresoft/*` `^25.1.0`).

## Build

```bash
npm install
npm run typecheck        # vue-tsc, .vue files included
npm run build            # dist/, production mode
```

The VMS web client loads a plugin's frontend only from the plugin jar, so the bundle goes into the
backend and the backend into VMS:

```bash
rm -rf ../vms-sample-plugin/src/main/resources/public
mkdir -p ../vms-sample-plugin/src/main/resources/public
cp -r dist/* ../vms-sample-plugin/src/main/resources/public/
cd ../vms-sample-plugin && mvn clean package
```

Upload the jar in Admin Center → Settings → Marketplace → Extra Settings → Install Plugin from Disk
(confirm the reinstall when the plugin is already installed) and reload the page. Always build with
`npm run build`: a development build points its chunk URLs at a local dev server and does not load
inside VMS. The `dev_path` in `settings.json` is used only by Incoresoft's internal development
build of the web client.

## Rules to keep

| Rule | Why |
|---|---|
| webpack `name` = `PLUGIN_ID` = backend `PLUGIN_NAME`, a valid JS identifier | The host matches backend and frontend by it and uses it as URL prefix |
| Every `component` in `component.json` is an `exposes` key | A missing module marks the whole plugin as failed |
| The client layout's component `name` is `ClientLayout` + PascalCase(module): `ClientLayoutSamplePlugin` | The host caches tabs by that name; otherwise your pages remount on every tab switch |
| Layouts render pages as `<keep-alive :max="1">` + `<component :is="isOwnRoute ? Component : null" />` | While another tab is active your `<router-view>` resolves the other tab's page; the guard keeps your own page cached |
| Pinia store ids, modal names and route names carry `PLUGIN_ID` | They are global in the host |
| Locale keys are prefixed (`SAMPLE_…`) and include `PERMISSION_*` and the trigger names | Host keys win on conflicts; the Roles UI and the rule wizard translate through your bundle |
| Add only query parameters that have a value | The host writes every key of `params` into the URL, `undefined` included |

## Rename checklist

1. `webpack.config.js`: `PLUGIN_NAME`, `DEV_PORT`, exposed component names.
2. `public/settings.json`: `title`, `dev_path`, `routes_permissions`, `client_permissions`.
3. `public/component.json`: component names (must match `exposes`).
4. `src/nameset.ts`, the layouts' `defineOptions({ name })`, route names, the locale key prefix.
5. `package.json` `name`.

## Support and license

Support: support@incoresoft.com

MIT License (see `LICENSE`): copy the template and license your plugin as you like. The Incoresoft
npm packages it depends on are distributed under the Incoresoft VMS Plugin SDK License shipped
with them.
