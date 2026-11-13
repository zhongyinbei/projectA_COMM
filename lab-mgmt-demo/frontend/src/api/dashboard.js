import request from './request'

export function getDashboardSummary() {
  return request.get('/dashboard/summary')
}

export function getEquipmentStatusStats() {
  return request.get('/dashboard/equipment-status')
}

export function getInspectionRateStats() {
  return request.get('/dashboard/inspection-rate')
}

export function getConsumableTopStats() {
  return request.get('/dashboard/consumable-top')
}
