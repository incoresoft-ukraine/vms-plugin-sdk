package com.example.vms.sample.rules;

/** Fires for every new item. */
public final class AnyItemTriggerSettings implements SampleTriggerSettings {
    @Override
    public Trigger getTrigger() {
        return Trigger.ANY_ITEM;
    }
}
