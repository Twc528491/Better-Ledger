# 实现备注（IMPLEMENTATION NOTES）

工程实现过程中在文档（PROJECT / MVP / DATA_MODEL / ARCHITECTURE / UI）之外做出的具体决策，
供确认或后续调整。均为易改项，改动后需重新构建。

## 工程标识

| 项 | 决策 | 说明 |
|---|---|---|
| 包名 / applicationId | `com.personal.ledger` | 改动需同步目录结构，建议尽早确认 |
| 应用名 | 个人账本 | 取自 UI.md 首页示例 |
| minSdk / targetSdk / compileSdk | 26 / 36 / 36 | minSdk 26 为直接使用 java.time；文档未规定 minSdk |
| 数据库文件名 | `ledger.db`，应用私有存储 | |
| Room 数据版本 | 3 | 显式迁移 `1→2` 增加订阅规则与执行标记，`2→3` 增加商家分类关联；不使用破坏式重建 |
| 当前版本 | `0.3.0`（beta） | 统计页增加四种可横向切换的真实数据图表，首页与统计摘要采用一致的平面主次卡片层级；当前以 debug 签名构建，供测试使用 |
| 统计图表 | 分类占比、近 6 个月收支、每日支出、分类排名 | 饼图合并前五名以外金额并精确保留总额；每日聚合按系统时区，图表提供币种轴标和无障碍摘要；完整分类明细与预算区块保留在图表下方 |

## 数据与业务

| 项 | 决策 |
|---|---|
| 默认分类种子 | 新账本预置 32 个支出、9 个收入一级分类，保留旧版种子 ID 和顺序；新增订阅、生鲜、零食、咖啡、家政、快递、捐赠、烟酒等。旧账本可在分类管理主动补充，按稳定 ID 和同名项跳过已存在或已归档分类；删除采用软归档 |
| 新账本种子账户 | 自动创建一个"现金"账户（0 初始余额），保证开箱即记；文档未规定 |
| 默认账本 | 名称"个人账本"、币种 CNY |
| 记账类型切换 | 切换支出/收入/转账时自动清空已选的分类 / 转入账户，防止脏数据 |
| 记一笔布局 | 顶部类型与语义色金额容器、横向分页的四列一级分类网格（依可用高度两或三行，常规徽章 48dp / 极小屏 36dp）、紧凑的账户/日期时间/商家/备注/标签字段；底部固定四行数字键盘，右侧清空与跨三行保存。金额输入不唤起系统 IME，文本在对话框中输入；日期与 HH:mm 分别编辑。转账保留双账户且无分类；小屏或大字体时正文整体可滚动。新选择不展示二级分类，编辑旧账单保留原 ID 并显示父 / 子 |
| 安全锁超时 | 离开应用超过 30 秒后重新验证（文档只说"较长时间"） |
| 安全锁开启流程 | 开启前先做一次生物识别 / 设备凭据验证，成功才启用 |
| 安全锁负按钮 | "退出"直接关闭应用，不可绕过 |
| 交易校验 | 纯规则（金额 / 分类类型匹配 / 转账账户）在 `domain/calc/TransactionRules`，有 JVM 测试 |
| 触感反馈 | 中央“记一笔”使用系统 `EFFECT_CLICK`，账单长按使用增强短触感，最终删除确认使用公开 `EFFECT_DOUBLE_CLICK` 并安全降级；普通导航、取消 / 返回 / 外部 dismiss 无应用触感，遵循系统触感开关 |
| 账单删除 | 首页与账单页均为长按账单 → 删除操作 → 二次确认，短按继续进入编辑 |
| 商家 / 分类删除 | 管理页移除归档 / 三点入口，长按进入重命名或删除菜单；删除采用软归档，保留历史账单与预算引用；一级分类删除同步停用二级分类 |
| 商家与分类 | 商家可关联多个一级分类；`2→3` 迁移从既有交易（含软删除历史）提取有效关联，保持同账本、同收支类型约束 |
| 分类图标 | 41 个预置一级分类按语义映射 Material Vector，常用分类用 TwoTone 内部明暗层次；自定义名称按关键词和收支类型确定性映射，未知名称使用固定回退。圆形底色有克制的同色渐变与边缘层次，保留图标无障碍名称 |
| 订阅自动记账 | 本地订阅规则支持每日 / 每周 / 每月 / 每年，到期自动生成普通支出交易；WorkManager 最短 15 分钟周期按系统调度尽力执行，打开应用时补处理到期项目。执行标记、交易、下次时间在同一事务内，防重复；暂停期间不补记，恢复后从下一次到期继续。不会向商家或支付渠道发起真实扣款 |
| 备份兼容 | JSON 格式版本 3，可读取旧版 1 / 2；包含订阅数据及商家分类关联，恢复时校验引用和版本。SQLite 备份按数据库版本检查 |
| 系统窗口留白 | 根 Scaffold 不重复处理系统栏；各页顶栏处理状态栏，tab 底栏处理手势区，非 tab 页由自身 Scaffold 处理底部安全区；导航内容消费已应用的 inset |

