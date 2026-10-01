<template>
  <div class="app-view" :data-el-name="`${TESTING_ID}-admin-categories`">
    <ISLoader v-if="!loaded" />
    <!-- ISTable: `data` is one object per row keyed by column, each cell `{ value }`; `id` keys the
         rows. Slots: `filter` (toolbar above the table) and `actions` ({ row }). -->
    <ISTable
      v-else
      :columns="columns"
      :data="rows"
      class="sample-categories-table"
      :dataElName="`${TESTING_ID}-categories`"
      @rowDblClick="openEdit($event.ID.value)"
    >
      <template #filter>
        <div class="flex justify-content-between align-items-end w-100 gap-15">
          <ISSearchField
            v-model.trim="search"
            class="sample-categories-table__search"
            :label="$t('SAMPLE_CATEGORY')"
            :placeholder="$t('SAMPLE_ENTER_CATEGORY_NAME')"
            :dataElName="`${TESTING_ID}-categories-search`"
          />
          <ISBigButton color="green" :dataElName="`${TESTING_ID}-create-category`" @click="openCreate">
            {{ $t('SAMPLE_CREATE_CATEGORY') }}
          </ISBigButton>
        </div>
      </template>

      <template #actions="{ row }">
        <ISIconPencil
          v-tooltip="$t('EDIT')"
          class="app-icon-hover cursor-pointer sample-categories-table__icon"
          :data-el-name="`${TESTING_ID}-edit-category`"
          @click="openEdit(row.ID.value)"
        />
        <ISIconDeleteSmall
          v-tooltip="$t('DELETE')"
          class="app-icon-hover cursor-pointer sample-categories-table__icon"
          :data-el-name="`${TESTING_ID}-delete-category`"
          @click="confirmDelete(row.ID.value)"
        />
      </template>
    </ISTable>

    <VCategoryModal @saved="load" />
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { ISBigButton, ISLoader, ISSearchField, ISTable, modalService } from '@incoresoft/incoresoft-ui'
import { ISIconDeleteSmall, ISIconPencil } from '@incoresoft/incoresoft-icons'
import { formatAsUserDate } from 'core/formatters/time'
import { useDevicesStore } from 'core/stores/useDevicesStore'

import { deleteCategory, getCategories, type ICategory } from '@/api/categories'
import VCategoryModal from '@/components/admin/VCategoryModal.vue'
import { CATEGORY_MODAL, TESTING_ID } from '@/nameset'

const { t } = useI18n()

const devicesStore = useDevicesStore()

const loaded = ref(false)
const categories = ref<ICategory[]>([])
const search = ref('')

// Column keys are locale keys; ISTable leaves 'ID' untranslated.
const columns = [
  { text: 'ID', width: '70px' },
  'NAME',
  { text: 'CAMERA', width: '25%' },
  { text: 'CREATED', width: '180px' },
]

const cameraName = (cameraId: number | null) =>
  devicesStore.camerasList.find((camera) => camera.id === cameraId)?.name ?? ''

const rows = computed(() => {
  const query = search.value.toLowerCase()
  return categories.value
    .filter((category) => !query || category.name.toLowerCase().includes(query))
    .map((category) => ({
      id: category.id,
      ID: { value: category.id },
      NAME: { value: category.name },
      CAMERA: { value: cameraName(category.camera_id) },
      CREATED: { value: formatAsUserDate(category.created_at) },
    }))
})

const load = async () => {
  try {
    // Admin Center does not load cameras on its own; they are needed for the names in the table.
    const cameras = devicesStore.camerasList.length ? Promise.resolve() : devicesStore.getCameras()
    ;[categories.value] = await Promise.all([getCategories(), cameras])
  } finally {
    loaded.value = true
  }
}

const openCreate = () => modalService.show(CATEGORY_MODAL)

const openEdit = (id: number) => {
  const category = categories.value.find((entry) => entry.id === id)
  if (category) modalService.show(CATEGORY_MODAL, { category })
}

// A category with items is refused by the backend and makeRequest toasts the reason. The confirm
// modal does not wait for the promise, so the refusal is caught here.
const confirmDelete = (id: number) => {
  const category = categories.value.find((entry) => entry.id === id)
  if (!category) return
  modalService.show('confirmModal', {
    title: t('DELETE_CONFIRMATION'),
    message: t('SAMPLE_DELETE_CATEGORY_CONFIRMATION', { name: category.name }),
    confirm: () =>
      deleteCategory(id)
        .then(load)
        .catch(() => undefined),
  })
}

// Activated on the first mount too: the admin layout caches its pages.
onActivated(load)
</script>

<style scoped lang="scss">
.sample-categories-table {
  &__search {
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
