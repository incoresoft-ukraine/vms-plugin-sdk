<template>
  <div class="app-view" :data-el-name="`${TESTING_ID}-items`">
    <ISLoader v-if="!loaded" />
    <template v-else>
      <ISTable :columns="columns" :data="rows" :dataElName="`${TESTING_ID}-items`">
        <template #filter>
          <div class="flex justify-content-between align-items-end w-100 gap-15">
            <ISSelectField
              v-model="categoryId"
              class="sample-items-table__category"
              :label="$t('SAMPLE_CATEGORY')"
              :placeholder="$t('ALL')"
              :options="categories"
              trackBy="id"
              column="name"
              :dataElName="`${TESTING_ID}-items-category`"
            />
            <ISBigButton
              v-if="canManage"
              color="green"
              :disabled="!categories.length"
              :dataElName="`${TESTING_ID}-create-item`"
              @click="openCreate"
            >
              {{ $t('SAMPLE_CREATE_ITEM') }}
            </ISBigButton>
          </div>
        </template>

        <!-- ISTable decides once, when it is created, whether it has an actions column. -->
        <template #actions="{ row }">
          <ISIconPlay
            v-if="row.camera_id"
            v-tooltip="$t('VIDEO')"
            class="app-icon-hover cursor-pointer sample-items-table__icon"
            :data-el-name="`${TESTING_ID}-item-video`"
            @click="openVideo(row.ID.value)"
          />
          <ISIconDeleteSmall
            v-if="canManage"
            v-tooltip="$t('DELETE')"
            class="app-icon-hover cursor-pointer sample-items-table__icon"
            :data-el-name="`${TESTING_ID}-delete-item`"
            @click="confirmDelete(row.ID.value)"
          />
        </template>
      </ISTable>

      <!-- Server-side pages: `page` is zero-based, `total` comes from the backend. -->
      <ISPagination
        v-model:page="page"
        v-model:limit="limit"
        :total="total"
        class="mt-10"
        :dataElName="`${TESTING_ID}-items-pagination`"
        @update:page="load"
        @update:limit="load"
      />
    </template>

    <VItemModal />
    <VItemVideoModal />
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

import {
  ISBigButton,
  ISLoader,
  ISPagination,
  ISSelectField,
  ISTable,
  modalService,
} from '@incoresoft/incoresoft-ui'
import { ISIconDeleteSmall, ISIconPlay } from '@incoresoft/incoresoft-icons'
import { formatAsUserDate } from 'core/formatters/time'
import { useDevicesStore } from 'core/stores/useDevicesStore'
import { usePermissionStore } from 'core/stores/usePermissionStore'

import { getCategories, type ICategory } from '@/api/categories'
import { deleteItem, getItems, type IItem } from '@/api/items'
import VItemModal from '@/components/modals/VItemModal.vue'
import VItemVideoModal from '@/components/modals/VItemVideoModal.vue'
import { ELiveMessageType, useItemsLive, type ILiveMessage } from '@/composables/useItemsLive'
import { ITEM_MODAL, ITEM_VIDEO_MODAL, PERMISSIONS, TESTING_ID } from '@/nameset'

const { t } = useI18n()

const permissions = usePermissionStore()
const devicesStore = useDevicesStore()
const canManage = computed(() => permissions.hasPluginPermission(PERMISSIONS.MANAGE_ITEMS))

const loaded = ref(false)
const items = ref<IItem[]>([])
const categories = ref<ICategory[]>([])
const categoryId = ref<number | null>(null)

const page = ref(0)
const limit = ref(20)
const total = ref(0)

// Column keys are locale keys; ISTable leaves 'ID' untranslated.
const columns = [
  { text: 'ID', width: '70px' },
  'SAMPLE_TEXT',
  { text: 'SAMPLE_CATEGORY', width: '20%' },
  { text: 'CAMERA', width: '20%' },
  { text: 'CREATED', width: '180px' },
]

const cameraName = (cameraId: number | null) =>
  devicesStore.camerasList.find((camera) => camera.id === cameraId)?.name ?? ''

const rows = computed(() =>
  items.value.map((item) => ({
    id: item.id,
    camera_id: item.camera_id,
    ID: { value: item.id },
    SAMPLE_TEXT: { value: item.text },
    SAMPLE_CATEGORY: { value: item.category_name },
    CAMERA: { value: cameraName(item.camera_id) },
    CREATED: { value: formatAsUserDate(item.created_at) },
  })),
)

const load = async () => {
  try {
    const [categoryList, pageData] = await Promise.all([
      getCategories(),
      getItems({ categoryId: categoryId.value, limit: limit.value, offset: page.value * limit.value }),
    ])
    categories.value = categoryList
    items.value = pageData.data
    total.value = pageData.total
  } finally {
    loaded.value = true
  }
}

// Picking a category and clearing the field are different events of the select; watching the
// value covers both. A new filter starts from the first page.
watch(categoryId, () => {
  page.value = 0
  load()
})

// Only the first page follows the live feed: new items land on top of it. Deeper pages stay as
// they are, so the operator does not lose the row they were looking at. A deletion can touch any
// page, so it reloads. A reconnect reloads too: changes made while the socket was down were never
// delivered.
const onMessage = (message: ILiveMessage) => {
  if (!message.item) return
  if (message.type === ELiveMessageType.ITEM_DELETED || page.value === 0) load()
}

useItemsLive({ onMessage, onReconnected: load })

const openCreate = () =>
  modalService.show(ITEM_MODAL, { categories: categories.value, categoryId: categoryId.value })

const openVideo = (id: number) => {
  const item = items.value.find((entry) => entry.id === id)
  if (item) modalService.show(ITEM_VIDEO_MODAL, { item })
}

const confirmDelete = (id: number) => {
  const item = items.value.find((entry) => entry.id === id)
  if (!item) return
  modalService.show('confirmModal', {
    title: t('DELETE_CONFIRMATION'),
    message: t('SAMPLE_DELETE_ITEM_CONFIRMATION', { text: item.text }),
    // The row disappears through the live feed. The confirm modal does not wait for the promise;
    // a refusal is already toasted by makeRequest, so only keep it from going unhandled.
    confirm: () => deleteItem(id).catch(() => undefined),
  })
}

// Activated on the first mount too; afterwards every time the tab comes back. Categories may have
// changed in Admin Center in the meantime.
onActivated(load)
</script>

<style scoped lang="scss">
.sample-items-table {
  &__category {
    width: 300px;
  }

  // app-icon-hover only colours an icon; 18px is what the other tables use.
  &__icon {
    width: 18px;
    height: 18px;
    flex-shrink: 0;
  }
}
</style>
