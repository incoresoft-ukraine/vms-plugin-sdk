package com.example.vms.sample;

import com.example.vms.sample.db.CategoriesRepository;
import com.example.vms.sample.db.ItemsRepository;
import com.example.vms.sample.host.CleaningCategories;
import com.example.vms.sample.host.HostServicesModule;
import com.example.vms.sample.permissions.SamplePermissionsManager;
import com.example.vms.sample.rules.SampleRuleManager;
import com.example.vms.sample.rules.SampleTriggerSettings;
import com.example.vms.sample.rules.SampleTriggerSettingsDeserializer;
import com.example.vms.sample.ws.ItemsWebSocketController;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.incoresoft.middleware.model.analytics.module.AnalyticsModule;
import com.incoresoft.middleware.vms.plugin.VmsExtension;
import com.incoresoft.middleware.vms.plugin.VmsExtensionContext;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Entry point of the plugin.
 *
 * <p>The host (VMS core) discovers this class through PF4J: {@link VmsExtension} carries the
 * inherited {@code @org.pf4j.Extension} annotation, so the PF4J annotation processor writes the
 * fully qualified name of this class into {@code META-INF/extensions.idx} at compile time.
 * No annotation is needed here; do NOT make the class abstract and keep a public no-arg constructor.
 *
 * <p>Lifecycle: {@link #init(VmsExtensionContext)} is called when the plugin is started
 * (at host boot for enabled plugins, or right after upload/installation), {@link #terminate()}
 * when it is stopped, updated or uninstalled. Everything you register with the host in init()
 * (rule types, notification types, permissions, map emitters, MQ listeners) must be
 * unregistered in terminate(). HTTP routes are removed by the host automatically.
 */
public class PluginExtension extends VmsExtension {
    private static final Logger log = LoggerFactory.getLogger(PluginExtension.class);

    /**
     * Plugin name = "module name" used everywhere on the wire:
     * <ul>
     *   <li>Module Federation container name of the frontend bundle (webpack {@code name}),</li>
     *   <li>URL prefix of the frontend bundle: {@code /resources/sample_plugin/app.js},</li>
     *   <li>client/admin route prefix: {@code /sample_plugin/...}, {@code /admin/sample_plugin/...},</li>
     *   <li>key of the plugin in every frontend slot map (component.json),</li>
     *   <li>id of the rule type and of the notification type.</li>
     * </ul>
     * It MUST be a valid JavaScript identifier (letters, digits, underscore; no hyphens).
     */
    public static final String PLUGIN_NAME = "sample_plugin";

    /** Liquibase changelog on the plugin classpath. */
    public static final String DB_CHANGELOG_FILE = "sql/db-changelog.xml";

    /** Tables owned by the plugin (used by the host for backups). */
    public static final List<Table<?>> MANAGED_TABLES = List.of(
            DSL.table(DSL.name(CategoriesRepository.TABLE_NAME)),
            DSL.table(DSL.name(ItemsRepository.TABLE_NAME))
    );

    private SamplePermissionsManager permissionsManager;
    private SampleRuleManager ruleManager;
    private ItemsWebSocketController webSocketController;

    @Override
    public void init(VmsExtensionContext context) {
        // 1. Teach the host's ObjectMapper how to read our polymorphic rule options. Without this
        //    the rule wizard cannot save a rule of our type. The mapper is shared with the core, so
        //    register only deserializers for your own classes.
        context.getObjectMapper().registerModule(new SimpleModule()
                .addDeserializer(SampleTriggerSettings.class, new SampleTriggerSettingsDeserializer()));

        // 2. Database schema: create/upgrade our tables in the shared VMS database.
        boolean ok = context.getDatabaseConnector().checkStructure(DB_CHANGELOG_FILE, getClass().getClassLoader());
        if (!ok) {
            throw new IllegalStateException("Cannot check/update database structure of " + PLUGIN_NAME);
        }

        // 3. Dependency injection. HostServicesModule exposes the host services from the context
        //    (Javalin, DSLContext, UsersManager, RuleManager, Cleaner, ...) as Guice bindings;
        //    PluginModule contains our own bindings. Eager singletons register HTTP routes,
        //    WebSocket routes and event listeners while the injector is created.
        Injector injector = Guice.createInjector(new HostServicesModule(context, this))
                .createChildInjector(new PluginModule(context));

        // Keep every instance terminate() needs before registering anything: if a later step
        // throws, the host calls terminate() and it must still find them.
        permissionsManager = injector.getInstance(SamplePermissionsManager.class);
        ruleManager = injector.getInstance(SampleRuleManager.class);
        webSocketController = injector.getInstance(ItemsWebSocketController.class);

        // 4. Register permissions so they appear in Settings > Roles and can be used as route roles.
        permissionsManager.register();

        // 5. Register the rule type. Because it also implements VmsNotificationType, the host
        //    registers the notification type at the same time, and our events become available both
        //    as alarms and as notifications.
        ruleManager.registerRules();

        // 6. Retention: the host deletes old items with the retention time the operator set for
        //    "plugins data" in Settings. There is no "remove task" call, so a restart inside one core
        //    run adds the same task again; deleting the same rows twice is harmless.
        context.getCleaner().addDatabaseCleaningTask(CleaningCategories.PLUGINS_DATA,
                ItemsRepository.TABLE_NAME, ItemsRepository.CREATED_AT_COLUMN);

        log.info("{} plugin initialized", PLUGIN_NAME);
    }

    @Override
    public void terminate() {
        // Reverse order of init(). Anything left registered here keeps the plugin classloader alive
        // and makes the host fail on data it can no longer deserialize.
        if (webSocketController != null) {
            webSocketController.terminate();
        }
        if (ruleManager != null) {
            ruleManager.unregisterRules();
        }
        if (permissionsManager != null) {
            permissionsManager.unregister();
        }
        log.info("{} plugin terminated", PLUGIN_NAME);
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    /**
     * {@code null} = "service plugin" (no video analytics module). Return an
     * {@link AnalyticsModule} class only for analytics plugins backed by a VEZHA analytics server.
     */
    @Override
    public Class<? extends AnalyticsModule> getModuleClass() {
        return null;
    }

    @Override
    public List<Table<?>> getManagedTables() {
        return MANAGED_TABLES;
    }
}
