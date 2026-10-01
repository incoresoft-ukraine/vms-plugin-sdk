import makeRequest from './makeRequest'
import { PLUGIN_ID } from '@/nameset'

/** Mirrors ItemDTO on the backend: snake_case keys, timestamps in epoch milliseconds. */
export interface IItem {
  id: number
  text: string
  category_id: number
  /** The category name at the moment of the request (a copy, kept in alarms as it was). */
  category_name: string
  /** The category's camera when the item was created; null when it had none. */
  camera_id: number | null
  created_by: number
  created_at: number
}

/** One page of a list, the shape the host uses for its own paginated answers. */
export interface IPage<T> {
  data: T[]
  total: number
  pages: number
}

/** Filters of the items list; every field is optional. */
export interface IItemsQuery {
  categoryId?: number | null
  categoryIds?: number[]
  cameraIds?: number[]
  /** A fragment of the text, matched without regard to case. */
  text?: string
  /** Created within [startDate, endDate], epoch milliseconds. */
  startDate?: number | null
  endDate?: number | null
  /** Newest first by default. */
  sortOrder?: 'asc' | 'desc'
  /** Page size; the backend caps it at 500. */
  limit?: number
  offset?: number
}

const BASE = `/api/v2/${PLUGIN_ID}/items`

export const getItems = (query: IItemsQuery = {}) => {
  // The host writes every key into the query string, `undefined` included ("category_id=undefined"),
  // so only the parameters that are set go in. Arrays become `category_ids=[1,2]`.
  const params: Record<string, number | string | number[]> = {}
  if (query.categoryId) params.category_id = query.categoryId
  if (query.categoryIds?.length) params.category_ids = query.categoryIds
  if (query.cameraIds?.length) params.camera_ids = query.cameraIds
  if (query.text?.trim()) params.text = query.text.trim()
  if (query.startDate) params.start_date = query.startDate
  if (query.endDate) params.end_date = query.endDate
  if (query.sortOrder) params.sort_order = query.sortOrder
  if (query.limit !== undefined) params.limit = query.limit
  if (query.offset !== undefined) params.offset = query.offset
  return makeRequest<IPage<IItem>>({ url: BASE, method: 'GET', params })
}

export const createItem = (text: string, categoryId: number | null) =>
  makeRequest<IItem>({ url: BASE, method: 'POST', data: { text, category_id: categoryId } })

export const deleteItem = (id: number) => makeRequest<void>({ url: `${BASE}/${id}`, method: 'DELETE' })
