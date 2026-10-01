<template>
  <ISModal
    :width="500"
    :name="ITEM_MODAL"
    :loading="saving"
    :dataElName="`${TESTING_ID}-item`"
    @beforeOpen="onBeforeOpen"
    @beforeClose="onBeforeClose"
  >
    <template #title>{{ $t('SAMPLE_CREATE_ITEM') }}</template>

    <template #content>
      <div class="app-card">
        <ISTextField
          v-model.trim="text"
          class="mb-20"
          required
          :label="$t('SAMPLE_TEXT')"
          :placeholder="$t('SAMPLE_ENTER_TEXT')"
          :dataElName="`${TESTING_ID}-item-text`"
          :error="validationStore.getErrorForField('text')"
          @input="validationStore.resetFieldError('text')"
        />
        <ISSelectField
          v-model="categoryId"
          required
          :label="$t('SAMPLE_CATEGORY')"
          :placeholder="$t('SELECT')"
          :options="categories"
          trackBy="id"
          column="name"
          :cleanable="false"
          :dataElName="`${TESTING_ID}-item-category`"
          :error="validationStore.getErrorForField('category_id')"
          @change="validationStore.resetFieldError('category_id')"
        />
      </div>
    </template>

    <template #activities>
      <ISBigButton color="green" :dataElName="`${TESTING_ID}-item-save`" @click="save">
        {{ $t('SAVE') }}
      </ISBigButton>
    </template>
  </ISModal>
</template>

<script setup lang="ts">
import { ref } from 'vue'

import { ISBigButton, ISModal, ISSelectField, ISTextField, modalService } from '@incoresoft/incoresoft-ui'
import { ToastsService } from '@incoresoft/incoresoft-toasts'
import { useValidationErrorStore } from 'styleguide/stores/useValidationErrorStore'

import type { ICategory } from '@/api/categories'
import { createItem } from '@/api/items'
import { ITEM_MODAL, TESTING_ID } from '@/nameset'

interface IModalParams {
  categories: ICategory[]
  /** Pre-selected category, e.g. the one the page is filtered by. */
  categoryId?: number | null
}

// The close animation takes about 250 ms; clearing the form earlier makes it flash empty.
const RESET_DELAY_MS = 250

const validationStore = useValidationErrorStore()

const categories = ref<ICategory[]>([])
const text = ref('')
const categoryId = ref<number | null>(null)
const saving = ref(false)
let resetTimer: ReturnType<typeof setTimeout> | undefined

// Parameters of modalService.show arrive here, not as props.
const onBeforeOpen = (event: { ref: { params: { value?: IModalParams } } }) => {
  // A reset still pending from the previous close must not wipe what we fill in now.
  clearTimeout(resetTimer)
  validationStore.clearErrors()
  text.value = ''
  const params = event?.ref?.params?.value
  categories.value = params?.categories ?? []
  categoryId.value = params?.categoryId ?? categories.value[0]?.id ?? null
}

const onBeforeClose = () => {
  resetTimer = setTimeout(() => {
    text.value = ''
    categoryId.value = null
    validationStore.clearErrors()
  }, RESET_DELAY_MS)
}

const save = async () => {
  saving.value = true
  try {
    // The new item reaches the table through the live feed, like everyone else's.
    await createItem(text.value, categoryId.value)
    ToastsService.success('SAMPLE_ITEM_SAVED')
    modalService.hide(ITEM_MODAL)
  } catch (error: any) {
    // 400 with a field: the message goes under the input. Other errors are toasted by makeRequest.
    if (Array.isArray(error?.response?.data)) validationStore.setErrors(error.response.data)
  } finally {
    saving.value = false
  }
}
</script>
