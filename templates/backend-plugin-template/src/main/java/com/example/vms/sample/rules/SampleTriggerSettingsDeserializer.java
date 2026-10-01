package com.example.vms.sample.rules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Picks the concrete options class by the {@code trigger} field.
 *
 * <p>Important detail of the host's rule wizard: on the first step it sends the options object
 * <em>before</em> the user picked a trigger. A missing or unknown discriminator is therefore an
 * expected case — return {@code null} instead of throwing, otherwise the admin UI gets a 500.
 */
public class SampleTriggerSettingsDeserializer extends JsonDeserializer<SampleTriggerSettings> {
    private static final Logger log = LoggerFactory.getLogger(SampleTriggerSettingsDeserializer.class);

    @Override
    public SampleTriggerSettings deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        TreeNode node = parser.getCodec().readTree(parser);
        TreeNode triggerNode = node.get(SampleTriggerSettings.TRIGGER_JSON_NAME);

        if (triggerNode == null || triggerNode.isMissingNode()) {
            return null;
        }

        String value = triggerNode.toString().replace("\"", "");
        Trigger trigger = Trigger.fromWireName(value).orElse(null);
        if (trigger == null) {
            log.warn("Unknown trigger '{}', treating the rule options as incomplete", value);
            return null;
        }

        Class<? extends SampleTriggerSettings> target = switch (trigger) {
            case ANY_ITEM -> AnyItemTriggerSettings.class;
            case CONTAINS_WORD -> ContainsWordTriggerSettings.class;
        };
        return parser.getCodec().treeToValue(node, target);
    }
}
