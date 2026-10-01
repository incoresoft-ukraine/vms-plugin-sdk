import coreApi, { type IMakeRequestConfig } from 'core/api'

/**
 * core.makeRequest = axios pre-configured with the VMS base URL, cookies (withCredentials),
 * the VMS array query-param serializer, 401 → logout handling and error toasts.
 * It resolves with the full AxiosResponse; we unwrap `.data` once here.
 */
export default function makeRequest<T = unknown>(config: IMakeRequestConfig): Promise<T> {
  return coreApi.makeRequest<T>(config).then((response) => response.data)
}
