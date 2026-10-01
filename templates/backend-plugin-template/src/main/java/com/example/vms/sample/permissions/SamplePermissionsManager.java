package com.example.vms.sample.permissions;

import com.example.vms.sample.PluginExtension;
import com.google.inject.Inject;
import com.incoresoft.middleware.model.permission.PluginPermissions;
import com.incoresoft.middleware.vms.roles.VmsPluginRolesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;

/**
 * Registers the permissions declared in {@link SamplePermissions} with the host role manager, so
 * they appear in Settings &gt; Roles and can be used as route roles. Call {@link #register()} in
 * {@code init()} and {@link #unregister()} in {@code terminate()}.
 */
public class SamplePermissionsManager {
    private static final Logger log = LoggerFactory.getLogger(SamplePermissionsManager.class);

    public static final PluginPermissions PLUGIN_PERMISSIONS = new PluginPermissions(
            Collections.emptyList(),                       // permission groups (optional)
            Arrays.asList(SamplePermissions.values())       // flat permissions
    );

    private final VmsPluginRolesManager rolesManager;

    @Inject
    public SamplePermissionsManager(VmsPluginRolesManager rolesManager) {
        this.rolesManager = rolesManager;
    }

    public void register() {
        rolesManager.register(PluginExtension.PLUGIN_NAME, PLUGIN_PERMISSIONS);
        log.info("Permissions registered for plugin {}", PluginExtension.PLUGIN_NAME);
    }

    public void unregister() {
        rolesManager.unregister(PluginExtension.PLUGIN_NAME, PLUGIN_PERMISSIONS);
        log.info("Permissions cleared for plugin {}", PluginExtension.PLUGIN_NAME);
    }
}
