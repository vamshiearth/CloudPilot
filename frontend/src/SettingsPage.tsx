import { useState } from 'react'
import './SettingsPage.css'

type Tenant = {
  name: string
  slug: string
  status: string
}

type AuthContext = {
  role: string
  permissions: string[]
}

type SettingsPageProps = {
  tenant: Tenant | null
  authContext: AuthContext | null
  theme: 'light' | 'dark'
  onThemeChange: (theme: 'light' | 'dark') => void
}

function SettingsPage({ tenant, authContext, theme, onThemeChange }: SettingsPageProps) {
  const [emailUpdates, setEmailUpdates] = useState(
    () => localStorage.getItem('cloudpilot_email_updates') !== 'false',
  )
  const [saved, setSaved] = useState(false)

  function savePreferences() {
    localStorage.setItem('cloudpilot_email_updates', String(emailUpdates))
    setSaved(true)
    window.setTimeout(() => setSaved(false), 2200)
  }

  return (
    <div className="settings-page">
      <header className="settings-header">
        <div>
          <p className="settings-eyebrow">Workspace control</p>
          <h1>Settings</h1>
          <p>Manage workspace preferences and your CloudPilot experience.</p>
        </div>
      </header>

      {saved && <div className="settings-success">Preferences saved.</div>}

      <div className="settings-sections">
        <section className="settings-card">
          <div className="settings-card-heading">
            <div>
              <h2>Appearance</h2>
              <p>Choose how CloudPilot looks on this device.</p>
            </div>
          </div>
          <div className="theme-buttons" role="group" aria-label="Theme">
            <button
              className={theme === 'light' ? 'theme-button selected' : 'theme-button'}
              onClick={() => onThemeChange('light')}
            >
              Light
            </button>
            <button
              className={theme === 'dark' ? 'theme-button selected' : 'theme-button'}
              onClick={() => onThemeChange('dark')}
            >
              Dark
            </button>
          </div>
        </section>

        <section className="settings-card">
          <div className="settings-card-heading">
            <div>
              <h2>Workspace</h2>
              <p>Current organization configuration.</p>
            </div>
            <span className="settings-badge">{tenant?.status || 'Active'}</span>
          </div>
          <div className="settings-details">
            <div><span>Organization name</span><strong>{tenant?.name || 'CloudPilot'}</strong></div>
            <div><span>Workspace slug</span><strong>{tenant?.slug || 'Not available'}</strong></div>
            <div><span>Your role</span><strong>{authContext?.role || 'Member'}</strong></div>
          </div>
        </section>

        <section className="settings-card">
          <div className="settings-card-heading">
            <div>
              <h2>Notifications</h2>
              <p>Choose which workspace updates reach your inbox.</p>
            </div>
          </div>
          <label className="settings-toggle-row">
            <span>
              <strong>Workspace activity updates</strong>
              <small>Receive important project and task activity by email.</small>
            </span>
            <input
              type="checkbox"
              checked={emailUpdates}
              onChange={(event) => setEmailUpdates(event.target.checked)}
            />
          </label>
          <button className="settings-save-button" onClick={savePreferences}>
            Save preferences
          </button>
        </section>

        <section className="settings-card settings-security-card">
          <div className="settings-card-heading">
            <div>
              <h2>Security</h2>
              <p>Your access is managed by your workspace role.</p>
            </div>
          </div>
          <div className="settings-security-note">
            <strong>Role-based access enabled</strong>
            <span>{authContext?.permissions.length || 0} permissions currently granted to your account.</span>
          </div>
        </section>
      </div>
    </div>
  )
}

export default SettingsPage