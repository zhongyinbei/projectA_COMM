

# 实施文档：实训室设备巡检与耗材智能管理平台 Demo

## 0. 给 Harness 的元指令（必须先读）

1. **严格按 Phase 顺序执行**。Phase N 的验收命令全部通过后，才允许进入 Phase N+1。禁止一次性生成全部代码。
2. **本文件中的所有路径、字段名、API 路径、状态枚举值是契约**，不得改名、不得增删字段、不得“优化”命名。
3. **每个 Phase 结束必须运行该 Phase 的“验收命令”并粘贴实际输出**。验收失败时，只修复本 Phase 范围内的问题，禁止扩大改动。
4. 遇到本文件未覆盖的细节，**选择最简单的实现**，并在代码注释中写 `// DEMO-SIMPLIFIED: <原因>`。
5. **禁止引入本文件未列出的依赖**。特别是：禁止 Spring Security、禁止 Redis、禁止消息队列、禁止前端摄像头扫码库。
6. 所有面向用户的中文文案、注释使用中文；代码标识符使用英文。
7. 目标运行环境：Windows 10/11 或 macOS，本机安装 JDK 17、Maven 3.8+、Node.js 18+、MySQL 8.0。

---

## 1. 项目目标与验收标准

### 1.1 一句话目标

交付一个前后端分离的三端 Web Demo：**报修者端**提交报修、**维修员端**接单与巡检、**管理员端**管理设备/耗材/巡检点并查看看板。

### 1.2 Demo 完成定义（DoD）

全部满足即视为 Demo 完成：

| 编号 | 验收项                                                  | 验证方式                        |
| ---- | ------------------------------------------------------- | ------------------------------- |
| D1   | 三类角色可登录，Token 生效，越权访问返回 403            | curl + 浏览器                   |
| D2   | 报修者提交报修 → 维修员接单 → 完成，状态机全链路可走通  | 浏览器操作 + 数据库核对         |
| D3   | 巡检点可生成二维码图片，巡检记录提交后可查询            | 浏览器 + curl                   |
| D4   | 耗材出库后库存扣减，低于阈值时看板出现红色告警          | 浏览器操作                      |
| D5   | 管理员看板显示 4 张 ECharts 图表                        | 浏览器                          |
| D6   | 离线巡检：断网时记录存 localStorage，恢复后批量同步成功 | 浏览器 DevTools Network Offline |
| D7   | 后端分层为 Controller → Service → Mapper，无跨层调用    | 代码审查                        |

### 1.3 明确不在 Demo 范围（禁止实现）

短信/邮件通知、真实摄像头扫码、文件云存储、多租户、审批流引擎、AI 推荐、Docker 部署（仅提供可选脚本，不作为验收项）。

---

## 2. 技术栈与版本锁定

### 2.1 后端

| 项         | 选型                         | 锁定版本                                                     |
| ---------- | ---------------------------- | ------------------------------------------------------------ |
| JDK        | OpenJDK                      | 17                                                           |
| 构建       | Maven                        | 3.8+                                                         |
| 框架       | Spring Boot                  | 3.2.5                                                        |
| ORM        | MyBatis-Plus                 | 3.5.6                                                        |
| 数据库驱动 | mysql-connector-j            | 8.3.0                                                        |
| JWT        | jjwt                         | 0.12.5（api/impl/jackson 三件套）                            |
| 密码       | spring-security-crypto       | 随 Boot 版本（仅用 BCryptPasswordEncoder，不引入 Security 框架） |
| 二维码     | com.google.zxing core+javase | 3.5.3                                                        |
| 简化代码   | Lombok                       | 随 Boot 版本                                                 |
| 端口       | server.port                  | 8080                                                         |

### 2.2 前端

| 项   | 选型             | 锁定版本 |
| ---- | ---------------- | -------- |
| 构建 | Vite             | 5.x      |
| 框架 | Vue              | 3.4.x    |
| 路由 | vue-router       | 4.x      |
| 状态 | pinia            | 2.x      |
| UI   | element-plus     | 2.7.x    |
| 请求 | axios            | 1.x      |
| 图表 | echarts          | 5.5.x    |
| 端口 | vite server.port | 5173     |

### 2.3 数据库

MySQL 8.0，库名 `lab_mgmt`，字符集 `utf8mb4`，账号默认 `root`，密码由用户在 Phase 0 填入 `application.yml`。

---

