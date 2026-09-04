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
| 当前版本 | `0.2.1`（beta） | 信息密度、浅色配色和导航修订版；当前以 debug 签名构建，供测试使用 |

## 数据与业务

| 项 | 决策 |
|---|---|
| 默认分类种子（决策 7 占位） | 支出：餐饮 / 交通 / 购物 / 居住 / 娱乐 / 医疗 / 其他；收入：工资 / 其他收入。全部可编辑、可归档 |
| 新账本种子账户 | 自动创建一个"现金"账户（0 初始余额），保证开箱即记；文档未规定 |
| 默认账本 | 名称"个人账本"、币种 CNY |
| 记账类型切换 | 切换支出/收入/转账时自动清空已选的分类 / 转入账户，防止脏数据 |
| 安全锁超时 | 离开应用超过 30 秒后重新验证（文档只说"较长时间"） |
| 安全锁开启流程 | 开启前先做一次生物识别 / 设备凭据验证，成功才启用 |
| 安全锁负按钮 | "退出"直接关闭应用，不可绕过 |
| 交易校验 | 纯规则（金额 / 分类类型匹配 / 转账账户）在 `domain/calc/TransactionRules`，有 JVM 测试 |

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
| JVM 单元测试 | 33 个：金额（含币种格式化） / 余额 / 预算 / 月度趋势 / 交易规则 / 备份轮换 / JSON 往返 / CSV 转义 |
| 仪器测试（真机/模拟器运行） | 14 个：`DataLayerTest`（10 个：种子数据、余额与转账、交易约束、回收站、预算、分类唯一性、标签商家、账本归档回退、跨账本关联、转入账户初始时间）+ `DatabaseSmokeTest` + `MigrationTest`（v1 schema 校验）+ `LedgerComponentsTest`（2 个 Compose UI 测试：设置区块动作与环形图无障碍摘要） |
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
