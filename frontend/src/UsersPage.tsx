import { useEffect, useState } from 'react'
import './UsersPage.css'

type TenantMember = {
  membershipId: number
  userId: number
  email: string
  firstName: string
  lastName: string
  role: string
  status: string
  joinedAt: string
}

type TenantRole = {
  id: number
  name: string
  description: string
}

type AuthContext = {
  userId: number
  email: string
  tenantId: number
  role: string
  permissions: string[]
}

type SubscriptionUsage = {
  features: string[]
}

type Invitation = {
  id: number
  email: string
  role: string
  status: string
  invitedBy: string
  expiresAt: string
  createdAt: string
}

type CreateInvitationResponse = {
  invitation: Invitation
  invitationUrl: string
}

type UsersPageProps = {
  token: string
}

function UsersPage({ token }: UsersPageProps) {
  const [members, setMembers] = useState<TenantMember[]>([])
  const [roles, setRoles] = useState<TenantRole[]>([])
  const [invitations, setInvitations] = useState<Invitation[]>([])
  const [authContext, setAuthContext] = useState<AuthContext | null>(null)
  const [subscription, setSubscription] = useState<SubscriptionUsage | null>(null)
  const [showInviteForm, setShowInviteForm] = useState(false)
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRole, setInviteRole] = useState('MEMBER')
  const [inviting, setInviting] = useState(false)
  const [newInvitationUrl, setNewInvitationUrl] = useState('')
  const [copied, setCopied] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [updatingId, setUpdatingId] = useState<number | null>(null)
  const [removingId, setRemovingId] = useState<number | null>(null)

  async function loadData() {
    try {
      setLoading(true)
      setError('')

      const contextResponse = await fetch(
        '/api/auth/context',
        { headers: { Authorization: `Bearer ${token}` } },
      )
      if (!contextResponse.ok) throw new Error('Failed to load authentication context')

      const contextData: AuthContext = await contextResponse.json()
      setAuthContext(contextData)

      if (!contextData.permissions.includes('USER_READ')) {
        setMembers([])
        setRoles([])
        setInvitations([])
        return
      }

      const headers = { Authorization: `Bearer ${token}` }
      const [membersResponse, invitationsResponse, subscriptionResponse] = await Promise.all([
        fetch('/api/tenants/current/members', { headers }),
        fetch('/api/tenants/current/invitations', { headers }),
        fetch('/api/tenants/current/subscription', { headers }),
      ])

      if (!membersResponse.ok) {
        const data = await membersResponse.json().catch(() => null)
        throw new Error(data?.message || 'Failed to load organization users')
      }
      if (!invitationsResponse.ok) throw new Error('Failed to load invitations')

      setMembers(await membersResponse.json())
      setInvitations(await invitationsResponse.json())
      if (subscriptionResponse.ok) {
        setSubscription(await subscriptionResponse.json())
      } else {
        setSubscription(null)
      }

      if (contextData.permissions.includes('ROLE_READ')) {
        const rolesResponse = await fetch(
          '/api/tenants/current/roles',
          { headers },
        )
        if (!rolesResponse.ok) throw new Error('Failed to load roles')
        setRoles(await rolesResponse.json())
      }
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [token])

  async function updateRole(membershipId: number, role: string) {
    try {
      setUpdatingId(membershipId)
      setError('')
      const response = await fetch(
        `/api/tenants/current/members/${membershipId}/role`,
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ role }),
        },
      )
      if (!response.ok) {
        const data = await response.json().catch(() => null)
        throw new Error(data?.message || 'Failed to update role')
      }
      const updatedMember: TenantMember = await response.json()
      setMembers((currentMembers) =>
        currentMembers.map((member) =>
          member.membershipId === updatedMember.membershipId
            ? updatedMember
            : member,
        ),
      )
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to update role')
      await loadData()
    } finally {
      setUpdatingId(null)
    }
  }

  async function createInvitation(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    try {
      setInviting(true)
      setError('')
      setNewInvitationUrl('')
      setCopied(false)

      const response = await fetch(
        '/api/tenants/current/invitations',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ email: inviteEmail, role: inviteRole }),
        },
      )
      const data = await response.json()
      if (!response.ok) throw new Error(data.message || 'Failed to create invitation')

      const result = data as CreateInvitationResponse
      setInvitations((current) => [result.invitation, ...current])
      setNewInvitationUrl(result.invitationUrl)
      setInviteEmail('')
      setInviteRole('MEMBER')
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to create invitation')
    } finally {
      setInviting(false)
    }
  }

  async function revokeInvitation(invitationId: number) {
    if (!window.confirm('Cancel this invitation?')) return

    try {
      setError('')
      const response = await fetch(
        `/api/tenants/current/invitations/${invitationId}`,
        {
          method: 'DELETE',
          headers: { Authorization: `Bearer ${token}` },
        },
      )

      if (!response.ok) {
        const data = await response.json().catch(() => null)
        throw new Error(data?.message || 'Failed to cancel invitation')
      }

      setInvitations((current) =>
        current.filter((invitation) => invitation.id !== invitationId),
      )
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to cancel invitation')
    }
  }

  async function removeMember(member: TenantMember) {
    if (!window.confirm(`Remove ${member.firstName} ${member.lastName} from this organization?`)) return

    try {
      setRemovingId(member.membershipId)
      setError('')
      const response = await fetch(
        `/api/tenants/current/members/${member.membershipId}`,
        {
          method: 'DELETE',
          headers: { Authorization: `Bearer ${token}` },
        },
      )
      if (!response.ok) {
        const data = await response.json().catch(() => null)
        throw new Error(data?.message || 'Failed to remove member')
      }
      setMembers((current) => current.filter((item) => item.membershipId !== member.membershipId))
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to remove member')
    } finally {
      setRemovingId(null)
    }
  }

  if (loading) {
    return <div className="users-page"><p>Loading users...</p></div>
  }

  if (authContext && !authContext.permissions.includes('USER_READ')) {
    return (
      <div className="users-page">
        <div className="users-access-denied">
          <h2>Users</h2>
          <p>You do not have permission to manage organization users.</p>
        </div>
      </div>
    )
  }

  const hasAdvancedRbac = subscription?.features.includes('ADVANCED_RBAC') ?? false
  const hasRoleAssignPermission = authContext?.permissions.includes('ROLE_ASSIGN') ?? false
  const canAssignRoles = hasRoleAssignPermission && hasAdvancedRbac
  const canInviteUsers = authContext?.permissions.includes('USER_INVITE') ?? false
  const canRemoveUsers = authContext?.permissions.includes('USER_REMOVE') ?? false
  const activeOwners = members.filter((member) => member.role === 'OWNER').length
  const assignableRoles = roles.filter(
    (role) => role.name !== 'OWNER' || authContext?.role === 'OWNER',
  )

  return (
    <div className="users-page">
      <div className="users-header">
        <div>
          <h1>Users</h1>
          <p>Manage organization members and their access.</p>
        </div>
        <div className="users-header-actions">
          <div className="users-count">
            {members.length} {members.length === 1 ? 'Member' : 'Members'}
          </div>
          {canInviteUsers && (
            <button
              className="invite-user-button"
              onClick={() => {
                setShowInviteForm(true)
                setNewInvitationUrl('')
              }}
            >
              + Invite User
            </button>
          )}
        </div>
      </div>

      {error && <div className="users-error">{error}</div>}

      {hasRoleAssignPermission && !hasAdvancedRbac && (
        <div className="feature-upgrade-notice">
          Advanced role management is available on Starter and Pro plans.
        </div>
      )}

      <div className="role-info">
        <strong>Your role:</strong> {authContext?.role}
      </div>

      {showInviteForm && canInviteUsers && (
        <div className="invite-panel">
          <div className="invite-panel-header">
            <div>
              <h2>Invite User</h2>
              <p>Invite someone to join this organization.</p>
            </div>
            <button
              type="button"
              className="invite-close-button"
              onClick={() => {
                setShowInviteForm(false)
                setNewInvitationUrl('')
              }}
              aria-label="Close invite form"
            >
              ×
            </button>
          </div>

          <form className="invite-form" onSubmit={createInvitation}>
            <div className="invite-field">
              <label htmlFor="invite-email">Email</label>
              <input
                id="invite-email"
                type="email"
                value={inviteEmail}
                onChange={(event) => setInviteEmail(event.target.value)}
                placeholder="employee@company.com"
                required
              />
            </div>
            <div className="invite-field">
              <label htmlFor="invite-role">Role</label>
              <select
                id="invite-role"
                value={inviteRole}
                onChange={(event) => setInviteRole(event.target.value)}
              >
                {assignableRoles.map((role) => (
                  <option key={role.id} value={role.name}>{role.name}</option>
                ))}
              </select>
            </div>
            <button
              type="submit"
              className="invite-submit-button"
              disabled={inviting}
            >
              {inviting ? 'Sending...' : 'Create Invitation'}
            </button>
          </form>

          {newInvitationUrl && (
            <div className="invite-success">
              <strong>Invitation created</strong>
              <p>Copy this link and send it to the invited user:</p>
              <div className="invite-link-row">
                <input readOnly value={newInvitationUrl} aria-label="Invitation link" />
                <button
                  type="button"
                  onClick={async () => {
                    await navigator.clipboard.writeText(newInvitationUrl)
                    setCopied(true)
                  }}
                >
                  {copied ? 'Copied' : 'Copy'}
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      <div className="users-table-container">
        <table className="users-table">
          <thead>
            <tr><th>User</th><th>Email</th><th>Role</th><th>Status</th>{canRemoveUsers && <th>Actions</th>}</tr>
          </thead>
          <tbody>
            {members.map((member) => (
              <tr key={member.membershipId}>
                <td>
                  <div className="user-name-cell">
                    <div className="user-avatar">
                      {member.firstName.charAt(0).toUpperCase()}
                      {member.lastName.charAt(0).toUpperCase()}
                    </div>
                    <strong>{member.firstName} {member.lastName}</strong>
                  </div>
                </td>
                <td>{member.email}</td>
                <td>
                  {canAssignRoles ? (
                    <select
                      className="role-select"
                      value={member.role}
                      disabled={updatingId === member.membershipId || removingId !== null}
                      onChange={(event) => updateRole(member.membershipId, event.target.value)}
                    >
                      {roles.map((role) => (
                        <option
                          key={role.id}
                          value={role.name}
                          disabled={role.name === 'OWNER' && authContext?.role !== 'OWNER'}
                        >
                          {role.name}
                        </option>
                      ))}
                    </select>
                  ) : <span className="role-badge">{member.role}</span>}
                </td>
                <td>
                  <span className={`status-badge ${member.status.toLowerCase()}`}>
                    {member.status}
                  </span>
                </td>
                {canRemoveUsers && (
                  <td>
                    {member.userId !== authContext?.userId &&
                      (member.role !== 'OWNER' || (authContext?.role === 'OWNER' && activeOwners > 1)) && (
                      <button
                        type="button"
                        className="cancel-invite-button"
                        disabled={removingId !== null || updatingId !== null}
                        onClick={() => removeMember(member)}
                      >
                        {removingId === member.membershipId ? 'Removing...' : 'Remove'}
                      </button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <section className="pending-invitations">
        <div className="pending-header">
          <div>
            <h2>Pending Invitations</h2>
            <p>Invitations that have not yet been accepted.</p>
          </div>
          <span>{invitations.length}</span>
        </div>
        {invitations.length === 0 ? (
          <div className="pending-empty">No pending invitations.</div>
        ) : (
          <div className="pending-list">
            {invitations.map((invitation) => (
              <div key={invitation.id} className="pending-row">
                <div>
                  <strong>{invitation.email}</strong>
                  <span>Invited by {invitation.invitedBy}</span>
                </div>
                <span className="role-badge">{invitation.role}</span>
                <span className="pending-badge">{invitation.status}</span>
                {canInviteUsers && (
                  <button
                    type="button"
                    className="cancel-invite-button"
                    onClick={() => revokeInvitation(invitation.id)}
                  >
                    Cancel
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

export default UsersPage
