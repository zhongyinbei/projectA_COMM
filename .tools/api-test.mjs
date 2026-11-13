import { writeFile } from 'node:fs/promises';
import assert from 'node:assert/strict';

const BASE = 'http://localhost:8080';
const out = [];

function record(name, detail) {
  out.push({ name, detail });
  console.log(`PASS ${name} :: ${typeof detail === 'string' ? detail : JSON.stringify(detail)}`);
}

async function request(method, path, token, body, raw = false, extraHeaders = {}) {
  const headers = { ...extraHeaders };
  if (token) headers.Authorization = `Bearer ${token}`;
  let payload;
  if (body instanceof FormData) {
    payload = body;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }
  const res = await fetch(BASE + path, { method, headers, body: payload, redirect: 'follow' });
  const arrayBuffer = await res.arrayBuffer();
  const buffer = Buffer.from(arrayBuffer);
  const contentType = res.headers.get('content-type') || '';
  let data = null;
  if (!raw && contentType.includes('application/json')) {
    data = JSON.parse(buffer.toString('utf8'));
  }
  return { status: res.status, data, buffer, contentType, headers: res.headers };
}

async function login(username, password) {
  const res = await request('POST', '/api/auth/login', null, { username, password });
  assert.equal(res.status, 200);
  assert.equal(res.data.code, 0);
  assert.ok(res.data.data.token);
  return res.data.data;
}

const admin = await login('admin', 'admin123');
const repairer = await login('repairer1', '123456');
const reporter = await login('reporter1', '123456');
record('Phase 2 三角色登录', {
  admin: admin.user.role,
  repairer: repairer.user.role,
  reporter: reporter.user.role
});

const me = await request('GET', '/api/auth/me', admin.token);
assert.equal(me.data.data.role, 'ADMIN');
record('Phase 2 /auth/me', me.data.data);

const noToken = await request('GET', '/api/equipments', null);
assert.equal(noToken.status, 401);
record('Phase 2 未登录 401', noToken.data);

const list = await request('GET', '/api/equipments?page=1&size=10', admin.token);
assert.equal(list.status, 200);
assert.equal(list.data.data.total, 5);
record('Phase 3 种子设备 5 条', { total: list.data.data.total, records: list.data.data.records.length });

const created = await request('POST', '/api/equipments', admin.token, {
  assetNo: 'LAB-TEST-901',
  name: '接口验收测试设备',
  location: '测试工位',
  category: '测试',
  status: 'NORMAL'
});
assert.equal(created.status, 200);
assert.ok(created.data.data.id);
record('Phase 3 管理员新增设备', { id: created.data.data.id, assetNo: created.data.data.assetNo });

const list6 = await request('GET', '/api/equipments?page=1&size=10', admin.token);
assert.equal(list6.data.data.total, 6);
record('Phase 3 新增后总数 6', { total: list6.data.data.total });

const forbiddenCreate = await request('POST', '/api/equipments', reporter.token, {
  assetNo: 'LAB-TEST-902', name: '越权设备', location: 'X'
});
assert.equal(forbiddenCreate.status, 403);
record('Phase 3 REPORTER 新增设备 403', forbiddenCreate.data);

const deleted = await request('DELETE', `/api/equipments/${created.data.data.id}`, admin.token);
assert.equal(deleted.status, 200);
const list5 = await request('GET', '/api/equipments?page=1&size=10', admin.token);
assert.equal(list5.data.data.total, 5);
record('Phase 3 删除测试设备恢复 5 条', { total: list5.data.data.total });

const order = await request('POST', '/api/repair-orders', reporter.token, {
  equipmentId: 1,
  description: '接口验收：显示器无信号',
  imageUrl: null
});
assert.equal(order.status, 200);
assert.equal(order.data.data.status, 'PENDING');
const orderId = order.data.data.id;
record('Phase 4 REPORTER 提交工单', { id: orderId, orderNo: order.data.data.orderNo, status: order.data.data.status });

const accepted = await request('PATCH', `/api/repair-orders/${orderId}/accept`, repairer.token);
assert.equal(accepted.status, 200);
record('Phase 4 REPAIRER 接单', { id: orderId, http: accepted.status });

const acceptedAgain = await request('PATCH', `/api/repair-orders/${orderId}/accept`, repairer.token);
assert.equal(acceptedAgain.status, 409);
assert.equal(acceptedAgain.data.code, 409);
record('Phase 4 重复接单 409', acceptedAgain.data);

const completed = await request('PATCH', `/api/repair-orders/${orderId}/complete`, repairer.token, {
  repairNote: '更换 HDMI 线，恢复正常'
});
assert.equal(completed.status, 200);
record('Phase 4 完成工单', { id: orderId, http: completed.status });

const orderAfter = await request('GET', '/api/repair-orders?page=1&size=10', reporter.token);
const finished = orderAfter.data.data.records.find(item => item.id === orderId);
assert.equal(finished.status, 'COMPLETED');
const equipmentAfter = await request('GET', '/api/equipments?page=1&size=10&keyword=LAB-PC-001', admin.token);
assert.equal(equipmentAfter.data.data.records[0].status, 'NORMAL');
record('Phase 4 状态与设备恢复', { orderStatus: finished.status, equipmentStatus: equipmentAfter.data.data.records[0].status });

