# 03 — Backend

Reference implementation: `templates/backend-plugin-template`. The patterns are the ones
Incoresoft's own plugins use. This chapter describes the VMS **25.1** host (Java 21, Javalin 5.6.3).

## 1. Project

### 1.1 `pom.xml`

Everything the host already has is `provided`; the template pins these versions:

```xml
<dependency> <groupId>com.incoresoft.commons</groupId> <artifactId>middleware</artifactId>        <version>25.1.26</version> <scope>provided</scope> </dependency>
<dependency> <groupId>com.incoresoft</groupId>         <artifactId>device-driver-sdk</artifactId>  <version>25.1.27</version> <scope>provided</scope> </dependency> <!-- cameras, devices -->
<dependency> <groupId>io.javalin</groupId>             <artifactId>javalin</artifactId>            <version>5.6.3</version>  <scope>provided</scope> </dependency>
<dependency> <groupId>com.google.inject</groupId>      <artifactId>guice</artifactId>              <version>7.0.0</version>  <scope>provided</scope> </dependency>
<dependency> <groupId>org.jooq</groupId>               <artifactId>jooq</artifactId>               <version>3.18.7</version> <scope>provided</scope> </dependency>
<dependency> <groupId>com.fasterxml.jackson.core</groupId> <artifactId>jackson-databind</artifactId> <version>2.14.2</version> <scope>provided</scope> </dependency>
<dependency> <groupId>org.slf4j</groupId>              <artifactId>slf4j-api</artifactId>          <version>2.0.17</version> <scope>provided</scope> </dependency>
<dependency> <groupId>javax.validation</groupId>       <artifactId>validation-api</artifactId>     <version>2.0.1.Final</version> <scope>provided</scope> </dependency>
<dependency> <groupId>org.pf4j</groupId>               <artifactId>pf4j</artifactId>               <version>3.8.0</version>  <scope>provided</scope> </dependency>
```

Any 25.1.x `middleware` and `device-driver-sdk` work together; never mix them with libraries of
another release line (26.1 moves to Javalin 7).

What runs is the host's copy of each library: on 25.1 that is Jackson **2.14.2**, Guice 7.0.0,
jOOQ 3.18.7, Liquibase 4.28, Javalin 5.6.3. Call only API that exists in those versions. Bundle
**only** libraries the host does not have; a second copy of a host library inside your jar is loaded
by your class loader and ends in `ClassCastException`s at the boundary.

jOOQ: the host runs the Professional edition, whose packages are identical to the open-source
`org.jooq:jooq`. Compile against the open-source artifact; at runtime the host's classes are used.
The template uses jOOQ's dynamic API (`table(name(...))`, `field(name(...))`) and needs no code
generation.

### 1.2 Manifest

The assembly plugin (`jar-with-dependencies`, output directory `build/`) writes the PF4J
descriptor:

| Attribute | Required | Meaning |
|---|---|---|
| `Plugin-Id` | yes | Unique id, equals the marketplace product id. The template uses the Maven `artifactId` (`vms-sample-plugin`). |
| `Plugin-Version` | yes | Strict SemVer; **major.minor must equal the core's release line** (`25.1.x`), otherwise the upload answers 400 `ERROR_PLUGIN_NOT_COMPATIBLE`. A missing `Plugin-Id` or a non-SemVer version answers 400 `ERROR_PLUGIN_IS_INVALID`. |
| `Plugin-Name` | yes | Display name in Admin Center → Settings → Marketplace. |
| `Plugin-Description` | recommended | Free text or a locale key of your frontend (the template: `SAMPLE_PLUGIN_DESCRIPTION`). |
| `Plugin-Provider` | recommended | Vendor name. |
| `Plugin-Icon-Resource-Path` | recommended | Classpath path of an SVG or PNG, served by the host at `GET /resources/<Plugin-Id>/icon`. |
| `Plugin-Requires` | leave out | A PF4J version constraint. When it does not match the core, PF4J keeps the plugin disabled with only a warning in the log. The major.minor rule already pins the release line, so the template does not set it. |

`GET /api/v1/plugins/installed` reports `id`, `name`, `description` and `version` of a plugin; other
manifest attributes are not exposed by any host API.

### 1.3 Rename checklist

