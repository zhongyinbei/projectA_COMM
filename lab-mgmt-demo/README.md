# LabCare · 实训室设备巡检与耗材智能管理平台 Demo

一个前后端分离的三端 Web Demo：**报修者端**提交报修、**维修员端**接单与巡检、**管理员端**管理设备/耗材/巡检点并查看看板。

## 1. 功能范围

- 三类角色登录与 JWT 鉴权，越权接口返回 403，未登录返回 401。
- 报修工单状态机：`PENDING → ACCEPTED → COMPLETED`，或 `PENDING → REJECTED`；重复操作返回 409。
- 设备台账 CRUD，删除存在工单引用的设备返回 409。
- 巡检点管理与 300 × 300 二维码生成，在线巡检与离线批量同步。
- 耗材入库/出库，出库乐观扣减库存，低于阈值时写告警日志并在看板红色提示。
- 管理员看板：4 张 ECharts 图表（设备状态、近 7 天巡检、耗材出库 TOP5、工单状态）。
- 图片上传与静态访问，报修单可附带现场图片。

不在 Demo 范围：短信/邮件、真实摄像头扫码、文件云存储、多租户、审批流、AI 推荐。

## 2. 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | JDK 17 目标字节码、Spring Boot 3.2.5、MyBatis-Plus 3.5.6、MySQL 8.3 驱动 |
| 鉴权 | jjwt 0.12.5 + 自定义 `JwtInterceptor` / `@RequireRole` |
| 密码 | `BCryptPasswordEncoder`（仅引入 spring-security-crypto，不使用 Security 框架） |
| 二维码 | ZXing core + javase 3.5.3 |
| 前端 | Vite 5.4.8、Vue 3.4.38、vue-router 4.4.3、Pinia 2.2.2、Element Plus 2.7.8、axios 1.7.7、ECharts 5.5.1 |
| 数据库 | MySQL 8.0，库名 `lab_mgmt`，`utf8mb4` |

Lombok 固定为 `1.18.48` 并在 `maven-compiler-plugin` 中声明注解处理器路径，使项目在 JDK 17 目标字节码下可编译，同时兼容本机较新的 JDK。若使用标准 JDK 17 环境，无需额外配置。

## 3. 目录结构

```text
lab-mgmt-demo/
├── README.md
├── db/schema.sql
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/lab/
│       ├── LabApplication.java
│       ├── common/          Result / PageResult / BizException / GlobalExceptionHandler
│       ├── config/          WebConfig / MybatisPlusConfig / SeedDataRunner
│       ├── interceptor/     JwtInterceptor / RequireRole
│       ├── util/            JwtUtil / QrCodeUtil
│       ├── entity/          8 个实体
│       ├── mapper/          8 个 Mapper
│       ├── service/         8 个 Service 接口
│       ├── service/impl/    8 个实现
│       ├── controller/      7 个 Controller
│       └── dto/             请求 DTO 与响应 VO
└── frontend/
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── api/             所有请求封装
        ├── stores/          auth
        ├── router/          三端路由与角色守卫
        ├── layouts/         三端布局
        ├── views/           登录、报修者、维修员、管理员页面
        └── components/      OrderStatusTag
```

## 4. 启动说明

### 4.1 环境要求

- JDK 17（项目按 `--release 17` 编译）
- Maven 3.8+
- Node.js 18+（本机验证使用 Node 24）
- MySQL 8.0，默认端口 3306

### 4.2 建库与建表

```bash
# macOS / Linux
mysql -u root -p < db/schema.sql

# Windows PowerShell
cmd /c "mysql -u root -p < db\schema.sql"
```

默认数据库配置位于 `backend/src/main/resources/application.yml`：

```yaml
username: root
password: 123456
```

请改成你本机的 MySQL 密码。当前演示库密码为 `123456`。

如需重置演示数据：

```bash
mysql -u root -p -e "DROP DATABASE IF EXISTS lab_mgmt;"
cmd /c "mysql -u root -p < db\schema.sql"
```