const qr = await request('GET', '/api/inspection-points/1/qrcode', admin.token, undefined, true);
assert.equal(qr.status, 200);
assert.equal(qr.contentType, 'image/png');
assert.equal(qr.buffer.subarray(0, 8).toString('hex'), '89504e470d0a1a0a');
await writeFile('D:/zhongyinbei/.tools/qr.png', qr.buffer);
record('Phase 5 二维码 PNG', { bytes: qr.buffer.length, contentType: qr.contentType });

const online = await request('POST', '/api/inspection-records', repairer.token, {
  pointCode: 'P-001', result: 'NORMAL', remark: '接口验收在线巡检'
});
assert.equal(online.status, 200);
record('Phase 5 在线巡检', { id: online.data.data.id, pointCode: online.data.data.pointCode });

const sync = await request('POST', '/api/inspection-records/sync', repairer.token, {
  records: [
    { pointCode: 'P-002', result: 'ABNORMAL', remark: '接口验收离线巡检：网线松动', inspectedAt: '2026-11-11T10:00:00' }
  ]
});
assert.equal(sync.status, 200);
assert.deepEqual(sync.data.data, { success: 1, failed: 0 });
record('Phase 5 离线同步', sync.data.data);

const inspectionPage = await request('GET', '/api/inspection-records?page=1&size=10', repairer.token);
assert.ok(inspectionPage.data.data.total >= 2);
const offlineRecord = inspectionPage.data.data.records.find(item => item.pointCode === 'P-002');
assert.equal(offlineRecord.syncedFromOffline, 1);
record('Phase 5 巡检记录查询', {
  total: inspectionPage.data.data.total,
  offlineRecord: { pointCode: offlineRecord.pointCode, syncedFromOffline: offlineRecord.syncedFromOffline }
});

const outbound = await request('POST', '/api/consumables/5/outbound', admin.token, {
  quantity: 1, remark: '接口验收领用'
});
assert.equal(outbound.status, 200);
assert.equal(outbound.data.data.stock, 1);
record('Phase 6 耗材出库扣减', { id: 5, stock: outbound.data.data.stock });

const insufficient = await request('POST', '/api/consumables/5/outbound', admin.token, {
  quantity: 99, remark: '接口验收超领'
});
assert.equal(insufficient.status, 400);
assert.equal(insufficient.data.message, '库存不足');
record('Phase 6 库存不足 400', insufficient.data);

const summary = await request('GET', '/api/dashboard/summary', admin.token);
assert.equal(summary.status, 200);
assert.ok(summary.data.data.alertConsumables.some(item => item.id === 5));
record('Phase 6 看板汇总与红色告警数据', {
  equipmentTotal: summary.data.data.equipmentTotal,
  pendingOrders: summary.data.data.pendingOrders,
  abnormalInspections: summary.data.data.abnormalInspections,
  alerts: summary.data.data.alertConsumables.map(item => `${item.name}:${item.stock}/${item.threshold}`)
});

const statusChart = await request('GET', '/api/dashboard/equipment-status', admin.token);
const rateChart = await request('GET', '/api/dashboard/inspection-rate', admin.token);
const topChart = await request('GET', '/api/dashboard/consumable-top', admin.token);
assert.ok(Array.isArray(statusChart.data.data) && statusChart.data.data.length > 0);
assert.equal(rateChart.data.data.length, 7);
assert.ok(Array.isArray(topChart.data.data));
record('Phase 6 看板三组聚合', {
  equipmentStatus: statusChart.data.data,
  inspectionRate: rateChart.data.data,
  consumableTop: topChart.data.data
});

const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=', 'base64');
const form = new FormData();
form.append('file', new Blob([png], { type: 'image/png' }), 'acceptance.png');
const upload = await request('POST', '/api/files/upload', reporter.token, form);
assert.equal(upload.status, 200);
assert.match(upload.data.data.url, /^\/uploads\/[a-f0-9]+\.png$/);
const image = await request('GET', upload.data.data.url, null, undefined, true);
assert.equal(image.status, 200);
assert.equal(image.buffer.subarray(0, 8).toString('hex'), '89504e470d0a1a0a');
record('Phase 6 图片上传与静态访问', { url: upload.data.data.url, bytes: image.buffer.length });

const reporterConsumables = await request('GET', '/api/consumables', reporter.token);
assert.equal(reporterConsumables.status, 403);
record('A8 越权耗材接口 403', reporterConsumables.data);

const cors = await fetch(BASE + '/api/auth/login', {
  method: 'OPTIONS',
  headers: {
    Origin: 'http://localhost:5173',
    'Access-Control-Request-Method': 'POST',
    'Access-Control-Request-Headers': 'content-type'
  }
});
assert.equal(cors.status, 200);
assert.equal(cors.headers.get('access-control-allow-origin'), 'http://localhost:5173');
record('CORS 仅放行 5173', { status: cors.status, allowOrigin: cors.headers.get('access-control-allow-origin') });

await writeFile('D:/zhongyinbei/.tools/api-test-result.json', JSON.stringify(out, null, 2));
console.log('ALL_API_TESTS_PASSED');
