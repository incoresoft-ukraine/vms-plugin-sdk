import { useSystemStore } from 'host/stores/useSystemStore'
import en from './messages/en.json'
import es from './messages/es.json'
import uk from './messages/uk.json'

// The host merges these into vue-i18n once, after all plugins are loaded. Allowed locales: en, es,
// uk. Host keys win on conflicts, so prefix your keys with the plugin name. Permission labels
// (PERMISSION_*), error types the API returns and rule trigger names belong here too.
useSystemStore().setExtensionsLocales({ en, es, uk })
