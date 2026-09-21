import type { AuditEventPage } from '../types/audit'

const API_BASE_URL = ''

export async function getAuditEvents(
  page = 0,
  size = 10,
): Promise<AuditEventPage> {
  const token = localStorage.getItem('cloudpilot_token')

  if (!token) {
    throw new Error('Authentication token not found')
  }

  const response = await fetch(
    `${API_BASE_URL}/api/audit/events?page=${page}&size=${size}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  )

  if (!response.ok) {
    if (response.status === 403) {
      throw new Error('You do not have permission to view activity history.')
    }
    if (response.status === 503) {
      throw new Error('Activity service is temporarily unavailable.')
    }
    throw new Error('Failed to load activity history.')
  }

  return response.json() as Promise<AuditEventPage>
}
