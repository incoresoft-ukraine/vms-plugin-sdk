<!--
  Slot `admin_events_and_rules_options_block`. The plugin's part of the "What" step in
  Admin Center > Alarm Rules wizard, shown once the operator picks our rule type.

  Contract with the host:
   - `v-model` carries `rule.options`, the exact JSON the backend deserializes;
   - `viewMode` is create / edit / details, and the details mode has to render read-only text;
   - the emits let us hide wizard steps that make no sense for the chosen trigger, and ask the
     "Where" step to drop a selection that a trigger change made meaningless.
-->
<template>
  <template v-if="viewMode !== EViewMode.DETAILS">
    <p class="mt-10 rule-detail__title">{{ $t('PLUGIN_OPTIONS') }}:</p>

    <div class="grid-2 gap-10 mt-10">
      <ISSelectField
        v-model="trigger"
        :label="$t('TRIGGER_TYPE')"
        :options="triggerOptions"
        trackBy="id"
        column="text"
        :cleanable="false"
        :dataElName="`${TESTING_ID}-rule-trigger`"
        :error="validationStore.getErrorForField('options.trigger')"
      />

      <ISTextField
        v-if="trigger === ETrigger.CONTAINS_WORD"
        required
        :label="$t('SAMPLE_WORD')"
        :placeholder="$t('SAMPLE_ENTER_WORD')"
        :modelValue="word"
        :dataElName="`${TESTING_ID}-rule-word`"
        :error="validationStore.getErrorForField('options.word')"
        @update:modelValue="onWordInput"
      />
    </div>
  </template>

  <template v-else>
    <div class="rule-detail__row mt-5">
      <span class="rule-detail__title">{{ $t('TRIGGER_TYPE') }}:</span>
      <span class="rule-detail__value">{{ $t(trigger.toUpperCase()) }}</span>
    </div>
    <div v-if="trigger === ETrigger.CONTAINS_WORD" class="rule-detail__row mt-5">
      <span class="rule-detail__title">{{ $t('SAMPLE_WORD') }}:</span>
      <span class="rule-detail__value">{{ word }}</span>
    </div>
  </template>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'

import { ISSelectField, ISTextField } from '@incoresoft/incoresoft-ui'
import { useValidationErrorStore } from 'styleguide/stores/useValidationErrorStore'

import { ETrigger, EViewMode, type ISampleTriggerOptions } from '@/models/rules'
import { TESTING_ID } from '@/nameset'

const props = defineProps({
  modelValue: {
    type: Object as PropType<ISampleTriggerOptions>,
    required: true,
  },
  viewMode: {
    type: String as PropType<EViewMode>,
    required: true,
  },
})

const emit = defineEmits([
  'update:modelValue',
  'showWhereStep',
  'hideWhereStep',
  'resetSources',
])

const { t } = useI18n()
// Errors of a rejected save (400 with field `options.<name>`) land in this shared store.
const validationStore = useValidationErrorStore()

const triggerOptions = computed(() =>
  Object.values(ETrigger).map((value) => ({ id: value, text: t(value.toUpperCase()) })),
)

const trigger = computed<ETrigger>({
  get: () => props.modelValue?.trigger ?? ETrigger.ANY_ITEM,
  set: (value) => {
    // Switching the trigger drops the fields of the previous one; otherwise stale keys travel to
    // the backend inside options and the deserializer builds the wrong class.
    emit('update:modelValue', { trigger: value })

    // "Contains word" watches the text of every item, so a category scope would silently narrow it.
    // The backend cannot catch that on its own: both triggers accept the same sources.
    if (value === ETrigger.CONTAINS_WORD) {
      emit('resetSources')
    }
  },
})

const word = computed(() => props.modelValue?.word ?? '')

const onWordInput = (value: string) => {
  validationStore.resetFieldError('options.word')
  emit('update:modelValue', { ...props.modelValue, word: value })
}

/** The "Where" step picks categories, which only makes sense for the category-scoped trigger. */
const syncWhereStep = () => {
  if (trigger.value === ETrigger.CONTAINS_WORD) {
    emit('hideWhereStep')
  } else {
    emit('showWhereStep')
  }
}
watch(trigger, syncWhereStep)

/**
 * A default that only lives in the getter never reaches the model, so a form that looks filled in
 * would post an empty options object, which the backend cannot type. Write the default out instead.
 */
const ensureTrigger = () => {
  if (!props.modelValue?.trigger) {
    emit('update:modelValue', { trigger: ETrigger.ANY_ITEM })
  }
}

const mounted = ref(false)

onMounted(() => {
  syncWhereStep()
  if (props.viewMode !== EViewMode.DETAILS) {
    ensureTrigger()
  }
  mounted.value = true
})

// Leaving the block (the operator switched to another rule type) must clear our options, otherwise
// they would be sent as the options of a foreign rule type.
onBeforeUnmount(() => {
  if (mounted.value && props.viewMode !== EViewMode.DETAILS) {
    emit('update:modelValue', {})
  }
})
</script>
