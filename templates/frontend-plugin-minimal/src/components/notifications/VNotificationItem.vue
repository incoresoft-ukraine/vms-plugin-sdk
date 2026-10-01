<!--
  Slot `plugin_notification_item`. The body of one notification card in the notification bar, shown
  when a rule of our type has "notification" among its actions.

  The host passes the notification as `item`, hides its own header while this component is present,
  and listens for `removeNotification` (with the notification id) and `setSystemMessage`.
-->
<template>
  <!-- The host draws the card and its close button; this is the content only. -->
  <div class="sample-notification" :data-el-name="`${TESTING_ID}-notification`">
    <div class="sample-notification__header">
      <div class="sample-notification__name"><VPluginIcon />{{ $t(PLUGIN_TITLE_KEY) }}</div>
      <div class="sample-notification__date" :data-el-name="`${TESTING_ID}-notification-date`">
        {{ formatAsUserDate(timestamp) }}
      </div>
    </div>

    <div v-if="payload" class="sample-notification__info">
      <div class="sample-notification__row">
        <div class="sample-notification__col">{{ $t('SAMPLE_TEXT') }}:</div>
        <div v-tooltip.ellipsis="payload.text" class="sample-notification__col">{{ payload.text }}</div>
      </div>
      <div class="sample-notification__row">
        <div class="sample-notification__col">{{ $t('SAMPLE_CATEGORY') }}:</div>
        <div v-tooltip.ellipsis="payload.category_name" class="sample-notification__col">
          {{ payload.category_name }}
        </div>
      </div>
    </div>
    <div v-else class="sample-notification__info">{{ $t('SAMPLE_ITEM_MISSING') }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'

import { formatAsUserDate } from 'core/formatters/time'

import VPluginIcon from '@/components/VPluginIcon.vue'
import { PLUGIN_TITLE_KEY, TESTING_ID } from '@/nameset'
import type { INotification } from '@/models/alarms'

const props = defineProps({
  item: {
    type: Object as PropType<INotification>,
    required: true,
  },
})

// removeNotification is emitted by the host's own close button; we only need setSystemMessage.
const emit = defineEmits(['removeNotification', 'setSystemMessage'])

const { t } = useI18n()

const payload = computed(() => props.item?.message?.item)

/** Prefer the moment the item was created; fall back to when the notification was stored. */
const timestamp = computed(() => payload.value?.created_at ?? props.item?.created_at)

// The host shows the desktop (system) notification only when the plugin asks for it; `is_desktop`
// says whether the rule wants one.
onMounted(() => {
  emit('setSystemMessage', {
    is_desktop: props.item.is_desktop,
    type: props.item.type,
    title: t(PLUGIN_TITLE_KEY),
    body: payload.value ? `${payload.value.category_name}: ${payload.value.text}` : '',
    image: '',
  })
})
</script>

<style scoped lang="scss">
// Same look as the notifications of the other VMS plugins.
.sample-notification {
  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: -1px;
    margin-bottom: 9px;
  }

  &__name {
    display: flex;
    align-items: center;
    gap: 3px;
    font-size: 14px;
    line-height: 19px;
    color: var(--text-color-20);

    svg {
      width: 14px;
      height: 14px;
    }
  }

  // Room on the right for the host's close button.
  &__date {
    padding-right: 16px;
    font-size: 12px;
    line-height: 14px;
    color: var(--text-color-20);
  }

  &__info {
    display: flex;
    flex-direction: column;
    margin-top: 3px;
    font-size: 14px;
    color: var(--text-color-20);
  }

  &__row {
    display: flex;
    gap: 14px;
  }

  &__col {
    min-width: 80px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
}
</style>
