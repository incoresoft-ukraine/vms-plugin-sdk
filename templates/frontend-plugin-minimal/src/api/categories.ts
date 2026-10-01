import makeRequest from './makeRequest'
import { PLUGIN_ID } from '@/nameset'

/** Mirrors CategoryDTO on the backend. */
export interface ICategory {
  id: number
  /** Stable id rules store in their "Where" step. */
  source_id: string
  name: string
  /** Host camera behind the category; alarms of the category show its video. Null: no camera. */
  camera_id: number | null
  created_at: number
}

export interface ICategoryRequest {
  name: string
  camera_id: number | null
}

const BASE = `/api/v2/${PLUGIN_ID}/categories`

export const getCategories = () => makeRequest<ICategory[]>({ url: BASE, method: 'GET' })

export const createCategory = (data: ICategoryRequest) =>
  makeRequest<ICategory>({ url: BASE, method: 'POST', data })

export const updateCategory = (id: number, data: ICategoryRequest) =>
  makeRequest<ICategory>({ url: `${BASE}/${id}`, method: 'PUT', data })

export const deleteCategory = (id: number) =>
  makeRequest<void>({ url: `${BASE}/${id}`, method: 'DELETE' })
