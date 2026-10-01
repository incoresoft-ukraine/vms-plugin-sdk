package com.example.vms.sample.host;

import com.incoresoft.middleware.cleaner.CleaningCategory;

/**
 * Retention categories the host knows. The host matches them by {@link #getTypeId()} (the lower-case
 * name) and deletes rows older than the retention the operator set for that category in Settings.
 */
public enum CleaningCategories implements CleaningCategory {
    LOGS,
    ALERTS,
    PLUGINS_DATA;

    @Override
    public String getTypeId() {
        return name().toLowerCase();
    }
}
