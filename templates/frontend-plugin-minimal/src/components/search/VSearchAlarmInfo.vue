<!--
  Slot `client_search_alarm_info`. Details of one of our alarms in Search > Alarms. Without it the
  panel stays empty for this plugin's alarms (settings.json has alarms_available: true).

  The host passes the alarm as `item` and two callbacks: one sets the header above the player, the
  other the video to play. A category has no camera, so there is no video.
-->
<template>
  <div class="sample-search-alarm-info mt-15" :data-el-name="`${TESTING_ID}-search-alarm-info`">
    <div class="sample-search-alarm-info__title">
      <ISIconAlarmLight class="sample-search-alarm-info__title-icon" />
      <span>{{ title }}</span>
    </div>

    <div class="sample-search-alarm-info__content mt-15">
      <div class="sample-search-alarm-info__grid">
        <div v-for="row in rows" :key="row.title">
          <div class="sample-search-alarm-info__label">{{ row.title }}:</div>
          <div class="sample-search-alarm-info__value" :data-el-meta="row.value || '-'">
            {{ row.value || '-' }}
          </div>
        </div>
      </div>

      <div class="mt-15">
        <div class="sample-search-alarm-info__label">{{ $t('MESSAGE') }}:</div>
        <VAlarmsDetailsMessage class="sample-search-alarm-info__value" :event="item" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'

import { ISIconAlarmLight } from '@incoresoft/incoresoft-icons'
import { formatAsUserDate } from 'core/formatters/time'
import { useUserStore } from 'core/stores/useUserStore'

import VAlarmsDetailsMessage from '@/components/alarms/VAlarmsDetailsMessage.vue'
import type { IAlarm } from '@/models/alarms'
import { PLUGIN_TITLE_KEY, TESTING_ID } from '@/nameset'

const props = defineProps({
  item: {
    type: Object as PropType<IAlarm>,
    required: true,
  },
  setSearchPlayerSettings: {
    type: Function as PropType<(settings: unknown) => void>,
    required: true,
  },
  setSearchDetailsHeaderInfo: {
    type: Function as PropType<(title: string, text: string) => void>,
    required: true,
  },
})

const { t } = useI18n()
const userStore = useUserStore()

const title = computed(() => {
  const trigger = props.item?.options?.trigger
  return trigger ? `${t(PLUGIN_TITLE_KEY)} (${t(trigger.toUpperCase())})` : t(PLUGIN_TITLE_KEY)
})

const rows = computed(() => [
  { title: t('SAMPLE_CATEGORY'), value: props.item?.message?.item?.category_name },
  { title: t('TIME'), value: props.item?.timestamp ? formatAsUserDate(Number(props.item.timestamp)) : '' },
  { title: t('ALARM_ID'), value: props.item?.id },
  { title: t('RULE'), value: props.item?.rule_name },
  {
    title: t('ASSIGNED_TO'),
    value: userStore.usersList.find((user) => user.id === props.item?.owner_id)?.fullname,
  },
  { title: t('STATE'), value: props.item?.state ? t(props.item.state.toUpperCase()) : '' },
  {
    title: t('PRIORITY'),
    value: props.item?.priority_level ? t(props.item.priority_level.toUpperCase()) : '',
  },
])

// The header above the player names the source of the alarm; with no camera there is no video.
const updatePlayer = () => {
  props.setSearchDetailsHeaderInfo(t('SAMPLE_CATEGORY'), props.item?.message?.item?.category_name ?? '-')
  props.setSearchPlayerSettings(null)
}

watch(() => props.item, updatePlayer)
onMounted(updatePlayer)
</script>

<style scoped lang="scss">
// Same look as the Search alarm details of the other VMS plugins.
.sample-search-alarm-info {
  &__title {
    display: flex;
    align-items: center;

    span {
      margin-left: 5px;
      font-size: 16px;
      color: var(--text-color-0);
    }
  }

  &__title-icon {
    height: 15px;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 15px;
  }

  &__label,
  &__value {
    font-size: 14px;
    font-weight: 300;
    line-height: 18px;
  }

  &__label {
    margin-bottom: 8px;
    color: var(--text-color-30);
  }

  &__value {
    color: var(--text-color-0);
  }
}
</style>
