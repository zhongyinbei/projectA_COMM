import request from './request'

export function getInspectionPoints() {
  return request.get('/inspection-points')
}

export function createInspectionPoint(data) {
  return request.post('/inspection-points', data)
}

export function getInspectionPointQr(id) {
  return request.get(`/inspection-points/${id}/qrcode`, { responseType: 'blob' })
}

export function getInspectionRecords(params) {
  return request.get('/inspection-records', { params })
}

export function createInspectionRecord(data) {
  return request.post('/inspection-records', data)
}

export function syncInspectionRecords(records) {
  return request.post('/inspection-records/sync', { records })
}
