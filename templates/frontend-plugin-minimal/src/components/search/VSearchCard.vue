<!--
  Slot `client_search_card`. The text part of a result card on the Search page. The host draws the
  card itself with a frame from the camera archive (see models/search.ts) and puts this component
  over it. `app-name-pill` is a host class: the same pill the other result cards use.
-->
<template>
  <div class="sample-search-card" :data-el-name="`${TESTING_ID}-search-card`">
    <div class="app-name-pill">
      <VPluginIcon class="app-name-pill__icon" />
      <span v-tooltip.ellipsis="item.text" class="app-name-pill__title">{{ item.text }}</span>
    </div>
    <div class="app-name-pill sample-search-card__time">
      <span>{{ formatAsUserDate(item.timestamp) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from 'vue'

import { formatAsUserDate } from 'core/formatters/time'

import VPluginIcon from '@/components/VPluginIcon.vue'
import type { ISearchResult } from '@/models/search'
import { TESTING_ID } from '@/nameset'

defineProps({
  item: { type: Object as PropType<ISearchResult>, required: true },
})
</script>

<style scoped lang="scss">
// Same placement as the cards of the other VMS plugins: the title pill top left, the time bottom right.
.sample-search-card {
  width: 100%;

  .app-name-pill {
    z-index: 0;
    max-width: 80%;

    :deep(svg path) {
      fill: var(--text-color-60);
    }

    &__icon {
      width: 14px;
      min-width: 14px;
    }

    &__title {
      margin-left: 5px;
    }
  }

  &__time {
    top: auto;
    right: 5px;
    bottom: 5px;
    left: auto;
    min-height: 16px;
    padding: 2px 5px;
    font-size: 10px;
  }
}
</style>
