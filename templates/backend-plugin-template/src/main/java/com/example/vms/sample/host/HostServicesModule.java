package com.example.vms.sample.host;

import com.google.inject.AbstractModule;
import com.incoresoft.middleware.cleaner.Cleaner;
import com.incoresoft.middleware.database.DatabaseConnector;
import com.incoresoft.middleware.model.stream.StreamsManager;
import com.incoresoft.middleware.model.user.UsersManager;
import com.incoresoft.middleware.settings.GlobalSettings;
import com.incoresoft.middleware.storage.Storage;
import com.incoresoft.middleware.vms.mapplans.MapPlanEventManager;
import com.incoresoft.middleware.vms.notifications.VmsNotificationManager;
import com.incoresoft.middleware.vms.plugin.VmsExtension;
import com.incoresoft.middleware.vms.plugin.VmsExtensionContext;
import com.incoresoft.middleware.vms.roles.VmsPluginRolesManager;
import com.incoresoft.middleware.vms.rules.RuleManager;
import com.incoresoft.vms.drivers.sdk.DeviceManager;
import io.javalin.Javalin;
import org.jooq.DSLContext;

/**
 * Exposes the host services from {@link VmsExtensionContext} as Guice bindings, so plugin classes
 * can simply {@code @Inject} them. Everything here comes from the public {@code middleware} API;
 * add a line for any other context getter you need.
 */
public class HostServicesModule extends AbstractModule {
    private final VmsExtensionContext context;
    private final VmsExtension extension;

    public HostServicesModule(VmsExtensionContext context, VmsExtension extension) {
        this.context = context;
        this.extension = extension;
    }

    @Override
    protected void configure() {
        bind(VmsExtension.class).toInstance(extension);
        bind(Javalin.class).toInstance(context.getJavalin());
        bind(DSLContext.class).toInstance(context.getDSLContext());
        bind(DatabaseConnector.class).toInstance(context.getDatabaseConnector());
        bind(UsersManager.class).toInstance(context.getUsersManager());
        bind(VmsPluginRolesManager.class).toInstance(context.getRolesManager());
        bind(RuleManager.class).toInstance(context.getRuleManager());
        bind(VmsNotificationManager.class).toInstance(context.getNotificationsManager());
        bind(MapPlanEventManager.class).toInstance(context.getMapPlanEventManager());
        bind(Cleaner.class).toInstance(context.getCleaner());
        bind(GlobalSettings.class).toInstance(context.getGlobalSettings());
        bind(StreamsManager.class).toInstance(context.getStreamsManager());
        bind(Storage.class).toInstance(context.getStorage());
        bind(DeviceManager.class).toInstance(context.getDeviceManager());
    }
}
