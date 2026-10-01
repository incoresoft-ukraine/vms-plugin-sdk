package com.example.vms.sample.rules;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.incoresoft.middleware.vms.rules.RuleOptions;

/**
 * "What" step of a rule of our type: the JSON the admin UI stores in {@code rule.options}.
 *
 * <p>It is polymorphic — the concrete class is chosen by the {@code trigger} field in
 * {@link SampleTriggerSettingsDeserializer}, which the plugin registers on the host's ObjectMapper
 * during {@code init()}. This is the standard pattern for rule options in VMS plugins.
 */
public interface SampleTriggerSettings extends RuleOptions {
    String TRIGGER_JSON_NAME = "trigger";

    @JsonProperty(TRIGGER_JSON_NAME)
    Trigger getTrigger();
}