## 3. Phase 0：环境自检与初始化

### 3.1 动作

依次执行并记录输出：

```bash
java -version
mvn -v
node -v
npm -v
mysql --version
```

检查端口占用（Windows PowerShell）：

```powershell
netstat -ano | findstr :8080
netstat -ano | findstr :5173
netstat -ano | findstr :3306
```

### 3.2 数据库准备

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS lab_mgmt DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 3.3 验收

- 上述 5 条版本命令均返回版本号，JDK 主版本为 17，Node 主版本 ≥18。
- `lab_mgmt` 数据库存在。
- 8080 / 5173 未被占用；3306 被 MySQL 占用。

**若任一条件不满足，停止执行并输出缺失项，禁止继续 Phase 1。**

---

## 4. 最终目录结构（必须精确一致）

```
lab-mgmt-demo/
├── README.md
├── db/
│   └── schema.sql
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/lab/
│       │   ├── LabApplication.java
│       │   ├── common/
│       │   │   ├── Result.java
│       │   │   ├── PageResult.java
│       │   │   ├── BizException.java
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── config/
│       │   │   ├── WebConfig.java
│       │   │   ├── MybatisPlusConfig.java
│       │   │   └── SeedDataRunner.java
│       │   ├── interceptor/JwtInterceptor.java
│       │   ├── util/
│       │   │   ├── JwtUtil.java
│       │   │   └── QrCodeUtil.java
│       │   ├── entity/
│       │   │   ├── SysUser.java
│       │   │   ├── Equipment.java
│       │   │   ├── RepairOrder.java
│       │   │   ├── InspectionPoint.java
│       │   │   ├── InspectionRecord.java
│       │   │   ├── Consumable.java
│       │   │   ├── ConsumableTransaction.java
│       │   │   └── OperationLog.java
│       │   ├── mapper/            # 8 个 Mapper 接口
│       │   ├── service/           # 8 个 Service 接口
│       │   ├── service/impl/      # 8 个实现
│       │   ├── controller/
│       │   │   ├── AuthController.java
│       │   │   ├── EquipmentController.java
│       │   │   ├── RepairOrderController.java
│       │   │   ├── InspectionController.java
│       │   │   ├── ConsumableController.java
│       │   │   ├── DashboardController.java
│       │   │   └── FileController.java
│       │   └── dto/               # 请求 DTO 与响应 VO
│       └── resources/
│           ├── application.yml
│           └── static/uploads/.gitkeep
└── frontend/
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── main.js
        ├── App.vue
        ├── router/index.js
        ├── stores/auth.js
        ├── api/
        │   ├── request.js
        │   ├── auth.js
        │   ├── equipment.js
        │   ├── repairOrder.js
        │   ├── inspection.js
        │   ├── consumable.js
        │   └── dashboard.js
        ├── layouts/
        │   ├── ReporterLayout.vue
        │   ├── RepairerLayout.vue
        │   └── AdminLayout.vue
        ├── views/
        │   ├── Login.vue
        │   ├── reporter/{Home,RepairNew,MyOrders}.vue
        │   ├── repairer/{OrderList,Inspection}.vue
        │   └── admin/{Equipment,Consumable,InspectionPoint,Dashboard}.vue
        └── components/OrderStatusTag.vue
```

---

## 5. 数据库设计（精确 DDL）

`db/schema.sql` 内容必须为：

