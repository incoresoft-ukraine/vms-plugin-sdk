import { createApp } from 'vue'
import App from './App.vue'
// Standalone mount is only used when you open the dev server URL directly. Inside VMS the host
// mounts the exposed components itself; this file is never executed there.
createApp(App).mount('#app')
