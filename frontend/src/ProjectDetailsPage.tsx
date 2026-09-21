import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ArrowLeft, Pencil, Plus, Save, Trash2, X } from 'lucide-react'
import './ProjectDetailsPage.css'

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

type ProjectDetailsPageProps = {
  projectId: number
  token: string
  refreshKey: number
  onDataChanged: () => void
  onBack: () => void
  hasPermission: (permission: string) => boolean
}

function ProjectDetailsPage({
  projectId,
  token,
  refreshKey,
  onDataChanged,
  onBack,
  hasPermission,
}: ProjectDetailsPageProps) {
  const [project, setProject] = useState<Project | null>(null)
  const [tasks, setTasks] = useState<Task[]>([])
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [priority, setPriority] = useState('MEDIUM')
  const [dueDate, setDueDate] = useState('')
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [updatingTaskId, setUpdatingTaskId] = useState<number | null>(null)
  const [editingTask, setEditingTask] = useState<Task | null>(null)
  const [deletingTaskId, setDeletingTaskId] = useState<number | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadProjectDetails() {
      try {
        setLoading(true)
        setError('')

        const projectResponse = await fetch(
          `/api/projects/${projectId}`,
          { headers: { Authorization: `Bearer ${token}` } },
        )
        if (!projectResponse.ok) throw new Error('Failed to load project')
        setProject(await projectResponse.json())

        const tasksResponse = await fetch(
          `/api/projects/${projectId}/tasks`,
          { headers: { Authorization: `Bearer ${token}` } },
        )
        if (!tasksResponse.ok) throw new Error('Failed to load tasks')
        setTasks(await tasksResponse.json())
      } catch (error) {
        setError(error instanceof Error ? error.message : 'Something went wrong')
      } finally {
        setLoading(false)
      }
    }

    loadProjectDetails()
  }, [projectId, token, refreshKey])

  function startEditingTask(task: Task) {
    setEditingTask(task)
    setTitle(task.title)
    setDescription(task.description || '')
    setPriority(task.priority)
    setDueDate(task.dueDate ? task.dueDate.slice(0, 16) : '')
  }

  function cancelEditingTask() {
    setEditingTask(null)
    setTitle('')
    setDescription('')
    setPriority('MEDIUM')
    setDueDate('')
  }

  async function handleTaskSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setCreating(true)
    setError('')

    try {
      const isEditing = editingTask !== null
      const url = isEditing
        ? `/api/tasks/${editingTask.id}`
        : `/api/projects/${projectId}/tasks`

      const response = await fetch(url, {
        method: isEditing ? 'PUT' : 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(
          isEditing
            ? {
                title,
                description,
                status: editingTask.status,
                priority,
                dueDate: dueDate === '' ? null : dueDate,
              }
            : {
                title,
                description,
                priority,
                dueDate: dueDate === '' ? null : dueDate,
              },
        ),
      })

      const data = await response.json()
      if (!response.ok) throw new Error(data.message || 'Failed to save task')

      const savedTask = data as Task
      setTasks((currentTasks) =>
        isEditing
          ? currentTasks.map((task) =>
              task.id === savedTask.id ? savedTask : task,
            )
          : [...currentTasks, savedTask],
      )
      cancelEditingTask()
      onDataChanged()
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Something went wrong')
    } finally {
      setCreating(false)
    }
  }

  async function handleTaskStatusChange(task: Task, newStatus: string) {
    try {
      setUpdatingTaskId(task.id)
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

      const updatedTask = data as Task
      setTasks((currentTasks) =>
        currentTasks.map((currentTask) =>
          currentTask.id === updatedTask.id ? updatedTask : currentTask,
        ),
      )
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Something went wrong')
    } finally {
      setUpdatingTaskId(null)
    }
  }

  async function handleDeleteTask(taskId: number) {
    if (!window.confirm('Are you sure you want to delete this task?')) return

    try {
      setDeletingTaskId(taskId)
      setError('')

      const response = await fetch(`/api/tasks/${taskId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!response.ok) throw new Error('Failed to delete task')

      setTasks((currentTasks) =>
        currentTasks.filter((task) => task.id !== taskId),
      )
      if (editingTask?.id === taskId) cancelEditingTask()
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Something went wrong')
    } finally {
      setDeletingTaskId(null)
    }
  }

  if (loading) return <p>Loading project...</p>
  if (!project) return <p>Project not found.</p>

  return (
    <div className="project-details-page">
      <button className="back-button" onClick={onBack}>
        <ArrowLeft size={16} aria-hidden="true" />
        Back to Projects
      </button>

      <div className="project-details-header">
        <div className="project-heading-row">
          <h1>{project.name}</h1>
          <span className="details-status">{project.status}</span>
        </div>
        <p>{project.description || 'No project description'}</p>
        <span className="project-meta">
          Project #{project.id} • Created by {project.createdByEmail}
        </span>
      </div>

      {error && <div className="project-details-error">{error}</div>}

      <div className="project-details-grid">
        {hasPermission('TASK_CREATE') || editingTask ? <section className="create-task-card">
          <h2>{editingTask ? 'Edit Task' : 'Create Task'}</h2>
          <p>
            {editingTask
              ? `Editing task #${editingTask.id}`
              : 'Add a task to this project.'}
          </p>

          <form onSubmit={handleTaskSubmit}>
            <div className="task-form-group">
              <label htmlFor="task-title">Task Title</label>
              <input
                id="task-title"
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="Configure AWS"
                required
              />
            </div>

            <div className="task-form-group">
              <label htmlFor="task-description">Description</label>
              <textarea
                id="task-description"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="Describe the task..."
                rows={4}
              />
            </div>

            <div className="task-form-group">
              <label htmlFor="priority">Priority</label>
              <select
                id="priority"
                value={priority}
                onChange={(event) => setPriority(event.target.value)}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
              </select>
            </div>

            <div className="task-form-group">
              <label htmlFor="due-date">Due Date</label>
              <input
                id="due-date"
                type="datetime-local"
                value={dueDate}
                onChange={(event) => setDueDate(event.target.value)}
              />
            </div>

            <button type="submit" className="create-task-button" disabled={creating}>
              {editingTask ? <Save size={16} aria-hidden="true" /> : <Plus size={16} aria-hidden="true" />}
              {creating
                ? 'Saving...'
                : editingTask
                  ? 'Save Changes'
                  : 'Create Task'}
            </button>

            {editingTask && (
              <button
                type="button"
                className="cancel-edit-button"
                onClick={cancelEditingTask}
              >
                <X size={16} aria-hidden="true" />
                Cancel Edit
              </button>
            )}
          </form>
        </section>
        : null}

        <section className="tasks-card">
          <div className="tasks-header">
            <h2>Tasks</h2>
            <p>
              {tasks.length} task{tasks.length === 1 ? '' : 's'}
            </p>
          </div>

          {tasks.length === 0 ? (
            <div className="tasks-empty">No tasks yet.</div>
          ) : (
            <div className="task-list">
              {tasks.map((task) => (
                <div className="task-row" key={task.id}>
                  <div className="task-main">
                    <h3>{task.title}</h3>
                    <p>{task.description || 'No description'}</p>
                    <span>Task #{task.id}</span>
                  </div>

                  <div className="task-right">
                    <div className="task-badges">
                      <span
                        className={`priority-badge priority-${task.priority.toLowerCase()}`}
                      >
                        <span className="priority-dot" aria-hidden="true" />
                        {task.priority}
                      </span>
                      {hasPermission('TASK_UPDATE') ? <select
                        className="task-status-select"
                        value={task.status}
                        disabled={updatingTaskId === task.id}
                        onChange={(event) =>
                          handleTaskStatusChange(task, event.target.value)
                        }
                      >
                        <option value="TODO">TODO</option>
                        <option value="IN_PROGRESS">IN PROGRESS</option>
                        <option value="DONE">DONE</option>
                      </select> : <span className="task-status">{task.status}</span>}
                    </div>

                    <div className="task-actions">
                      {hasPermission('TASK_UPDATE') && <button
                        className="edit-task-button"
                        onClick={() => startEditingTask(task)}
                      >
                        <Pencil size={14} aria-hidden="true" />
                        Edit
                      </button>}
                      {hasPermission('TASK_DELETE') && <button
                        className="delete-task-button"
                        disabled={deletingTaskId === task.id}
                        onClick={() => handleDeleteTask(task.id)}
                      >
                        <Trash2 size={14} aria-hidden="true" />
                        {deletingTaskId === task.id ? 'Deleting...' : 'Delete'}
                      </button>}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}

export default ProjectDetailsPage
