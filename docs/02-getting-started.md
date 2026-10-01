# 02 — Getting started and the development loop

## 1. Prerequisites

| Tool | Version | Why |
|---|---|---|
| JDK | **21** | The host and its libraries are compiled for 21 |
| Maven | 3.9+ | Builds the plugin jar. Every dependency, `middleware` 25.1.26 and `device-driver-sdk` 25.1.27 included, comes from Maven Central; no repository to add, no credentials |
| Node.js | 18+ | Frontend build. The UI packages `@incoresoft/incoresoft-ui`, `-icons` and `-toasts` come from the public npm registry; nothing to configure |
| A VMS installation | 25.1, build 25.1.1063 or newer | Your test system, including Admin Center access as the root user. You install and replace the plugin through Admin Center; no VMS source code is needed. Older 25.1 builds make you delete the plugin's alarm rules before every update |

## 2. The Sample Plugin

The templates form one small but complete service plugin. Its data are **categories** and
**items**; read them as "your devices or zones" and "events from your system" (§5).

* **Admin Center** is where the plugin is set up: a section with the categories table. A category
  has a name and, optionally, a VMS camera.
* **The client** is where operators work: a tab with the items of all categories, shown in pages,
  filtered by category, updated live for everyone. An item remembers its category's camera, so a
  click shows a frame and the archive video of the moment it was created.
* **Rules.** Operators build alarm rules on items (Admin Center → Alarm Rules): "Any item" in chosen
  categories, or "Item contains a word". A matching item raises an alarm, and a notification if
  the rule says so. When the category has a camera, the alarm shows its video.
* **Search.** The Search page gets a block for the plugin: items by period, category, text and
  camera, with a frame per result and the archive video in the details.
* **Layout cell.** A cell on a layout shows the newest items of one category, updated live.

## 3. Thirty-minute walkthrough

### Step 1 — build the backend

```bash
cp -r vms-plugin-sdk/templates/backend-plugin-template vms-sample-plugin
cd vms-sample-plugin
mvn clean package               # → build/vms-sample-plugin-25.1.0.jar
```

The jar already contains a build of the frontend template under `public/`, so this is the whole
plugin. What is in it (chapter 03 explains each piece):

* `PluginExtension extends VmsExtension` — the lifecycle; `PLUGIN_NAME = "sample_plugin"`.
* A Liquibase changelog creating `sample_categories` and `sample_items`.
* Three permissions (`ViewSampleItems`, `ManageSampleItems`, `ManageSampleCategories`) that appear
  in Admin Center → Roles.
* REST API `/api/v2/sample_plugin/categories` and `/api/v2/sample_plugin/items`, guarded by those
  permissions.
* `FrontendResources`, serving the frontend bundle at `/resources/sample_plugin/*`.
* A rule type with two triggers, scoped to categories, with alarms and notifications.
* A live WebSocket feed at `/api/v2/ws/sample_plugin/items`.
* A retention task that deletes old items on the schedule configured for plugin data.

### Step 2 — install

Admin Center → **Settings → Marketplace → Extra Settings → Install Plugin from Disk**, pick the
jar. VMS starts the plugin at once; no restart.

The same over the API, with an API token of a **root** user (Admin Center → Settings → API Token;
plugin management is root-only):

```bash
TOKEN=<api token>
curl -k -X POST https://<vms-host>:2443/api/v1/plugins/vezha \
     -H "Authorization: Bearer $TOKEN" -F file=@build/vms-sample-plugin-25.1.0.jar
```

Verify:

```bash
curl -k https://<vms-host>:2443/api/v1/plugins/extensions | jq '.[] | select(.module_name=="sample_plugin")'
curl -k -H "Authorization: Bearer $TOKEN" https://<vms-host>:2443/api/v2/sample_plugin/items
```

The first call shows your extension with `type: "service_plugin"` and `plugin_id:
"vms-sample-plugin"`. The second answers `{"data":[],"total":0,"pages":0}` (no items yet), or
`403` until a role of the token's user holds `ViewSampleItems`; grant it in Admin Center → Roles,
where the permission appears under the plugin name. If the upload answered
`400 ERROR_PLUGIN_NOT_COMPATIBLE`, the `Plugin-Version` does not share major.minor with the core
(the template uses `25.1.0`).

