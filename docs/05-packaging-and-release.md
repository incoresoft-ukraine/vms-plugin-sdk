# 05 — Packaging, versioning, release

## 1. The artifact

One fat jar (`maven-assembly-plugin`, `jar-with-dependencies`, `appendAssemblyId=false`) named
`<artifactId>-<version>.jar` in `build/`, with the manifest attributes of 03 §1.2 and:

```
public/app.js, public/*.js, public/component.json, public/settings.json   ← frontend
META-INF/extensions.idx                                                     ← PF4J
sql/db-changelog.xml, icon.svg                                              ← resources
```

## 2. Getting the frontend into the jar

1. `npm run build` → `dist/` (`app.js`, the chunks and the copied `public/*.json`).
2. Replace the content of the backend's `src/main/resources/public/` with `dist/*`. Replace, do not
   add: chunk names carry a content hash and old chunks would pile up.
3. `mvn clean package`.

If frontend and backend live in separate repositories, publish `dist/` as a jar whose root contains
`public/…` and let the backend depend on it with compile scope, so the assembly unpacks it into the
fat jar; the template's `pom.xml` has such a dependency commented out. Both ways give the same jar.

## 3. Versioning

* **Plugin-Version** is strict SemVer and its **major.minor equals the VMS release line**: `25.1.N`
  for VMS 25.1, checked on upload (`ERROR_PLUGIN_NOT_COMPATIBLE` otherwise). Your own product
  version can go into `Plugin-Name` or into your release notes.
* A plugin is built for one release line. Another line needs its own build: its own library
  versions, its own `Plugin-Version` series and possibly source changes (26.1 moves to Javalin 7).
  Keep one branch per release line and never mix libraries of two lines.
* `Plugin-Requires` is not needed and the template omits it (03 §1.2).

| Release line | middleware | device-driver-sdk | Javalin | jOOQ | Guice | Jackson | Java | npm `@incoresoft/*` |
|---|---|---|---|---|---|---|---|---|
| **25.1** | 25.1.26 | 25.1.27 | 5.6.3 | 3.18.7 | 7.0.0 | 2.14.2 | 21 | `^25.1.0` |

Newer 25.1.x libraries are compatible within the line.

## 4. CI outline

```
build:    mvn -B clean compile                              # backend
          npm ci && npm run typecheck && npm run build       # frontend
test:     mvn test
package:  replace src/main/resources/public with dist ; mvn -DskipTests package
          unzip -p build/*.jar META-INF/extensions.idx       # must list your VmsExtension
deploy:   PATCH /api/v1/plugins/vezha/<Plugin-Id>/update_from_file on your test VMS (§6)
```

## 5. Host HTTP API for plugin management

Base URL: `https://<host>:2443` (or `http://…:2002`). All plugin-management calls need the **root
user**: a session or an API token of root (Admin Center → Settings → API Token), sent as
`Authorization: Bearer <token>`.

Errors are the array `[{"type": "<LOCALE_KEY>", "field": <name|null>, "args": [...]}]` (03 §8).

### Discovery

`GET /api/v1/plugins/extensions` — anonymous. The entry of the sample plugin:

```json
{
  "module_name": "sample_plugin",
  "plugin_id": "vms-sample-plugin",
  "type": "service_plugin",
  "path": "resources/sample_plugin/app.js",
  "title": "Sample Plugin",
  "alarms_available": true,
  "live_view_disabled": false,
  "integration_required": false,
  "components": [ { "component": "ClientLayoutSamplePlugin", "path": "/", "type": "view", "location": null, "children": null }, … ],
  "routes_permissions": { "/sample_plugin": "ViewSampleItems" },
  "client_permissions": { "search": "[\"ViewSampleItems\"]", "plugin_page": "[\"ViewSampleItems\"]" }
}
```

`title`, `alarms_available`, `routes_permissions` and `client_permissions` come from
`settings.json` (the `client_permissions` values are passed through as JSON strings), `components`
from `component.json`.

### Management

| Method and path | Purpose |
|---|---|
| `GET /api/v1/plugins/installed` | Installed plugins and drivers: `id`, `name`, `description`, `version`, `enabled`, `type`, `install_path` |
| `POST /api/v1/plugins/vezha` (multipart `file`) | Upload and start a new plugin; 204 |
| `PATCH /api/v1/plugins/vezha/{id}/update_from_file` (multipart `file`) | Replace an installed plugin with the uploaded jar; the plugin's tables, settings and alarm rules stay; 204. Any version compatible with the core is accepted, the installed one included |
| `PATCH /api/v1/plugins/vezha/{id}/start` · `…/stop` | Start or stop; the state survives a VMS restart |
| `DELETE /api/v1/plugins/vezha/{id}` | Stop, unload, delete the jar. Refused while alarm rules of the plugin's rule type exist |
| `PATCH /api/v1/plugins/vezha/{id}/update` | Update to a marketplace version |
| `PUT /api/v1/plugins/vezha/start` · `…/stop` · `PUT /api/v1/plugins/update` | All plugins |
| SSE `GET /api/v1/sse/plugins` | Plugin started / stopped / installed / uninstalled events; the web client shows its reload prompt on them |

`{id}` is the **Plugin-Id**. The `vezha` path segment is historical and means "VMS plugin"; the same
routes exist under `driver` for device drivers.

Answers of the upload and the update (400 unless noted):

| `type` | Cause |
|---|---|
| `ERROR_PLUGIN_NOT_COMPATIBLE` | `Plugin-Version` major.minor differs from the core's |
| `ERROR_PLUGIN_IS_INVALID` | `Plugin-Id` missing, `Plugin-Version` not SemVer, or (update) the jar belongs to another plugin |
| `ERROR_FILE_MUST_HAVE_JAR_EXTENSION` | the file name does not end in `.jar` |
| `ERROR_PLUGIN_IS_ALREADY_INSTALLED` | upload of a `Plugin-Id` that is installed; use the update |
| `ERROR_PLUGIN_NOT_FOUND` | update of a `Plugin-Id` that is not installed |
| `ERROR_PLUGIN_IS_BEING_INSTALLED` | another install or update of the same plugin is running |
| `ERROR_PLUGIN_LOADING_FAILED` (500) | the jar could not be loaded or started (`init()` threw, …). An upload deletes the jar again; an update puts the previous jar back |
| `ERROR_FORBIDDEN` (403) | the user is not root |

### Resources

| Path | Content |
|---|---|
| `GET /resources/<module_name>/*` | the frontend bundle, served by the plugin (03 §10) |
| `GET /resources/<module_name>/icon` | the plugin icon, served by the plugin |
| `GET /resources/<Plugin-Id>/icon` | the icon from the manifest, served by the host |
| `GET /storage/<path>` | files saved through `Storage` |
