<template>
  <!-- Rendered by the host inside the "+" popover of the client header.
       Keep these class names: the host styles .navigation-plus__button. -->
  <div
    class="navigation-plus__button"
    :data-el-name="`${TESTING_ID}-header-plus-item`"
    @click="emit('tabClicked', tab)"
  >
    <div class="navigation-plus__button-icon"><VPluginIcon /></div>
    <span class="navigation-plus__button-title">{{ $t(PLUGIN_TITLE_KEY) }}</span>
  </div>
</template>

<script setup lang="ts">
import { PLUGIN_ID, PLUGIN_TITLE_KEY, TESTING_ID } from '@/nameset'
import type { INavigationTab } from '@/models/navigation'
import VPluginIcon from './VPluginIcon.vue'

const emit = defineEmits<{ tabClicked: [tab: INavigationTab] }>()

const tab: INavigationTab = {
  id: PLUGIN_ID,
  type: 'view',
  // A locale key: the host translates tab titles itself.
  title: PLUGIN_TITLE_KEY,
  path: `/${PLUGIN_ID}/items`,
  icon: PLUGIN_ID,
}
</script>
