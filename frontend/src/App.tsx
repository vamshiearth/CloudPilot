import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import {
  Activity,
  ArrowDown,
  ArrowRight,
  ArrowUpRight,
  BarChart3,
  ChartNoAxesCombined,
  Check,
  FolderKanban,
  Gauge,
  Globe2,
  LineChart,
  ListTodo,
  LockKeyhole,
  MonitorCog,
  LogIn,
  RefreshCw,
  Settings2,
  ShieldCheck,
  Sparkles,
  WalletCards,
  UsersRound,
  X,
} from 'lucide-react'
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
            <a href="#roles">For your role</a>
            <a href="#platform">Platform</a>
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
          <div className="section-intro"><p className="eyebrow">WHY CLOUDPILOT</p><h2>One operating layer.<br /><em>Clearer decisions.</em></h2><p>CloudPilot brings delivery, access, usage, and operational signals into one workspace so teams can spend less time coordinating systems and more time acting on what matters.</p></div>
          <div className="feature-grid"><article><span className="feature-number">01</span><h3>Coordinate delivery</h3><p>Connect projects, tasks, owners, and deadlines so progress is visible across the organization.</p><a href="#platform">View work management <span>↗</span></a></article><article><span className="feature-number">02</span><h3>Control access</h3><p>Give each organization its own secure boundary and each person the permissions required for their role.</p><a href="#security">View security model <span>↗</span></a></article><article><span className="feature-number">03</span><h3>Operate with evidence</h3><p>Use activity, observability, workload, and cost signals to identify issues before they become interruptions.</p><a href="#workflow">See the operating flow <span>↗</span></a></article></div>
        </section>

        <section className="roles-section" id="roles">
          <div className="roles-heading">
            <div>
              <p className="eyebrow"><span className="eyebrow-dot" /> BUILT AROUND YOUR ROLE</p>
              <h2>Everyone gets<br /><em>the right view.</em></h2>
            </div>
            <p>Access follows responsibility. Each role sees the controls, information, and actions needed to contribute without exposing what it should not.</p>
          </div>
          <div className="role-layout">
            <div className="role-stack">
              <article className="role-card role-owner">
                <div className="role-icon"><ShieldCheck size={21} aria-hidden="true" /></div>
                <div className="role-card-copy"><span className="role-label">OWNER</span><h3>Set direction and guardrails</h3><p>Own the workspace, shape access, manage plans, and keep spend visible with Cost Intelligence.</p><div className="role-features"><span><Check size={13} /> Manage members and roles</span><span><Check size={13} /> Configure budgets</span><span><Check size={13} /> See workspace health</span></div></div>
                <ArrowRight className="role-arrow" size={18} aria-hidden="true" />
              </article>
              <article className="role-card role-admin">
                <div className="role-icon"><Settings2 size={21} aria-hidden="true" /></div>
                <div className="role-card-copy"><span className="role-label">ADMIN</span><h3>Keep the operation moving</h3><p>Coordinate people, projects, and tasks with the permissions to maintain a healthy team rhythm.</p><div className="role-features"><span><Check size={13} /> Invite and organize users</span><span><Check size={13} /> Assign workspace roles</span><span><Check size={13} /> Manage project flow</span></div></div>
                <ArrowRight className="role-arrow" size={18} aria-hidden="true" />
              </article>
              <article className="role-card role-member">
                <div className="role-icon"><FolderKanban size={21} aria-hidden="true" /></div>
                <div className="role-card-copy"><span className="role-label">MEMBER</span><h3>Focus on meaningful work</h3><p>See the projects and tasks that matter, collaborate with context, and keep progress moving forward.</p><div className="role-features"><span><Check size={13} /> Work in assigned projects</span><span><Check size={13} /> Create and update tasks</span><span><Check size={13} /> View team progress</span></div></div>
                <ArrowRight className="role-arrow" size={18} aria-hidden="true" />
              </article>
            </div>
            <div className="role-visual" aria-label="CloudPilot role-based workspace preview">
              <div className="role-visual-grid" />
              <div className="role-visual-orbit orbit-a" />
              <div className="role-visual-orbit orbit-b" />
              <div className="role-visual-core"><Sparkles size={22} aria-hidden="true" /><span>ONE<br />SHARED<br /><b>WORKSPACE</b></span></div>
              <div className="role-chip chip-owner"><ShieldCheck size={14} /> Owner <b>full control</b></div>
              <div className="role-chip chip-admin"><UsersRound size={14} /> Admin <b>team flow</b></div>
              <div className="role-chip chip-member"><ListTodo size={14} /> Member <b>focused work</b></div>
              <div className="role-visual-caption"><Activity size={14} /><span>Permissions stay clear as teams grow.</span></div>
            </div>
          </div>
        </section>

        <section className="capabilities-section" id="platform">
          <div className="capabilities-heading">
            <div><p className="eyebrow"><span className="eyebrow-dot" /> THE CLOUDPILOT PLATFORM</p><h2>Everything your<br /><em>team needs to run.</em></h2></div>
            <p>One operating layer for the work, people, money, and signals behind a modern multi-tenant business.</p>
          </div>
          <div className="capability-grid">
            <article className="capability-card capability-wide capability-blue"><div className="capability-icon"><FolderKanban size={20} /></div><div><span>WORK MANAGEMENT</span><h3>Projects and tasks that stay connected</h3><p>Turn initiatives into clear projects, assign work, track statuses, and keep delivery visible from the first task to the final handoff.</p></div><div className="capability-mini-board"><span /><span /><span /><b>12 active projects</b></div></article>
            <article className="capability-card capability-green"><div className="capability-icon"><UsersRound size={20} /></div><span>TEAM OPERATIONS</span><h3>People, roles, and invitations</h3><p>Bring the right people into each workspace with tenant-aware membership and role-based access.</p><div className="capability-avatars"><i>AM</i><i>SK</i><i>JR</i><i>+8</i></div></article>
            <article className="capability-card capability-coral"><div className="capability-icon"><ChartNoAxesCombined size={20} /></div><span>COST INTELLIGENCE</span><h3>Understand spend before it surprises you</h3><p>See budgets, projections, service breakdowns, anomaly signals, and advisory insights in one view.</p><div className="capability-sparkline"><LineChart size={72} /></div></article>
            <article className="capability-card capability-dark"><div className="capability-icon"><BarChart3 size={20} /></div><span>OBSERVABILITY</span><h3>Prometheus and Grafana visibility</h3><p>Monitor service health, requests, latency, JVM, database pools, Kafka, Redis, and aggregate cost signals through dashboards built for CloudPilot.</p><div className="capability-monitor"><Gauge size={18} /><b>CloudPilot Overview</b><small>30 dashboard panels</small></div></article>
            <article className="capability-card capability-purple"><div className="capability-icon"><Activity size={20} /></div><span>ACTIVITY AND AUDIT</span><h3>Know what changed and when</h3><p>Follow workspace activity and preserve operational context across the Core and independent Audit Service.</p></article>
            <article className="capability-card capability-gold"><div className="capability-icon"><WalletCards size={20} /></div><span>SUBSCRIPTIONS</span><h3>Plans that fit the workspace</h3><p>Track projects, members, invitations, feature access, and plan limits without losing sight of usage.</p></article>
            <article className="capability-card capability-wide capability-teal"><div className="capability-icon"><LockKeyhole size={20} /></div><div><span>MULTI-TENANT SECURITY</span><h3>Isolation and access control by design</h3><p>Authenticated tenant context, permission-aware screens, protected APIs, and no tenant identity in aggregate Prometheus labels.</p></div><div className="capability-security-lines"><span><Globe2 size={14} /> Tenant scoped</span><span><ShieldCheck size={14} /> Role protected</span></div></article>
            <article className="capability-card capability-slate"><div className="capability-icon"><MonitorCog size={20} /></div><span>DEPLOYMENT READY</span><h3>Compose today, Kubernetes when ready</h3><p>Run locally with Docker Compose or move to Kubernetes with health checks, persistent services, and observability components.</p></article>
          </div>
        </section>

        <section className="workflow-section journey-section" id="workflow">
          <div className="journey-intro"><p className="eyebrow">HOW IT WORKS</p><h2>From setup<br /><em>to signal.</em></h2><p>CloudPilot follows the natural operating model of a growing organization: establish control, coordinate the team, deliver the work, and use reliable signals to improve the system.</p></div>
          <div className="journey-flow">
            <article className="journey-step journey-owner"><div className="journey-step-marker"><span>01</span></div><div className="journey-step-icon"><ShieldCheck size={21} /></div><div className="journey-step-copy"><span className="journey-role">THE CUSTOMER / OWNER</span><h3>Creates the organization</h3><p>The customer starts a workspace, chooses a plan, and brings the company into one secure home.</p><div className="journey-actions"><span>Creates organization</span><span>Chooses subscription</span><span>Sets workspace rules</span></div></div><div className="journey-output"><ArrowDown size={16} /><b>Invites the team</b></div></article>
            <article className="journey-step journey-admin"><div className="journey-step-marker"><span>02</span></div><div className="journey-step-icon"><UsersRound size={21} /></div><div className="journey-step-copy"><span className="journey-role">THE ADMIN</span><h3>Sets the operation in motion</h3><p>The admin invites people, assigns roles, organizes projects, and keeps the workspace ready for action.</p><div className="journey-actions"><span>Invites members</span><span>Assigns access</span><span>Organizes projects</span></div></div><div className="journey-output"><ArrowDown size={16} /><b>Creates clarity</b></div></article>
            <article className="journey-step journey-member"><div className="journey-step-marker"><span>03</span></div><div className="journey-step-icon"><FolderKanban size={21} /></div><div className="journey-step-copy"><span className="journey-role">THE MEMBER</span><h3>Turns plans into progress</h3><p>Members see their work, update tasks, collaborate with context, and keep delivery moving without noise.</p><div className="journey-actions"><span>Works on tasks</span><span>Updates progress</span><span>Collaborates clearly</span></div></div><div className="journey-output"><ArrowDown size={16} /><b>Creates signals</b></div></article>
            <div className="journey-outcome"><div className="outcome-icon"><Activity size={21} /></div><div><span>THE CLOUDPILOT OUTCOME</span><h3>One connected view of the business</h3><p>Tenant-aware security keeps organizations separated. Noisy-neighbor detection surfaces unusual workload pressure. Cost Intelligence explains spend and budget health. Observability shows what the platform is doing, so teams can act early.</p><div className="outcome-pills"><span><LockKeyhole size={13} /> Multi-tenant security</span><span><Gauge size={13} /> Noisy-neighbor signals</span><span><ChartNoAxesCombined size={13} /> Cost Intelligence</span><span><BarChart3 size={13} /> Prometheus + Grafana</span></div></div></div>
          </div>
        </section>

        <section className="security-section" id="security"><div><p className="eyebrow">TRUSTED OPERATIONS</p><h2>Clarity with<br /><em>guardrails built in.</em></h2></div><div className="security-points"><p><span><ShieldCheck size={19} /></span><strong>Tenant boundaries</strong><small>Keep every organization’s data and operating context isolated as the platform scales.</small></p><p><span><UsersRound size={19} /></span><strong>Permission-aware work</strong><small>Make responsibility explicit with role-based access across the workspace.</small></p><p><span><ChartNoAxesCombined size={19} /></span><strong>Signals before surprises</strong><small>Use activity, workload, cost, and service health signals to guide the next decision.</small></p></div></section>

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
