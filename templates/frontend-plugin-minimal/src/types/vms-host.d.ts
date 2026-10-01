// Types of the modules the Incoresoft VMS 25.1 host shares with plugins through Module Federation.
// They exist only at runtime, so TypeScript learns about them from this file. Only the supported
// plugin surface is typed; any other host module resolves through the wildcards at the end (`any`).

declare module 'core/api' {
  export interface IApiConfig {
    /** Base URL of the REST API; `makeRequest` prefixes it to `url`. */
    API: string
    WEBSOCKET_API: string
    WEBSITE: string
    DOCUMENTATION_URL: string
    STORAGE_URL: string
  }

  export interface IMakeRequestConfig {
    /** Path relative to `config.API`, e.g. `/api/v2/<plugin>/items`. */
    url?: string
    method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | 'get' | 'post' | 'put' | 'patch' | 'delete'
    headers?: Record<string, string>
    /** Every key is written as `key=value`, `undefined` and `null` included — leave unset ones out.
     *  Arrays are serialised as `key=[a,b]`, strings inside them quoted. */
    params?: Record<string, unknown>
    data?: unknown
    responseType?: 'arraybuffer' | 'blob' | 'document' | 'json' | 'text' | 'stream'
    signal?: AbortSignal
    onUploadProgress?: (event: { loaded: number; total?: number }) => void
    /** Toast `response.data[0].type` for 400 (no field), 404, 409, 422, 498. Default true. */
    withErrorNotification?: boolean
    /** Cancel the previous still-running request to the same URL. Default false. */
    withCancelToken?: boolean
  }

  /** Axios response; a 401 logs the user out, other failures reject with the axios error. */
  export interface IMakeRequestResponse<T = unknown> {
    data: T
    status: number
    statusText: string
    headers: Record<string, string>
  }

  const coreApi: {
    config: IApiConfig
    makeRequest<T = unknown>(config: IMakeRequestConfig): Promise<IMakeRequestResponse<T>>
  }
  export default coreApi
}

declare module 'core/stores/useUserStore' {
  export interface IUserSettings {
    [key: string]: unknown
  }

  export interface IUser {
    id: number
    email: string
    fullname: string
    settings: IUserSettings
    role_ids: number[]
    permissions: string[]
    /** Root user: every permission check passes. */
    admin: boolean
    storage: string
    last_login: number
    created_at: number
  }

  export interface IUserStore {
    /** The signed-in user, null before login. */
    user: IUser | null
    /** Users the current user may see (filled by the host shell). */
    usersList: IUser[]
    /** UI language: `en`, `uk` or `es`. */
    locale: string
    /** dayjs formats chosen by the user. */
    dateFormat: string
    timeFormat: string
    /** First day of the week, 0 = Sunday. */
    weekStart: number
    /** URL prefix of files in VMS storage. */
    readonly SYSTEM_STORAGE: string
  }

  export function useUserStore(): IUserStore
}

declare module 'core/stores/usePermissionStore' {
  export interface IPermissionStore {
    /** Root user; every check below returns true for it. */
    readonly isAdmin: boolean
    /** A built-in (general) permission held by any of the user's roles. */
    hasPermission(permission: string): boolean
    /** A permission registered by a plugin backend, held by any of the user's roles. */
    hasPluginPermission(permission: string): boolean
    hasCameraItemPermission(permission: string, cameraId: number | string): boolean
    hasViewCameraItemPermission(cameraId: number): boolean
    hasViewLiveCameraItemPermission(cameraId: number): boolean
    hasViewPlaybackCameraItemPermission(cameraId: number): boolean
    hasViewLayoutItemPermission(layoutId: number): boolean
    hasViewMapPlanItemPermission(mapId: number, type: string): boolean
  }

  export function usePermissionStore(): IPermissionStore
}

declare module 'core/formatters/time' {
  /** Formats with the user's date/time formats and locale; `-` when nobody is signed in. */
  export function formatAsUserDate(
    timestamp: number,
    options?: { date?: boolean; time?: boolean; timezone?: string },
  ): string
  export function formatAsUserTime(timestamp: number): string
  export function getStartAndEndDay(timestamp: number): { start: number; end: number }
  /** `1d 2h 3m 4s`, starting from the largest non-zero unit. */
  export function formatMillisecondsAsTime(milliseconds: number): string
}

declare module 'core/features/layoutCellsRegistry' {
  import type { Component } from 'vue'

  /** An entry in the layout sidebar that adds a cell to the layout. */
  export interface ILayoutSidebarItem {
    /** Unique across all plugins; used as the drag payload. */
    id: string
    /** Plugins use `'plugin'`; the cell then renders the plugin's `client_layout_cell` component. */
    cellType: string
    /** Only these two groups are rendered. Plugins use `'plugins'`. */
    group: 'others' | 'plugins'
    icon: Component
    /** Locale key of the label. */
    labelKey: string
    /** Resolves the cell content (for plugin cells `<pluginId>:<payload>`), or null to cancel. */
    acquireContent: () => Promise<string | null>
    /** Components that must be mounted for `acquireContent` to work, typically its modal. */
    globalComponents?: Component[]
  }

  /** Receives the payload after `<pluginId>:` and resolves the new payload, or null to cancel. */
  export type TPluginCellEditor = (payload: string) => Promise<string | null>

  export function registerSidebarItem(item: ILayoutSidebarItem): void
  /** Adds an edit button to this plugin's cells. */
  export function registerPluginCellEditor(pluginId: string, editor: TPluginCellEditor): void
}

declare module 'host/stores/useSystemStore' {
  import type { App, Component } from 'vue'
  import type { Router } from 'vue-router'
  import type { IExtension } from 'host/models/extensions'

