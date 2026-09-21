import { useEffect, useState } from 'react'
import './TasksPage.css'

type Project = {
  id: number
  name: string
}

type Task = {
  id: number
  projectId: number
  title: string
  description: string
  status: string
  priority: string
  assignedTo: number | null
  createdBy: number
  createdByEmail: string
  dueDate: string | null
  createdAt: string
  updatedAt: string
}

type TaskWithProject = Task & {
  projectName: string
}

type TasksPageProps = {
  token: string
  hasPermission: (permission: string) => boolean
}

function TasksPage({ token, hasPermission }: TasksPageProps) {
  const [tasks, setTasks] = useState<TaskWithProject[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')

  useEffect(() => {
    async function loadTasks() {
      try {
        setLoading(true)
        setError('')

        const projectResponse = await fetch('/api/projects', {
          headers: { Authorization: `Bearer ${token}` },
        })
        if (!projectResponse.ok) throw new Error('Failed to load projects')

        const projects: Project[] = await projectResponse.json()
        const taskGroups = await Promise.all(
          projects.map(async (project) => {
            const response = await fetch(
              `/api/projects/${project.id}/tasks`,
              { headers: { Authorization: `Bearer ${token}` } },
            )
            if (!response.ok) {
              throw new Error(`Failed to load tasks for ${project.name}`)
            }

            const projectTasks: Task[] = await response.json()
            return projectTasks.map((task) => ({
              ...task,
              projectName: project.name,
            }))
          }),
        )

        setTasks(taskGroups.flat())
      } catch (error) {
        setError(error instanceof Error ? error.message : 'Something went wrong')
      } finally {
        setLoading(false)
      }
    }

    loadTasks()
  }, [token])

  async function handleStatusChange(task: TaskWithProject, newStatus: string) {
    try {
      setError('')
      const response = await fetch(`/api/tasks/${task.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          title: task.title,
          description: task.description,
          status: newStatus,
          priority: task.priority,
          dueDate: task.dueDate,
        }),
      })

      const data = await response.json()
      if (!response.ok) throw new Error(data.message || 'Failed to update task')

      setTasks((currentTasks) =>
        currentTasks.map((currentTask) =>
          currentTask.id === task.id
            ? { ...currentTask, ...(data as Task) }
            : currentTask,
        ),
      )
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Something went wrong')
    }
  }

  const filteredTasks =
    statusFilter === 'ALL'
      ? tasks
      : tasks.filter((task) => task.status === statusFilter)

  return (
    <div className="tasks-page">
      <div className="tasks-page-header">
        <div>
          <h1>Tasks</h1>
          <p>View tasks across all your projects.</p>
        </div>

        <select
          className="tasks-filter"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
        >
          <option value="ALL">All Tasks</option>
          <option value="TODO">Todo</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="DONE">Done</option>
        </select>
      </div>

      {error && <div className="tasks-page-error">{error}</div>}

      <div className="tasks-summary">
        {[
          ['Total', tasks.length],
          ['Todo', tasks.filter((task) => task.status === 'TODO').length],
          [
            'In Progress',
            tasks.filter((task) => task.status === 'IN_PROGRESS').length,
          ],
          ['Done', tasks.filter((task) => task.status === 'DONE').length],
        ].map(([label, count]) => (
          <div className="tasks-summary-card" key={label}>
            <span>{label}</span>
            <strong>{count}</strong>
          </div>
        ))}
      </div>

      <section className="all-tasks-card">
        {loading ? (
          <p>Loading tasks...</p>
        ) : filteredTasks.length === 0 ? (
          <div className="tasks-page-empty">No tasks found.</div>
        ) : (
          <div className="all-tasks-list">
            {filteredTasks.map((task) => (
              <div className="all-task-row" key={task.id}>
                <div className="all-task-main">
                  <h3>{task.title}</h3>
                  <p>{task.description || 'No description'}</p>
                  <span>
                    {task.projectName} • Task #{task.id}
                  </span>
                </div>

                <div className="all-task-right">
                  <span
                    className={`global-priority global-priority-${task.priority.toLowerCase()}`}
                  >
                    {task.priority}
                  </span>
                  {hasPermission('TASK_UPDATE') ? <select
                    className="global-status-select"
                    value={task.status}
                    onChange={(event) =>
                      handleStatusChange(task, event.target.value)
                    }
                  >
                    <option value="TODO">TODO</option>
                    <option value="IN_PROGRESS">IN PROGRESS</option>
                    <option value="DONE">DONE</option>
                  </select> : <span className="task-status-readonly">{task.status}</span>}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

export default TasksPage
