# PROJECT.md

> 本文档是项目最初的规划，保留以记录来龙去脉；部分内容（如“V1 暂不处理”的范围）已被后续版本取代，以 [DESIGN_NOTES](DESIGN_NOTES.md) 与 [CHANGELOG](../CHANGELOG.md) 为准。

## 1. 项目定位

一个长期可用的 Android 本地记账 App，面向 Android 8.0（API 26）及以上的各类设备，只依赖标准 Android / Jetpack API，不针对特定厂商系统做定制。

核心目标：

- 快速记账
- 数据长期可靠
- 完整离线可用
- 可备份、可恢复、可迁移
- 后续可以持续扩展

---

## 2. 技术方向

```text
Android Only
Kotlin
Jetpack Compose
Room
MVVM + Repository
Local-first
```

核心功能不依赖账号、服务器或网络。

---

## 3. 核心功能

V1 包括：

- 多账本
- 收入 / 支出 / 转账
- 账户管理
- 两级分类
- 商家与标签
- 月度预算
- 账单查询与筛选
- 基础统计
- 数据导入 / 导出
- 自动备份
- 本地安全锁

具体功能以 `MVP.md` 为准。

---

## 4. 数据原则

- 本地数据库是核心数据源。
- 金额不得使用浮点数保存。
- 历史账目不得因配置修改而损坏。
- 数据库升级必须提供 Migration。
- 禁止 destructive migration。
- 删除优先采用归档或软删除。
- 统计结果由原始交易数据计算。
- 用户数据必须能够完整导出和恢复。

具体结构以 `DATA_MODEL.md` 为准。

---

## 5. 开发原则

优先级：

```text
正确性
> 数据安全
> 可维护性
> 使用效率
> UI 效果
> 功能数量
```

开发过程中：

- 不为未来可能的功能过度设计。
- 不为了架构完整而增加无必要复杂度。
- 优先使用 Android / Jetpack 官方方案。
- 新增依赖前确认其必要性和维护状态。
- UI、业务逻辑和数据库访问保持分离。
- 核心功能必须在无网络环境正常工作。

架构以 `ARCHITECTURE.md` 为准。

---

## 6. V1 暂不做

```text
云同步
账号系统
家庭共享
iOS
Web
自动读取微信 / 支付宝
OCR
AI 自动记账
复杂投资分析
贷款计划
自动汇率
桌面 Widget
```

未来功能按实际需求逐步增加，不提前实现。

---

## 7. 项目文档

```text
PROJECT.md
├─ 项目目标与原则

MVP.md
├─ V1 功能范围

DATA_MODEL.md
├─ 数据结构与规则

ARCHITECTURE.md
├─ 技术架构

UI.md
└─ 界面与交互规范
```

发生冲突时：

```text
数据正确性与安全
> DATA_MODEL.md
> MVP.md
> ARCHITECTURE.md
> UI.md
> 实现便利性