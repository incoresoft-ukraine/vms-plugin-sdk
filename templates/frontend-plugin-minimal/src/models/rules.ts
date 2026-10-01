/**
 * Rule options of this plugin — the JSON stored in `rule.options` and read on the backend by
 * SampleTriggerSettingsDeserializer. Both sides must agree on the `trigger` discriminator and on
 * the field names, so keep this file and the Java trigger classes in step.
 */
/*
 * The host translates a trigger as t(trigger.toUpperCase()) in a namespace shared with every other
 * plugin, so the values carry the plugin prefix: SAMPLE_ANY_ITEM, SAMPLE_CONTAINS_WORD.
 */
export enum ETrigger {
  ANY_ITEM = 'sample_any_item',
  CONTAINS_WORD = 'sample_contains_word',
}

export interface ISampleTriggerOptions {
  trigger?: ETrigger
  /** Only for CONTAINS_WORD. */
  word?: string
}

/** The three states the rule wizard renders its blocks in. */
export enum EViewMode {
  CREATE = 'create',
  EDIT = 'edit',
  DETAILS = 'details',
}
