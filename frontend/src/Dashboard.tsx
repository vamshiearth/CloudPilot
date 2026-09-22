import { useEffect, useState } from 'react'
import {
  Activity,
  BarChart3,
  CreditCard,
  ChartNoAxesCombined,
  FolderKanban,
  LayoutDashboard,
  ListTodo,
  PanelLeftClose,
  PanelLeftOpen,
  RadioTower,
  Settings,
  UserRound,
  Users,
} from 'lucide-react'
import ProjectDetailsPage from './ProjectDetailsPage'
import ProjectsPage from './ProjectsPage'
import TasksPage from './TasksPage'
import UsersPage from './UsersPage'
import SubscriptionPage from './SubscriptionPage'
import ActivityPage from './ActivityPage'
import ProfilePage from './ProfilePage'
import SettingsPage from './SettingsPage'
import CostIntelligencePage from './CostIntelligencePage'
import './Dashboard.css'

type User = {
  id: number
  email: string
  firstName: string
  lastName: string
  status: string
  tenantId: number | null
  tenantName: string | null
}

type Project = {
  id: number
  name: string
  description: string
  status: string
  createdBy: number
  createdByEmail: string
  createdAt: string
  updatedAt: string
}

type Task = {
  id: number
  projectId: number
  title: string
  status: string
}

type AuthContext = {
  userId: number
  email: string
  tenantId: number
  role: string
  permissions: string[]
}

type Tenant = {
  id: number
  name: string
  slug: string
  status: string
}

type DashboardProps = {
  user: User
  token: string
  onLogout: () => void
}

