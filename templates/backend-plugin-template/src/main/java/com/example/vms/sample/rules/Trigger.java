package com.example.vms.sample.rules;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Discriminator of {@link SampleTriggerSettings}; {@link #wireName()} travels as JSON.
 *
 * <p>The host UI translates a trigger as {@code t(trigger.toUpperCase())}, in one namespace shared
 * with every other plugin. The plugin prefix in the wire name keeps our translation keys apart
 * ({@code SAMPLE_ANY_ITEM}, {@code SAMPLE_CONTAINS_WORD}).
 */
public enum Trigger {
    ANY_ITEM("sample_any_item"),
    CONTAINS_WORD("sample_contains_word");

    private final String wireName;

    Trigger(String wireName) {
        this.wireName = wireName;
    }

    @JsonValue
    public String wireName() {
        return wireName;
    }

    public static Optional<Trigger> fromWireName(String value) {
        return Arrays.stream(values())
                .filter(trigger -> trigger.wireName.equals(value == null ? null : value.toLowerCase(Locale.ROOT)))
                .findFirst();
    }

    @Override
    public String toString() {
        return wireName;
    }
}
