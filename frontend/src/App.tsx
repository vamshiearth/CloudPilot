import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ArrowDown, ArrowUpRight, LogIn, RefreshCw, Sparkles, X } from 'lucide-react'
import Dashboard from './Dashboard'
import InviteAcceptancePage from './InviteAcceptancePage'
import RegisterPage from './RegisterPage'
import './App.css'

type User = {
  id: number
  email: string
  firstName: string
  lastName: string
  status: string
  tenantId: number | null
  tenantName: string | null
}

type LoginResponse = User & {
  token: string
}

function App() {
  const inviteMatch = window.location.pathname.match(/^\/invite\/([^/]+)$/)
  const invitationToken = inviteMatch
    ? decodeURIComponent(inviteMatch[1])
    : null

  const [email, setEmail] = useState('john@example.com')
  const [password, setPassword] = useState('')
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login')
  const [showAuth, setShowAuth] = useState(window.location.pathname === '/login')
  const [loginNotice, setLoginNotice] = useState('')
  const [token, setToken] = useState<string | null>(
    localStorage.getItem('cloudpilot_token'),
  )
  const [user, setUser] = useState<User | null>(null)
  const [error, setError] = useState('')
  const [sessionError, setSessionError] = useState('')
  const [loading, setLoading] = useState(false)
  const [checkingSession, setCheckingSession] = useState(true)
  const [sessionRetry, setSessionRetry] = useState(0)

  useEffect(() => {
    async function restoreSession() {
      if (invitationToken) {
        setCheckingSession(false)
        return
      }

      if (!token) {
        setCheckingSession(false)
        return
      }

      try {
        const response = await fetch('/api/auth/me', {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        })

        if (response.status === 401 || response.status === 403) {
          localStorage.removeItem('cloudpilot_token')
          setToken(null)
          setUser(null)
          return
        }

        if (!response.ok) {
          throw new Error(`Authentication service temporarily unavailable (${response.status})`)
        }

        const userData: User = await response.json()
        setUser(userData)
        setSessionError('')
      } catch (error) {
        console.error('Unable to restore session', error)
        setSessionError(
          error instanceof Error
            ? error.message
            : 'Authentication service temporarily unavailable',
        )
        setUser(null)
      } finally {
        setCheckingSession(false)
      }
    }

    restoreSession()
  }, [token, invitationToken, sessionRetry])

  useEffect(() => {
    function handleBrowserBack() {
      setShowAuth(window.location.pathname === '/login')
      if (window.location.pathname === '/') {
        setAuthMode('login')
      }
    }

    window.addEventListener('popstate', handleBrowserBack)
    return () => window.removeEventListener('popstate', handleBrowserBack)
  }, [])

  function openAuth(mode: 'login' | 'register') {
    setAuthMode(mode)
    setShowAuth(true)
  }

  function closeAuth() {
    setShowAuth(false)
    setAuthMode('login')
    setError('')
    setLoginNotice('')
    if (window.location.pathname === '/login') {
      window.history.replaceState({ page: 'home' }, '', '/')
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setLoading(true)

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      })

      const data = await response.json()

      if (!response.ok) {
        throw new Error(data.message || 'Login failed')
      }

      const loginData = data as LoginResponse
      localStorage.setItem('cloudpilot_token', loginData.token)
      setToken(loginData.token)
      setUser({
        id: loginData.id,
        email: loginData.email,
        firstName: loginData.firstName,
        lastName: loginData.lastName,
        status: loginData.status,
        tenantId: loginData.tenantId,
        tenantName: loginData.tenantName,
      })
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  function handleLogout() {
    localStorage.removeItem('cloudpilot_token')
    setToken(null)
    setUser(null)
    setPassword('')
  }

  if (checkingSession) {
    return (
      <div className="login-page">
        <p>Loading CloudPilot...</p>
      </div>
    )
  }

  if (token && sessionError) {
    return (
      <div className="login-page">
        <div className="login-card">
          <h2>CloudPilot is temporarily unavailable</h2>
          <p className="error-message">{sessionError}</p>
          <button type="button" onClick={() => {
            setCheckingSession(true)
            setSessionRetry((retry) => retry + 1)
          }}>
            <RefreshCw size={16} aria-hidden="true" />
            Retry
          </button>
        </div>
      </div>
    )
  }

  if (invitationToken) {
    return <InviteAcceptancePage token={invitationToken} />
  }

  if (token && user) {
    return <Dashboard user={user} token={token} onLogout={handleLogout} />
  }

  if (authMode === 'register') {
    return (
      <RegisterPage
        onClose={closeAuth}
        onBackToLogin={() => {
          setAuthMode('login')
          setLoginNotice('')
        }}
        onRegistered={(registeredEmail) => {
          setEmail(registeredEmail)
          setPassword('')
          setAuthMode('login')
          setLoginNotice('Account created successfully. Please sign in.')
        }}
      />
    )
  }

  if (!showAuth) {
    return (
      <main className="home-page">
        <nav className="home-nav">
          <button className="home-brand" type="button" onClick={() => { window.history.pushState({ page: 'home' }, '', '/'); setShowAuth(false) }}>
            <span className="home-brand-mark"><Sparkles size={16} aria-hidden="true" /></span>
            <span>CloudPilot</span>
          </button>
          <div className="home-nav-links">
            <a href="#why-cloudpilot">Why CloudPilot</a>
            <a href="#workflow">How it works</a>
            <a href="#security">Security</a>
          </div>
          <button className="nav-signin" type="button" onClick={() => openAuth('login')}>
            Sign in <LogIn size={15} aria-hidden="true" />
          </button>
        </nav>

        <section className="home-hero">
          <div className="hero-copy">
            <p className="eyebrow"><span className="eyebrow-dot" /> The calm command center for busy teams</p>
            <h1>Move work forward.<br /><em>Keep everyone aligned.</em></h1>
            <p className="hero-lede">CloudPilot brings projects, people, permissions, and progress into one clear workspace built for modern teams.</p>
            <div className="hero-actions">
              <button className="primary-action" type="button" onClick={() => openAuth('register')}>
                Start for free <ArrowUpRight size={16} aria-hidden="true" />
              </button>
              <a className="text-action" href="#workflow">See how it works <ArrowDown size={14} aria-hidden="true" /></a>
            </div>
            <div className="hero-proof"><span className="proof-avatars"><i>AM</i><i>SK</i><i>JR</i></span><span>Built for teams that do their best work together</span></div>
          </div>
          <div className="hero-visual" aria-label="CloudPilot project workspace preview">
            <div className="visual-glow" />
            <div className="workspace-window">
              <div className="window-top"><span className="window-dots"><b /><b /><b /></span><span className="window-title">CloudPilot / Overview</span><span className="window-menu">•••</span></div>
              <div className="window-body">
                <aside className="mini-sidebar"><strong>✦</strong><span className="active-side">▦</span><span>◫</span><span>◌</span><span>⚙</span></aside>
                <div className="mini-main">
                  <div className="mini-heading"><div><small>MONDAY, SEPTEMBER 21</small><h3>Good morning, Alex</h3></div><span className="mini-avatar">AM</span></div>
                  <div className="metric-row"><div><small>ACTIVE PROJECTS</small><strong>12</strong><span className="positive">↑ 18%</span></div><div><small>TEAM CAPACITY</small><strong>84%</strong><span className="neutral">On track</span></div></div>
                  <div className="chart-card"><div className="chart-title"><strong>Project momentum</strong><span>Last 30 days ˅</span></div><svg viewBox="0 0 360 108" role="img" aria-label="Project momentum trending upward"><path d="M0 91 C35 88, 46 72, 77 78 S116 49, 145 65 S183 55, 210 42 S249 48, 270 25 S322 31, 360 7" /><path className="chart-fill" d="M0 91 C35 88, 46 72, 77 78 S116 49, 145 65 S183 55, 210 42 S249 48, 270 25 S322 31, 360 7 L360 108 L0 108 Z" /></svg></div>
                  <div className="task-card"><div className="chart-title"><strong>Team activity</strong><span className="live-label"><i /> Live</span></div><p><span className="activity-avatar purple">SK</span><b>Samira</b> completed <strong>API integration</strong><time>2m</time></p><p><span className="activity-avatar orange">JR</span><b>Jordan</b> moved <strong>Launch campaign</strong><time>14m</time></p></div>
                </div>
              </div>
            </div>
            <div className="floating-note note-one"><span>✓</span><div><strong>Milestone reached</strong><small>Website launch is on track</small></div></div>
            <div className="floating-note note-two"><span>↗</span><div><strong>Team momentum</strong><small>+24% this month</small></div></div>
          </div>
        </section>

        <section className="trust-strip"><span>ONE WORKSPACE FOR THE WHOLE TEAM</span><b>Projects</b><b>People</b><b>Progress</b><b>Peace of mind</b></section>

        <section className="why-section" id="why-cloudpilot">
          <div className="section-intro"><p className="eyebrow">WHY CLOUDPILOT</p><h2>Less chasing.<br /><em>More doing.</em></h2><p>When work lives in five different places, momentum gets lost. CloudPilot gives every team a shared source of truth, without adding another layer of complexity.</p></div>
          <div className="feature-grid"><article><span className="feature-number">01</span><h3>See the whole picture</h3><p>Projects, tasks, deadlines, and team activity stay connected so you always know what matters next.</p><a href="#workflow">Explore projects <span>↗</span></a></article><article><span className="feature-number">02</span><h3>Give work a clear home</h3><p>Organize every initiative by tenant and team with roles and permissions that make sense from day one.</p><a href="#security">Explore workspace <span>↗</span></a></article><article><span className="feature-number">03</span><h3>Move with confidence</h3><p>Built-in visibility into usage and workload helps teams spot friction before it becomes a surprise.</p><a href="#workflow">Explore insights <span>↗</span></a></article></div>
        </section>

        <section className="story-section">
          <div className="story-heading"><p className="eyebrow">SEE IT IN THE FLOW</p><h2>A small team.<br /><em>A much clearer day.</em></h2><p>Meet the people behind a typical CloudPilot workspace. Their conversation is the difference between wondering what is happening and knowing what to do next.</p></div>
          <div className="story-board">
            <div className="story-person person-ava"><span className="person-face">👩🏽‍💻</span><strong>Ava</strong><small>Project lead</small></div>
            <div className="story-chat chat-left"><b>Ava · 9:08 AM</b><p>“Can everyone see the launch tasks and owners?”</p><span className="chat-tail" /></div>
            <div className="story-chat chat-right"><b>Samir · 9:10 AM</b><p>“Yep. I picked up the API task and the deadline is clear.”</p><span className="chat-tail" /></div>
            <div className="story-person person-samir"><span className="person-face">👨🏾‍💻</span><strong>Samir</strong><small>Engineer</small></div>
            <div className="story-flow-line"><i /><i /><i /></div>
            <div className="story-result"><span>✓</span><div><strong>Everyone sees the same picture</strong><small>Owner assigned · Progress visible · Team moving</small></div></div>
          </div>
        </section>

        <section className="workflow-section" id="workflow"><div className="workflow-copy"><p className="eyebrow">A BETTER WAY TO WORK</p><h2>From first idea<br />to <em>final delivery.</em></h2><p>CloudPilot keeps the handoffs simple, the work visible, and the team moving in the same direction.</p><div className="steps"><div className="step active"><span>01</span><div><strong>Plan together</strong><small>Turn goals into focused projects and tasks.</small></div></div><div className="step"><span>02</span><div><strong>Work transparently</strong><small>Give everyone the context to contribute.</small></div></div><div className="step"><span>03</span><div><strong>Deliver with clarity</strong><small>Track momentum and celebrate progress.</small></div></div></div></div><div className="workflow-art"><div className="orbit orbit-large" /><div className="orbit orbit-small" /><div className="art-core">✦<small>ONE<br />CLEAR<br />VIEW</small></div><div className="art-tag tag-top">Projects <span>12</span></div><div className="art-tag tag-right">Tasks <span>48</span></div><div className="art-tag tag-bottom">Team <span>24</span></div></div></section>

        <section className="security-section" id="security"><div><p className="eyebrow">BUILT FOR TRUST</p><h2>Your work deserves<br /><em>a safe place to grow.</em></h2></div><div className="security-points"><p><span>◈</span><strong>Tenant-aware by design</strong><small>Keep teams and their work cleanly separated as you scale.</small></p><p><span>⌁</span><strong>Role-based access</strong><small>Give people the access they need, and nothing they don’t.</small></p><p><span>◷</span><strong>Ready when you are</strong><small>Start small, stay organized, and grow without re-platforming.</small></p></div></section>

        <section className="home-cta"><p className="eyebrow">YOUR NEXT GOOD DECISION</p><h2>Make room for<br /><em>meaningful work.</em></h2><p>Bring your team’s best work into focus with CloudPilot.</p><button className="primary-action" type="button" onClick={() => openAuth('register')}>Create your workspace <ArrowUpRight size={16} aria-hidden="true" /></button></section>
        <footer className="home-footer"><span className="home-brand"><span className="home-brand-mark">✦</span> CloudPilot</span><span>Projects with purpose.</span><span>© 2026 CloudPilot</span></footer>
      </main>
    )
  }

  return (
    <div className="login-page auth-modal-backdrop" onMouseDown={(event) => {
      if (event.target === event.currentTarget) closeAuth()
    }}>
      <div className="login-card">
        <div className="auth-card-header">
          <div className="brand">
            <div className="brand-icon">☁</div>
            <h1>CloudPilot</h1>
          </div>
          <button className="card-close" type="button" aria-label="Close sign in" onClick={closeAuth}>
            <X size={18} aria-hidden="true" />
          </button>
        </div>

        <h2>Welcome back</h2>

        <p className="subtitle">Sign in to manage your projects and tasks.</p>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Enter your password"
              required
            />
          </div>

          {loginNotice && <p className="success-message">{loginNotice}</p>}

          {error && <p className="error-message">{error}</p>}

          <button type="submit" disabled={loading}>
            <LogIn size={16} aria-hidden="true" />
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <p className="register-text">
          Don&apos;t have an account?{' '}
          <button
            type="button"
            className="auth-link-button"
            onClick={() => {
              setError('')
              setLoginNotice('')
              setAuthMode('register')
            }}
          >
            Create account
          </button>
        </p>
      </div>
    </div>
  )
}

export default App
