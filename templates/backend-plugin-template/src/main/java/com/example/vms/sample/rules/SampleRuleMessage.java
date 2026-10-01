package com.example.vms.sample.rules;

import com.example.vms.sample.dto.ItemDTO;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.incoresoft.middleware.vms.notifications.VmsNotificationMessage;
import com.incoresoft.middleware.vms.rules.RuleMessage;

/**
 * Payload of the alarm and of the notification. It ends up in {@code alarm.message} on the wire,
 * and the frontend slot components (`alarms_message_column`, `alarms_details_message`,
 * `plugin_notification_item`) read exactly these fields.
 */
public class SampleRuleMessage implements RuleMessage, VmsNotificationMessage {
    private final ItemDTO item;

    @JsonCreator
    public SampleRuleMessage(@JsonProperty("item") ItemDTO item) {
        this.item = item;
    }

    @JsonProperty("item")
    public ItemDTO getItem() {
        return item;
    }
}