1. `pom.xml`: `groupId`, `artifactId` (= `Plugin-Id`), `plugin.name`, `plugin.provider`.
2. `PluginExtension.PLUGIN_NAME` and the Java package.
3. Table prefix and `logicalFilePath` in `db-changelog.xml`, table names in the repositories.
4. Permission ids and locale keys in `SamplePermissions` (translations live in the frontend).
5. Trigger wire names in `rules/Trigger` and `getTypeLocaleKey()` in `SampleRuleType`, together
   with the frontend's `models/rules.ts` and locales.

## 2. Extension discovery

The host asks PF4J for every `VmsExtension` in your jar. PF4J finds extension classes through
`META-INF/extensions.idx`, generated at **compile time** by PF4J's annotation processor, which
`javac` picks up whenever `pf4j.jar` is on the compile classpath. `VmsExtension` carries an
inherited `@Extension` annotation, so your subclass is indexed without any annotation of its own.
Verify after `mvn package`:

```bash
unzip -p build/vms-sample-plugin-25.1.0.jar META-INF/extensions.idx
# # Generated by PF4J
# com.example.vms.sample.PluginExtension
```

Without the index (annotation processing off, or a build tool without the processor) the host logs
`Plugin <id> misses expected extensions` and does not start the plugin.

## 3. Map of the sample

