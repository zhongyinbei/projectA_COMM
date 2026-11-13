import request from './request'

export function getEquipments(params) {
  return request.get('/equipments', { params })
}

export function createEquipment(data) {
  return request.post('/equipments', data)
}

export function updateEquipment(id, data) {
  return request.put(`/equipments/${id}`, data)
}

export function deleteEquipment(id) {
  return request.delete(`/equipments/${id}`)
}
