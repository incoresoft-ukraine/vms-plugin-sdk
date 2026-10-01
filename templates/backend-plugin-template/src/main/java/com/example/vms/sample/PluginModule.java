package com.example.vms.sample;

import com.example.vms.sample.core.ItemEventBus;
import com.example.vms.sample.db.CategoriesRepository;
import com.example.vms.sample.db.ItemsRepository;
import com.example.vms.sample.http.ItemsController;
import com.example.vms.sample.http.CategoriesController;
import com.example.vms.sample.http.FrontendResources;
import com.example.vms.sample.permissions.SamplePermissionsManager;
import com.example.vms.sample.rules.SampleRuleListener;
import com.example.vms.sample.rules.SampleRuleManager;
import com.example.vms.sample.ws.ItemsWebSocketController;
import com.google.inject.AbstractModule;
import com.incoresoft.middleware.vms.plugin.VmsExtensionContext;

/**
 * Guice bindings of the plugin. Controllers are eager singletons: their constructors (or
 * {@code @Inject init(Javalin)} methods) register routes on the shared host Javalin instance.
 *
 * <p>Two ordering rules worth remembering:
 * <ul>
 *   <li>Register specific API controllers BEFORE any controller that mounts a wildcard under the
 *       same prefix; Javalin matches routes in registration order.</li>
 *   <li>Listeners that subscribe to the event bus must be eager singletons too, otherwise nothing
 *       ever constructs them and the events go nowhere.</li>
 * </ul>
 */
public class PluginModule extends AbstractModule {
    /** Kept for bindings that need host services directly, e.g. {@code bind(X.class).toInstance(context.getDeviceManager())}. */
    private final VmsExtensionContext context;

    public PluginModule(VmsExtensionContext context) {
        this.context = context;
    }

    @Override
    protected void configure() {
        bind(SamplePermissionsManager.class).asEagerSingleton();
        bind(CategoriesRepository.class).asEagerSingleton();
        bind(ItemsRepository.class).asEagerSingleton();
        bind(ItemEventBus.class).asEagerSingleton();

        // Rules and notifications.
        bind(SampleRuleManager.class).asEagerSingleton();
        bind(SampleRuleListener.class).asEagerSingleton();

        // HTTP and WebSocket.
        bind(CategoriesController.class).asEagerSingleton();
        bind(ItemsController.class).asEagerSingleton();
        bind(ItemsWebSocketController.class).asEagerSingleton();
        // Serves the frontend bundle at /resources/sample_plugin/* (host contract, see the class).
        bind(FrontendResources.class).asEagerSingleton();
    }
}
