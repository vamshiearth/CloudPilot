import { useState } from 'react'
import type { FormEvent } from 'react'
import { ArrowLeft, UserPlus, X } from 'lucide-react'
import './App.css'

type RegisterPageProps = {
  onClose: () => void
  onBackToLogin: () => void
  onRegistered: (email: string) => void
}

function RegisterPage({
  onClose,
  onBackToLogin,
  onRegistered,
}: RegisterPageProps) {
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [organizationName, setOrganizationName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')

    if (password !== confirmPassword) {
      setError('Passwords do not match')
      return
    }

    if (password.length < 8) {
      setError('Password must be at least 8 characters')
      return
    }

    setLoading(true)

    try {
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email,
          password,
          firstName,
          lastName,
          organizationName,
        }),
      })

      const data = await response.json()

      if (!response.ok) {
        throw new Error(data.message || 'Registration failed')
      }

      onRegistered(data.email)
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page auth-modal-backdrop" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose()
    }}>
      <div className="login-card register-card">
        <div className="auth-card-header">
          <div className="brand">
          <div className="brand-icon">☁</div>
          <h1>CloudPilot</h1>
          </div>
          <button className="card-close" type="button" aria-label="Close registration" onClick={onClose}>
            <X size={18} aria-hidden="true" />
          </button>
        </div>

        <h2>Create account</h2>
        <p className="subtitle">Create your CloudPilot account.</p>

        <form className="register-form" onSubmit={handleRegister}>
          <div className="form-group">
            <label htmlFor="firstName">First Name</label>
            <input
              id="firstName"
              value={firstName}
              onChange={(event) => setFirstName(event.target.value)}
              placeholder="John"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="lastName">Last Name</label>
            <input
              id="lastName"
              value={lastName}
              onChange={(event) => setLastName(event.target.value)}
              placeholder="Smith"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="organizationName">Organization Name</label>
            <input
              id="organizationName"
              value={organizationName}
              onChange={(event) => setOrganizationName(event.target.value)}
              placeholder="MegaCorp"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="registerEmail">Email</label>
            <input
              id="registerEmail"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@example.com"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="registerPassword">Password</label>
            <input
              id="registerPassword"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Minimum 8 characters"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">Confirm Password</label>
            <input
              id="confirmPassword"
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              placeholder="Enter password again"
              required
            />
          </div>

          {error && <p className="error-message">{error}</p>}

          <button type="submit" disabled={loading}>
            <UserPlus size={16} aria-hidden="true" />
            {loading ? 'Creating account...' : 'Create Account'}
          </button>
        </form>

        <p className="register-text">
          Already have an account?{' '}
          <button
            type="button"
            className="auth-link-button"
            onClick={onBackToLogin}
          >
            <ArrowLeft size={14} aria-hidden="true" />
            Sign in
          </button>
        </p>
      </div>
    </div>
  )
}

export default RegisterPage
