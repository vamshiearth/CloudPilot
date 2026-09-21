import { useEffect, useState } from 'react'
import { getAuditEvents } from './api/auditApi'
import type { AuditEvent, AuditEventPage } from './types/audit'
import './ActivityPage.css'

type ActivityPageProps = {
  token: string
}

function ActivityPage({ token }: ActivityPageProps) {
  const [data, setData] = useState<AuditEventPage | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function loadEvents() {
      try {
        setLoading(true)
        setError(null)
        const result = await getAuditEvents(page, 10)
        setData(result)
      } catch (loadError) {
        setError(
          loadError instanceof Error
            ? loadError.message
            : 'Failed to load activity history.',
        )
      } finally {
        setLoading(false)
      }
    }

    void loadEvents()
  }, [page, token])

  async function retry() {
    try {
      setLoading(true)
      setError(null)
      setData(await getAuditEvents(page, 10))
    } catch (retryError) {
      setError(
        retryError instanceof Error
          ? retryError.message
          : 'Failed to load activity history.',
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="activity-page">
      <header className="activity-header">
        <div>
          <p className="activity-kicker">Organization history</p>
          <h1>Activity</h1>
          <p>Recent activity across your organization.</p>
        </div>
        {data && !loading && (
          <span className="activity-count">
            {data.totalElements} {data.totalElements === 1 ? 'event' : 'events'}
          </span>
        )}
      </header>

      <section className="activity-panel" aria-live="polite">
        {loading ? (
          <div className="activity-state">Loading activity...</div>
        ) : error ? (
          <div className="activity-error">
            <strong>{error}</strong>
            <br />
            <button className="activity-retry" onClick={() => void retry()}>
              Retry
            </button>
          </div>
        ) : !data || data.events.length === 0 ? (
          <div className="activity-state">No activity yet.</div>
        ) : (
          <div className="activity-list">
            {data.events.map((event) => (
              <ActivityItem key={event.eventId} event={event} />
            ))}
          </div>
        )}
      </section>

      {data && !loading && !error && data.totalElements > 0 && (
        <div className="activity-pagination">
          <button
            disabled={data.first}
            onClick={() => setPage((current) => Math.max(current - 1, 0))}
          >
            Previous
          </button>
          <span className="activity-page-label">
            Page {data.page + 1} of {Math.max(data.totalPages, 1)}
          </span>
          <button
            disabled={data.last}
            onClick={() => setPage((current) => current + 1)}
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}

function ActivityItem({ event }: { event: AuditEvent }) {
  return (
    <article className="activity-item">
      <div className="activity-item-top">
        <h2>{formatEventType(event.eventType)}</h2>
        <time className="activity-time" dateTime={event.occurredAt}>
          {formatDate(event.occurredAt)}
        </time>
      </div>
      <p className="activity-description">{event.description}</p>
      <span className="activity-resource">
        {event.resourceType}
        {event.resourceId ? ` #${event.resourceId}` : ''}
      </span>
    </article>
  )
}

function formatEventType(eventType: string) {
  return eventType
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ')
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

export default ActivityPage