```sql
CREATE DATABASE IF NOT EXISTS lab_mgmt DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lab_mgmt;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  role VARCHAR(20) NOT NULL COMMENT 'REPORTER/REPAIRER/ADMIN',
  phone VARCHAR(20),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE equipment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  asset_no VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  location VARCHAR(100) NOT NULL,
  category VARCHAR(50),
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/REPAIRING/SCRAPPED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_status (status)
);

CREATE TABLE repair_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL UNIQUE,
  equipment_id BIGINT NOT NULL,
  reporter_id BIGINT NOT NULL,
  assignee_id BIGINT NULL,
  description VARCHAR(500) NOT NULL,
  image_url VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
    COMMENT 'PENDING/ACCEPTED/REJECTED/COMPLETED',
  repair_note VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  accepted_at DATETIME,
  completed_at DATETIME,
  KEY idx_status (status),
  KEY idx_reporter (reporter_id),
  KEY idx_assignee (assignee_id)
);

CREATE TABLE inspection_point (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  point_code VARCHAR(50) NOT NULL UNIQUE,
  location VARCHAR(100) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inspection_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  point_id BIGINT NOT NULL,
  inspector_id BIGINT NOT NULL,
  result VARCHAR(20) NOT NULL COMMENT 'NORMAL/ABNORMAL',
  remark VARCHAR(255),
  inspected_at DATETIME NOT NULL,
  synced_from_offline TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_point (point_id),
  KEY idx_inspector (inspector_id)
);

CREATE TABLE consumable (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  spec VARCHAR(100),
  unit VARCHAR(20) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  threshold INT NOT NULL DEFAULT 5,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE consumable_transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  consumable_id BIGINT NOT NULL,
  type VARCHAR(10) NOT NULL COMMENT 'IN/OUT',
  quantity INT NOT NULL,
  operator_id BIGINT NOT NULL,
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_consumable (consumable_id)
);

CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  biz_type VARCHAR(30) NOT NULL COMMENT 'REPAIR_ORDER/INSPECTION/CONSUMABLE/STOCK_ALERT',
  biz_id BIGINT,
  action VARCHAR(50) NOT NULL,
  detail VARCHAR(500),
  operator_id BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_biz (biz_type, biz_id)
);
```

**状态机约束（后端 Service 层必须强制校验，前端也必须按此渲染按钮）：**

```
repair_order.status:
PENDING  → ACCEPTED | REJECTED
ACCEPTED → COMPLETED
REJECTED / COMPLETED 为终态
其他转换一律抛 BizException(400, "非法状态流转")
```

**并发要求**：`accept` / `complete` 使用 `UPDATE ... WHERE id=? AND status=?` 的乐观校验，影响行数为 0 时抛 `BizException(409, "工单状态已变化")`。

---

## 6. API 契约（冻结，不得变更）

### 6.1 统一约定

- 前缀 `/api`
- 请求头：`Authorization: Bearer <token>`（除登录接口）
- 统一响应体：

```json
{ "code": 0, "message": "ok", "data": {} }
```

- `code=0` 成功；非 0 失败，HTTP 状态码同步为 400/401/403/404/409/500
- 分页响应 `data`：

```json
{ "records": [], "total": 0, "page": 1, "size": 10 }
```

### 6.2 端点表

| #    | 方法   | 路径                                 | 角色           | 说明                                                         |
| ---- | ------ | ------------------------------------ | -------------- | ------------------------------------------------------------ |
| 1    | POST   | `/api/auth/login`                    | 匿名           | 入参 `{username,password}`，返回 `{token, user:{id,username,realName,role}}` |
| 2    | GET    | `/api/auth/me`                       | 全部           | 返回当前用户                                                 |
| 3    | GET    | `/api/equipments`                    | ADMIN/REPORTER | query: `page,size,keyword,status`                            |
| 4    | POST   | `/api/equipments`                    | ADMIN          | 新增设备                                                     |
| 5    | PUT    | `/api/equipments/{id}`               | ADMIN          | 修改设备                                                     |
| 6    | DELETE | `/api/equipments/{id}`               | ADMIN          | 删除设备（有工单引用则 409）                                 |
| 7    | POST   | `/api/repair-orders`                 | REPORTER       | 入参 `{equipmentId,description,imageUrl}`                    |
| 8    | GET    | `/api/repair-orders`                 | 全部           | REPORTER 只看自己；REPAIRER 看 PENDING + 自己名下；ADMIN 看全部；query: `page,size,status` |
| 9    | PATCH  | `/api/repair-orders/{id}/accept`     | REPAIRER       | PENDING→ACCEPTED，写 assignee_id、accepted_at                |
| 10   | PATCH  | `/api/repair-orders/{id}/reject`     | REPAIRER       | PENDING→REJECTED                                             |
| 11   | PATCH  | `/api/repair-orders/{id}/complete`   | REPAIRER       | ACCEPTED→COMPLETED，入参 `{repairNote}`                      |
| 12   | GET    | `/api/inspection-points`             | ADMIN/REPAIRER | 列表                                                         |
| 13   | POST   | `/api/inspection-points`             | ADMIN          | 新增巡检点                                                   |
| 14   | GET    | `/api/inspection-points/{id}/qrcode` | ADMIN/REPAIRER | 返回 `image/png`                                             |
| 15   | POST   | `/api/inspection-records`            | REPAIRER       | 在线巡检，入参 `{pointCode,result,remark}`，`inspected_at` 由后端生成 |
| 16   | POST   | `/api/inspection-records/sync`       | REPAIRER       | 离线批量同步，入参 `{records:[{pointCode,result,remark,inspectedAt}]}`，`synced_from_offline=1` |
| 17   | GET    | `/api/inspection-records`            | ADMIN/REPAIRER | query: `page,size,pointId`                                   |
| 18   | GET    | `/api/consumables`                   | ADMIN          | query: `page,size,keyword`                                   |
| 19   | POST   | `/api/consumables`                   | ADMIN          | 新增耗材                                                     |
| 20   | POST   | `/api/consumables/{id}/inbound`      | ADMIN          | 入参 `{quantity,remark}`                                     |
| 21   | POST   | `/api/consumables/{id}/outbound`     | ADMIN          | 入参 `{quantity,remark}`；库存不足 400                       |
| 22   | GET    | `/api/dashboard/summary`             | ADMIN          | `{equipmentTotal, pendingOrders, abnormalInspections, alertConsumables:[...]}` |
| 23   | GET    | `/api/dashboard/equipment-status`    | ADMIN          | `[{status,count}]`                                           |
| 24   | GET    | `/api/dashboard/inspection-rate`     | ADMIN          | 近 7 天 `[{date,count}]`                                     |
| 25   | GET    | `/api/dashboard/consumable-top`      | ADMIN          | 出库 TOP5 `[{name,quantity}]`                                |
| 26   | POST   | `/api/files/upload`                  | 全部           | multipart `file`，返回 `{url:"/uploads/xxx.png"}`            |