  export interface ISystemStore {
    router: Router | null
    /** The host's vue-i18n instance. */
    i18n: { global: { t(key: string, args?: Record<string, unknown>): string; locale: unknown } } | null
    EXTENSIONS_LOCALES: Record<string, Record<string, string>>
    ANALYTICS_EXTENSIONS: IExtension[]
    ERRORED_EXTENSIONS: IExtension[]
    /** Registers translations; allowed languages are `en`, `uk`, `es`. Host keys win on conflicts. */
    setExtensionsLocales(locales: Record<string, Record<string, string>>): void
    /** A Vue app with the host's i18n, router and UI kit installed, e.g. for map pop-ups. */
    createSubApp(component: Component, props?: Record<string, unknown> | null): App
  }

  export function useSystemStore(): ISystemStore
}

declare module 'host/models/extensions' {
  export enum ExtensionComponentLocation {
    ADMIN = 'admin',
    CORE = 'core',
  }

  export enum ExtensionType {
    SERVICE_PLUGIN = 'service_plugin',
    ANALYTICS_PLUGIN = 'analytics_plugin',
    DRIVER = 'driver',
  }

  export interface IExtensionComponent {
    component: string
    path: string
    type: string
    location: ExtensionComponentLocation
    children: IExtensionComponent[] | null
    redirectPath?: string
  }

  export interface IExtension {
    components: IExtensionComponent[]
    dev_path: string
    module_name: string
    path: string
    title: string
    type: ExtensionType
    alarms_available: boolean
    integration_required: boolean
    routes_permissions: Record<string, string>
    plugin_id?: string
    live_view_disabled?: boolean
  }
}

declare module 'styleguide/services/modal-service' {
  /** Shows or hides a modal registered under `name` (prefix names with your plugin id). */
  const modalService: {
    show(name: string, data?: unknown): void
    hide(name: string): void
  }
  export default modalService
}

declare module 'styleguide/services/WebSocketService' {
  export interface IWebSocketServiceOptions {
    /** Sent (JSON) after every successful (re)connect. */
    messageAfterConnect?: unknown
    /** Reconnect every 5 s, at most 10 times. Default true. */
    isReconnectNeeded?: boolean
    onOpen?: ((event: Event) => void) | null
    /** Receives the parsed JSON message. */
    onMessage?: ((data: any) => void) | null
    onClose?: ((event: CloseEvent) => void) | null
  }

  /** Connects on construction. */
  export default class WebSocketService {
    constructor(url: string, options?: IWebSocketServiceOptions)
    /** Sends JSON; logs an error when the socket is not open. */
    sendMessage(data: unknown): void
    /** Closes the socket and stops reconnecting. */
    disconnect(): void
  }
}

declare module 'styleguide/services/notification-service' {
  export interface INotificationOptions {
    /** Translate `message` as a locale key. Default true. */
    translate?: boolean
    autoClose?: number | false
    [option: string]: unknown
  }

  export const NotificationService: {
    error(message: string, options?: INotificationOptions): void
    success(message: string, options?: INotificationOptions): void
    warning(message: string, options?: INotificationOptions): void
  }
}

declare module 'styleguide/stores/useValidationErrorStore' {
  /** One item of a 400 response body. */
  export interface IValidationError {
    type: string
    field: string
    args: Array<{ validation_parameters?: Record<string, number | string | boolean | undefined> }>
  }

  export interface IValidationErrorStore {
    errors: Record<string, { types: string[]; args: Record<string, unknown>[] }>
    readonly hasErrors: boolean
    /** Maps a 400 response body onto form fields. */
    setErrors(validationErrors: IValidationError[]): void
    setError(fieldName: string, errorType: string, clearErrors?: boolean): void
    /** Translated message of the field's first error, or `''`. */
    getErrorForField(fieldName: string): string | null
    resetFieldError(fieldName: string): void
    clearErrors(): void
  }

  export function useValidationErrorStore(): IValidationErrorStore
}

declare module 'core/stores/useDevicesStore' {
  /** A camera as the host lists it. */
  export interface IDeviceItem {
    id: number
    uuid: string
    name: string
    type: string
    driver_id: string
    is_enabled: boolean
    state: string
  }

  export interface IDevicesStore {
    /** Cameras the current user may see. Empty until `getCameras()` ran, which the client shell
     *  does on start; Admin Center pages call it themselves. */
    readonly camerasList: IDeviceItem[]
    getCameras(params?: { is_ptz: boolean }, showDisabledAndInactive?: boolean): Promise<void>
  }

  export function useDevicesStore(): IDevicesStore
}

declare module 'core/components/AppPlayer' {
  import type { DefineComponent } from 'vue'
  import type { IDeviceItem } from 'core/stores/useDevicesStore'

  /** Archive range the player is allowed to move in, in epoch milliseconds. */
  export interface IPlayerEvent {
    start: number
    end: number
  }

  /** The host's video player: live by default, archive at `currentTime` with `isPlayback`. */
  const AppPlayer: DefineComponent<{
    camera: IDeviceItem
    isPlayback?: boolean
    currentTime?: number
    event?: IPlayerEvent | null
    showContextMenu?: boolean
  }>
  export default AppPlayer
}

declare module 'core/components/VCameraThumbnail' {
  import type { DefineComponent } from 'vue'

  /** A frame from the camera's archive at `timestamp`; fills its container. */
  const VCameraThumbnail: DefineComponent<{
    cameraId: number | null
    timestamp: number | null
    noDataLabel?: string
  }>
  export default VCameraThumbnail
}

// Every other module the host exposes. Untyped: prefer the modules above, which form the
// supported plugin surface of the 25.1 release line.
declare module 'core/*'
declare module 'host/*'
declare module 'styleguide/*'
