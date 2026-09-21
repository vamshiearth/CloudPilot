import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import './ProjectsPage.css'

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

type ProjectsPageProps = {
  token: string
  refreshKey: number
  onDataChanged: () => void
  onSelectProject: (projectId: number) => void
  hasPermission: (permission: string) => boolean
}

function ProjectsPage({
  token,
  refreshKey,
  onDataChanged,
  onSelectProject,
  hasPermission,
}: ProjectsPageProps) {
  const [projects, setProjects] = useState<Project[]>([])
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [editingProject, setEditingProject] = useState<Project | null>(null)
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [deletingProjectId, setDeletingProjectId] = useState<number | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadProjects() {
      try {
        setLoading(true)
        const response = await fetch('/api/projects', {
          headers: { Authorization: `Bearer ${token}` },
        })

        if (!response.ok) throw new Error('Failed to load projects')
        setProjects(await response.json())
      } catch (error) {
        setError(error instanceof Error ? error.message : 'Something went wrong')
      } finally {
        setLoading(false)
      }
    }

    loadProjects()
  }, [token, refreshKey])

  function startEditingProject(project: Project) {
    setEditingProject(project)
    setName(project.name)
    setDescription(project.description || '')
  }

  function cancelEditingProject() {
    setEditingProject(null)
    setName('')
    setDescription('')
  }

  async function handleProjectSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setCreating(true)

    try {
      const isEditing = editingProject !== null
      const url = isEditing
        ? `/api/projects/${editingProject.id}`
        : '/api/projects'

      const response = await fetch(url, {
        method: isEditing ? 'PUT' : 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(
          isEditing
            ? { name, description, status: editingProject.status }
            : { name, description },
        ),
      })

      const data = await response.json()
      if (!response.ok) throw new Error(data.message || 'Failed to save project')

      const savedProject = data as Project
      setProjects((currentProjects) =>
        isEditing
          ? currentProjects.map((project) =>
              project.id === savedProject.id ? savedProject : project,
            )
          : [savedProject, ...currentProjects],
      )
      cancelEditingProject()
      onDataChanged()
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Something went wrong')
    } finally {
      setCreating(false)
    }
  }

  async function handleDeleteProject(projectId: number) {
    if (!window.confirm('Are you sure you want to delete this project?')) return

    try {
      setDeletingProjectId(projectId)
      setError('')

      const response = await fetch(
        `/api/projects/${projectId}`,
        {
          method: 'DELETE',
          headers: { Authorization: `Bearer ${token}` },
        },
      )

      if (!response.ok) throw new Error('Failed to delete project')

      setProjects((currentProjects) =>
        currentProjects.filter((project) => project.id !== projectId),
      )

      if (editingProject?.id === projectId) cancelEditingProject()
      onDataChanged()
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Something went wrong')
    } finally {
      setDeletingProjectId(null)
    }
  }

  return (
    <div className="projects-page">
      <div className="projects-title">
        <div>
          <h1>Projects</h1>
          <p>Create and manage your CloudPilot projects.</p>
        </div>
      </div>

      {error && <div className="projects-error">{error}</div>}

      <div className="projects-content">
        <section className="create-project-card">
          <h2>{editingProject ? 'Edit Project' : 'Create Project'}</h2>
          <p>
            {editingProject
              ? `Editing project #${editingProject.id}`
              : 'Start a new project in your workspace.'}
          </p>

          {hasPermission('PROJECT_CREATE') || editingProject ? (
          <form onSubmit={handleProjectSubmit}>
            <div className="project-form-group">
              <label htmlFor="project-name">Project Name</label>
              <input
                id="project-name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="Customer Portal"
                required
              />
            </div>

            <div className="project-form-group">
              <label htmlFor="project-description">Description</label>
              <textarea
                id="project-description"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="Describe the project..."
                rows={5}
              />
            </div>

            <button
              className="create-project-button"
              type="submit"
              disabled={creating}
            >
              {creating
                ? 'Saving...'
                : editingProject
                  ? 'Save Changes'
                  : 'Create Project'}
            </button>

            {editingProject && (
              <button
                type="button"
                className="cancel-project-edit"
                onClick={cancelEditingProject}
              >
                Cancel Edit
              </button>
            )}
          </form>
          ) : null}
        </section>

        <section className="projects-list-card">
          <div className="projects-list-header">
            <div>
              <h2>All Projects</h2>
              <p>
                {projects.length} project{projects.length === 1 ? '' : 's'}
              </p>
            </div>
          </div>

          {loading ? (
            <p>Loading projects...</p>
          ) : projects.length === 0 ? (
            <div className="projects-empty">No projects yet.</div>
          ) : (
            <div className="projects-list">
              {projects.map((project) => (
                <div
                  className="projects-list-row clickable-project"
                  key={project.id}
                  onClick={() => onSelectProject(project.id)}
                >
                  <div className="project-info">
                    <h3>{project.name}</h3>
                    <p>{project.description || 'No description'}</p>
                    <span>Created by {project.createdByEmail}</span>
                  </div>

                  <div className="project-row-right">
                    <span className="project-status">{project.status}</span>
                    <span className="project-id">#{project.id}</span>
                    <div className="project-actions">
                      {hasPermission('PROJECT_UPDATE') && <button
                        className="project-edit-button"
                        onClick={(event) => {
                          event.stopPropagation()
                          startEditingProject(project)
                        }}
                      >
                        Edit
                      </button>}
                      {hasPermission('PROJECT_DELETE') && <button
                        className="project-delete-button"
                        disabled={deletingProjectId === project.id}
                        onClick={(event) => {
                          event.stopPropagation()
                          handleDeleteProject(project.id)
                        }}
                      >
                        {deletingProjectId === project.id
                          ? 'Deleting...'
                          : 'Delete'}
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

export default ProjectsPage