### 6.3 关键业务规则（后端必须实现）

1. **库存事务**：`outbound` 在单个 `@Transactional` 内完成：`UPDATE consumable SET stock = stock - ? WHERE id=? AND stock >= ?` → 影响行数 0 则 `BizException(400,"库存不足")` → 插入 `consumable_transaction(type=OUT)` → 若扣减后 `stock < threshold`，插入 `operation_log(biz_type='STOCK_ALERT', action='LOW_STOCK')`。
2. **工单状态变更留痕**：每次 accept/reject/complete 插入 `operation_log(biz_type='REPAIR_ORDER', biz_id=订单id, action=新状态)`。
3. **巡检记录**：在线提交 `inspected_at=now()`；离线同步使用入参 `inspectedAt`，且 `synced_from_offline=1`，并插入 `operation_log(biz_type='INSPECTION', action='SYNC_OFFLINE')`。
4. **权限**：`JwtInterceptor` 解析 Token 得到 `userId/role`，存入 `ThreadLocal`；Controller 通过自定义注解 `@RequireRole({"ADMIN"})` 校验，不匹配返回 403。

---

## 7. 后端施工步骤

### Phase 1：后端骨架与数据库连通

**动作**

1. 创建 `backend/pom.xml`，依赖锁定为第 2.1 节所列。
2. 创建 `application.yml`：

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lab_mgmt?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: <用户填写>
    driver-class-name: com.mysql.cj.jdbc.Driver
  servlet:
    multipart:
      max-file-size: 5MB
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
lab:
  jwt:
    secret: lab-mgmt-demo-secret-key-please-change-in-prod
    expire-hours: 12
  upload-dir: ./uploads/