### Step 3 — try it

Reload the web client: a page that was open during the install does not see a new plugin by
itself.

* **Admin Center:** the plugin is in the sidebar under **PLUGINS**. Create a category; pick a camera
  if you want video on its alarms.
* **Client:** click **+** in the header → "Sample Plugin" → the items page opens. Create an item: it
  appears at once in every open items page and layout cell. The play button in its row shows the
  frame and the archive video from the camera.
* **Admin Center → Alarm Rules:** create a rule of type "Sample Plugin", pick a trigger and, for
  "Any item", the categories. The next matching item raises an alarm; add the notification action
  to get a notification too.
* **Search:** open the Search page, choose a period, and the "Sample Plugin" block in the sidebar
  lists the items with a frame each.
* **Layout:** the layout sidebar has a "Sample Plugin" entry in the **Plugins** group. It asks for a
  category and adds a cell with that category's newest items.

### Step 4 — change the frontend and ship it

```bash
cp -r vms-plugin-sdk/templates/frontend-plugin-minimal frontend-sample
cd frontend-sample && npm install
npm run typecheck                             # vue-tsc, .vue files included
npm run build                                 # production bundle in dist/
rm -rf ../vms-sample-plugin/src/main/resources/public
mkdir -p ../vms-sample-plugin/src/main/resources/public
cp -r dist/* ../vms-sample-plugin/src/main/resources/public/
cd ../vms-sample-plugin && mvn clean package
```

Upload the new jar as in Step 2. VMS sees that the plugin is installed and asks whether to
reinstall it; confirm. The new jar replaces the old one while VMS runs: the plugin's tables, its
data and its alarm rules stay. Reload the page to load the new bundle.

## 4. The development loop

Every change, frontend or backend, reaches VMS as a new jar. The web client shipped with VMS loads
a plugin's frontend only from the jar (`/resources/<name>/app.js`); the `dev_path` in
`settings.json` is used by Incoresoft's internal development build of the client and has no effect
on an installed VMS.

1. Frontend: `npm run typecheck`, `npm run build` → `dist/`. Always a production build: a
   development build points its chunk URLs at a local dev server and does not load inside VMS.
2. Replace the content of the backend's `src/main/resources/public/` with `dist/*`.
3. Backend: `mvn clean package` → `build/<artifactId>-<version>.jar`.
4. Upload the jar in Admin Center and confirm the reinstall. The host stops the plugin
   (`terminate()`), replaces the jar, starts the new one (`init()`), and keeps the old jar as
   `.bak` next to it. If the new jar fails to start, the old one is put back.
5. Reload the browser page.

A backend-only change skips steps 1–2. Over the API, the update of an installed plugin is
`PATCH /api/v1/plugins/vezha/<Plugin-Id>/update_from_file` with the jar as multipart `file`
(chapter 05).

| Action | How |
|---|---|
| First install | Admin Center → Settings → Marketplace → Extra Settings → Install Plugin from Disk, or `POST /api/v1/plugins/vezha` |
| Replace with a new build | Upload again and confirm the reinstall prompt, or `PATCH …/vezha/<Plugin-Id>/update_from_file` |
| Stop / start without uninstalling | Admin Center → Settings → Marketplace, or `PATCH …/vezha/<Plugin-Id>/stop`, `…/start`. The state survives a VMS restart |
| Uninstall | Admin Center, or `DELETE …/vezha/<Plugin-Id>`. Refused while alarm rules of the plugin's rule type exist (`ERROR_CANNOT_DELETE_PLUGIN_WITH_EXISTING_RULES`); delete them first |
| Watch what happened | `log/application.log` in the VMS working directory: `Plugin <id> is loaded.`, `Plugin <id> misses expected extensions`, `Couldn't start plugin` with the stack trace |

