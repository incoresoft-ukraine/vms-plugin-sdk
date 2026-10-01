<!--
  Slot `alarms_details_preview`. The video block of the alarm details modal.

  The host shows its own player only for its own alarm kinds; a plugin alarm gets this component
  instead, with the alarm as `event`. `event.camera_id` is filled in by the host from the camera of
  the alarm's source (RuleSource.getCameraId on the backend), so the plugin does not resolve it.
-->
<template>
  <div v-if="camera" class="sample-alarm-preview" :data-el-name="`${TESTING_ID}-alarm-preview`">
    <VCameraThumbnail class="sample-alarm-preview__part" :cameraId="camera.id" :timestamp="event.timestamp" />
    <div class="sample-alarm-preview__part">
      <AppPlayer
        :key="event.id"
        isPlayback
        :camera="camera"
        :currentTime="event.timestamp"
        :event="playbackEvent"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue'

import AppPlayer from 'core/components/AppPlayer'
import VCameraThumbnail from 'core/components/VCameraThumbnail'
import { useDevicesStore } from 'core/stores/useDevicesStore'

import type { IAlarm } from '@/models/alarms'
import { TESTING_ID } from '@/nameset'

/** How far around the alarm moment the player may be moved. */
const PLAYBACK_WINDOW_MS = 5000

const props = defineProps({
  event: {
    type: Object as PropType<IAlarm>,
    required: true,
  },
})

const devicesStore = useDevicesStore()

// Nothing is rendered when the alarm has no camera or the user may not see it.
const camera = computed(() =>
  devicesStore.camerasList.find((entry) => entry.id === props.event?.camera_id) ?? null,
)

const playbackEvent = computed(() => ({
  start: props.event.timestamp - PLAYBACK_WINDOW_MS,
  end: props.event.timestamp + PLAYBACK_WINDOW_MS,
}))
</script>

<style scoped lang="scss">
// Same layout as the alarm preview of the other VMS plugins: frame on the left, player on the right.
.sample-alarm-preview {
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
}
</style>
