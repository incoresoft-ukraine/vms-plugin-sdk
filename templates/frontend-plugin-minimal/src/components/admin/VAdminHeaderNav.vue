<template>
  <ISScrollableTabs
    v-model="currentTab"
    :tabs="tabs"
    :dataElName="`${TESTING_ID}-admin-navigation`"
    @update:modelValue="navigateTo"
  />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'

import { ISScrollableTabs } from '@incoresoft/incoresoft-ui'

import { adminNavigation } from '@/router/adminRoutes'
import { TESTING_ID } from '@/nameset'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const tabs = computed(() => adminNavigation.map((item) => ({ key: item.key, name: t(item.name) })))

const currentTab = ref(adminNavigation[0].key)

watch(
  () => route.path,
  (path) => {
    const active = adminNavigation.find((item) => path.startsWith(item.to))
    if (active) currentTab.value = active.key
  },
  { immediate: true },
)

const navigateTo = (key: string | number) => {
  const target = adminNavigation.find((item) => item.key === key)
  if (target && route.path !== target.to) router.push(target.to)
}
</script>
