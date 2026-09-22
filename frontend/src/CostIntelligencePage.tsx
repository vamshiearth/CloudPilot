import { useEffect, useState } from 'react'
import { AlertTriangle, ArrowUpRight, CalendarDays, CheckCircle2, DollarSign, RefreshCw, ShieldAlert, Sparkles } from 'lucide-react'
import {
  getBudget,
  getCostAnomaly,
  getCostInsights,
  getCostSummary,
  getDailyCosts,
  getServiceCosts,
  updateBudget,
} from './api/costs'
import type {
  BudgetSummary,
  CostAnomalyAssessment,
  CostInsight,
  CostSummary,
  DailyCostSummary,
  ServiceCostSummary,
} from './api/costs'
import './CostIntelligencePage.css'

type CostIntelligencePageProps = {
  token: string
  role: string | null
}

const currency = (value: number | null, code = 'USD') =>
  value === null
    ? 'Not configured'
    : value.toLocaleString('en-US', { style: 'currency', currency: code })

function CostIntelligencePage({ token, role }: CostIntelligencePageProps) {
  const [summary, setSummary] = useState<CostSummary | null>(null)
  const [budget, setBudget] = useState<BudgetSummary | null>(null)
  const [daily, setDaily] = useState<DailyCostSummary[]>([])
  const [services, setServices] = useState<ServiceCostSummary[]>([])
  const [insights, setInsights] = useState<CostInsight[]>([])
  const [anomaly, setAnomaly] = useState<CostAnomalyAssessment | null>(null)
  const [budgetInput, setBudgetInput] = useState('')
  const [savingBudget, setSavingBudget] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const canEditBudget = role === 'OWNER'

  async function loadData() {
    try {
      setLoading(true)
      setError('')
      const [summaryData, dailyData, servicesData, insightsData, anomalyData, budgetData] = await Promise.all([
        getCostSummary(token),
        getDailyCosts(token),
        getServiceCosts(token),
        getCostInsights(token),
        getCostAnomaly(token),
        getBudget(token),
      ])
      setSummary(summaryData)
      setDaily(dailyData)
      setServices(servicesData)
      setInsights(insightsData)
      setAnomaly(anomalyData)
      setBudget(budgetData)
      setBudgetInput(budgetData.monthlyBudget?.toFixed(2) || '')
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Failed to load cost intelligence')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [token])

  async function saveBudget(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const amount = Number(budgetInput)
    if (!Number.isFinite(amount) || amount <= 0) {
      setError('Enter a monthly budget greater than zero.')
      return
    }
    try {
      setSavingBudget(true)
      setError('')
      setBudget(await updateBudget(token, amount))
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'Failed to update budget')
    } finally {
      setSavingBudget(false)
    }
  }

  if (loading) return <div className="cost-page cost-loading">Loading cost intelligence...</div>
  if (error && !summary) return <div className="cost-page"><div className="cost-error">{error}</div></div>
  if (!summary || !budget || !anomaly) return null

  const budgetPercentage = Math.min(100, Math.max(0, budget.usagePercentage))
  const anomalyClass = anomaly.status.toLowerCase().replaceAll('_', '-')

  return (
    <div className="cost-page">
      <header className="cost-header">
        <div>
          <p className="cost-eyebrow">FinOps workspace</p>
          <h1>Cost Intelligence</h1>
          <p>Read the signals behind today&apos;s cloud spend before it becomes tomorrow&apos;s surprise.</p>
        </div>
        <button className="cost-refresh" type="button" onClick={loadData} title="Refresh cost intelligence">
          <RefreshCw size={16} aria-hidden="true" />
          Refresh
        </button>
      </header>

      {error && <div className="cost-error">{error}</div>}

      <section className="cost-metrics" aria-label="Cost summary">
        <div className="cost-metric metric-accent"><span>Today</span><strong>{currency(summary.todayCost)}</strong><small>{summary.asOfDate}</small><DollarSign size={20} /></div>
        <div className="cost-metric"><span>Month to date</span><strong>{currency(summary.monthToDateCost)}</strong><small>Daily average {currency(summary.dailyAverage)}</small><CalendarDays size={20} /></div>
        <div className="cost-metric"><span>Projected month</span><strong>{currency(summary.projectedMonthlyCost)}</strong><small>Highest: {summary.highestCostService || 'None'}</small><ArrowUpRight size={20} /></div>
        <div className="cost-metric"><span>Budget</span><strong>{currency(budget.monthlyBudget, budget.currency)}</strong><small>{budget.health === 'NO_BUDGET' ? 'No budget configured' : `${budget.usagePercentage.toFixed(2)}% used`}</small><ShieldAlert size={20} /></div>
      </section>

      <section className="cost-panel budget-panel">
        <div className="panel-heading"><div><p className="section-kicker">Guardrail</p><h2>Budget health</h2></div><span className={`status-pill status-${budget.health.toLowerCase()}`}>{budget.health.replaceAll('_', ' ')}</span></div>
        {budget.health === 'NO_BUDGET' ? <p className="muted-copy">No monthly budget configured.</p> : <><div className="budget-line"><strong>{budget.usagePercentage.toFixed(2)}%</strong><span>{currency(budget.monthToDateCost)} of {currency(budget.monthlyBudget, budget.currency)}</span></div><div className="progress-track"><div className={`progress-fill progress-${budget.health.toLowerCase()}`} style={{ width: `${budgetPercentage}%` }} /></div></>}
        {canEditBudget && <form className="budget-editor" onSubmit={saveBudget}><label htmlFor="monthly-budget">Monthly budget</label><div><input id="monthly-budget" type="number" min="0.01" step="0.01" value={budgetInput} onChange={(event) => setBudgetInput(event.target.value)} /><button type="submit" disabled={savingBudget}>{savingBudget ? 'Saving...' : 'Save budget'}</button></div></form>}
      </section>

      <div className="cost-content-grid">
        <section className="cost-panel trend-panel"><div className="panel-heading"><div><p className="section-kicker">Daily history</p><h2>Daily cost trend</h2></div><span className="panel-note">{daily.length} days</span></div><div className="daily-table-wrap"><table><thead><tr><th>Date</th><th>Cost</th></tr></thead><tbody>{[...daily].reverse().map((day) => <tr key={day.date}><td>{day.date}</td><td>{currency(day.costAmount)}</td></tr>)}</tbody></table></div></section>
        <section className="cost-panel services-panel"><div className="panel-heading"><div><p className="section-kicker">Allocation</p><h2>Cost by service</h2></div></div><div className="service-list">{services.map((service) => <div className="service-row" key={service.serviceCategory}><div className="service-name"><span>{service.serviceCategory}</span><strong>{currency(service.costAmount)}</strong></div><div className="service-bar"><span style={{ width: `${Math.min(100, service.percentage)}%` }} /></div><small>{service.percentage.toFixed(2)}%</small></div>)}</div></section>
      </div>

      <section className={`cost-panel anomaly-panel anomaly-${anomalyClass}`}><div className="anomaly-icon">{anomaly.status === 'NORMAL' ? <CheckCircle2 size={22} /> : <AlertTriangle size={22} />}</div><div><p className="section-kicker">Signal check</p><h2>Anomaly status: {anomaly.status.replaceAll('_', ' ')}</h2><p>Today {currency(anomaly.todayCost)} against a 7-day baseline of {currency(anomaly.baselineAverage)} at <strong>{anomaly.costPressure.toFixed(4)}x</strong> pressure.</p><small>{anomaly.historicalDays} historical days available</small></div></section>

      <section className="cost-panel insights-panel"><div className="panel-heading"><div><p className="section-kicker">Advisory signals</p><h2>Cost insights</h2></div><Sparkles size={20} /></div>{insights.length === 0 ? <p className="muted-copy">No cost optimization insights right now.</p> : <div className="insight-list">{insights.map((insight, index) => <article className={`insight-card insight-${insight.severity.toLowerCase()}`} key={`${insight.type}-${index}`}><div className="insight-card-top"><strong>{insight.type.replaceAll('_', ' ')}</strong><span>{insight.severity}</span></div><p>{insight.message}</p><small>Measured {insight.measuredValue} · Threshold {insight.thresholdValue}</small></article>)}</div>}</section>
    </div>
  )
}

export default CostIntelligencePage
