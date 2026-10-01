<template>
  <div class="client-app">
    <router-view v-slot="{ Component }">
      <keep-alive :max="1">
        <component :is="isOwnRoute ? Component : null" />
      </keep-alive>
    </router-view>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

// Registering locales is a side effect of importing this module (see src/locales/index.ts).
import '@/locales'

// The host caches the tab by component name: 'ClientLayout' + PascalCase(module_name).
defineOptions({ name: 'ClientLayoutSamplePlugin' })

const route = useRoute()

// While another plugin's tab is active, the host keeps this layout alive and our <router-view>
// briefly resolves THAT plugin's view. Rendering null instead keeps our page cached, not destroyed.
// Ternary on :is, never v-if: v-if on a keep-alive child crashes.
const ownRecord = route.matched[0]
const isOwnRoute = computed(() => route.matched.includes(ownRecord))
</script>

<style scoped lang="scss">
.client-app {
  height: 100%;
  min-height: 0;
}
</style>
