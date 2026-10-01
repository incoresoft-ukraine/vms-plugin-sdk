<template>
  <ISModal
    :width="500"
    :name="CATEGORY_MODAL"
    :loading="saving"
    :dataElName="`${TESTING_ID}-category`"
    @beforeOpen="onBeforeOpen"
    @beforeClose="onBeforeClose"
  >
    <template #title>
      {{ editingId ? $t('SAMPLE_EDIT_CATEGORY') : $t('SAMPLE_CREATE_CATEGORY') }}
    </template>

    <template #content>
      <div class="app-card">
        <ISTextField
          v-model.trim="name"
          class="mb-20"
          required
          :label="$t('NAME')"
          :placeholder="$t('SAMPLE_ENTER_CATEGORY_NAME')"
          :dataElName="`${TESTING_ID}-category-name`"
          :error="validationStore.getErrorForField('name')"
          @input="validationStore.resetFieldError('name')"
        />
        <!-- The host's camera list; alarms of the category will show this camera's video. -->
        <ISSelectField
          v-model="cameraId"
          search
          :label="$t('CAMERA')"
          :placeholder="$t('SAMPLE_SELECT_CAMERA')"
          :options="devicesStore.camerasList"
          trackBy="id"
          column="name"
          :dataElName="`${TESTING_ID}-category-camera`"
          :error="validationStore.getErrorForField('camera_id')"
          @change="validationStore.resetFieldError('camera_id')"
        />
      </div>
    </template>

    <template #activities>
      <ISBigButton color="green" :dataElName="`${TESTING_ID}-category-save`" @click="save">
        {{ $t('SAVE') }}
      </ISBigButton>
    </template>
  </ISModal>
</template>

<script setup lang="ts">
import { ref } from 'vue'

import { ISBigButton, ISModal, ISSelectField, ISTextField, modalService } from '@incoresoft/incoresoft-ui'
import { useDevicesStore } from 'core/stores/useDevicesStore'
import { useValidationErrorStore } from 'styleguide/stores/useValidationErrorStore'

import { createCategory, updateCategory, type ICategory } from '@/api/categories'
import { CATEGORY_MODAL, TESTING_ID } from '@/nameset'

interface IModalParams {
  /** Present when editing; absent when creating. */
  category?: ICategory
}

const emit = defineEmits<{ (event: 'saved'): void }>()

// The close animation takes about 250 ms; clearing the form earlier makes it flash empty.
const RESET_DELAY_MS = 250

const validationStore = useValidationErrorStore()
const devicesStore = useDevicesStore()

const editingId = ref<number | null>(null)
const name = ref('')
const cameraId = ref<number | null>(null)
const saving = ref(false)
let resetTimer: ReturnType<typeof setTimeout> | undefined

// Parameters of modalService.show arrive here, not as props.
const onBeforeOpen = (event: { ref: { params: { value?: IModalParams } } }) => {
  // A reset still pending from the previous close must not wipe what we fill in now.
  clearTimeout(resetTimer)
  validationStore.clearErrors()
  const category = event?.ref?.params?.value?.category
  editingId.value = category?.id ?? null
  name.value = category?.name ?? ''
  cameraId.value = category?.camera_id ?? null
  // Admin Center does not load cameras on its own.
  if (!devicesStore.camerasList.length) devicesStore.getCameras()
}

const onBeforeClose = () => {
  resetTimer = setTimeout(() => {
    editingId.value = null
    name.value = ''
    cameraId.value = null
    validationStore.clearErrors()
  }, RESET_DELAY_MS)
}

const save = async () => {
  saving.value = true
  try {
    const data = { name: name.value, camera_id: cameraId.value }
    if (editingId.value) {
      await updateCategory(editingId.value, data)
    } else {
      await createCategory(data)
    }
    emit('saved')
    modalService.hide(CATEGORY_MODAL)
  } catch (error: any) {
    // 400 with a field: the message goes under the input. Other errors are toasted by makeRequest.
    if (Array.isArray(error?.response?.data)) validationStore.setErrors(error.response.data)
  } finally {
    saving.value = false
  }
}
</script>
