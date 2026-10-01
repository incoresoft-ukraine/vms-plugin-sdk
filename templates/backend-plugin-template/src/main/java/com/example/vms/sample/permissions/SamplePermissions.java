package com.example.vms.sample.permissions;

import com.incoresoft.middleware.model.permission.Permission;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Permissions of the plugin.
 *
 * <p>{@code id} is what roles store and what the frontend checks
 * ({@code usePermissionStore().hasPluginPermission('ViewSampleItems')}, {@code settings.json →
 * routes_permissions / client_permissions}). {@code localeKey} is translated by the frontend
 * (add it to your plugin locales). {@code required} lists permissions that must be granted
 * together with this one (e.g. "Manage" requires "View").
 *
 * <p>Permission ids are global across the core and all plugins: keep your plugin name in them.
 *
 * <p>Because {@link Permission} extends Javalin {@code RouteRole}, an enum constant can be passed
 * directly as a route role: {@code get(path, handler, LOGGED_IN, API_CALL, VIEW_SAMPLE_ITEMS)}.
 */
public enum SamplePermissions implements Permission {
    VIEW_SAMPLE_ITEMS("ViewSampleItems", "PERMISSION_VIEW_SAMPLE_ITEMS"),
    MANAGE_SAMPLE_ITEMS("ManageSampleItems", "PERMISSION_MANAGE_SAMPLE_ITEMS", VIEW_SAMPLE_ITEMS),
    /** Settings of the plugin (categories), separate from the operators' work with items. */
    MANAGE_SAMPLE_CATEGORIES("ManageSampleCategories", "PERMISSION_MANAGE_SAMPLE_CATEGORIES", VIEW_SAMPLE_ITEMS);

    private final String id;
    private final String localeKey;
    private final Set<SamplePermissions> required;

    SamplePermissions(String id, String localeKey, SamplePermissions... required) {
        this.id = id;
        this.localeKey = localeKey;
        this.required = Set.of(required);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getLocaleKey() {
        return localeKey;
    }

    @Override
    public Set<String> getRequired() {
        return required.stream().map(SamplePermissions::getId).collect(Collectors.toSet());
    }
}
