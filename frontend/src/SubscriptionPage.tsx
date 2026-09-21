import { useEffect, useState } from 'react'
import './SubscriptionPage.css'

type SubscriptionUsage = {
  planName: string
  displayName: string
  currentProjects: number
  maxProjects: number
  activeMembers: number
  pendingInvitations: number
  reservedMemberSlots: number
  maxMembers: number
  projectLimitReached: boolean
  memberLimitReached: boolean
  features: string[]
}

type AuthContext = {
  userId: number
  email: string
  tenantId: number
  role: string
  permissions: string[]
}

type PlanOption = {
  id: number
  name: string
  displayName: string
  description: string
  maxMembers: number
  maxProjects: number
}

type SubscriptionPageProps = {
  token: string
}

function SubscriptionPage({ token }: SubscriptionPageProps) {
  const [usage, setUsage] = useState<SubscriptionUsage | null>(null)
  const [authContext, setAuthContext] = useState<AuthContext | null>(null)
  const [plans, setPlans] = useState<PlanOption[]>([])
  const [selectedPlan, setSelectedPlan] = useState<PlanOption | null>(null)
  const [changingPlan, setChangingPlan] = useState(false)
  const [planMessage, setPlanMessage] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadData() {
      try {
        setLoading(true)
        setError('')

        const [subscriptionResponse, contextResponse] = await Promise.all([
          fetch('/api/tenants/current/subscription', {
            headers: { Authorization: `Bearer ${token}` },
          }),
          fetch('/api/auth/context', {
            headers: { Authorization: `Bearer ${token}` },
          }),
        ])

        if (!subscriptionResponse.ok) {
          throw new Error('Failed to load subscription')
        }
        if (!contextResponse.ok) {
          throw new Error('Failed to load authentication context')
        }

        const contextData: AuthContext = await contextResponse.json()
        setUsage(await subscriptionResponse.json())
        setAuthContext(contextData)

        if (contextData.permissions.includes('BILLING_READ')) {
          const plansResponse = await fetch(
            '/api/tenants/current/subscription/plans',
            { headers: { Authorization: `Bearer ${token}` } },
          )
          if (!plansResponse.ok) throw new Error('Failed to load subscription plans')
          setPlans(await plansResponse.json())
        }
      } catch (error) {
        setError(error instanceof Error ? error.message : 'Failed to load subscription')
      } finally {
        setLoading(false)
      }
    }

    loadData()
  }, [token])

  const percentage = (current: number, maximum: number) =>
    maximum <= 0 ? 0 : Math.min(100, Math.round((current / maximum) * 100))

  if (loading) {
    return <div className="subscription-page"><p>Loading subscription...</p></div>
  }

  if (error || !usage) {
    return (
      <div className="subscription-page">
        <div className="subscription-error">{error || 'Subscription information unavailable'}</div>
      </div>
    )
  }

  const projectPercentage = percentage(usage.currentProjects, usage.maxProjects)
  const memberPercentage = percentage(usage.reservedMemberSlots, usage.maxMembers)
  const canManageBilling = authContext?.permissions.includes('BILLING_UPDATE') ?? false

  async function changePlan() {
    if (!selectedPlan) return

    try {
      setChangingPlan(true)
      setError('')
      setPlanMessage('')

      const response = await fetch(
        '/api/tenants/current/subscription/plan',
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ plan: selectedPlan.name }),
        },
      )

      const data = await response.json().catch(() => null)
      if (!response.ok) {
        throw new Error(data?.message || 'Failed to change subscription plan')
      }

      setUsage(data)
      setPlanMessage(`Plan changed to ${selectedPlan.displayName}.`)
      setSelectedPlan(null)
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to change plan')
    } finally {
      setChangingPlan(false)
    }
  }

  return (
    <div className="subscription-page">
      <div className="subscription-header">
        <div>
          <p className="subscription-eyebrow">SUBSCRIPTION</p>
          <h1>Plan &amp; Usage</h1>
          <p>View your current CloudPilot plan and organization usage.</p>
        </div>
        <div className="current-plan-badge">{usage.displayName}</div>
      </div>

      <div className="plan-card">
        <div className="plan-card-top">
          <div>
            <span>Current Plan</span>
            <h2>{usage.displayName}</h2>
            <p>Your organization&apos;s current CloudPilot subscription.</p>
          </div>
          <div className="plan-name-badge">{usage.planName}</div>
        </div>

      </div>

      <div className="usage-grid">
        <div className="usage-card">
          <div className="usage-card-header">
            <div>
              <span>Projects</span>
              <strong>{usage.currentProjects} / {usage.maxProjects}</strong>
            </div>
            <span>{projectPercentage}%</span>
          </div>
          <div className="usage-progress">
            <div className="usage-progress-fill" style={{ width: `${projectPercentage}%` }} />
          </div>
          <p>
            {usage.projectLimitReached
              ? 'Project limit reached.'
              : `${usage.maxProjects - usage.currentProjects} project slots remaining.`}
          </p>
        </div>

        <div className="usage-card">
          <div className="usage-card-header">
            <div>
              <span>Members</span>
              <strong>{usage.reservedMemberSlots} / {usage.maxMembers}</strong>
            </div>
            <span>{memberPercentage}%</span>
          </div>
          <div className="usage-progress">
            <div className="usage-progress-fill" style={{ width: `${memberPercentage}%` }} />
          </div>
          <p>{usage.activeMembers} active · {usage.pendingInvitations} pending</p>
          {usage.memberLimitReached && <p className="usage-warning">Member limit reached.</p>}
        </div>
      </div>

      {canManageBilling && (
        <div className="available-plans-section">
          <div className="available-plans-header">
            <h2>Choose a Plan</h2>
            <p>Change your organization&apos;s CloudPilot subscription.</p>
          </div>

          {planMessage && <div className="plan-success-message">{planMessage}</div>}

          <div className="available-plans-grid">
            {plans.map((plan) => {
              const isCurrent = plan.name === usage.planName

              return (
                <div
                  key={plan.id}
                  className={`available-plan-card ${isCurrent ? 'current' : ''}`}
                >
                  <div className="available-plan-top">
                    <div>
                      <span className="plan-label">{plan.name}</span>
                      <h3>{plan.displayName}</h3>
                    </div>
                    {isCurrent && <span className="current-plan-pill">Current</span>}
                  </div>
                  <p className="plan-description">{plan.description}</p>
                  <div className="plan-limit-row"><span>Projects</span><strong>{plan.maxProjects}</strong></div>
                  <div className="plan-limit-row"><span>Members</span><strong>{plan.maxMembers}</strong></div>
                  <div className="plan-feature-row">
                    <span>Advanced role management</span>
                    <strong>{plan.name === 'FREE' ? '—' : 'Included'}</strong>
                  </div>
                  <button
                    className={isCurrent ? 'plan-current-button' : 'plan-change-button'}
                    disabled={isCurrent}
                    onClick={() => setSelectedPlan(plan)}
                  >
                    {isCurrent ? 'Current Plan' : `Choose ${plan.displayName}`}
                  </button>
                </div>
              )
            })}
          </div>
        </div>
      )}

      <div className="plan-summary">
        <h2>Plan limits</h2>
        <div className="plan-summary-row">
          <span>Projects</span>
          <strong>Up to {usage.maxProjects}</strong>
        </div>
        <div className="plan-summary-row">
          <span>Members</span>
          <strong>Up to {usage.maxMembers}</strong>
        </div>
        <div className="plan-summary-row">
          <span>Advanced role management</span>
          <strong>{usage.features.includes('ADVANCED_RBAC') ? 'Included' : 'Not included'}</strong>
        </div>
      </div>

      {selectedPlan && (
        <div className="plan-modal-overlay">
          <div className="plan-modal">
            <h2>Change subscription plan?</h2>
            <p>
              You&apos;re changing from <strong>{usage.displayName}</strong> to{' '}
              <strong>{selectedPlan.displayName}</strong>.
            </p>
            <div className="plan-change-summary">
              <div><span>Projects</span><strong>{usage.currentProjects} / {selectedPlan.maxProjects}</strong></div>
              <div><span>Members</span><strong>{usage.reservedMemberSlots} / {selectedPlan.maxMembers}</strong></div>
            </div>
            {(usage.currentProjects > selectedPlan.maxProjects ||
              usage.reservedMemberSlots > selectedPlan.maxMembers) && (
              <div className="plan-downgrade-warning">
                Your current usage exceeds this plan&apos;s limits. Existing data will remain, but you won&apos;t be able to add more resources until usage is within the new limits.
              </div>
            )}
            <div className="plan-modal-actions">
              <button
                type="button"
                className="plan-cancel-button"
                disabled={changingPlan}
                onClick={() => setSelectedPlan(null)}
              >Cancel</button>
              <button
                type="button"
                className="plan-confirm-button"
                disabled={changingPlan}
                onClick={changePlan}
              >{changingPlan ? 'Changing...' : 'Change Plan'}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default SubscriptionPage