function Dashboard({ user, token, onLogout }: DashboardProps) {
  const [activePage, setActivePage] = useState<
    | 'dashboard'
    | 'projects'
    | 'projectDetails'
    | 'tasks'
    | 'users'
    | 'subscription'
    | 'activity'
    | 'profile'
    | 'settings'
    | 'costs'
  >('dashboard')
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(
    null,
  )
  const [tenant, setTenant] = useState<Tenant | null>(null)
  const [authContext, setAuthContext] = useState<AuthContext | null>(null)
  const [projects, setProjects] = useState<Project[]>([])
  const [taskCount, setTaskCount] = useState(0)
  const [dataVersion, setDataVersion] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [sidebarOpen, setSidebarOpen] = useState(
    () => typeof window === 'undefined' || window.innerWidth > 800,
  )
  const [theme, setTheme] = useState<'light' | 'dark'>(
    () => (localStorage.getItem('cloudpilot_theme') as 'light' | 'dark') || 'dark',
  )

  function refreshDashboard() {
    setDataVersion((version) => version + 1)
  }

  function changeTheme(nextTheme: 'light' | 'dark') {
    setTheme(nextTheme)
    localStorage.setItem('cloudpilot_theme', nextTheme)
  }

  useEffect(() => {
    async function loadDashboard() {
      try {
        const contextResponse = await fetch(
          '/api/auth/context',
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          },
        )

        if (!contextResponse.ok) {
          throw new Error('Failed to load authentication context')
        }

        setAuthContext(await contextResponse.json())

        const tenantResponse = await fetch(
          '/api/tenants/current',
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          },
        )

        if (!tenantResponse.ok) {
          throw new Error('Failed to load organization')
        }

        setTenant(await tenantResponse.json())

        const projectResponse = await fetch(
          '/api/projects',
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          },
        )

        if (!projectResponse.ok) {
          throw new Error('Failed to load projects')
        }

        const projectData: Project[] = await projectResponse.json()
        setProjects(projectData)

        const taskResponses = await Promise.all(
          projectData.map((project) =>
            fetch(
              `/api/projects/${project.id}/tasks`,
              {
                headers: {
                  Authorization: `Bearer ${token}`,
                },
              },
            ),
          ),
        )

        let totalTasks = 0

        for (const response of taskResponses) {
          if (response.ok) {
            const tasks: Task[] = await response.json()
            totalTasks += tasks.length
          }
        }

        setTaskCount(totalTasks)
      } catch (error) {
        setError(
          error instanceof Error ? error.message : 'Something went wrong',
        )
      } finally {
        setLoading(false)
      }
    }

    loadDashboard()
  }, [token, dataVersion])

  const canViewUsers = authContext?.permissions.includes('USER_READ') ?? false
  const hasPermission = (permission: string) =>
    authContext?.permissions.includes(permission) ?? false

  return (
    <div
      className={sidebarOpen ? 'dashboard-layout' : 'dashboard-layout sidebar-collapsed'}
      data-theme={theme}
    >
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="sidebar-logo">☁</div>
          <span className="sidebar-brand-name">CloudPilot</span>
        </div>

        <button
          className="sidebar-toggle"
          type="button"
          aria-label={sidebarOpen ? 'Collapse sidebar' : 'Expand sidebar'}
          title={sidebarOpen ? 'Collapse sidebar' : 'Expand sidebar'}
          onClick={() => setSidebarOpen((open) => !open)}
        >
          {sidebarOpen ? <PanelLeftClose size={16} /> : <PanelLeftOpen size={16} />}
        </button>

        <nav className="sidebar-nav" aria-label="Main navigation">
          {hasPermission('PROJECT_READ') && <button
            className={
              activePage === 'dashboard' ? 'nav-item active' : 'nav-item'
            }
            title="Dashboard"
            onClick={() => setActivePage('dashboard')}
          >
            <LayoutDashboard className="nav-icon" size={18} aria-hidden="true" />
            <span className="nav-label">Dashboard</span>
          </button>}
          {hasPermission('TASK_READ') && <button
            className={
              activePage === 'projects' || activePage === 'projectDetails'
                ? 'nav-item active'
                : 'nav-item'
            }
            title="Projects"
            onClick={() => setActivePage('projects')}
          >
            <FolderKanban className="nav-icon" size={18} aria-hidden="true" />
            <span className="nav-label">Projects</span>
          </button>}
          <button
            className={activePage === 'tasks' ? 'nav-item active' : 'nav-item'}
            title="Tasks"
            onClick={() => setActivePage('tasks')}
          >
            <ListTodo className="nav-icon" size={18} aria-hidden="true" />
            <span className="nav-label">Tasks</span>
          </button>
          {canViewUsers && (
            <button
              className={activePage === 'users' ? 'nav-item active' : 'nav-item'}
              title="Users"
              onClick={() => setActivePage('users')}
            >
              <Users className="nav-icon" size={18} aria-hidden="true" />
              <span className="nav-label">Users</span>
            </button>
          )}
          {hasPermission('BILLING_READ') && (
            <button
              className={activePage === 'subscription' ? 'nav-item active' : 'nav-item'}
              title="Subscription"
              onClick={() => setActivePage('subscription')}
            >
              <CreditCard className="nav-icon" size={18} aria-hidden="true" />
              <span className="nav-label">Subscription</span>
            </button>
          )}
          {hasPermission('BILLING_READ') && (
            <button
              className={activePage === 'costs' ? 'nav-item active' : 'nav-item'}
              title="Cost Intelligence"
              onClick={() => setActivePage('costs')}
            >
              <ChartNoAxesCombined className="nav-icon" size={18} aria-hidden="true" />
              <span className="nav-label">Cost Intelligence</span>
            </button>
          )}
          {canViewUsers && (
            <button
              className={activePage === 'activity' ? 'nav-item active' : 'nav-item'}
              title="Activity"
              onClick={() => setActivePage('activity')}
            >
              <Activity className="nav-icon" size={18} aria-hidden="true" />
              <span className="nav-label">Activity</span>
            </button>
          )}
          <button
            className={activePage === 'profile' ? 'nav-item active' : 'nav-item'}
            title="Profile"
            onClick={() => setActivePage('profile')}
          >
            <UserRound className="nav-icon" size={18} aria-hidden="true" />
            <span className="nav-label">Profile</span>
          </button>
          {hasPermission('SETTINGS_READ') && (
            <button
              className={activePage === 'settings' ? 'nav-item active' : 'nav-item'}
              title="Settings"
              onClick={() => setActivePage('settings')}
            >
              <Settings className="nav-icon" size={18} aria-hidden="true" />
              <span className="nav-label">Settings</span>
            </button>
          )}
        </nav>

        <div className="sidebar-observability">
          <span className="sidebar-section-label">Observability</span>
          <a
            className="observability-link"
            href="http://localhost:30090"
            target="_blank"
            rel="noreferrer"
            title="Open Prometheus"
          >
            <BarChart3 className="nav-icon" size={17} aria-hidden="true" />
            <span className="nav-label">Prometheus</span>
          </a>
          <a
            className="observability-link"
            href="http://localhost:30091"
            target="_blank"
            rel="noreferrer"
            title="Open Grafana"
          >
            <Activity className="nav-icon" size={17} aria-hidden="true" />
            <span className="nav-label">Grafana</span>
          </a>
          <a
            className="observability-link"
            href="http://localhost:9092"
            target="_blank"
            rel="noreferrer"
            title="Open Kafka endpoint"
          >
            <RadioTower className="nav-icon" size={17} aria-hidden="true" />
            <span className="nav-label">Kafka</span>
          </a>
        </div>

        <div className="sidebar-bottom">
          <span className="tenant-label">
            {tenant ? tenant.name : 'CloudPilot'}
          </span>
          <p>
            {user.firstName} {user.lastName}
          </p>
          <span className="sidebar-user-email">{user.email}</span>
        </div>
      </aside>

      <main className="dashboard-main">
        {activePage === 'projects' ? (
          <ProjectsPage
            token={token}
            hasPermission={hasPermission}
            refreshKey={dataVersion}
            onDataChanged={refreshDashboard}
            onSelectProject={(projectId) => {
              setSelectedProjectId(projectId)
              setActivePage('projectDetails')
            }}
          />
        ) : activePage === 'projectDetails' && selectedProjectId !== null ? (
          <ProjectDetailsPage
            projectId={selectedProjectId}
            token={token}
            hasPermission={hasPermission}
            refreshKey={dataVersion}
            onDataChanged={refreshDashboard}
            onBack={() => setActivePage('projects')}
          />
        ) : activePage === 'tasks' ? (
          <TasksPage token={token} hasPermission={hasPermission} />
        ) : activePage === 'users' ? (
          <UsersPage token={token} />
        ) : activePage === 'subscription' ? (
          <SubscriptionPage token={token} />
        ) : activePage === 'activity' ? (
          <ActivityPage token={token} />
        ) : activePage === 'profile' ? (
          <ProfilePage user={user} tenant={tenant} authContext={authContext} />
        ) : activePage === 'settings' ? (
          <SettingsPage
            tenant={tenant}
            authContext={authContext}
            theme={theme}
            onThemeChange={changeTheme}
          />
        ) : activePage === 'costs' ? (
          <CostIntelligencePage token={token} role={authContext?.role || null} />
        ) : (
          <>
            <header className="dashboard-header">
              <div>
                <p className="welcome-small">Welcome back,</p>
                <h1>
                  {user.firstName} {user.lastName}
                </h1>
                {tenant && <p className="tenant-name">{tenant.name}</p>}
                {authContext && (
                  <span className="current-role-badge">
                    {authContext.role}
                  </span>
                )}
              </div>

              <button className="logout-button" onClick={onLogout}>
                Logout
              </button>
            </header>

            {error && <div className="dashboard-error">{error}</div>}

            <section className="stats-grid">
              <div className="stat-card">
                <span>Projects</span>
                <strong>{loading ? '...' : projects.length}</strong>
                <p>Active workspace projects</p>
              </div>

              <div className="stat-card">
                <span>Tasks</span>
                <strong>{loading ? '...' : taskCount}</strong>
                <p>Total project tasks</p>
              </div>

              <div className="stat-card">
                <span>Organization</span>
                <strong className="tenant-card-name">
                  {tenant ? tenant.name : '...'}
                </strong>
                <p>{tenant ? tenant.status : 'Loading...'}</p>
              </div>

              <div className="stat-card">
                <span>Account</span>
                <strong>{user.status}</strong>
                <p>{user.email}</p>
              </div>
            </section>

            <section className="projects-panel">
              <div className="panel-header">
                <div>
                  <h2>Recent Projects</h2>
                  <p>Projects currently available in CloudPilot.</p>
                </div>
              </div>

              {loading ? (
                <p>Loading projects...</p>
              ) : projects.length === 0 ? (
                <div className="empty-state">No projects yet.</div>
              ) : (
                <div className="project-list">
                  {projects.map((project) => (
                    <div className="project-row" key={project.id}>
                      <div>
                        <h3>{project.name}</h3>
                        <p>{project.description || 'No description'}</p>
                      </div>
                      <span className="status-badge">{project.status}</span>
                    </div>
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  )
}

export default Dashboard
