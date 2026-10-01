<!--
  Slot `alarms_details_message`. The "Message" block of the alarm details modal, where there is room
  for the full payload.

  The host passes the alarm as `event` and listens for `closeModalEvent`, which lets the component
  navigate away and close the modal behind it. The notification details modal renders the same
  component for our notifications.
-->
<template>
  <div v-if="item" class="sample-alarm-message" :data-el-name="`${TESTING_ID}-alarm-details-message`">
    <div v-tooltip.ellipsis="item.text" class="sample-alarm-message__item">
      {{ $t('SAMPLE_TEXT') }} - {{ item.text }}
    </div>
    <div class="sample-alarm-message__item">
      {{ $t('SAMPLE_CATEGORY') }} - {{ item.category_name }}
    </div>
    <div class="sample-alarm-message__item">
      <span class="sample-alarm-message__link cursor-pointer" @click="openItems">
        {{ $t('SAMPLE_OPEN_PAGE') }}
      </span>
    </div>
  </div>
  <div v-else class="sample-alarm-message">{{ $t('SAMPLE_ITEM_MISSING') }}</div>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue'
import { useRouter } from 'vue-router'


import { PLUGIN_ID, TESTING_ID } from '@/nameset'
import type { IAlarm } from '@/models/alarms'

const props = defineProps({
  event: {
    type: Object as PropType<IAlarm>,
    required: true,
  },
})

const emit = defineEmits(['closeModalEvent'])

const router = useRouter()

const item = computed(() => props.event?.message?.item)

const openItems = () => {
  emit('closeModalEvent')
  router.push(`/${PLUGIN_ID}/items`)
}
</script>

<style scoped lang="scss">
// Same look as the alarm details of the other VMS plugins. The block sits in
// a flex column of the modal, so it needs its own width and overflow to clip long text.
.sample-alarm-message {
  width: 100%;
  overflow: hidden;
  font-size: 14px;
  color: var(--text-color-30);

  &__item {
    display: -webkit-box;
    margin-bottom: 5px;
    overflow: hidden;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
  }

  &__link {
    color: var(--success-color);
  }
}
</style>