```

3. 创建 `LabApplication.java`（`@SpringBootApplication`、`@MapperScan("com.lab.mapper")`）。
4. 创建 `common/Result.java`、`PageResult.java`、`BizException.java`、`GlobalExceptionHandler.java`。
5. 执行 `db/schema.sql` 建表。

**验收**

```bash
mysql -u root -p -e "USE lab_mgmt; SHOW TABLES;"
```

必须列出 8 张表。`mvn -q compile` 成功。

---

### Phase 2：认证与用户

**动作**

1. `entity/SysUser.java`（MyBatis-Plus `@TableName("sys_user")`）。
2. `util/JwtUtil.java`：`generate(userId, role)`、`parse(token)`。
3. `interceptor/JwtInterceptor.java`：放行 `/api/auth/login`、`/uploads/**`；解析失败返回 401 JSON。
4. `config/WebConfig.java`：注册拦截器；`addResourceHandlers` 将 `/uploads/**` 映射到 `file:./uploads/`；CORS 放行 `http://localhost:5173`。
5. `config/SeedDataRunner.java`（`CommandLineRunner`）：
   - 若 `sys_user` 为空，插入 3 个账号：`admin/admin123/ADMIN`、`repairer1/123456/REPAIRER`、`reporter1/123456/REPORTER`，密码用 `BCryptPasswordEncoder` 运行时加密。
   - 插入 5 台设备、3 个巡检点（`P-001`~`P-003`）、5 种耗材（其中 1 种 `stock=2, threshold=5` 用于告警演示）。
6. `AuthController` 实现端点 #1 #2。

**验收**

```bash
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

返回 `code=0` 且 `data.token` 非空；用该 token 请求 `/api/auth/me` 返回 ADMIN 用户；不带 token 请求返回 401。

---

### Phase 3：设备台账 CRUD

**动作**：`Equipment` 实体、Mapper、Service（含删除前检查 `repair_order` 引用）、Controller（#3~#6），分页用 MyBatis-Plus `Page`。

**验收**

```bash
TOKEN=<上一步token>
curl "http://localhost:8080/api/equipments?page=1&size=10" -H "Authorization: Bearer $TOKEN"
```

返回 5 条种子设备；`POST` 新增一台后再查总数变为 6；用 REPORTER 的 token 调 `POST /api/equipments` 返回 403。

---

### Phase 4：报修工单状态机

**动作**

1. `RepairOrder` 实体、Mapper。
2. `RepairOrderService` 实现 #7~#11，严格执行第 5 节状态机与第 6.3 节规则 2。
3. 工单号生成：`RO` + `yyyyMMddHHmmss` + 3 位随机数。
4. `accept` 时同步 `equipment.status='REPAIRING'`；`complete` 时恢复 `NORMAL`（同一事务）。

**验收**（用 curl 走完整链路）

```bash
# 1. reporter 提交
curl -X POST http://localhost:8080/api/repair-orders -H "Authorization: Bearer $REPORTER_TOKEN" -H "Content-Type: application/json" -d "{\"equipmentId\":1,\"description\":\"显示器无信号\"}"
# 2. repairer 接单
curl -X PATCH http://localhost:8080/api/repair-orders/1/accept -H "Authorization: Bearer $REPAIRER_TOKEN"
# 3. repairer 完成
curl -X PATCH http://localhost:8080/api/repair-orders/1/complete -H "Authorization: Bearer $REPAIRER_TOKEN" -H "Content-Type: application/json" -d "{\"repairNote\":\"更换HDMI线\"}"
# 4. 再次 accept 应失败
curl -X PATCH http://localhost:8080/api/repair-orders/1/accept -H "Authorization: Bearer $REPAIRER_TOKEN"
```

第 4 步必须返回 `code!=0` 且 HTTP 409；`operation_log` 应有 3 条对应记录；`equipment` 状态最终为 `NORMAL`。

---

### Phase 5：巡检与二维码

**动作**

1. `QrCodeUtil`：ZXing 生成 300×300 PNG，内容为 `pointCode`。
2. 端点 #12~#17。`POST /inspection-records` 按 `pointCode` 查 point，不存在返回 404。
3. `/sync` 批量循环处理，单条失败不阻断其余，返回 `{success: n, failed: m}`。

**验收**

```bash
curl -o qr.png http://localhost:8080/api/inspection-points/1/qrcode -H "Authorization: Bearer $ADMIN_TOKEN"
curl -X POST http://localhost:8080/api/inspection-records -H "Authorization: Bearer $REPAIRER_TOKEN" -H "Content-Type: application/json" -d "{\"pointCode\":\"P-001\",\"result\":\"NORMAL\",\"remark\":\"ok\"}"
curl -X POST http://localhost:8080/api/inspection-records/sync -H "Authorization: Bearer $REPAIRER_TOKEN" -H "Content-Type: application/json" -d "{\"records\":[{\"pointCode\":\"P-002\",\"result\":\"ABNORMAL\",\"remark\":\"网线松动\",\"inspectedAt\":\"2026-11-11T10:00:00\"}]}"
```

`qr.png` 可打开识别出 `P-001`；两条记录入库，离线那条 `synced_from_offline=1`。

---

### Phase 6：耗材、库存联动与看板

**动作**

1. `Consumable`、`ConsumableTransaction`、`OperationLog` 实体与 Mapper。
2. 端点 #18~#21，严格实现第 6.3 节规则 1（事务 + 乐观扣减 + 告警日志）。
3. `DashboardController` 实现 #22~#25，SQL 聚合：
   - `equipment-status`：`SELECT status, COUNT(*) FROM equipment GROUP BY status`
   - `inspection-rate`：近 7 天 `DATE(inspected_at)` 分组
   - `consumable-top`：`SELECT c.name, SUM(t.quantity) FROM consumable_transaction t JOIN consumable c ON ... WHERE t.type='OUT' GROUP BY ... ORDER BY 2 DESC LIMIT 5`
   - `summary.alertConsumables`：`stock < threshold`
4. `FileController` 实现 #26，保存到 `./uploads/`，返回 `/uploads/<uuid>.<ext>`。

**验收**

```bash
# 出库使种子耗材(id=5, stock=2, threshold=5)继续降为 1
curl -X POST http://localhost:8080/api/consumables/5/outbound -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d "{\"quantity\":1,\"remark\":\"领用\"}"
# 库存不足
curl -X POST http://localhost:8080/api/consumables/5/outbound -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d "{\"quantity\":99,\"remark\":\"超领\"}"
curl http://localhost:8080/api/dashboard/summary -H "Authorization: Bearer $ADMIN_TOKEN"
```

第一次出库成功且 `summary.alertConsumables` 含该耗材；第二次返回 400“库存不足”；`operation_log` 有 `STOCK_ALERT` 记录。

**Phase 6 结束里程碑：后端全部端点可用。运行 `mvn -q package` 并保留 jar。**

---

## 8. 前端施工步骤

### Phase 7：前端骨架与登录

**动作**

1. `npm create vite@latest frontend -- --template vue`，安装 element-plus、pinia、vue-router、axios、echarts。
2. `vite.config.js`：`server.port=5173`，`proxy: {'/api':'http://localhost:8080', '/uploads':'http://localhost:8080'}`。
3. `api/request.js`：axios 实例，请求拦截注入 `Authorization`，响应拦截：`code!==0` 弹 `ElMessage.error` 并 reject；401 清 token 跳登录。
4. `stores/auth.js`：token + user 持久化到 localStorage。
5. `router/index.js`：按角色定义三组路由，`beforeEach` 校验角色。
6. `Login.vue`：表单 + 登录后按 role 跳转 `reporter/home` / `repairer/orders` / `admin/dashboard`。

**验收**：`npm run dev` 后浏览器打开 `http://localhost:5173`，3 个账号分别登录跳转正确页面；刷新不掉登录；手动改 localStorage 角色后访问其他端被重定向回登录。

---

### Phase 8：报修者端

**动作**

- `reporter/RepairNew.vue`：设备下拉（调 #3）+ 描述 + `el-upload` 调 #26 得 imageUrl + 提交 #7。
- `reporter/MyOrders.vue`：列表 + 状态标签（`OrderStatusTag.vue` 统一映射颜色）。
- `reporter/Home.vue`：统计卡片（我的报修总数/进行中/已完成，前端从列表计算即可）。

**验收**：浏览器完整提交一单，`MyOrders` 出现 PENDING 记录。

---

### Phase 9：维修员端

**动作**

- `repairer/OrderList.vue`：待接单 + 我的工单两个 tab；接单/拒单/完成按钮按状态渲染；完成弹框填 `repairNote`。
- `repairer/Inspection.vue`：
  - 输入 `pointCode` 模拟扫码（`el-input` + 提交按钮，**禁止引入摄像头库**）
  - 在线提交走 #15
  - 监听 `window.online/offline`；离线时把 `{pointCode,result,remark,inspectedAt}` push 到 localStorage key `pending_inspections`
  - 页面顶部显示“离线待同步 N 条”，恢复网络后自动调 #16 并清空

**验收**：DevTools Network 设为 Offline → 提交 2 条巡检 → 顶部显示待同步 2 条 → 恢复 Online → 自动同步成功提示，后端 `inspection_record` 出现 2 条且 `synced_from_offline=1`。

---

### Phase 10：管理员端

**动作**

- `admin/Equipment.vue`：表格 + 搜索 + 新增/编辑/删除弹框。
- `admin/Consumable.vue`：表格 + 入库/出库弹框（数量校验 >0）。
- `admin/InspectionPoint.vue`：列表 + 新增 + 每行“查看二维码”弹框展示 `img`。
- `admin/Dashboard.vue`：顶部 4 个统计卡（#22），若 `alertConsumables` 非空显示红色 `el-alert`；4 张 ECharts：设备状态饼图（#23）、近 7 天巡检折线（#24）、耗材出库 TOP5 柱状（#25）、工单状态分布（前端从 #8 全量计算）。

**验收**：看板 4 图渲染有数据；红色告警可见；点“查看二维码”弹窗图片可识别。

---

## 9. Phase 11：集成验收（必须逐条执行并记录结果）

| 编号 | 操作                                        | 预期                                        |
| ---- | ------------------------------------------- | ------------------------------------------- |
| A1   | 三个账号分别登录                            | 跳转正确，越权路由被拦                      |
| A2   | reporter 提交报修（含图片）                 | 图片可预览，订单 PENDING                    |
| A3   | repairer 接单 → 完成                        | 状态 ACCEPTED → COMPLETED，设备状态随之变化 |
| A4   | repairer 对已完成单再点接单（用 curl）      | 409 提示                                    |
| A5   | repairer 离线巡检 2 条 → 恢复网络           | 自动同步，后端 2 条离线记录                 |
| A6   | admin 出库耗材至低于阈值                    | 看板红色告警出现                            |
| A7   | admin 查看看板                              | 4 图有数据                                  |
| A8   | 用 reporter token 调 `/api/equipments` POST | 403                                         |
| A9   | 后端重启                                    | 种子数据不重复插入                          |
| A10  | `mvn -q package` + `npm run build`          | 均成功                                      |

**A1~A10 全部通过，Demo 完成。** 任一失败，只修复对应模块，然后重跑该条及受影响条目。

---

## 10. 强制约束与禁止项（Harness 必须遵守）

1. **禁止**修改第 5 节 DDL 的字段名/类型/枚举值；禁止在实体中新增未列字段。
2. **禁止**修改第 6.2 节任何路径与方法；新增端点须先在本文件追加并经用户确认。
3. **禁止**引入 Spring Security、Redis、Docker（作为必选）、前端扫码库、UI 框架替换（必须 Element Plus）。
4. **禁止**用 `@CrossOrigin("*")`；CORS 只允许 `http://localhost:5173`。
5. **禁止**在 Controller 中写业务逻辑；Controller 只做参数接收、调用 Service、返回 Result。
6. **禁止**在循环中逐条查库实现 N+1（看板聚合必须用 SQL 聚合）。
7. 所有 `BizException` 必须由 `GlobalExceptionHandler` 统一转换为对应 HTTP 状态码与 `Result`。
8. 密码禁止明文入库；禁止在日志中打印密码与完整 Token。
9. 前端所有接口调用必须走 `api/` 目录下的封装，禁止在组件内直接写 `axios`。
10. 每完成一个 Phase，在 `README.md` 追加一行 `[Phase N] done at <时间>`。

---

## 11. 故障排查速查（Harness 自用）

| 现象                                 | 优先检查                                                     |
| ------------------------------------ | ------------------------------------------------------------ |
| 启动报 `Communications link failure` | MySQL 未启动 / `application.yml` 密码错误 / 库 `lab_mgmt` 不存在 |
| 登录返回 500                         | `SeedDataRunner` 未执行或 BCrypt 依赖缺失                    |
| 所有接口 401                         | 前端请求头未带 `Bearer ` 前缀 / Token 过期（12h）            |
| 跨域报错                             | `WebConfig` CORS 未放行 5173 或拦截器提前返回未加 CORS 头    |
| 图片 404                             | `upload-dir` 相对路径工作目录不对；改绝对路径或确认从 `backend/` 启动 |
| ECharts 空白                         | 容器无高度；给 `div` 设 `height:300px`                       |
| 离线同步失败                         | `inspectedAt` 非 ISO 格式；后端 DTO 用 `LocalDateTime` + `@JsonFormat` |
| 分页 total 为 0                      | MyBatis-Plus 分页插件未注册（`MybatisPlusConfig`）           |

---

## 12. 交付给用户时的启动说明（Harness 在 README 中生成）

```bash
# 1. 建库
mysql -u root -p < db/schema.sql
# 2. 后端
cd backend && mvn spring-boot:run
# 3. 前端（新终端）
cd frontend && npm install && npm run dev
# 4. 浏览器
http://localhost:5173
# 账号：admin/admin123  repairer1/123456  reporter1/123456
```

---

**文档结束。Harness 从 Phase 0 开始执行，每完成一个 Phase 输出该 Phase 验收命令的实际结果，再进入下一 Phase。**