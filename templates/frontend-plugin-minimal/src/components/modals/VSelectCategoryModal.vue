<!--
  Slot `plugin_select_modal`. The host mounts every plugin's select modal once, high in the tree and
  with no props, so it is always available: any code can open it with
  `modalService.show(SELECT_CATEGORY_MODAL, { callbackFunction })`.

  Parameters passed to `modalService.show` arrive through the `beforeOpen` event, not as props.
-->
<template>
  <ISModal
    :name="SELECT_CATEGORY_MODAL"
    :width="500"
    :dataElName="`${TESTING_ID}-select-category`"
    @beforeOpen="onBeforeOpen"
    @beforeClose="onBeforeClose"
  >
    <template #title>{{ $t('SAMPLE_SELECT_CATEGORY') }}</template>

    <template #content>
      <div class="app-card">
        <ISSelectField
          v-model="selected"
          :label="$t('SAMPLE_CATEGORY')"
          :placeholder="$t('SELECT')"
          :options="categories"
          trackBy="id"
          column="name"
          :cleanable="false"
          :dataElName="`${TESTING_ID}-select-category-field`"
        />
      </div>
    </template>

    <template #activities>
      <ISBigButton color="green" :disabled="!selected" :dataElName="`${TESTING_ID}-select-category-confirm`" @click="submit">
        {{ $t('SELECT') }}
      </ISBigButton>
    </template>
  </ISModal>
</template>

<script setup lang="ts">
import { ref } from 'vue'

import { ISBigButton, ISModal, ISSelectField, modalService } from '@incoresoft/incoresoft-ui'

import { getCategories, type ICategory } from '@/api/categories'
import { SELECT_CATEGORY_MODAL, TESTING_ID } from '@/nameset'

interface IModalParams {
  callbackFunction?: (categoryId: number | null) => void
}

const categories = ref<ICategory[]>([])
const selected = ref<number | null>(null)
const callback = ref<IModalParams['callbackFunction'] | null>(null)

const onBeforeOpen = async (event: { ref: { params: { value: IModalParams } } }) => {
  callback.value = event?.ref?.params?.value?.callbackFunction ?? null
  categories.value = await getCategories()
}

/**
 * Closing without choosing must still answer the caller: the sidebar flow waits on a promise, and a
 * modal that resolves nothing leaves the layout stuck with a half-added cell.
 */
const onBeforeClose = () => {
  callback.value?.(null)
  callback.value = null
  selected.value = null
}

const submit = () => {
  const chosen = selected.value
  callback.value?.(chosen)
  callback.value = null
  selected.value = null
  modalService.hide(SELECT_CATEGORY_MODAL)
}
</script>
