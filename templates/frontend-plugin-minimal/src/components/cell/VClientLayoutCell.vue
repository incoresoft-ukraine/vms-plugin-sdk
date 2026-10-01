<!--
  Slot `client_layout_cell`. What the plugin draws inside a layout cell.

  The host wraps it: the name pill with the plugin icon and title, the padding, fullscreen and the
  remove button are already there, so this component only fills the content area. `payload` is
  everything after the colon of the cell content; for this plugin, the category id.
-->
<template>
  <div class="sample-cell" :data-el-name="`${TESTING_ID}-cell`">
    <div v-if="state === ECellState.NO_ACCESS" class="sample-cell__message">
      {{ $t('REQUIRED_PERMISSIONS') }}
    </div>
    <div v-else-if="state === ECellState.MISSING" class="sample-cell__message">
      {{ $t('SAMPLE_CATEGORY_MISSING') }}
    </div>
    <template v-else-if="state === ECellState.READY">
      <div class="sample-cell__title text-ellipsis">{{ categoryName }}</div>
      <div v-if="items.length" class="sample-cell__list">
        <div v-for="item in items" :key="item.id" class="sample-cell__row">
          <span v-tooltip.ellipsis="item.text" class="text-ellipsis">{{ item.text }}</span>
          <span class="sample-cell__time">{{ formatAsUserTime(item.created_at) }}</span>
        </div>
      </div>
      <div v-else class="sample-cell__message">{{ $t('NOTHING_FOUND') }}</div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { formatAsUserTime } from 'core/formatters/time'
import { usePermissionStore } from 'core/stores/usePermissionStore'

import { getCategories } from '@/api/categories'
import { getItems, type IItem } from '@/api/items'
import { ELiveMessageType, useItemsLive, type ILiveMessage } from '@/composables/useItemsLive'
import { PERMISSIONS, TESTING_ID } from '@/nameset'

enum ECellState {
  LOADING = 'loading',
  READY = 'ready',
  NO_ACCESS = 'no_access',
  /** The category was deleted in Admin Center after the cell was placed. */
  MISSING = 'missing',
  /** The request failed; makeRequest has already shown why. */
  FAILED = 'failed',
}

const props = defineProps<{ payload: string }>()

/** A cell is small; keep the newest few. */
const MAX_ITEMS = 20

const permissions = usePermissionStore()

const state = ref(ECellState.LOADING)
const categoryName = ref('')
const items = ref<IItem[]>([])
const categoryId = computed(() => Number(props.payload))

// A layout can be shared with users who may not see this plugin's data: check before loading.
const load = async () => {
  if (!permissions.hasPluginPermission(PERMISSIONS.VIEW_ITEMS)) {
    state.value = ECellState.NO_ACCESS
    return
  }
  try {
    const [categories, page] = await Promise.all([
      getCategories(),
      getItems({ categoryId: categoryId.value, limit: MAX_ITEMS }),
    ])
    const category = categories.find((entry) => entry.id === categoryId.value)
    if (!category) {
      state.value = ECellState.MISSING
      return
    }
    categoryName.value = category.name
    items.value = page.data
    state.value = ECellState.READY
  } catch (error: any) {
    // The cell stays empty rather than half-drawn; makeRequest has already toasted the reason.
    state.value = error?.response?.status === 403 ? ECellState.NO_ACCESS : ECellState.FAILED
  }
}

const onMessage = (message: ILiveMessage) => {
  const item = message.item
  if (!item || item.category_id !== categoryId.value) return
  if (message.type === ELiveMessageType.ITEM_DELETED) {
    items.value = items.value.filter((existing) => existing.id !== item.id)
  } else if (!items.value.some((existing) => existing.id === item.id)) {
    items.value = [item, ...items.value].slice(0, MAX_ITEMS)
  }
}

// A reconnect reloads: changes made while the socket was down were never delivered.
useItemsLive({ onMessage, onReconnected: load })

onMounted(load)
</script>

<style scoped lang="scss">
.sample-cell {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  font-size: 14px;
  color: var(--text-color-20);

  &__title {
    margin-bottom: 5px;
    color: var(--text-color-30);
  }

  &__list {
    flex: 1;
    overflow-y: auto;
  }

  &__row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 10px;
    padding: 5px 0;
    border-bottom: 1px solid var(--background-color-20);
  }

  &__time {
    flex-shrink: 0;
    font-size: 12px;
    color: var(--text-color-30);
  }

  &__message {
    display: flex;
    flex: 1;
    align-items: center;
    justify-content: center;
    color: var(--text-color-30);
  }
}
</style>
