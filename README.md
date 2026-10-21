# Better Ledger

一个完全离线的 Android 个人记账 App：数据只保存在本机，不需要账号、服务器或网络。

An offline-first personal bookkeeping app for Android (Chinese / English).

## 功能

- 支出 / 收入 / 转账记账，两级分类（可选图标库）、商家、标签、备注
- 多账本；账户（资产）在所有账本间共享，可合并重复账户
- 账单按月浏览、搜索与筛选，长按多选批量删除，回收站可恢复或彻底删除
- 统计：分类占比、收支趋势（6 / 12 个月）、每日累计对比、分类环比、预算执行
- 预算、定时扣款（本地按周期自动生成支出）
- 备份：默认目录 `Documents/LedgerBackups` 每日自动备份并轮换，备份管理页；导出 CSV / JSON / SQLite，整库恢复
- 归档数据可彻底删除；生物识别 / 设备凭据安全锁；深色模式；简体中文 / English

## 技术栈

Kotlin · Jetpack Compose (Material 3) · Room · MVVM · DataStore · WorkManager · kotlinx-serialization。
minSdk 26（Android 8.0），targetSdk 36；只使用标准 Android / Jetpack API，不依赖特定厂商系统。

## 构建

需要 JDK 21 与 Android SDK（platform 36）。

```bash
cd ledger-app
./gradlew assembleDebug testDebugUnitTest   # Windows 用 gradlew.bat
```

- 首次构建前在 `ledger-app/local.properties` 中写入 `sdk.dir=<Android SDK 路径>`，或设置环境变量 `ANDROID_HOME`。
- release 包：`./gradlew assembleRelease`，输出在 `ledger-app/app/build/outputs/apk/release/`。目前使用 debug 签名，正式分发前需替换为独立 keystore。
- 仪器测试：`./gradlew connectedDebugAndroidTest`（需真机或模拟器）。debug 包名带 `.debug` 后缀，不会影响已安装的正式版。
- 根目录的 `build.ps1` 是作者本机脚本（依赖仓库外的自带工具链），一般无需使用。

## 文档

| 文件 | 内容 |
|---|---|
| [CHANGELOG.md](CHANGELOG.md) | 版本更新记录 |
| [docs/PROJECT.md](docs/PROJECT.md) | 项目定位与原则（初始规划） |
| [docs/MVP.md](docs/MVP.md) | 最初的功能范围（初始规划） |
| [docs/DATA_MODEL.md](docs/DATA_MODEL.md) | 数据模型与约束 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 架构 |
| [docs/UI.md](docs/UI.md) | 界面设计 |
| [docs/DESIGN_NOTES.md](docs/DESIGN_NOTES.md) | 实现中的设计决策与取舍 |

## 关于开发方式

本项目的代码与文档在 AI 编程助手（Anthropic Claude）的协助下编写，所有改动均经作者审阅、构建与测试后提交。

## 许可证

[MIT](LICENSE)
