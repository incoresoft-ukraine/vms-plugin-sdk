import type { IDeviceItem } from 'core/stores/useDevicesStore'
import type { IItem } from '@/api/items'

/**
 * The Search page contract. The host owns the page: the calendar, the camera tree, the sort order,
 * the result list and the details panel. A plugin adds one block to the filter sidebar and does
 * the searching itself; it hands the host the rows, and the host draws them with the plugin's card
 * and details components.
 */

/** Filters this plugin's block offers. The host keeps them while the user is on another block. */
export interface ISearchFilters {
  category_ids: number[]
  text: string
}

/** What the host gives the filter block (`client_search_filter_block`). */
export interface ISearchFilterBlockProps {
  /** Our plugin id; the key of every result row and of the saved filters. */
  blockKey: string
  /** The plugin title from settings.json, shown as the block header. */
  blockName: string
  /** Keys of the expanded sidebar blocks; the block toggles its own key. */
  activeExpanded: string[]
  /** The period picked in the host calendar, epoch milliseconds. */
  startTime: number
  endTime: number
  /** Cameras ticked in the host camera tree; empty means all. */
  cameras: IDeviceItem[]
  searchOrder: 'asc' | 'desc'
  /** Filters saved by the host the last time this block was open, or null on the first open. */
  savedBlockFilters: ISearchFilters | null
  /** Flips when the user scrolls to the end of the results: load the next page. */
  reachEndTrigger: boolean
  /** Grows when the user presses refresh: load the first page again. */
  refreshTrigger: number
}

/**
 * A result row as the host expects it. Two fields beyond the item: `timestamp` places the row on
 * the time axis, and `__SHOW_CAMERA_THUMBNAIL` with `camera_id` makes the host draw a frame from
 * the camera archive next to the card.
 */
export interface ISearchItem extends IItem {
  timestamp: number
  __SHOW_CAMERA_THUMBNAIL: boolean
}

/** What the host passes to the card and the details component. */
export interface ISearchResult extends ISearchItem {
  /** Row key assigned by the host. */
  key: string
  /** Equals our plugin id. */
  searchedElementType: string
}

/** Settings of the host player in the details panel. */
export interface ISearchPlayerSettings {
  camera: IDeviceItem
  startTime: number
  endTime: number
}