| Package / file | What it shows |
|---|---|
| `PluginExtension` | Lifecycle: registering the rule-options deserializer, applying the schema, registering permissions, the rule type and the retention task; undoing it in `terminate()` |
| `PluginModule` | Guice bindings; which classes are eager singletons and why |
| `host/HostServicesModule`, `host/CleaningCategories` | Host services from `VmsExtensionContext` as Guice bindings; the host's retention categories |
| `permissions/` | `SamplePermissions` (`ViewSampleItems`, `ManageSampleItems`, `ManageSampleCategories`) and their registration with the host |
| `db/`, `resources/sql/db-changelog.xml` | Liquibase schema in the shared database: `sample_categories` (name, camera) and `sample_items` (text, category, the camera copied from the category); a second changeSet adding columns; `TIMESTAMP(3)` columns; jOOQ without code generation; `ItemsQuery` for the list filters |
| `dto/` | Wire format: snake_case keys, epoch milliseconds; `PageDTO` for paged lists |
| `http/CategoriesController` | `/api/v2/sample_plugin/categories`: reading needs `ViewSampleItems`, writing `ManageSampleCategories`; the camera is validated through `DeviceManager`; the rule-source cache is dropped on every change |
| `http/ItemsController` | `/api/v2/sample_plugin/items`: paged list with the filters the client page and the Search page need (`category_id` / `category_ids`, `camera_ids`, `text`, `start_date`, `end_date`, `sort_order`, `limit`, `offset`), creation, deletion, domain events |
| `http/Requests` | Request parsing that answers 400/404 in the host's error format instead of the host's catch-all 500; list parameters as the host frontend sends them (`ids=[1,2]`) |
| `http/FrontendResources` | Serving the frontend bundle and the icon at `/resources/<plugin_name>/…` |
| `core/ItemEventBus` | Fan-out inside the plugin: the HTTP layer knows nothing about rules or WebSockets |
| `rules/` | A rule type whose sources are the categories (with the category's camera, so alarms show video), two triggers with polymorphic options, the alarm payload, the bridge that fires rules |
| `ws/` | A live WebSocket feed: permission check on connect and on every send, heartbeats, a scheduler stopped on terminate |

## 4. `VmsExtension` — the contract

```java
public abstract class VmsExtension extends SystemExtension<VmsExtensionContext> { }

// SystemExtension, middleware 25.1:
public abstract String getPluginName();                                    // module name — a JavaScript identifier
public abstract Class<? extends AnalyticsModule> getModuleClass();         // return null: service plugin
public abstract List<Table<?>> getManagedTables();                         // your jOOQ tables (included in backups)
public List<Table<?>> getBackupIgnoredTables() { return List.of(); }       // tables to leave out of backups
public List<String> getFrontComponents() { ... }                           // reads public/component.json — do not override
public JSONObject   getSettings()        { ... }                           // reads public/settings.json — do not override
// init(VmsExtensionContext) and terminate() from the lifecycle (01 §3)
```

Rules:

* Public no-arg constructor, non-abstract class.
* `init()` runs on the host's plugin-start thread: keep it fast, start background work on your own
  executor and stop it in `terminate()`.
* If `init()` throws, the host stops the plugin and calls `terminate()` on the same instance; keep
  the fields you need before registering anything and null-check them in `terminate()`.
* `getPluginName()` is called often; return a constant.
* `getManagedTables()`: list the tables your changelog creates, so system backups include them.
* Do not override `customAuditActions()`: the host never unregisters custom audit actions, so the
  plugin would fail to start again until VMS restarts.

What happens on stop, update or uninstall:

* Removed by the host: the HTTP and WebSocket routes registered during `init()`.
* Must be undone in `terminate()`: permissions, the rule type (which also removes the notification
  type), map emitters, listeners, your threads and executors, open WebSocket sessions.
* Not undone by anyone until VMS restarts: modules registered on the host's `ObjectMapper`,
  `Cleaner` tasks (there is no remove call), Javalin `exception(...)` handlers. Keep them idempotent
  and harmless when your plugin is gone.

## 5. Wiring with Guice

```java
Injector injector = Guice.createInjector(new HostServicesModule(context, this))
                         .createChildInjector(new PluginModule(context));
```

`HostServicesModule` binds the host services from the context so you can `@Inject` them anywhere:
`Javalin`, `DSLContext`, `DatabaseConnector`, `UsersManager`, `VmsPluginRolesManager`,
`RuleManager`, `VmsNotificationManager`, `MapPlanEventManager`, `Cleaner`, `GlobalSettings`,
`StreamsManager`, `Storage`, `DeviceManager` and `VmsExtension` (yourself). Any other context
getter is one more line: `bind(X.class).toInstance(context.getX())`.

Services and controllers are `asEagerSingleton()`: controllers register their routes in the
constructor or in an `@Inject public void init(Javalin app)` method, so creating the injector wires
the whole plugin. A listener that subscribes to the in-plugin event bus must be an eager singleton
too, otherwise nothing constructs it.

## 6. Host services

What `VmsExtensionContext` gives a service plugin (types from `middleware` unless stated):

| Getter | Type | Purpose |
|---|---|---|
| `getJavalin()` | `io.javalin.Javalin` | Register HTTP, WebSocket and SSE routes |
| `getDSLContext()` | `org.jooq.DSLContext` | SQL on the shared database |
| `getDatabaseConnector()` | `DatabaseConnector` | `checkStructure(changelog, classLoader)`, `getDataSource()`, `getDBType()` |
| `getUsersManager()` | `UsersManager` | `getUser(Context)`, `getUser(WsContext)`, `getUser(token)`, `getAll()` |
| `getRolesManager()` | `VmsPluginRolesManager` | `register(pluginName, PluginPermissions)`, `unregister(...)`, `getAll()`, `getPermissions()` |
| `getRuleManager()` | `RuleManager` | Alarm rules: `registerRuleType`, `unregisterRuleType`, `getRulesOfType`, `triggerRule(rule, alarm)` |
| `getNotificationsManager()` | `VmsNotificationManager` | `registerNotificationType`, `addNotification` |
| `getMapPlanEventManager()` | `MapPlanEventManager` | Objects and live events on maps and plans |
| `getDeviceManager()` | `com.incoresoft.vms.drivers.sdk.DeviceManager` | Cameras and devices: `getItem(id)`, `getDeviceByItemId(id)`, `getCameraByStreamUuid`, `isCameraAllowed(user, cameraId)` |
| `getStreamsManager()` | `StreamsManager` | Video streams: `get(uuid)`, `getAll(user)`, `createSnapshot(uuid)`, folders, `StreamListener` |
| `getStorage()` | `Storage` | `get(path)`, `save(path, File | InputStream)`, `delete(path)` in the VMS storage directory |
| `getGlobalSettings()` | `GlobalSettings` | `getSettingsMap()`, `getValue(key)`, `addListener(OnSettingsChangeListener)` |
| `getCleaner()` | `Cleaner` | Retention tasks (§13) |
| `getObjectMapper()` | Jackson `ObjectMapper` | The host's mapper; register your (de)serializers on it |

The context has more getters (message queue, analytics, VEZHA integration, audit). They serve
Incoresoft's own plugins and are not part of the supported surface for partners; `getSettings()`
and `getEventsManagerConnector()` throw `UnsupportedOperationException` on 25.1.

Users and permissions:

```java
interface User extends ClientId {
    int getId(); String getEmail(); String getFullname(); int getRoleId(); boolean isRoot();
    boolean hasPermission(Permission); boolean hasAllPermissions(Collection<? extends Permission>);
    boolean hasAnyPermission(Collection<? extends Permission>);
    default ZoneId getTimezone(); default DayOfWeek getFirstDayOfWeek(); default boolean hasAccessTo(ClientId);
}
interface UsersManager {   // also get(Integer), getAll()
    User getUser(io.javalin.http.Context) throws UnauthorizedResponse;        // throws without a valid session or token
    User getUser(io.javalin.websocket.WsContext);                             // 25.1 host: null when the session is gone
}
interface Permission extends io.javalin.security.RouteRole { String getId(); String getLocaleKey(); Set<String> getRequired(); }
```

## 7. Database schema

```java
boolean ok = context.getDatabaseConnector().checkStructure("sql/db-changelog.xml", getClass().getClassLoader());
if (!ok) throw new IllegalStateException("Cannot check/update database structure of " + PLUGIN_NAME);
```

* Liquibase runs against the **shared VMS database** on every plugin start. Prefix table names
  with your plugin (`sample_categories`, `sample_items`).
* Every plugin ships its changelog as `sql/db-changelog.xml`, and Liquibase identifies a changeSet
  by id + author + file path. Give the changelog a unique `logicalFilePath` (the template:
  `sample_plugin/db-changelog.xml`). Never edit an applied changeSet; add a new one, as the
  template's `sample-003-cameras` does.
* Keep `objectQuotingStrategy="QUOTE_ALL_OBJECTS"` and the `${now}` property pattern from the
  template: they keep the changelog portable across database types.
* `INT` for user ids (`User.getId()` is `int`) and camera ids, `TIMESTAMP(3)` for instants written
  as `Timestamp.from(Instant)`, the same convention as the core; the host's retention task compares
  the column in the server's time zone.

## 8. HTTP API

```java
@Inject
public void init(Javalin app) {
    app.routes(() -> path(API_PATH, () -> {                       // "/api/v2/sample_plugin/items"
        get(this::list, LOGGED_IN, API_CALL, VIEW_SAMPLE_ITEMS);
        post(this::create, LOGGED_IN, API_CALL, MANAGE_SAMPLE_ITEMS);
        path("{id}", () -> {
            get(this::getOne, LOGGED_IN, API_CALL, VIEW_SAMPLE_ITEMS);
            delete(this::remove, LOGGED_IN, API_CALL, MANAGE_SAMPLE_ITEMS);
        });
    }));
}
```

`get`, `post`, `put`, `patch`, `delete` and `path` are the static methods of
`io.javalin.apibuilder.ApiBuilder`.

* **Path prefix** `/api/v2/<module_name>/…`. `/api/v1/*` belongs to the core. Never register a
  wildcard outside your prefix.
* **Roles** (`com.incoresoft.middleware.http.AppRole`): `ANYONE` = no authentication. `LOGGED_IN`
  and `API_CALL` trigger the same check: a session cookie **or** an `Authorization: Bearer <api
  token>` header is accepted. Incoresoft's code lists both to say "for the UI and for integrations".
  Every `Permission` in the role list is required; a missing one answers 403. The root user passes
  every permission check.
* **Current user:** `usersManager.getUser(ctx)`; `user.hasPermission(p)`, `user.getTimezone()`,
  `user.getClientId()`.
* **JSON:** `ctx.json(obj)` and `ctx.bodyAsClass(Dto.class)` use the host's Jackson mapper. It uses
  `SNAKE_CASE` names, ignores unknown properties and writes `Instant` as epoch milliseconds. The
  template annotates every field with `@JsonProperty` and keeps timestamps as `long` milliseconds
  in Java records. Register (de)serializers for your own classes on `context.getObjectMapper()` in
  `init()`; they stay registered after your plugin stops, so register only your own types.
* **Query parameters:** the host frontend writes arrays as `ids=[1,2]` and writes **every** key it
  is given, unset ones as `name=undefined`. The template's `Requests.listParam` parses the bracket
  form (and plain `1,2`); on the frontend side the template adds only parameters that have a value.
* **Paged lists:** `?limit=&offset=` in, `{"data": [...], "total": n, "pages": n}` out, the shape
  the host uses for its own lists (template: `PageDTO`).
* **Errors:** the body of every error is the array `[{"type", "field", "args"}]`. `type` is a
  locale key the frontend translates, `field` names the input (`null` for a general error), `args`
  fills placeholders. With a `field` the host UI shows the message under that input; without one, a
  toast. Plugin keys (`SAMPLE_…`) are translated by your frontend locales, host keys by the host.

  | Thrown | Status | `type` |
  |---|---|---|
  | `com.incoresoft.middleware.http.exceptions.ValidationException(type, field, args…)` | 400 | your key, or a host key such as `FIELD_REQUIRED` |
  | Javalin `NotFoundResponse` | 404 | `ERROR_NOT_FOUND` |
  | Javalin `ForbiddenResponse` | 403 | `ERROR_FORBIDDEN` |
  | Javalin `UnauthorizedResponse` | 401 | `ERROR_UNAUTHORIZED` |
  | Javalin `BadRequestResponse` | 400 | `ERROR_BAD_REQUEST` |
  | A body `ctx.bodyAsClass` cannot deserialize | 498 | `ERROR_DESERIALIZATION_FAILED` |
  | Anything else | 500 | `ERROR_FAILED_TO_PROCESS_REQUEST` |

* **Validation:** the template checks input by hand. `POST /items` with a blank `text` answers
  400 `[{"type":"FIELD_REQUIRED","field":"text","args":[]}]`; a category with items cannot be
  deleted: 400 `[{"type":"SAMPLE_CATEGORY_IN_USE","field":null,"args":[]}]`. `http/Requests` turns a
  body it cannot read into 400 `ERROR_DESERIALIZATION_FAILED` and a non-numeric `{id}` into 404, so
  parse errors never reach the catch-all 500.
* **Exception handlers:** prefer throwing `ValidationException`. `app.exception(...)` handlers are
  global on the shared Javalin and stay registered after your plugin stops; map only your own
  exception classes.

## 9. WebSocket and SSE

```java
app.ws("/api/v2/ws/sample_plugin/items", ws -> {
    ws.onConnect(ctx -> {
        User user = usersManager.getUser(ctx);                 // null when the session is gone
        if (user == null || !user.hasPermission(VIEW_SAMPLE_ITEMS)) { ctx.closeSession(1008, "Forbidden"); return; }
        ctx.enableAutomaticPings();
        sessions.put(ctx, user.getId());
    });
    ws.onClose(ctx -> sessions.remove(ctx));
    ws.onError(ctx -> sessions.remove(ctx));
}, LOGGED_IN, API_CALL);
```

The route roles authenticate the upgrade request but check no plugin permission; check it in
`onConnect` and close with 1008 when it is missing. The template's `ws/ItemsWebSocketController`
is the whole pattern: a session map filled after the check and emptied on close **and** error,
`broadcast` = serialise once and send to every session, re-checking the user's permission on every
send (permissions can be revoked while the socket is open), a heartbeat every 30 s
(`{"type":"HEALTH_CHECK"}`), and `terminate()` that stops the scheduler and closes the sessions.

Frontend side: `${coreApi.config.WEBSOCKET_API}/api/v2/ws/<name>/…`; the host's
`styleguide/services/WebSocketService` reconnects and parses JSON (template:
`composables/useItemsLive.ts`).

## 10. Serving the frontend bundle

The web client loads a plugin from fixed URLs; the plugin serves them itself. The template's
`http/FrontendResources` registers both routes in its constructor:

| Route | Source |
|---|---|
| `GET /resources/<name>/*` | files under `public/` on the plugin class loader (`app.js`, chunks, `component.json`, `settings.json`); `Cache-Control: public, max-age=3600` for everything but `app.js` |
| `GET /resources/<name>/icon` | `icon.svg` at the jar root |

Both routes are `ANYONE`: the browser fetches them before login.

## 11. Permissions

Permissions are `Permission` objects registered per plugin with
`VmsPluginRolesManager.register(pluginName, PluginPermissions)`. The host shows them in Admin
Center → Roles under the plugin's name, with the label from `getLocaleKey()` translated by your
frontend, stores grants per role, and enforces them as route roles, through `User.hasPermission`
and on the frontend through `usePermissionStore().hasPluginPermission(id)` and `settings.json`.

```java
public enum SamplePermissions implements Permission {
    VIEW_SAMPLE_ITEMS("ViewSampleItems", "PERMISSION_VIEW_SAMPLE_ITEMS"),
    MANAGE_SAMPLE_ITEMS("ManageSampleItems", "PERMISSION_MANAGE_SAMPLE_ITEMS", VIEW_SAMPLE_ITEMS),
    MANAGE_SAMPLE_CATEGORIES("ManageSampleCategories", "PERMISSION_MANAGE_SAMPLE_CATEGORIES", VIEW_SAMPLE_ITEMS);
    // getId(), getLocaleKey(), getRequired() — "Manage requires View": the Roles UI selects them together
}
rolesManager.register(PLUGIN_NAME, PLUGIN_PERMISSIONS);     // init()
rolesManager.unregister(PLUGIN_NAME, PLUGIN_PERMISSIONS);   // terminate(), the same object
```

Permission ids are global across VMS and all plugins: keep your plugin's name in them.

## 12. Alarm rules, alarms, notifications

Operators create **rules** in Admin Center → Alarm Rules: a type with options ("What"), sources
("Where"), a schedule ("When") and actions (alarm, notification, recording, …). A plugin adds rule
**types**; the host runs the actions.

### 12.1 The rule type

One class implements `RuleType` and `VmsNotificationType` (template: `rules/SampleRuleType`), so
one type id covers alarms and notifications, and registering it with `RuleManager` registers the
notification type too.

| Method | Contract |
|---|---|
| `getTypeId()` | The `type` of your alarms and notifications on the wire; the frontend picks your alarm components by it. Use the plugin name. |
| `getTypeLocaleKey()`, `getPluginName()` | Label in the rule wizard; the plugin whose "What" block is rendered. |
| `getRuleDataClass()` | Jackson target of `rule.options`. Usually polymorphic on a `trigger` field, with a deserializer registered on `context.getObjectMapper()` in `init()`. The wizard sends `options` before the user picked a trigger: return `null` for a missing or unknown trigger instead of throwing. |
| `getRuleMessageClass()`, `getNotificationMessageClass()` | Jackson target of `alarm.message`; any JSON your alarm components understand. |
| `getSources()` | The objects a rule can be scoped to in the "Where" step (the template: the categories). Each `RuleSource` has `getId()` (a stable UUID stored in rules and carried by alarms), `getName()` (shown as the alarm's source), `isAllowed(user)` (who may see alarms of this source) and `getCameraId()`. **Called on hot paths** (per alarm, per user): cache it. |
| `getCameraId()` on a `RuleSource` | The VMS camera behind the source. The host attaches it to every alarm of the source as `camera_id`, together with the camera's recording server, which is what gives the alarm its video. |
| `getDynamicSources(options)` | Source UUIDs a rule covers by itself, added to the ones picked in "Where". The template returns every category for "Item contains a word". |
| `requiresSources(options)` | `false` for triggers without object scope; the rule is then saved without sources. |

### 12.2 Firing

```java
for (Rule rule : ruleManager.getRulesOfType(ruleType)) {
    if (!matches(item, rule.getOptions())) continue;
    ruleManager.triggerRule(rule, new SampleAlarm(sourceId, serverId, message, recipients, Instant.ofEpochMilli(item.createdAt())));
}
```

`triggerRule` ignores the call when the rule is disabled or outside its schedule, and **silently
drops** an alarm whose `getSourceId()` is not among the rule's sources (the "Where" selection plus
`getDynamicSources`). Then it creates the alarm, the notification if the rule has that action, and
runs the other actions. `Alarm` is five getters: `getSourceId()`, `getServerId()` (the recording
server UUID of the related camera, or `null`), `getMessage()`, `getAllowedUserIds()` (who may see
it; keep it consistent with `RuleSource.isAllowed`) and `getTimestamp()` (when it happened).

Anything you call on the host while firing (the template resolves the camera's server through
`DeviceManager`) should be guarded: an exception in your listener loses the alarm and the
notification, while an alarm without a server is still an alarm.

### 12.3 How the sample does it

| Trigger (wire name) | Options class | Scope |
|---|---|---|
| `sample_any_item` | `AnyItemTriggerSettings` | The categories picked in "Where" (`requiresSources` is `true`) |
| `sample_contains_word` | `ContainsWordTriggerSettings` (`word`, `@NotBlank`) | Every category, including ones created later (`requiresSources` false, `getDynamicSources` returns all) |

* The alarm always carries the item's category as source; the host shows the category name and
  attaches the category's camera.
* `getSources()` is cached for 5 s and dropped whenever a category changes
  (`SampleRuleType.invalidateSources()`, called by `CategoriesController`).
* Trigger wire names carry the plugin prefix: the host translates a trigger as
  `t(trigger.toUpperCase())` in a namespace shared by every plugin, so the keys are
  `SAMPLE_ANY_ITEM` and `SAMPLE_CONTAINS_WORD`.
* The host bean-validates the options when a rule is saved: a blank word answers 400 with
  `{"type":"NOT_BLANK","field":"options.word"}`, shown under the input by the "What" block.
* A category that rules still point at can be deleted once it has no items; those rules keep the
  source id and stop matching.

### 12.4 Frontend needs

`admin_events_and_rules_options_block` (the "What" step is empty without it),
`admin_events_and_rules_where_block` (your own source picker), `alarms_type_column`,
`alarms_message_column`, `alarms_details_message`, `alarms_details_preview` (video),
`client_search_alarm_info`, `plugin_notification_item`, and `"alarms_available": true` in
`settings.json`. Chapter 04 lists them.

### 12.5 Notifications

Notifications normally come from the rule action. `VmsNotificationManager.addNotification(...)`
lets a plugin push one directly. A notification type registered through the rule type needs no
second registration.

## 13. Retention

Operators set the retention per category in Admin Center → Settings → Storage Cleaner. The host
matches a category by its type id (`logs`, `alerts`, `plugins_data`); the template's
`host/CleaningCategories` implements `CleaningCategory` with those ids.

```java
context.getCleaner().addDatabaseCleaningTask(CleaningCategories.PLUGINS_DATA, "sample_items", "created_at");
cleaner.addStorageCleaningTask(CleaningCategories.PLUGINS_DATA, "sample_plugin/images");
cleaner.addCustomTask(() -> ...);
```

The column must hold instants written like the core's (`TIMESTAMP(3)`, `Timestamp.from(Instant)`);
index it. The cleaner runs once a day, ten minutes after midnight, and the retention is set in whole
days (at least one), so a test of your task takes a night. There is no unregister call: a task lives
until VMS restarts and every plugin start adds it again. Deleting rows twice is harmless; keep
custom tasks idempotent.

## 14. Cameras and devices

* `deviceManager.getItem(cameraId)` → `DeviceItemSettings` (`id`, `uuid`, `name`, `type`); check
  `type() == DeviceItemType.CAMERA` when a user hands you a camera id (template:
  `CategoriesController`).
* `deviceManager.getDeviceByItemId(cameraId).map(Device::getServerId)` → the recording server of
  the camera, for alarms.
* `deviceManager.isCameraAllowed(user, cameraId)` → respect per-camera permissions when you expose
  camera-bound data.
* `streamsManager.createSnapshot(uuid)` → a JPEG in storage.

## 15. Maps and plans

A plugin can add its own placeable objects to maps and floor plans and push live events onto them.
Implement `MapPlanEventEmitter` (`getId()` stable UUID, `getPluginName()`, `getLocaleKey()`,
`getIcon()` = URL of an icon in your bundle, `getGroups()`, `getSources()` with parameters,
`getEventSettingsClass()`), register it in `init()` and unregister in `terminate()`; on events call
`mapPlanEventManager.processEvent(sourceId, filter, factory)`. The frontend side needs the
`map_plan_*` slots of chapter 04. The sample does not use maps; Incoresoft's access-control plugin
is the reference and can be shared on request.

## 16. Logging, threads, configuration

* Logging: SLF4J; the host writes `log/application.log`. Prefix messages with your plugin name.
* Threads: your own daemon executors, named, shut down in `terminate()`.
* Configuration: there is no per-plugin configuration API. Keep settings in your own tables, edited
  through your Admin Center pages (the sample's categories), or read environment variables.
  `GlobalSettings` gives read access to the system settings.
* Not available to plugins on 25.1: writing audit records (no custom audit actions), the Event
  Manager, a background job scheduler (use your own executor).