### 4.3 启动后端

```bash
cd backend
mvn spring-boot:run
```

或使用已构建的 jar：

```bash
java -jar target/lab-mgmt-demo-1.0.0.jar
```

后端地址：<http://localhost:8080>

### 4.4 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问：<http://localhost:5173>

Vite 已配置 `/api` 与 `/uploads` 到 `http://localhost:8080` 的代理。

生产构建：

```bash
npm run build
```

### 4.5 演示账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `admin123` |
| 维修员 | `repairer1` | `123456` |
| 报修者 | `reporter1` | `123456` |

首次启动时 `SeedDataRunner` 自动写入 3 个账号、5 台设备、3 个巡检点、5 种耗材；已有用户时跳过，不会重复插入。第五种耗材“光纤跳线”初始库存 2、阈值 5，用于库存告警演示。

当前数据库保留了验收过程产生的演示数据（2 张工单、4 条巡检记录、光纤跳线库存 1），便于直接查看看板效果；如需干净种子数据，按 4.2 重置即可。

## 5. 核心机制

### 5.1 鉴权

- 登录成功后签发 HS256 JWT，载荷含 `sub=userId`、`role`，有效期 12 小时。
- `JwtInterceptor` 解析 `Authorization: Bearer <token>`，把当前用户放入 `ThreadLocal`。
- Controller 使用 `@RequireRole({"ADMIN"})` 声明角色；拦截器读取注解并返回 403。
- 未登录或 Token 失效返回 401 JSON；前端收到 401 后清理本地会话并回到登录页。
- 前端路由守卫还会校验 JWT 中的角色与 `localStorage` 用户角色是否一致，手动篡改角色会清空会话并跳回登录。

### 5.2 工单状态机

```text
PENDING  → ACCEPTED | REJECTED
ACCEPTED → COMPLETED
REJECTED / COMPLETED 为终态
```

`accept` / `complete` 使用 `UPDATE ... WHERE id=? AND status=?` 乐观校验，影响行数为 0 时返回 409“工单状态已变化”。接单写 `assignee_id`、`accepted_at` 并把设备改成 `REPAIRING`；完成时把设备恢复为 `NORMAL`。每次成功流转写入 `operation_log`。

### 5.3 库存与告警

出库在单个事务内完成：

1. `UPDATE consumable SET stock=stock-? WHERE id=? AND stock>=?`
2. 影响行数为 0 → 400“库存不足”
3. 写 `consumable_transaction(type=OUT)`
4. 库存低于阈值 → 写 `operation_log(biz_type=STOCK_ALERT, action=LOW_STOCK)`

### 5.4 离线巡检

- 页面监听 `window.online` / `window.offline`。
- 离线提交把 `{pointCode,result,remark,inspectedAt}` 写入 `localStorage` 的 `pending_inspections`。
- 顶部显示“离线待同步 N 条”；恢复网络后自动调用 `/api/inspection-records/sync`。
- 后端使用请求中的 `inspectedAt`，写入 `synced_from_offline=1`，并为每条成功同步记录写 `SYNC_OFFLINE` 日志。

## 6. API 一览

所有请求前缀 `/api`，统一响应：

```json
{ "code": 0, "message": "ok", "data": {} }
```

分页响应：`{ "records": [], "total": 0, "page": 1, "size": 10 }`。

| 模块 | 端点 |
| --- | --- |
| 认证 | `POST /auth/login`、`GET /auth/me` |
| 设备 | `GET/POST /equipments`、`PUT/DELETE /equipments/{id}` |
| 工单 | `POST/GET /repair-orders`、`PATCH /repair-orders/{id}/accept|reject|complete` |
| 巡检点 | `GET/POST /inspection-points`、`GET /inspection-points/{id}/qrcode` |
| 巡检记录 | `POST /inspection-records`、`POST /inspection-records/sync`、`GET /inspection-records` |
| 耗材 | `GET/POST /consumables`、`POST /consumables/{id}/inbound|outbound` |
| 看板 | `GET /dashboard/summary|equipment-status|inspection-rate|consumable-top` |
| 文件 | `POST /files/upload` |

