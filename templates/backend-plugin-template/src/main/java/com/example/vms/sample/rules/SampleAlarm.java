package com.example.vms.sample.rules;

import com.incoresoft.middleware.vms.rules.Alarm;
import com.incoresoft.middleware.vms.rules.RuleMessage;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * One alarm occurrence handed to the host.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code sourceId} — the object the alarm belongs to, taken from {@link SampleRuleType}'s
 *       source catalog. The host drops the alarm when the rule was scoped ("Where" step) to other
 *       sources. {@code null} is allowed only for triggers that declare
 *       {@link SampleRuleType#requiresSources} false.</li>
 *   <li>{@code serverId} — the recording server the alarm relates to: the one behind the item's
 *       camera, or null when the category has no camera.</li>
 *   <li>{@code allowedUserIds} — who may see the alarm. It must agree with
 *       {@code RuleSource.isAllowed}, otherwise an alarm can arrive live and then disappear from
 *       the list after a reload.</li>
 *   <li>{@code timestamp} — when the event happened, not when the alarm was built.</li>
 * </ul>
 */
public record SampleAlarm(
        UUID sourceId,
        UUID serverId,
        RuleMessage message,
        Set<Integer> allowedUserIds,
        Instant timestamp
) implements Alarm {

    @Override
    public UUID getSourceId() {
        return sourceId;
    }

    @Override
    public UUID getServerId() {
        return serverId;
    }

    @Override
    public RuleMessage getMessage() {
        return message;
    }

    @Override
    public Set<Integer> getAllowedUserIds() {
        return allowedUserIds;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
}
