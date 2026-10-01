# 01 — Architecture

## 1. The host in one paragraph

Incoresoft VMS is one Java 21 process. It serves HTTP, WebSocket and SSE with **Javalin 5** on a
single shared instance, keeps its data in **PostgreSQL** through **jOOQ** and **Liquibase**, and
loads plugins at runtime with **PF4J**: every plugin is a jar in the `plugins/` directory with its
own class loader. The web client is a **Vue 3** application assembled from **Webpack Module
Federation** remotes; a plugin's frontend is one more remote, served from the plugin jar.

A plugin compiles against one small public library, `com.incoresoft.commons:middleware`
(interfaces only: `VmsExtension`, `VmsExtensionContext`, `RuleManager`, `Permission`, `User`, …).
Plugins that resolve cameras and devices also use `com.incoresoft:device-driver-sdk`
(`DeviceManager`, `Device`, `DeviceItemSettings`). Both are `provided` dependencies: the host
already has them. Everything else a plugin needs (Guice bindings of the host services, serving the
frontend bundle, registering permissions, parsing requests) is a few small classes that live in the
plugin itself; the backend template contains all of them.

## 2. What a plugin is

One jar:

```
vms-sample-plugin-25.1.0.jar
├── META-INF/MANIFEST.MF          ← PF4J descriptor: Plugin-Id, Plugin-Version, Plugin-Name, …
├── META-INF/extensions.idx       ← generated at compile time: the class name of your VmsExtension
├── com/example/vms/sample/**     ← your classes (+ third-party libraries the host does not have)
├── sql/db-changelog.xml          ← Liquibase changelog of your tables
├── icon.svg                      ← Plugin-Icon-Resource-Path
└── public/                       ← the frontend bundle
    ├── app.js                    ← Module Federation container
    ├── <chunks>.js
    ├── component.json            ← which exposed components go into which UI slots
    └── settings.json             ← title, permissions, flags
```

The backend half is a subclass of `com.incoresoft.middleware.vms.plugin.VmsExtension`. The
frontend half is a Module Federation remote whose container name equals the value the backend
returns from `getPluginName()`. The host joins them through `GET /api/v1/plugins/extensions`,
which the web client calls at start to learn which remotes to load and which components to mount
where.

### 2.1 Plugin types

| Type (`type` in the extensions API) | How the host decides | Who builds it |
|---|---|---|
| `service_plugin` | `getModuleClass()` returns `null` | **Partners.** Own REST API, own tables, own UI, own rule types. Incoresoft's POS-terminals and access-control plugins are of this kind. |
| `analytics_plugin` | `getModuleClass()` returns an analytics module class | Incoresoft only: plugins paired with Incoresoft's VEZHA analytics server. |
| driver | a `DeviceDriver` extension in `drivers/` | Device drivers; separate SDK, not covered here. |

A plugin with **your own video analytics** is an ordinary service plugin: your analytics server
posts its detections to your plugin's API, the plugin stores them with the camera they came from,
and VMS does the rest — alarms with video, notifications, search, layout cells. Nothing in VMS
requires the VEZHA server for that.

### 2.2 Identifiers

| Identifier | Where it lives | Used for | Rules |
|---|---|---|---|
| **Plugin-Id** | `MANIFEST.MF` → `Plugin-Id` | Install, update, start, stop and uninstall API; the marketplace product id; the plugin's enabled state in the database; the icon URL `/resources/<Plugin-Id>/icon` | Unique across the marketplace. Convention: `vms-<name>-plugin`. |
| **Plugin name** (`module_name`) | `VmsExtension.getPluginName()`; webpack `name`; `nameset.ts → PLUGIN_ID` | Module Federation container, bundle URL `/resources/<name>/app.js`, route prefixes `/<name>/…` and `/admin/<name>/…`, the key of every UI slot, `RuleType.getPluginName()` | **A valid JavaScript identifier** (`[A-Za-z_][A-Za-z0-9_]*`), lower snake_case by convention (`sample_plugin`). |
| **Plugin-Version** | manifest | Compatibility check on upload, marketplace updates | Strict SemVer **and** the same major.minor as the VMS release line: `25.1.<patch>` for a 25.1 core, otherwise the upload answers `ERROR_PLUGIN_NOT_COMPATIBLE`. |
| Rule type id | `RuleType.getTypeId()` | The `type` of your alarms and notifications on the wire; the frontend picks your alarm components by it | Convention: equal to the plugin name. |
| Permission id | `Permission.getId()` | Roles UI, route roles, `settings.json`, `hasPluginPermission()` | Unique across VMS and all plugins, PascalCase with your plugin in it: `ManageSampleItems`. |

