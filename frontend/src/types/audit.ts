export interface AuditEvent {
  id: number
  eventId: string
  tenantId: number
  actorUserId: number | null
  eventType: string
  resourceType: string
  resourceId: string | null
  description: string
  eventData: string
  occurredAt: string
  receivedAt: string
}

export interface AuditEventPage {
  events: AuditEvent[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}