**Never overwrite a jar in the `plugins/` directory while VMS runs.** The JVM keeps the file open
and the running plugin breaks (`ZipException`), with HTTP 500 on `GET /api/v1/plugins/extensions`
until VMS is restarted. Copying a jar there is fine while VMS is stopped; it is loaded at the next
start, without the version check the upload performs.

## 5. Mapping the sample to your product

The sample is a map of the extension points, not a product. When you copy it:

| Sample | Your plugin |
|---|---|
| Category (set up by the administrator, bound to a camera) | A door, a cash desk, a sensor, a zone watched by your analytics — any object your system reports about |
| Item (created by operators or by your system through the API) | An event from your system: an access, a receipt, a detection |
| "Any item" / "Item contains a word" | Your triggers: "any event", "event of type X", "amount over N" |
| Camera of the category → video on alarms, in the list and in Search | Keep it: it is what makes your data useful in a VMS |

Rename the identifiers (03 §1.3 and the frontend template's README have checklists), replace the
fields, keep the structure.

## 6. Testing

* Backend: JUnit 5 and Mockito work as in any Java project; mock the `VmsExtensionContext`
  interfaces. There is no host test harness.
* Frontend: the templates ship no test runner; add the one you use. For end-to-end tests the
  template puts `data-el-name` attributes (`${TESTING_ID}-…`) on the elements a test needs.
* API: any HTTP client with `Authorization: Bearer <api token>`. The host serves no Swagger for
  plugins; add your own docs route if you want one.

## 7. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `Plugin X misses expected extensions` in the log | No `META-INF/extensions.idx`, or it lists no `VmsExtension` subclass. Keep `pf4j` on the compile classpath and annotation processing on; check the jar |
| Upload answers 400 `ERROR_PLUGIN_IS_INVALID` | `Plugin-Id` missing, or `Plugin-Version` is not strict SemVer |
| Upload answers 400 `ERROR_PLUGIN_NOT_COMPATIBLE` | `Plugin-Version` major.minor differs from the core's. Version your plugin `25.1.N` |
| Upload answers 400 `ERROR_PLUGIN_IS_ALREADY_INSTALLED` | A plugin with this `Plugin-Id` is installed. Use the update (`update_from_file`); Admin Center offers it as "reinstall" |
| Upload answers 403 | The user is not root. Plugin management needs the root user |
| Upload answers 500 `ERROR_PLUGIN_LOADING_FAILED` and the jar is gone | `init()` threw, or the plugin could not be loaded; the log has the cause |
| Plugin listed but never starts, log says `requires a minimum system version` | `Plugin-Requires` does not match the core; PF4J keeps the plugin disabled. Drop the attribute |
| Plugin starts, no "+" entry, no admin section | Page not reloaded after the upload; or the user lacks the permission in `client_permissions.plugin_page`; or a component failed to load (browser console: `[Extension Error]`) |
| `[Extension Error] Failed to load component X` | `component` in `component.json` is not an `exposes` key of the bundle, or the module has no default export |
| Plugin UI requests chunks from `localhost` | The bundle was built in development mode. `npm run build` |
| Frontend change not visible | `dist/` not copied into `src/main/resources/public/` before `mvn package`, jar not uploaded again, or page not reloaded |
| Request answers 400 with a key like `SAMPLE_CATEGORY_NOT_FOUND` for a filter you did not set | A query parameter went out as `name=undefined`: the host writes every key of `params` into the URL, unset ones included. Add only parameters that have a value |
| Alarm created but no message shown | `alarms_message_column` / `alarms_details_message` not registered for your type, or the alarm JSON does not match your component |
| Rule "What" step empty for your type | Missing `admin_events_and_rules_options_block` |
| Permission shows its raw key in Roles | Add the `PERMISSION_*` translation to your locales |
| Texts show raw keys | Locales registered after start (register them from a module the host loads at start, as the template's layouts do), or an unprefixed key overridden by a host key |
| Tab state lost on every tab switch | The client layout's component `name` is not `ClientLayout<PascalCase(name)>` |
| Alarms and notifications stop when you call the host | A host lookup threw inside your listener. Guard calls to the host (`DeviceManager` etc.) with try/catch and log; an alarm without extra details beats no alarm |
