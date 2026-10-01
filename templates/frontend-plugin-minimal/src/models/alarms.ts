import type { IItem } from '@/api/items'
import type { ISampleTriggerOptions } from './rules'

/**
 * Shapes the host hands to our alarm and notification slot components.
 *
 * <p>Only the fields this plugin actually reads are typed here; the host sends more. `message` is
 * whatever the backend put into the alarm, which for this plugin is SampleRuleMessage.
 */
export interface ISampleRuleMessage {
  item?: IItem
  /** Set by the core for its own alarm kinds; plugin messages usually leave it empty. */
  locale_key?: string
}

export interface IAlarm {
  id: number
  /** Equals the plugin name for our alarms — that is how the host picks our slot components. */
  type: string
  timestamp: number
  rule_name?: string
  source_name?: string
  /** Added by the host from the source's camera (RuleSource.getCameraId); null without one. */
  camera_id?: number | null
  server_name?: string | null
  owner_id?: number
  state?: string
  priority_level?: string
  /** Options of the rule that raised the alarm. */
  options?: ISampleTriggerOptions
  message: ISampleRuleMessage
}

export interface INotification {
  id: number
  type: string
  created_at: number
  /** The rule asked for a desktop (system) notification as well. */
  is_desktop?: boolean
  rule_name?: string
  message: ISampleRuleMessage
}
