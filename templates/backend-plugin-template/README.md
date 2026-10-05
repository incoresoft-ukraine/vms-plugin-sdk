# vms-plugin-backend-template

The backend of the **Sample Plugin** for Incoresoft VMS, release line **25.1**. Copy it, rename a few
identifiers, and you have a plugin that VMS loads at runtime with its own REST API, database
tables, permissions, alarm rule type, live feed and a place to ship your frontend bundle.

Companion repositories:

- [vms-plugin-frontend-template](https://github.com/incoresoft-ukraine/vms-plugin-frontend-template) — the matching Vue 3 frontend; a build of it is already in
  `src/main/resources/public/`.
- [vms-plugin-sdk](https://github.com/incoresoft-ukraine/vms-plugin-sdk) — the documentation. This file only gets you building; the SDK's chapter 03
  explains every piece.

## What the sample does

**Categories** are the plugin's settings, managed in Admin Center; a category has a name and,
optionally, a VMS camera. **Items** are the operators' data, created in the client and filed under
a category; an item remembers the category's camera, so VMS can show the archive video of the
moment it was created. Operators build alarm rules on items; the Search page gets a block for them;
a layout cell shows the newest ones.

| Path | Purpose |
|---|---|
| `PluginExtension.java` | Entry point: `init` / `terminate`, plugin name, managed tables, retention task |
| `PluginModule.java`, `host/` | Guice wiring; host services from `VmsExtensionContext` as bindings; retention categories |
| `permissions/` | `ViewSampleItems`, `ManageSampleItems`, `ManageSampleCategories`, registered in Admin Center → Roles |
| `db/`, `resources/sql/db-changelog.xml` | Liquibase schema (`sample_categories`, `sample_items`) and jOOQ repositories without code generation |
| `http/` | REST API `/api/v2/sample_plugin/categories` and `/api/v2/sample_plugin/items` (paged, filterable), request parsing in the host's error format, serving the frontend bundle |
| `rules/` | The alarm rule type: categories as sources (with their camera), two triggers, the alarm payload, the bridge that fires rules |
| `ws/` | Live WebSocket feed `/api/v2/ws/sample_plugin/items` |
| `src/main/resources/public/` | The built frontend bundle |
| `pom.xml` | `provided` dependencies, fat-jar assembly with the PF4J manifest |

## Requirements

- JDK **21**, Maven 3.9+. Every dependency, `com.incoresoft.commons:middleware` and
  `com.incoresoft:device-driver-sdk` included, comes from Maven Central; nothing to configure.
- A VMS 25.1 installation (build 25.1.1063 or newer) and its root user.

Everything else is provided by the host at runtime (Javalin 5.6.3, Guice 7.0.0, jOOQ 3.18.7,
Jackson 2.14.2, Liquibase 4.28, PF4J 3.8.0). Compile against open-source jOOQ; call only API that
exists in those versions.

## Build and install

```bash
mvn clean package                                                      # → build/vms-sample-plugin-25.1.0.jar
unzip -p build/vms-sample-plugin-25.1.0.jar META-INF/extensions.idx    # lists com.example.vms.sample.PluginExtension
```

Admin Center → Settings → Marketplace → Extra Settings → **Install Plugin from Disk**. VMS starts the
plugin at once. When the plugin is already installed, Admin Center asks whether to reinstall it; the
new jar then replaces the old one while VMS runs, and the plugin's tables, data and alarm rules
stay.

Over the API, as the root user: first install `POST /api/v1/plugins/vezha` (multipart `file`),
update `PATCH /api/v1/plugins/vezha/vms-sample-plugin/update_from_file` (multipart `file`).

Never copy a jar over the installed one in the `plugins/` directory while VMS runs.

## Development loop

1. Frontend: `npm run build` in `vms-plugin-frontend-template`, copy `dist/*` into
   `src/main/resources/public/` (replace the folder's content).
2. `mvn clean package`.
3. Upload the jar in Admin Center and confirm the reinstall. Reload the browser page.

## Rules to keep

| Rule | Why |
|---|---|
| `Plugin-Version` = `25.1.<patch>` | The host accepts only the core's major.minor (`ERROR_PLUGIN_NOT_COMPATIBLE` otherwise) |
| No `Plugin-Requires` | When it does not match, PF4J keeps the plugin disabled with only a log warning |
| `PLUGIN_NAME` is a valid JavaScript identifier and equals the frontend's webpack `name` | It is the Module Federation container name, the URL prefix and the key of every UI slot |
| Routes under `/api/v2/<PLUGIN_NAME>/…` | `/api/v1/*` is the core |
| Table names carry the plugin prefix; the changelog has its own `logicalFilePath`; applied changeSets are never edited | The plugin shares the VMS database; Liquibase identifies changeSets by id + author + path |
| Host libraries are `provided`, never bundled | A second copy inside the jar ends in `ClassCastException`s |
| Guard every call into the host in listeners | An exception in a listener loses the alarm; the sample logs and continues |
| Do not override `customAuditActions()` | The host never unregisters them; the plugin would fail to start again until VMS restarts |

## Rename checklist

1. `pom.xml`: `groupId`, `artifactId` (= `Plugin-Id`), `plugin.name`, `plugin.provider`.
2. `PluginExtension.PLUGIN_NAME` and the Java package.
3. Table prefix and `logicalFilePath` in `db-changelog.xml`, table names in the repositories.
4. Permission ids and locale keys in `SamplePermissions`; translations live in the frontend.
5. Trigger wire names in `rules/Trigger` and `getTypeLocaleKey()` in `SampleRuleType`, together
   with the frontend's `models/rules.ts` and locales.
6. `Plugin-Description`: free text or a locale key of your frontend.

## Support and license

Support: support@incoresoft.com

MIT License (see `LICENSE`): copy the template and license your plugin as you like. The Incoresoft
Java libraries it depends on (`middleware`, `device-driver-sdk`) are published on Maven Central under
the Apache License 2.0.
