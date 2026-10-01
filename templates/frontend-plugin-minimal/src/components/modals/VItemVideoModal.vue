<!--
  Archive video of an item: a frame from the moment the item was created next to the host's
  player positioned at that moment. Both are host components; the plugin only passes the camera
  and the time.
-->
<template>
  <ISModal :width="900" :name="ITEM_VIDEO_MODAL" :dataElName="`${TESTING_ID}-item-video`" @beforeOpen="onBeforeOpen">
    <template #title>{{ $t('VIDEO') }} - {{ item?.text }}</template>

    <template #content>
      <div v-if="camera && item" class="sample-item-video">
        <VCameraThumbnail class="sample-item-video__part" :cameraId="camera.id" :timestamp="item.created_at" />
        <div class="sample-item-video__part">
          <AppPlayer
            :key="item.id"
            isPlayback
            :camera="camera"
            :currentTime="item.created_at"
            :event="playbackEvent"
          />
        </div>
      </div>
      <div v-else class="sample-item-video__empty">{{ $t('SAMPLE_NO_CAMERA') }}</div>
    </template>
  </ISModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

import { ISModal } from '@incoresoft/incoresoft-ui'
import AppPlayer from 'core/components/AppPlayer'
import VCameraThumbnail from 'core/components/VCameraThumbnail'
import { useDevicesStore } from 'core/stores/useDevicesStore'

import type { IItem } from '@/api/items'
import { ITEM_VIDEO_MODAL, TESTING_ID } from '@/nameset'

interface IModalParams {
  item: IItem
}

/** How far around the moment the player may be moved. */
const PLAYBACK_WINDOW_MS = 5000

const devicesStore = useDevicesStore()

const item = ref<IItem | null>(null)

// The camera the item remembers, if the user may still see it.
const camera = computed(() =>
  devicesStore.camerasList.find((entry) => entry.id === item.value?.camera_id) ?? null,
)

const playbackEvent = computed(() => ({
  start: (item.value?.created_at ?? 0) - PLAYBACK_WINDOW_MS,
  end: (item.value?.created_at ?? 0) + PLAYBACK_WINDOW_MS,
}))

// Parameters of modalService.show arrive here, not as props.
const onBeforeOpen = (event: { ref: { params: { value?: IModalParams } } }) => {
  item.value = event?.ref?.params?.value?.item ?? null
}
</script>

<style scoped lang="scss">
// Same layout as the alarm preview of the other VMS plugins: frame on the left, player on the right.
.sample-item-video {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  height: 300px;

  &__part {
    height: 100%;
    min-width: 0;
    overflow: hidden;
    border-radius: 4px;

    :deep(> *) {
      height: 100%;
    }
  }

  &__empty {
    padding: 20px 0;
    text-align: center;
    color: var(--text-color-30);
  }
}
</style>
