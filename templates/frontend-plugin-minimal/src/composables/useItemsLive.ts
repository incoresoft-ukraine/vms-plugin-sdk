import { onBeforeUnmount, onMounted } from 'vue'

import coreApi from 'core/api'
import WebSocketService from 'styleguide/services/WebSocketService'

import { PLUGIN_ID } from '@/nameset'
import type { IItem } from '@/api/items'

/**
 * Live feed of item changes, served by ItemsWebSocketController on the backend.
 *
 * One socket is shared by every component that uses the feed and closed when the last one goes
 * away: a page and a layout cell showing the same data must not open two connections.
 * WebSocketService (host) reconnects on drop and parses JSON.
 */

export enum ELiveMessageType {
  ITEM_CREATED = 'ITEM_CREATED',
  ITEM_DELETED = 'ITEM_DELETED',
  HEALTH_CHECK = 'HEALTH_CHECK',
}

export interface ILiveMessage {
  type: ELiveMessageType
  item?: IItem
}

interface ISubscriber {
  onMessage: (message: ILiveMessage) => void
  /** Called after the socket reconnects: what changed while it was down was never delivered. */
  onReconnected: () => void
}

const subscribers = new Set<ISubscriber>()
let socket: WebSocketService | null = null

const open = () => {
  if (socket) return

  const url = `${coreApi.config.WEBSOCKET_API}/api/v2/ws/${PLUGIN_ID}/items`
  let openedBefore = false
  const current: WebSocketService = new WebSocketService(url, {
    // Callbacks of a socket we already dropped must not touch the shared state.
    onOpen: () => {
      if (socket !== current) return
      if (openedBefore) subscribers.forEach((subscriber) => subscriber.onReconnected())
      openedBefore = true
    },
    onMessage: (message: ILiveMessage) => {
      if (socket !== current || message?.type === ELiveMessageType.HEALTH_CHECK) return
      subscribers.forEach((subscriber) => subscriber.onMessage(message))
    },
  })
  socket = current
}

const close = () => {
  if (subscribers.size > 0) return
  // disconnect() also stops the automatic reconnect.
  socket?.disconnect()
  socket = null
}

/** Subscribes while the calling component is mounted. */
export function useItemsLive(subscriber: ISubscriber) {
  onMounted(() => {
    subscribers.add(subscriber)
    open()
  })

  onBeforeUnmount(() => {
    subscribers.delete(subscriber)
    close()
  })
}
