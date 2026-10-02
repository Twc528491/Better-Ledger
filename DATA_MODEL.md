# DATA_MODEL.md

## 1. 核心原则

- Local-first，本地数据为主。
- 金额使用最小货币单位整数保存，不使用 Float / Double。
- 核心实体使用 UUID。
- 历史数据原则上不物理删除。
- 支出、收入、转账严格区分。
- V1 数据结构预留多币种，但单个账本仅允许一种币种。

---

## 2. 核心实体

### Ledger

```text
id
name
baseCurrency
createdAt
updatedAt
isArchived
```

---

### Account

```text
id
ledgerId
name
type
currency
initialBalance
initialBalanceTime
createdAt
updatedAt
isArchived
```

`type`：

```text
CASH
BANK
ALIPAY
WECHAT
CREDIT_CARD
SAVINGS
INVESTMENT
LOAN
OTHER
```

余额语义：

- 正数表示资产。
- 负数表示负债。
- `CREDIT_CARD`、`LOAN` 通常以负余额表示欠款。
- 资产 / 负债分组按余额正负：`balance >= 0` 为资产，`balance < 0` 为负债；账户类型只表示账户性质，不决定分组。

---

### Transaction

```text
id
ledgerId
type
amount
currency
accountId
targetAccountId?
categoryId?
merchantId?
note?
transactionTime
createdAt
updatedAt
isDeleted
```

`type`：

```text
EXPENSE
INCOME
TRANSFER
```

规则：

- `EXPENSE`、`INCOME` 必须填写分类。
- `TRANSFER` 必须填写转出、转入账户。
- `TRANSFER` 不填写分类。
- 转账不计入收入或支出。
- `amount` 始终为正整数，方向由交易类型决定。
- 交易时间不得早于所属账户的 `initialBalanceTime`。
- `TRANSFER` 时间不得早于任一相关账户的 `initialBalanceTime`。
- `EXPENSE` 只能关联 `Category.type = EXPENSE` 的分类。
- `INCOME` 只能关联 `Category.type = INCOME` 的分类。

---

### Category

最多两级。

```text
id
ledgerId
name
type
parentId?
sortOrder
isArchived
iconKey?
```

`type`：

```text
EXPENSE
INCOME
```

`type` 创建后不可修改。

`iconKey`：用户在图标库中选择的图标标识（稳定字符串）；`null` 表示按分类名称自动匹配图标。
仅影响显示，不参与唯一性与统计；已发布的 key 不得更改或复用。

新账本预置少量默认分类（收入 / 支出各若干），全部可编辑、可归档；具体清单在实现前单独确定。

层级规则：

- 一级分类：`parentId = null`。
- 二级分类：`parentId` 必须指向同账本的一级分类。
- 父分类与子分类的 `type` 必须一致。
- 禁止三级分类和循环引用。

名称唯一规则：

- 同账本、同类型的一级分类名称唯一。
- 同一父分类下的二级分类名称唯一。
- 因 `parentId` 可为空，实现时不得仅依赖 SQLite 的复合 UNIQUE，
  Repository 必须在事务内进行唯一性校验。

---

### Merchant

```text
id
ledgerId
name
createdAt
isArchived
```

允许同名，UI 应避免误重复创建。

---

### Tag

```text
id
ledgerId
name
createdAt
isArchived
```

同账本内名称唯一。

### TransactionTag

```text
transactionId
tagId
```

---

### Budget

```text
id
ledgerId
month
categoryId?
amount
currency
createdAt
updatedAt
```

- `month` 为 `yyyy-MM` 格式（如 `2026-09`），表示预算所属月份。
- V1 每个月生成独立预算记录，不做月度模板。
- 分类预算只能关联 `EXPENSE` 分类。
- 同账本、同月份最多存在一个总预算。
- 同账本、同月份、同分类最多存在一个分类预算。
- 因总预算的 `categoryId = null`，实现时不得仅依赖 SQLite 的复合 UNIQUE，
  Repository 必须在事务内进行唯一性校验。

支持：

```text
categoryId = null   → 总预算（当月全部支出）
categoryId != null  → 分类预算（该分类树当月支出）
```

- 父分类预算包含所有子分类支出。
- 总预算与分类预算互相独立，不要求金额相加一致。
- 预算仅统计支出。

---

## 3. 实体关系

```text
Ledger
├─ Account
├─ Category
├─ Merchant
├─ Tag
├─ Budget
└─ Transaction
   ├─ Account
   ├─ TargetAccount?
   ├─ Category?
   ├─ Merchant?
   └─ Tag*
```

---

## 4. 币种规则

金额使用最小货币单位：

```text
¥25.36 → 2536
```

币种使用 ISO 4217：

```text
CNY
USD
HKD
JPY
EUR
```

V1 强制：

```text
Account.currency
=
Transaction.currency
=
Budget.currency
=
Ledger.baseCurrency
```

因此：

- 单个账本仅支持一种币种。
- V1 不支持跨币种转账。
- 可通过建立不同账本记录不同币种。
- 后续增加汇率功能时再放开限制。

---

## 5. 账户余额

账户余额由原始交易计算：

```text
initialBalance
+ initialBalanceTime 之后的收入
- initialBalanceTime 之后的支出
± 转账
```

V1 禁止 `initialBalanceTime` 之前的交易，因此所有交易均计入余额。

交易记录是主要数据源。

统计缓存不得成为唯一数据源。

---

## 6. 归档与删除

以下实体使用归档：

```text
Ledger
Account
Category
Merchant
Tag
```

归档后：

- 不再默认出现在新增交易选择列表。
- 历史交易仍显示原名称。
- 历史统计仍正常按原实体计算。
- 归档可恢复（取消归档），恢复后重新出现在新增交易选择列表。
- 已归档实体可由用户主动“彻底删除”（物理删除）：标签 / 商家清除引用；分类须先把引用迁移到同类型替代分类；账户连同其账单与定时扣款一起删除；账本删除其全部数据。不可恢复，默认先自动备份。

交易删除：

```text
isDeleted = true
```

- 删除的账单进入回收站，可恢复；恢复后重新参与余额和统计。
- 软删除交易不参与正常余额和统计。
- 不自动永久删除；用户可在回收站中手动彻底删除选中账单或清空回收站（同时删除其标签关联与定时扣款执行记录）。

---

## 7. 必须保证的约束

```text
Transaction.amount > 0

EXPENSE / INCOME → categoryId 必填

TRANSFER → targetAccountId 必填
TRANSFER → accountId != targetAccountId
TRANSFER → categoryId = null

Category 最多两级
Category.type 创建后不可修改

所有关联实体属于同一 Ledger

交易时间不得早于所属账户 initialBalanceTime
TRANSFER 时间不得早于任一账户 initialBalanceTime
修改账户 initialBalanceTime 不得使其晚于该账户已有交易时间

V1 所有金额币种必须等于 Ledger.baseCurrency

EXPENSE → Category.type = EXPENSE
INCOME → Category.type = INCOME

Budget.categoryId != null → Category.type = EXPENSE

Category 二级分类的 parentId 必须指向一级分类
Category 父子 type 必须一致
Category 禁止三级和循环引用
```

---

## 8. V1 暂不处理

```text
跨币种账户与转账
自动汇率
投资收益详细计算
股票 / 基金持仓
贷款还款计划
信用卡账单周期
多设备同步
共享账本
自动导入支付账单
转账手续费建模（手续费另记一笔支出）
回收站自动清理
```