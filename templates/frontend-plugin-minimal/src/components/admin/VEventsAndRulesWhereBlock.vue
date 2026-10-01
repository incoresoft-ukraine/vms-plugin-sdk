<!--
  Slot `admin_events_and_rules_where_block`. The "Where" step of the rule wizard for our rule type.

  Without it the host lists VEZHA analytics here, which a service plugin does not have, and a rule
  that needs sources could never be saved. The host passes:
   - `v-model`: the rule's `source_ids`, the source UUIDs the backend matches alarms against;
   - `options`: the rule options chosen in the "What" step;
   - `viewMode`: create / edit / details (details is read-only).
-->
<template>
  <ISListSelect
    v-if="rootGroup"
    v-model="sourceIds"
    class="sample-where-list"
    :rootGroup="rootGroup"
    trackBy="source_id"
    :hideIds="viewMode === EViewMode.DETAILS ? hiddenIds : []"
    :readonly="viewMode === EViewMode.DETAILS"
    :searchPlaceholder="$t('SAMPLE_ENTER_CATEGORY_NAME')"
    :dataElName="`${TESTING_ID}-rule-sources`"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, ref, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'

import { ISListSelect } from '@incoresoft/incoresoft-ui'

import { getCategories, type ICategory } from '@/api/categories'
import { EViewMode, type ISampleTriggerOptions } from '@/models/rules'
import { TESTING_ID } from '@/nameset'

const props = defineProps({
  modelValue: {
    type: Array as PropType<string[]>,
    required: true,
  },
  options: {
    type: Object as PropType<ISampleTriggerOptions>,
    default: () => ({}),
  },
  viewMode: {
    type: String as PropType<EViewMode>,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue'])

const { t } = useI18n()

const categories = ref<ICategory[]>([])

const sourceIds = computed({
  get: () => props.modelValue ?? [],
  set: (value: string[]) => emit('update:modelValue', value),
})

// ISListSelect shows a tree; categories are one flat group. The value it selects (trackBy) is the
// category's source UUID, exactly what rule.source_ids stores.
const rootGroup = computed(() => ({
  id: 0,
  name: t('SAMPLE_CATEGORIES'),
  groups: [],
  items: categories.value.map((category) => ({
    id: category.id,
    source_id: category.source_id,
    name: category.name,
  })),
}))

// In details mode only the chosen categories are shown.
const hiddenIds = computed(() =>
  categories.value
    .map((category) => category.source_id)
    .filter((sourceId) => !sourceIds.value.includes(sourceId)),
)

onMounted(async () => {
  categories.value = await getCategories()
})
</script>

<style scoped lang="scss">
.sample-where-list {
  max-height: 500px;
  min-height: 400px;
}
</style>
