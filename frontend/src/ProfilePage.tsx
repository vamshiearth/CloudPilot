import './ProfilePage.css'

type User = {
  id: number
  email: string
  firstName: string
  lastName: string
  status: string
  tenantId: number | null
  tenantName: string | null
}

type AuthContext = {
  role: string
  permissions: string[]
}

type Tenant = {
  name: string
  slug: string
  status: string
}

type ProfilePageProps = {
  user: User
  tenant: Tenant | null
  authContext: AuthContext | null
}

function ProfilePage({ user, tenant, authContext }: ProfilePageProps) {
  const initials = `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase()

  return (
    <div className="profile-page">
      <header className="profile-header">
        <div>
          <p className="profile-eyebrow">Account</p>
          <h1>Profile</h1>
          <p>Review your CloudPilot identity and workspace access.</p>
        </div>
        <span className="profile-status">{user.status}</span>
      </header>

      <section className="profile-hero">
        <div className="profile-avatar">{initials}</div>
        <div>
          <h2>{user.firstName} {user.lastName}</h2>
          <p>{user.email}</p>
          <span>{authContext?.role || 'Member'}</span>
        </div>
      </section>

      <div className="profile-grid">
        <section className="profile-card">
          <h2>Personal information</h2>
          <div className="profile-fields">
            <div><span>First name</span><strong>{user.firstName}</strong></div>
            <div><span>Last name</span><strong>{user.lastName}</strong></div>
            <div><span>Email address</span><strong>{user.email}</strong></div>
            <div><span>Account status</span><strong>{user.status}</strong></div>
          </div>
        </section>

        <section className="profile-card">
          <h2>Current workspace</h2>
          <div className="profile-fields">
            <div><span>Organization</span><strong>{tenant?.name || user.tenantName || 'CloudPilot'}</strong></div>
            <div><span>Workspace slug</span><strong>{tenant?.slug || 'Not available'}</strong></div>
            <div><span>Organization status</span><strong>{tenant?.status || 'Active'}</strong></div>
            <div><span>Workspace role</span><strong>{authContext?.role || 'Member'}</strong></div>
          </div>
        </section>
      </div>

      <section className="profile-card permissions-card">
        <div>
          <h2>Access permissions</h2>
          <p>Permissions granted by your workspace role.</p>
        </div>
        <div className="permission-list">
          {(authContext?.permissions || []).map((permission) => (
            <span key={permission}>{permission.replaceAll('_', ' ')}</span>
          ))}
        </div>
      </section>
    </div>
  )
}

export default ProfilePage