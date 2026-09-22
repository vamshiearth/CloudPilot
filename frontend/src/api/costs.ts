export type BudgetHealth =
  | 'HEALTHY'
  | 'WATCH'
  | 'HIGH'
  | 'OVER_BUDGET'
  | 'NO_BUDGET'

export type CostAnomalyStatus =
  | 'NORMAL'
  | 'ELEVATED'
  | 'ANOMALY_CANDIDATE'
  | 'INSUFFICIENT_DATA'

export interface ServiceCostSummary {
  serviceCategory: string
  costAmount: number
  percentage: number
}

export interface CostSummary {
  asOfDate: string
  todayCost: number
  monthToDateCost: number
  previousMonthCost: number
  dailyAverage: number
  projectedMonthlyCost: number
  highestCostService: string | null
  highestCostServiceAmount: number
  services: ServiceCostSummary[]
}

export interface DailyCostSummary {
  date: string
  costAmount: number
}

export interface BudgetSummary {
  monthlyBudget: number | null
  currency: string
  monthToDateCost: number
  usagePercentage: number
  health: BudgetHealth
  updatedAt: string | null
}

export interface CostAnomalyAssessment {
  asOfDate: string
  todayCost: number
  baselineAverage: number
  costPressure: number
  historicalDays: number
  status: CostAnomalyStatus
}

export interface CostInsight {
  type: string
  severity: 'INFO' | 'WARNING' | 'HIGH'
  message: string
  measuredValue: number
  thresholdValue: number
}

async function request<T>(token: string, path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`/api/costs${path}`, {
    ...options,
    headers: {
      ...(options?.headers || {}),
      Authorization: `Bearer ${token}`,
      ...(options?.body ? { 'Content-Type': 'application/json' } : {}),
    },
  })

  const data = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(data?.message || `Cost request failed (${response.status})`)
  }
  return data as T
}

export const getCostSummary = (token: string) =>
  request<CostSummary>(token, '/summary')

export const getDailyCosts = (token: string) =>
  request<DailyCostSummary[]>(token, '/daily')

export const getServiceCosts = (token: string) =>
  request<ServiceCostSummary[]>(token, '/services')

export const getCostInsights = (token: string) =>
  request<CostInsight[]>(token, '/insights')

export const getCostAnomaly = (token: string) =>
  request<CostAnomalyAssessment>(token, '/anomaly')

export const getBudget = (token: string) =>
  request<BudgetSummary>(token, '/budget')

export const updateBudget = (token: string, monthlyBudget: number) =>
  request<BudgetSummary>(token, '/budget', {
    method: 'PUT',
    body: JSON.stringify({ monthlyBudget }),
  })
