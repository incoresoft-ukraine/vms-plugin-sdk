package com.example.vms.sample.rules;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;

/** Fires only when the item's text contains the configured word (case-insensitive). */
public final class ContainsWordTriggerSettings implements SampleTriggerSettings {
    /** The host validates rule options when the rule is saved: a blank word is rejected (NOT_BLANK, field options.word). */
    @NotBlank
    private final String word;

    @JsonCreator
    public ContainsWordTriggerSettings(@JsonProperty("word") String word) {
        this.word = word;
    }

    @JsonProperty("word")
    public String getWord() {
        return word;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.CONTAINS_WORD;
    }
}
