# ARCHITECTURE.md

## 1. 技术栈

```text
Language        Kotlin
UI              Jetpack Compose + Material 3
Architecture    MVVM + Repository
Database        Room + SQLite
Async           Coroutines
State           Flow / StateFlow
Settings        DataStore
Navigation      Navigation Compose
Background      WorkManager
Build           Gradle Kotlin DSL
Android         targetSdk 36（edge-to-edge + 预测性返回）
```

V1 不引入复杂 Clean Architecture 和重型依赖注入框架。

---

## 2. 总体结构

```text
Compose UI
    ↓
ViewModel
    ↓
Repository
    ↓
Room DAO
    ↓
SQLite
```

UI 不直接访问数据库。

---

## 3. 项目结构

```text
app/
├─ ui/
│  ├─ home/
│  ├─ transaction/
│  ├─ statistics/
│  ├─ account/
│  └─ settings/
├─ viewmodel/
├─ data/
│  ├─ local/
│  │  ├─ entity/
│  │  ├─ dao/
│  │  └─ database/
│  ├─ repository/
│  └─ backup/
├─ domain/
│  └─ model/
├─ navigation/
├─ worker/
└─ util/
```

V1 保持单 `app` module。

---

## 4. 数据层

Room Entity 与业务 Model 分离：

```text
Room Entity
    ↓ Mapper
Domain Model
    ↓
ViewModel
    ↓
UI
```

数据库要求：

- 明确维护版本号。
- Schema 修改必须提供 Migration。
- 禁止 destructive migration。
- Migration 使用 MigrationTestHelper 做 instrumentation 测试，并导出保存 Room Schema。
- 对常用查询字段建立必要索引。
- 对包含 nullable 字段的业务唯一性约束，不得仅依赖 SQLite UNIQUE；
  必须由 Repository 在事务内完成校验。

---

## 5. 状态与异步

```text
数据库数据     Flow
页面状态       StateFlow
异步任务       Coroutines
后台任务       WorkManager
```

耗时操作不得阻塞主线程。

---

## 6. Repository

主要 Repository：

```text
TransactionRepository
AccountRepository
CategoryRepository
BudgetRepository
BackupRepository
```

ViewModel 不直接依赖 DAO。

---

## 7. DataStore

用于非账本数据：

```text
当前账本
主题
安全锁设置
自动备份设置
最近使用账户
```

核心账本数据必须存入 Room。

DataStore 内容（当前账本、主题、安全锁设置、自动备份设置、最近使用账户）属于 App 设置，不参与备份与恢复。

---

## 8. 备份

支持：

```text
CSV Export（UTF-8 BOM；含普通交易与转账，不含已删除交易）
JSON Backup（含所有账本数据，并包含备份格式版本）
SQLite Backup（含所有账本数据）
```

JSON 顶层至少包含：

backupFormatVersion
exportedAt
appVersion
data

JSON / SQLite 完整备份包含软删除交易；
CSV 不包含软删除交易。

自动备份由 WorkManager 执行，WorkManager 每日执行一次 best-effort 自动备份，
并按每日 / 每周 / 每月快照策略轮换；
不要求固定精确时间执行。

备份目录由用户通过系统文件选择器（SAF）指定，不申请全盘文件权限。
保留策略：7 个每日 + 4 个每周 + 12 个每月。

恢复为整库覆盖，不做合并；恢复前自动备份当前数据；恢复后若当前账本 ID 失效，自动选择第一个有效账本。

备份失败不得影响正常记账。

---

## 9. 安全

V1：

- 数据库存放于 Android 应用私有存储。
- 安全锁仅使用系统 BiometricPrompt（生物识别 / 设备凭据），不自行实现 PIN。
- 默认关闭；开启后冷启动和较长时间离开 App 后要求验证。
- 暂不引入 SQLCipher 或额外数据库加密。
- 默认无账号、无云端、无追踪。

后续如有需要，再单独设计数据库和备份加密。

---

## 10. 性能与轻量化

目标：快速启动、即时响应、保持精简。

- 优先使用 Android / Jetpack 官方能力。
- 不引入非必要第三方 SDK 和大型依赖。
- 首页只加载当前需要的数据。
- 数据筛选和统计优先由数据库完成。
- 自动备份不得阻塞用户操作。
- 统计图表使用 Compose Canvas 自绘（环形 / 柱状 / 折线 / 进度条），不引入图表库。
- 优先使用矢量资源。
- 无需求时不引入网络、广告、Analytics 等组件。
- 定期检查安装包大小和启动性能。

---

## 11. V1 不引入

```text
Flutter
Kotlin Multiplatform
复杂 Clean Architecture
Hilt / Dagger
云数据库
Remote API
账号系统
多模块工程
复杂事件总线
```

项目复杂度明显增加后再评估。

---

## 12. 测试

- 金额、余额、预算、转账等纯业务逻辑使用 JVM 单元测试。
- Room Migration 使用 MigrationTestHelper 做 instrumentation 测试。
- Room Schema 导出并纳入版本管理。