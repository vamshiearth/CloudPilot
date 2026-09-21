import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import './InviteAcceptancePage.css'

type Invitation = {
  email: string
  organizationName: string
  role: string
  status: string
  expiresAt: string
}

type InviteAcceptancePageProps = {
  token: string
}

function InviteAcceptancePage({ token }: InviteAcceptancePageProps) {
  const [invitation, setInvitation] = useState<Invitation | null>(null)
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [accepted, setAccepted] = useState(false)

  useEffect(() => {
    async function loadInvitation() {
      try {
        setLoading(true)
        setError('')

        const response = await fetch(
          `/api/invitations/${encodeURIComponent(token)}`,
        )
        const data = await response.json()

        if (!response.ok) {
          throw new Error(data.message || 'Invitation could not be loaded')
        }

        setInvitation(data)
      } catch (error) {
        setError(
          error instanceof Error
            ? error.message
            : 'Invitation could not be loaded',
        )
      } finally {
        setLoading(false)
      }
    }

    loadInvitation()
  }, [token])

  async function acceptInvitation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    try {
      setSubmitting(true)
      setError('')

      const response = await fetch(
        `/api/invitations/${encodeURIComponent(token)}/accept`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ firstName, lastName, password }),
        },
      )

      if (!response.ok) {
        let message = 'Failed to accept invitation'
        try {
          const data = await response.json()
          message = data.message || message
        } catch {
          // The backend may return an empty response for an error.
        }
        throw new Error(message)
      }

      setAccepted(true)
    } catch (error) {
      setError(
        error instanceof Error ? error.message : 'Failed to accept invitation',
      )
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="invite-accept-page">
        <div className="invite-accept-card">
          <p>Loading invitation...</p>
        </div>
      </div>
    )
  }

  if (error && !invitation) {
    return (
      <div className="invite-accept-page">
        <div className="invite-accept-card">
          <div className="invite-logo">CloudPilot</div>
          <h1>Invitation unavailable</h1>
          <p className="invite-error">{error}</p>
          <button onClick={() => { window.location.href = '/' }}>
            Go to Login
          </button>
        </div>
      </div>
    )
  }

  if (accepted) {
    return (
      <div className="invite-accept-page">
        <div className="invite-accept-card">
          <div className="invite-logo">CloudPilot</div>
          <div className="invite-success-icon">✓</div>
          <h1>Invitation accepted</h1>
          <p>
            You are now a member of <strong>{invitation?.organizationName}</strong>.
          </p>
          <button
            className="invite-primary-button"
            onClick={() => { window.location.href = '/' }}
          >
            Continue to Login
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="invite-accept-page">
      <div className="invite-accept-card">
        <div className="invite-logo">CloudPilot</div>
        <p className="invite-eyebrow">ORGANIZATION INVITATION</p>
        <h1>You&apos;ve been invited</h1>
        <p className="invite-description">
          Join <strong>{invitation?.organizationName}</strong> on CloudPilot.
        </p>

        <div className="invite-details">
          <div>
            <span>Email</span>
            <strong>{invitation?.email}</strong>
          </div>
          <div>
            <span>Role</span>
            <strong>{invitation?.role}</strong>
          </div>
        </div>

        <form onSubmit={acceptInvitation} className="invite-accept-form">
          <div>
            <label htmlFor="invite-first-name">First Name</label>
            <input
              id="invite-first-name"
              value={firstName}
              onChange={(event) => setFirstName(event.target.value)}
              required
            />
          </div>
          <div>
            <label htmlFor="invite-last-name">Last Name</label>
            <input
              id="invite-last-name"
              value={lastName}
              onChange={(event) => setLastName(event.target.value)}
              required
            />
          </div>
          <div>
            <label htmlFor="invite-password">Password</label>
            <input
              id="invite-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              minLength={8}
              required
            />
            <small>Minimum 8 characters.</small>
          </div>

          {error && <div className="invite-form-error">{error}</div>}

          <button
            type="submit"
            className="invite-primary-button"
            disabled={submitting}
          >
            {submitting ? 'Joining...' : 'Accept Invitation'}
          </button>
        </form>
      </div>
    </div>
  )
}

export default InviteAcceptancePage
