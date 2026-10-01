package com.example.vms.sample.rules;

import com.example.vms.sample.PluginExtension;
import com.example.vms.sample.db.CategoriesRepository;
import com.example.vms.sample.dto.CategoryDTO;
import com.example.vms.sample.permissions.SamplePermissions;
import com.incoresoft.middleware.model.user.User;
import com.incoresoft.middleware.vms.notifications.VmsNotificationMessage;
import com.incoresoft.middleware.vms.notifications.VmsNotificationType;
import com.incoresoft.middleware.vms.rules.RuleMessage;
import com.incoresoft.middleware.vms.rules.RuleOptions;
import com.incoresoft.middleware.vms.rules.RuleSource;
import com.incoresoft.middleware.vms.rules.RuleType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Declares "Sample Plugin" as a rule type, so operators can build alarm rules on top of our events
 * in Admin Center &gt; Alarm Rules.
 *
 * <p>Implementing {@link VmsNotificationType} as well is what makes the same events available as
 * desktop notifications: the host registers the notification type automatically when the rule type
 * is registered, and a rule whose action is "notification" then delivers
 * {@link SampleRuleMessage} to the notification panel.
 *
 * <p>The three classes the host needs to deserialize our JSON:
 * <ul>
 *   <li>{@code getRuleDataClass()} — the options the rule wizard stores (our polymorphic
 *       {@link SampleTriggerSettings});</li>
 *   <li>{@code getRuleMessageClass()} — the alarm payload;</li>
 *   <li>{@code getNotificationMessageClass()} — the notification payload.</li>
 * </ul>
 */
public class SampleRuleType implements RuleType, VmsNotificationType {
    private static final long SOURCES_CACHE_MS = 5_000;

    private final CategoriesRepository categories;
    private volatile Collection<RuleSource> sources = List.of();
    private volatile long sourcesLoadedAt;

    public SampleRuleType(CategoriesRepository categories) {
        this.categories = categories;
    }

    /**
     * Identifies the type everywhere: in the rules API, in the alarm records and in the frontend
     * slot maps. Using the plugin name keeps all of them in sync.
     */
    @Override
    public String getTypeId() {
        return PluginExtension.PLUGIN_NAME;
    }

    /** Shown in the rule wizard; the frontend translates it, falling back to the raw string. */
    @Override
    public String getTypeLocaleKey() {
        return "SAMPLE_RULE_TYPE";
    }

    @Override
    public String getPluginName() {
        return PluginExtension.PLUGIN_NAME;
    }

    @Override
    public Class<? extends RuleOptions> getRuleDataClass() {
        return SampleTriggerSettings.class;
    }

    @Override
    public Class<? extends RuleMessage> getRuleMessageClass() {
        return SampleRuleMessage.class;
    }

    @Override
    public Class<? extends VmsNotificationMessage> getNotificationMessageClass() {
        return SampleRuleMessage.class;
    }

    /**
     * The objects a rule of this type can be scoped to: the categories.
     *
     * <p>The host calls this on hot paths: for every alarm and every user it decides who may see
     * the alarm by looking the alarm's source up here. Hence the short cache; category changes
     * drop it at once through {@link #invalidateSources()}.
     */
    @Override
    public Collection<RuleSource> getSources() {
        long now = System.currentTimeMillis();
        if (now - sourcesLoadedAt > SOURCES_CACHE_MS) {
            sources = categories.findAll().stream()
                    .map(CategorySource::new)
                    .map(RuleSource.class::cast)
                    .toList();
            sourcesLoadedAt = now;
        }
        return sources;
    }

    /** Called after a category is created, renamed or deleted. */
    public void invalidateSources() {
        sourcesLoadedAt = 0;
    }

    /**
     * "Contains word" watches the text of every item, whatever its category, so it needs no "Where"
     * step: the host then accepts the rule with no sources selected.
     */
    @Override
    public boolean requiresSources(RuleOptions ruleOptions) {
        return !(ruleOptions instanceof ContainsWordTriggerSettings);
    }

    /**
     * Sources a rule covers by itself, added by the host to the ones chosen in the "Where" step. The
     * host keeps an alarm only if its source is among the rule's sources; for "contains word" that
     * is every category, including the ones created after the rule.
     */
    @Override
    public Set<UUID> getDynamicSources(RuleOptions ruleOptions) {
        if (ruleOptions instanceof ContainsWordTriggerSettings) {
            return getSources().stream().map(RuleSource::getId).collect(Collectors.toSet());
        }
        return Set.of();
    }

    /** One category exposed as a rule source. */
    private record CategorySource(CategoryDTO category) implements RuleSource {
        @Override
        public UUID getId() {
            return UUID.fromString(category.sourceId());
        }

        /** Shown as the source of the alarm. */
        @Override
        public String getName() {
            return category.name();
        }

        /**
         * Decides who may see alarms and notifications coming from this source. The host applies it
         * both to live delivery and to the stored alarm list, so it has to match the recipient list
         * computed in {@link SampleRuleListener}.
         */
        @Override
        public boolean isAllowed(User user) {
            return user.hasPermission(SamplePermissions.VIEW_SAMPLE_ITEMS);
        }

        /**
         * The camera behind the category. The host attaches it to every alarm of this source
         * ({@code alarm.camera_id}), together with the camera's recording server, so the alarm can
         * show video.
         */
        @Override
        public Optional<Integer> getCameraId() {
            return Optional.ofNullable(category.cameraId());
        }
    }
}
