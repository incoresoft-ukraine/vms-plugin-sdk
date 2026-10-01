<template>
  <!-- Mounted by the host at /admin/sample_plugin (component.json: type "view", location "admin").
       Navigation lives here once; views render content only. -->
  <div class="admin-app">
    <VAdminHeaderNav class="mb-15" />
    <div class="admin-app__content">
      <router-view v-slot="{ Component }">
        <keep-alive :max="1">
          <component :is="isOwnRoute ? Component : null" />
        </keep-alive>
      </router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import '@/locales'
import VAdminHeaderNav from '@/components/admin/VAdminHeaderNav.vue'

defineOptions({ name: 'AdminLayoutSamplePlugin' })

const route = useRoute()

// Same guard as in the client layout.
const ownRecord = route.matched[0]
const isOwnRoute = computed(() => route.matched.includes(ownRecord))
</script>

<style scoped lang="scss">
.admin-app {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;

  &__content {
    flex: 1;
    min-height: 0;
  }
}
</style>
