# Android 记账 App 构建脚本
# 用法:
#   powershell -ExecutionPolicy Bypass -File build.ps1                      # 默认 assembleDebug + 单元测试
#   powershell -ExecutionPolicy Bypass -File build.ps1 -Task "assembleRelease"
#   powershell -ExecutionPolicy Bypass -File build.ps1 -Install             # 构建后通过 adb 安装 release 包
#   powershell -ExecutionPolicy Bypass -File build.ps1 -Task "clean assembleDebug" -Install
param(
    [string]$Task = "assembleDebug testDebugUnitTest",
    [switch]$Install
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

# 自包含工具链（均位于工作区内）
$env:JAVA_HOME = "$root\.tools\jdk21"
$env:GRADLE_USER_HOME = "$root\ledger-app\.gradle-home"
$env:ANDROID_USER_HOME = "$root\.android-home"
Remove-Item Env:ANDROID_SDK_HOME -ErrorAction SilentlyContinue

if (-not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    Write-Error "JDK not found: $env:JAVA_HOME (download a full JDK 21 into .tools\jdk21)"
    exit 1
}

& "$root\.tools\gradle-8.13\bin\gradle.bat" -p "$root\ledger-app" $Task --console=plain
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed"
    exit $LASTEXITCODE
}

if ($Install) {
    $apk = "$root\ledger-app\app\build\outputs\apk\release\app-release.apk"
    if (-not (Test-Path $apk)) {
        # 只构建了 debug 时回退到 debug 包
        $apk = "$root\ledger-app\app\build\outputs\apk\debug\app-debug.apk"
    }
    if (-not (Test-Path $apk)) {
        Write-Error "APK not found. Run a build task first (e.g. assembleRelease)."
        exit 1
    }
    $adb = "$root\.android-sdk\platform-tools\adb.exe"
    if (-not (Test-Path $adb)) {
        Write-Error "adb not found: $adb"
        exit 1
    }
    & $adb install -r $apk
    exit $LASTEXITCODE
}
