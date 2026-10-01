package com.example.vms.sample.rules;

import com.example.vms.sample.db.CategoriesRepository;
import com.example.vms.sample.core.ItemEvent;
import com.example.vms.sample.core.ItemEventBus;
import com.example.vms.sample.dto.ItemDTO;
import com.example.vms.sample.permissions.SamplePermissions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.incoresoft.middleware.model.user.User;
import com.incoresoft.middleware.model.user.UsersManager;
import com.incoresoft.middleware.vms.rules.Rule;
import com.incoresoft.middleware.vms.rules.RuleManager;
import com.incoresoft.middleware.vms.rules.RuleOptions;
import com.incoresoft.vms.drivers.sdk.Device;
import com.incoresoft.vms.drivers.sdk.DeviceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Bridge between our events and the host rule engine.
 *
 * <p>For every new item it walks the rules of our type, keeps those whose trigger matches, and asks
 * the host to fire them. The host then runs whatever actions the operator configured on the rule:
 * raise an alarm, send a notification, start a recording, and so on. The plugin never decides that
 * itself, it only reports that something happened.
 */
@Singleton
public class SampleRuleListener {
    private static final Logger log = LoggerFactory.getLogger(SampleRuleListener.class);

    private final RuleManager ruleManager;
    private final SampleRuleManager sampleRuleManager;
    private final UsersManager usersManager;
    private final CategoriesRepository categories;
    private final DeviceManager deviceManager;

    @Inject
    public SampleRuleListener(RuleManager ruleManager,
                              SampleRuleManager sampleRuleManager,
                              UsersManager usersManager,
                              CategoriesRepository categories,
                              DeviceManager deviceManager,
                              ItemEventBus eventBus) {
        this.ruleManager = ruleManager;
        this.sampleRuleManager = sampleRuleManager;
        this.usersManager = usersManager;
        this.categories = categories;
        this.deviceManager = deviceManager;

        eventBus.addListener(this::onItemEvent);
    }

    private void onItemEvent(ItemEvent event) {
        if (event.type() != ItemEvent.Type.CREATED) {
            return;
        }
        ItemDTO item = event.item();

        List<Rule> matched = ruleManager.getRulesOfType(sampleRuleManager.getRuleType()).stream()
                .filter(rule -> matches(item, rule.getOptions()))
                .toList();
        if (matched.isEmpty()) {
            return;
        }

        SampleRuleMessage message = new SampleRuleMessage(item);
        Set<Integer> recipients = allowedUserIds();
        // The alarm's source is the item's category: the host shows its name next to the alarm and
        // keeps the alarm only for rules that cover this category (see SampleRuleType).
        UUID sourceId = categories.find(item.categoryId())
                .map(category -> UUID.fromString(category.sourceId()))
                .orElse(null);
        UUID serverId = resolveServerId(item);

        for (Rule rule : matched) {
            try {
                ruleManager.triggerRule(rule,
                        new SampleAlarm(sourceId, serverId, message, recipients,
                                Instant.ofEpochMilli(item.createdAt())));
            } catch (RuntimeException e) {
                log.error("Failed to trigger rule {}", rule.getId(), e);
            }
        }
    }

    /**
     * The recording server of the alarm: the one the item's camera records to. Null when the
     * category has no camera, or the camera is gone; the UI then shows no server.
     *
     * <p>A host lookup can fail on a broken device record. An alarm without a server is still an
     * alarm, so the failure is logged and the alarm goes on.
     */
    private UUID resolveServerId(ItemDTO item) {
        if (item.cameraId() == null) {
            return null;
        }
        try {
            return deviceManager.getDeviceByItemId(item.cameraId())
                    .map(Device::getServerId)
                    .orElse(null);
        } catch (RuntimeException e) {
            log.warn("Cannot resolve the server of camera {}, alarm goes without it", item.cameraId(), e);
            return null;
        }
    }

    /**
     * Trigger matching. Pattern matching on the sealed-in-practice options hierarchy keeps every
     * variant in one readable place; add a case here whenever you add a trigger.
     */
    static boolean matches(ItemDTO item, RuleOptions options) {
        return switch (options) {
            case AnyItemTriggerSettings ignored -> true;
            case ContainsWordTriggerSettings settings -> containsWord(item.text(), settings.getWord());
            case null, default -> false;
        };
    }

    private static boolean containsWord(String text, String word) {
        if (text == null || word == null || word.isBlank()) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(word.toLowerCase(Locale.ROOT));
    }

    /**
     * Who may see the resulting alarm or notification: everyone allowed to view our items.
     *
     * <p>Each user is checked separately on purpose. Resolving one account's roles can fail (for
     * example when it still points at a deleted role), and a single failure must not empty the whole
     * recipient list and hide the alarm from everybody.
     */
    private Set<Integer> allowedUserIds() {
        return usersManager.getAll().stream()
                .filter(this::canViewItems)
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    private boolean canViewItems(User user) {
        try {
            return user.hasPermission(SamplePermissions.VIEW_SAMPLE_ITEMS);
        } catch (RuntimeException e) {
            log.warn("Cannot resolve permissions of user {}, excluding from recipients", user.getId(), e);
            return false;
        }
    }
}
