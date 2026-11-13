import request from './request'

export function getRepairOrders(params) {
  return request.get('/repair-orders', { params })
}

export function createRepairOrder(data) {
  return request.post('/repair-orders', data)
}

export function acceptRepairOrder(id) {
  return request.patch(`/repair-orders/${id}/accept`)
}

export function rejectRepairOrder(id) {
  return request.patch(`/repair-orders/${id}/reject`)
}

export function completeRepairOrder(id, data) {
  return request.patch(`/repair-orders/${id}/complete`, data)
}

export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload', formData)
}
