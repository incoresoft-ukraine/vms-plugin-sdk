<!--
  Slot `alarms_type_column`. Renders the "Type" cell of an alarm of our plugin, in the alarms list
  and in the alarm details modal. Without it the host prints the raw type id.

  Props given by the host: `alarm` (the whole alarm) and `type` (its type id).
-->
<template>
  <div v-tooltip.ellipsis="label" class="text-ellipsis" :data-el-name="`${TESTING_ID}-alarm-type`">
    {{ label }}
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'

import { PLUGIN_TITLE_KEY, TESTING_ID } from '@/nameset'
import type { IAlarm } from '@/models/alarms'

const props = defineProps({
  alarm: {
    type: Object as PropType<IAlarm>,
    default: () => ({}) as IAlarm,
  },
  /** The alarm type (our plugin name); passed by the details modal. */
  type: {
    type: String,
    default: '',
  },
})

const { t } = useI18n()

/** "Sample Plugin (Contains word)" — the plugin name plus the trigger that fired. */
const label = computed(() => {
  const trigger = props.alarm?.options?.trigger
  const title = t(PLUGIN_TITLE_KEY)
  return trigger ? `${title} (${t(trigger.toUpperCase())})` : title
})
</script>
