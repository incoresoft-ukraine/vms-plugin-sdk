<!--
  Slot `alarms_message_column`. One line describing the alarm in the alarms table.

  The host only falls back to its own rendering when this component is absent, so keep it short:
  the cell is narrow and the full story belongs in the details modal.
-->
<template>
  <div v-tooltip.ellipsis="message" class="text-ellipsis" :data-el-name="`${TESTING_ID}-alarm-message`">
    {{ message || '-' }}
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'

import type { IAlarm } from '@/models/alarms'
import { TESTING_ID } from '@/nameset'

const props = defineProps({
  alarm: {
    type: Object as PropType<IAlarm>,
    required: true,
  },
})

const { t } = useI18n()

const message = computed(() => {
  const item = props.alarm?.message?.item
  if (!item) return t('SAMPLE_ITEM_MISSING')
  return `${item.text} (${item.category_name})`
})
</script>