权限严格按实施文档执行。例如设备写操作仅 ADMIN，巡检提交仅 REPAIRER，耗材接口仅 ADMIN。

## 7. 验收记录

| 编号 | 验收内容 | 结果 |
| --- | --- | --- |
| A1 | 三类账号登录、越权路由拦截、篡改角色回登录 | 通过（浏览器验收；篡改后跳转 `/login?reason=invalid` 并清除 Token） |
| A2 | 报修者提交带图片报修，订单 PENDING | 通过（图片上传成功，工单列表出现“待接单”） |
| A3 | 维修员接单 → 完成，设备状态变化 | 通过（ACCEPTED → COMPLETED，设备恢复 NORMAL） |
| A4 | 已完成工单再次接单返回 409 | 通过（HTTP 409，`code=409`） |
| A5 | 离线巡检 2 条 → 恢复网络自动同步 | 通过（本地待同步 2 条 → 0，后端记录 `synced_from_offline=1`） |
| A6 | 管理员出库至低于阈值，看板出现红色告警 | 通过（库存 2 → 1，`STOCK_ALERT` 日志，红色预警条） |
| A7 | 管理员看板 4 张 ECharts 有数据 | 通过（4 个 canvas，无前端控制台错误） |
| A8 | reporter token 调设备新增接口返回 403 | 通过 |
| A9 | 后端重启种子数据不重复插入 | 通过（重启日志“检测到已有种子数据，跳过初始化”，用户仍为 3） |
| A10 | `mvn package` + `npm run build` | 通过（Spring Boot jar 与 Vite dist 均生成成功） |

补充验证：

- 二维码实扫内容：`QR=P-001 WIDTH=300 HEIGHT=300`。
- 后端接口自动验收 24 项全部通过，覆盖登录、401/403、设备 5→6→5、409、巡检离线标记、库存不足 400、看板聚合、图片静态访问、CORS。
- 浏览器端验收 11 项全部通过，页面无 `pageerror` / 控制台错误；1440px 与 390px 宽度下均无横向溢出。

## 8. 演示流程建议

1. 用 `reporter1` 登录，发起一条带图片报修。
2. 用 `repairer1` 登录，接单并在“我的工单”完成维修，填写维修说明。
3. 打开“巡检作业”，在 DevTools Network 中设为 Offline，提交 2 条巡检；恢复 Online，观察自动同步。
4. 用 `admin` 登录，在“耗材库存”对“光纤跳线”出库 1，查看看板红色告警。
5. 在“巡检点管理”查看二维码，确认内容为巡检点编码。

## 9. 说明与简化

- Demo 不引入 Redis、消息队列、Spring Security 框架、摄像头扫码库。
- 图片保存在后端工作目录 `./uploads/`，通过 `/uploads/**` 静态映射访问；生产环境应替换为对象存储。
- 看板“工单状态分布”严格由前端调用工单列表接口后聚合，其余看板数据由后端 SQL 聚合。
- 所有中文面向用户文案与注释为中文，代码标识符为英文。

## 10. 实施进度

```text
[Phase 0] done at 2026-11-12 14:47
[Phase 1] done at 2026-11-12 15:07
[Phase 2] done at 2026-11-12 15:11
[Phase 3] done at 2026-11-12 15:14
[Phase 4] done at 2026-11-12 15:19
[Phase 5] done at 2026-11-12 15:19
[Phase 6] done at 2026-11-12 15:25
[Phase 7] done at 2026-11-12 17:56
[Phase 8] done at 2026-11-12 17:58
[Phase 9] done at 2026-11-12 17:59
[Phase 10] done at 2026-11-12 18:02
[Phase 11] done at 2026-11-12 18:25
```
