<!--
  Slot `client_search_info`. Details of the selected result on the Search page.

  The host passes the row as `item` and two callbacks: `setSearchDetailsHeaderInfo` writes the line
  above the player, `setSearchPlayerSettings` tells the player which camera and moment to show
  (null hides it). The item remembers its camera, so the archive around its creation is played.
-->
<template>
  <div class="sample-search-info mt-10" :data-el-name="`${TESTING_ID}-search-info`">
    <div class="sample-search-info__title">
      <VPluginIcon class="sample-search-info__title-icon" />
      <span>{{ $t(PLUGIN_TITLE_KEY) }}</span>
    </div>

    <div class="sample-search-info__grid mt-15">
      <div v-for="row in rows" :key="row.title">
        <div class="sample-search-info__label">{{ row.title }}:</div>
        <div class="sample-search-info__value" :data-el-meta="row.value || '-'">{{ row.value || '-' }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'

import { formatAsUserDate } from 'core/formatters/time'
import { useDevicesStore } from 'core/stores/useDevicesStore'

import VPluginIcon from '@/components/VPluginIcon.vue'
import type { ISearchPlayerSettings, ISearchResult } from '@/models/search'
import { PLUGIN_TITLE_KEY, TESTING_ID } from '@/nameset'

/** How far around the moment the player may be moved. */
const PLAYBACK_WINDOW_MS = 5000

const props = defineProps({
  item: { type: Object as PropType<ISearchResult>, required: true },
  setSearchPlayerSettings: {
    type: Function as PropType<(settings: ISearchPlayerSettings | null) => void>,
    required: true,
  },
  setSearchDetailsHeaderInfo: {
    type: Function as PropType<(title: string, text: string) => void>,
    required: true,
  },
})

const { t } = useI18n()
const devicesStore = useDevicesStore()

const camera = computed(
  () => devicesStore.camerasList.find((entry) => entry.id === props.item.camera_id) ?? null,
)

const rows = computed(() => [
  { title: t('SAMPLE_TEXT'), value: props.item.text },
  { title: t('SAMPLE_CATEGORY'), value: props.item.category_name },
  { title: t('CAMERA'), value: camera.value?.name },
  { title: t('TIME'), value: formatAsUserDate(props.item.created_at) },
  { title: 'ID', value: props.item.id },
])

const updatePlayer = () => {
  props.setSearchDetailsHeaderInfo(t('CAMERA'), camera.value?.name ?? t('NONE'))
  props.setSearchPlayerSettings(
    camera.value
      ? {
          camera: camera.value,
          startTime: props.item.created_at - PLAYBACK_WINDOW_MS,
          endTime: props.item.created_at + PLAYBACK_WINDOW_MS,
        }
      : null,
  )
}

watch(() => props.item, updatePlayer)
onMounted(updatePlayer)
</script>

<style scoped lang="scss">
// Same look as the Search details of the other VMS plugins.
.sample-search-info {
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

    :deep(path) {
      fill: var(--text-color-0);
    }
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