## 构建与工具链（本机沙箱环境）

| 项 | 决策 |
|---|---|
| JDK | 工作区 `.tools/jdk21`（完整 OpenJDK 21；CLion JBR 缺 jlink 不可用） |
| Gradle | 8.13，发行版在工作区 `.tools/gradle-8.13` |
| Android SDK | 工作区 `.android-sdk`（platform 36 / build-tools 36.0.0 / platform-tools） |
| wrapper 发行版地址 | 腾讯镜像（官方地址跳转 GitHub 被本网络拦截） |
| 依赖仓库 | 阿里云镜像优先（google / central / gradle-plugin） |
| KSP | `ksp.useKSP2=false`（KSP1 模式；KSP2 在本环境有 MissingType 兼容问题） |
| 构建脚本 | 根目录 `build.ps1`（自动设置 JAVA_HOME / GRADLE_USER_HOME / ANDROID_USER_HOME） |
| release 签名 | 暂用 debug keystore（个人自用便于直接安装）；正式分发需独立 keystore |

### 构建 / 安装命令

```powershell
# 默认构建（debug + 单元测试）
powershell -ExecutionPolicy Bypass -File build.ps1

# 构建 release 并直接安装到 USB 连接的手机（需开启 USB 调试）
powershell -ExecutionPolicy Bypass -File build.ps1 -Task "assembleRelease" -Install
```

## 测试

| 项 | 状态 |
|---|---|
| JVM 单元测试 | 本轮 `testDebugUnitTest` 成功；XML 共 51 项，0 失败、0 错误、0 跳过 |
| 仪器测试（真机/模拟器运行） | `assembleDebugAndroidTest` 编译打包成功；本轮未在真机或模拟器运行 |
| Room Schema | 导出至 `app/schemas/`，纳入版本管理 |

真机运行仪器测试（USB 连接 + 开启 USB 调试）：

```powershell
powershell -ExecutionPolicy Bypass -File build.ps1 -Task "connectedDebugAndroidTest"
```

> 备注：曾尝试 Robolectric 在 JVM 上运行数据层测试，最终因测试进程写 SQLite 原生
> DLL 被系统安全防护拒绝（沙箱无特权场景）而放弃，数据层测试改为仪器测试。
> Robolectric 依赖已从工程移除。

## 版本与依赖（2025 年稳定组合，仓库中已验证存在）

AGP 8.13.2 · Kotlin 2.1.21 · KSP 2.1.21-2.0.2 · Compose BOM 2025.06.01 ·
Room 2.7.2 · Navigation 2.9.0 · Lifecycle 2.9.1 · DataStore 1.1.7 ·
WorkManager 2.10.1 · Biometric 1.1.0 · kotlinx-serialization 1.8.1
