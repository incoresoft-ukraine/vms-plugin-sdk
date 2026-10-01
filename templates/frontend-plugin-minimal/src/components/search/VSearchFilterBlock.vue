<!--
  Slot `client_search_filter_block`. Our block in the sidebar of the Search page.

  The host owns the page; see models/search.ts for the contract. This block owns the request: it
  combines its own filters with the host period, cameras and sort order, calls our backend, and
  hands the rows to the host with `onSetSearchData`. The host clears the list when it receives
  `onChangeFilters`, so a new first page is always announced that way first; the next pages
  (`reachEndTrigger`) are simply appended.
-->
<template>
  <ISExpandBlock
    v-model="activeExpanded"
    :title="blockName"
    :expandKey="blockKey"
    class="sample-search-block"
    :dataElName="`${TESTING_ID}-search-filter`"
    @close="emit('onCloseBlock', blockKey)"
  >
    <template #icon>
      <VPluginIcon />
    </template>

    <template #activities>
      <ISIconReset v-show="!isDefault" :data-el-name="`${TESTING_ID}-search-reset`" @click.stop="reset" />
    </template>

    <div class="sample-search-block__fields">
      <ISSelectField
        v-model="filters.category_ids"
        multiple
        :label="$t('SAMPLE_CATEGORY')"
        :placeholder="$t('ALL')"
        :options="categories"
        trackBy="id"
        column="name"
        :dataElName="`${TESTING_ID}-search-category`"
      />
      <ISTextField
        v-model.trim="filters.text"
        class="mt-15"
        :label="$t('SAMPLE_TEXT')"
        :placeholder="$t('SAMPLE_ENTER_TEXT')"
        :dataElName="`${TESTING_ID}-search-text`"
      />
    </div>
  </ISExpandBlock>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch, type PropType } from 'vue'

import { ISExpandBlock, ISSelectField, ISTextField } from '@incoresoft/incoresoft-ui'
import { ISIconReset } from '@incoresoft/incoresoft-icons'
import type { IDeviceItem } from 'core/stores/useDevicesStore'

import { getCategories, type ICategory } from '@/api/categories'
import { getItems } from '@/api/items'
import VPluginIcon from '@/components/VPluginIcon.vue'
import type { ISearchFilters, ISearchItem } from '@/models/search'
import { TESTING_ID } from '@/nameset'

const props = defineProps({
  blockKey: { type: String, required: true },
  blockName: { type: String, required: true },
  activeExpanded: { type: Array as PropType<string[]>, required: true },
  startTime: { type: Number, required: true },
  endTime: { type: Number, required: true },
  cameras: { type: Array as PropType<IDeviceItem[]>, required: true },
  searchOrder: { type: String as PropType<'asc' | 'desc'>, required: true },
  savedBlockFilters: { type: Object as PropType<ISearchFilters | null>, default: null },
  reachEndTrigger: { type: Boolean, required: true },
  refreshTrigger: { type: Number, required: true },
})

const emit = defineEmits<{
  (event: 'update:activeExpanded', keys: string[]): void
  (event: 'onCloseBlock', blockKey: string): void
  (event: 'onChangeFilters', blockKey: string, filters: ISearchFilters): void
  (event: 'onSetSearchData', blockKey: string, rows: ISearchItem[], total: number): void
  (event: 'onSetShowLoader', shown: boolean): void
}>()

const PAGE_SIZE = 20
/** Typing in the text field must not fire a request per keystroke. */
const DEBOUNCE_MS = 500

const defaultFilters = (): ISearchFilters => ({ category_ids: [], text: '' })

const filters = reactive<ISearchFilters>(props.savedBlockFilters ?? defaultFilters())
const categories = ref<ICategory[]>([])
const offset = ref(0)
const total = ref(0)

const activeExpanded = computed({
  get: () => props.activeExpanded,
  set: (keys: string[]) => emit('update:activeExpanded', keys),
})

const isDefault = computed(() => !filters.category_ids.length && !filters.text)

const reset = () => Object.assign(filters, defaultFilters())

// A reply to an older request must not overwrite a newer one.
let requestId = 0

const load = async () => {
  const current = ++requestId
  emit('onSetShowLoader', true)
  try {
    const page = await getItems({
      categoryIds: filters.category_ids,
      cameraIds: props.cameras.map((camera) => camera.id),
      text: filters.text,
      startDate: props.startTime,
      endDate: props.endTime,
      sortOrder: props.searchOrder,
      limit: PAGE_SIZE,
      offset: offset.value,
    })
    if (current !== requestId) return
    total.value = page.total
    const rows: ISearchItem[] = page.data.map((item) => ({
      ...item,
      timestamp: item.created_at,
      __SHOW_CAMERA_THUMBNAIL: true,
    }))
    emit('onSetSearchData', props.blockKey, rows, page.total)
  } finally {
    if (current === requestId) emit('onSetShowLoader', false)
  }
}

// First page: tell the host about the filters (it clears the list) and load.
const search = () => {
  offset.value = 0
  emit('onChangeFilters', props.blockKey, { ...filters, category_ids: [...filters.category_ids] })
  load()
}

let debounceTimer: ReturnType<typeof setTimeout> | undefined
const searchDebounced = () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(search, DEBOUNCE_MS)
}

watch([filters, () => props.startTime, () => props.endTime, () => props.searchOrder, () => props.cameras], searchDebounced, {
  deep: true,
})

watch(
  () => props.reachEndTrigger,
  () => {
    if (offset.value + PAGE_SIZE >= total.value) return
    offset.value += PAGE_SIZE
    load()
  },
)

watch(() => props.refreshTrigger, search)

onMounted(async () => {
  categories.value = await getCategories()
  // With saved filters the host still has the results of the last visit.
  if (!props.savedBlockFilters) search()
})
</script>

<style scoped lang="scss">
.sample-search-block {
  &__fields {
    padding: 0 30px;
  }
}
</style>
