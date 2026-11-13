import request from './request'

export function getConsumables(params) {
  return request.get('/consumables', { params })
}

export function createConsumable(data) {
  return request.post('/consumables', data)
}

export function inboundConsumable(id, data) {
  return request.post(`/consumables/${id}/inbound`, data)
}

export function outboundConsumable(id, data) {
  return request.post(`/consumables/${id}/outbound`, data)
}
