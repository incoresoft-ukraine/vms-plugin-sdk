<template>
  <!-- Rendered by Admin Center inside the "PLUGINS" group of the left sidebar.
       Keep the app-sidebar__menu--* classes: the admin shell styles them. -->
  <router-link
    :to="`/admin/${PLUGIN_ID}/categories`"
    :class="{ 'router-link-active': isActive }"
    class="app-sidebar__menu--item"
    :data-el-name="`${TESTING_ID}-admin-sidebar-item`"
  >
    <span class="app-sidebar__menu--wrapper">
      <VPluginIcon class="app-sidebar__menu--icon" />
      <span class="app-sidebar__menu--text">{{ $t(PLUGIN_TITLE_KEY) }}</span>
    </span>
  </router-link>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { PLUGIN_ID, PLUGIN_TITLE_KEY, TESTING_ID } from '@/nameset'
import VPluginIcon from './VPluginIcon.vue'

const route = useRoute()
// Highlight the item for every page of the plugin, not only the exact link target.
const isActive = computed(() => route.path.startsWith(`/admin/${PLUGIN_ID}`))
</script>