The plugin id, the plugin name and the permission ids become part of the customer's data after
the first installation. Choose them once.

## 3. Lifecycle

```
install / start            1. PF4J loads the jar and validates the manifest.
                           2. PF4J starts the plugin; the host checks that the jar contains a
                              VmsExtension ("Plugin X misses expected extensions" otherwise).
                           3. The host remembers the plugin as enabled (database).
                           4. ext.init(VmsExtensionContext)            ◀── YOUR CODE
                              Every HTTP and WebSocket route added during init() is recorded.
                           5. The icon endpoint is registered; "plugin started" goes out over SSE,
                              and open web clients offer to reload.

stop / update / uninstall  1. PF4J stops the plugin.
                           2. The host removes every recorded route.
                           3. ext.terminate()                          ◀── YOUR CODE
                           4. Uninstall only: unload, delete the jar.
```

What this means for you:

* **Routes are removed for you; nothing else is.** Unregister in `terminate()` what you registered
  in `init()`: permissions, the rule type, map emitters, listeners; stop your threads and close your
  sockets.
* If `init()` throws, the host stops the plugin again, which calls `terminate()` on the same
  instance; write `terminate()` so that it survives a half-finished `init()`. An uploaded jar whose
  start fails is deleted again.
* Plugins are hot-pluggable: install, update, start, stop and uninstall happen while VMS runs. An
  update replaces the jar in place and keeps the plugin's tables, settings and alarm rules. Open web
  clients need a page reload to pick up a new frontend bundle; the host asks them to.
* A plugin runs **inside the VMS process with its privileges**: a fault in the plugin is a fault
  in VMS, so guard every call to the host and follow the SDK's conventions.

## 4. How the web client loads a plugin

1. The client shell calls `GET /api/v1/plugins/extensions` and receives, for every installed
   plugin, its bundle URL (`/resources/<name>/app.js`), its `settings.json` values and the entries
   of its `component.json`.
2. It loads its own modules, then all plugin bundles in parallel. A plugin whose script or any
   declared component fails to load is marked as failed, all its components are dropped and the
   user sees a message naming it.
3. Entries of type `view` become routes: `/<name>/…` for the client, `/admin/<name>/…` for Admin
   Center. The child routes come from the modules `./clientRoutes` and `./adminRoutes` your bundle
   exposes.
4. Every other entry is put into a map keyed by plugin name and provided to the whole application;
   the host modules render `map[<name>]` in the matching slot. Chapter 04 lists the slots.
5. Translations: a module the host loads at start (the template: the layouts, through
   `import '@/locales'`) registers the plugin's messages. The host applies them once, after all
   plugins are loaded, and its own keys win on conflict — prefix yours.

## 5. Shared singletons

Vue, vue-router, vue-i18n, pinia and every `@incoresoft/*` package are Module Federation
singletons **provided by the host** (`import: false` in the plugin's `shared` config). The plugin
never bundles its own copy, so:

* build and type-check against the versions the host runs (the template pins them: Vue 3.4.15,
  vue-router 4.5.1, vue-i18n 9.9.1, pinia 2.1.7, `@incoresoft/*` `^25.1.0`);
* the host's router, i18n and Pinia are yours to use (`useRouter()`, `useI18n()`, host stores);
* Pinia store ids, modal names and route names are global: prefix them with the plugin name.
