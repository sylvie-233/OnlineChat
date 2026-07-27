# ==================================================
# OnlineChat IM 系统 — 项目构建与运行脚本
# 用法: ./build.ps1 [command] [options]
# 命令:
#   clean       - 清理构建目录
#   build       - 编译后端
#   package     - clean + 打包后端 fat jar
#   up          - 启动中间件（MySQL/Redis/MinIO）
#   down        - 停止全部服务
#   run         - 启动后端 jar（需先 package）
#   dev         - 启动中间件 + 本地开发提示
#   logs        - 实时查看后端日志（tail -f）
#   install     - 下载 Maven 依赖
# ==================================================

param(
    [Parameter(Position = 0)]
    [ValidateSet('clean', 'build', 'package', 'up', 'down', 'run', 'dev', 'logs', 'install')]
    [string]$Command = '',

    [Parameter()]
    [switch]$Help,

    [Parameter()]
    [switch]$SkipTests,

    [Parameter()]
    [int]$TailLines = 100,

    [Parameter()]
    [string]$LogFile = ''
)

# 项目路径
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = Join-Path $ProjectRoot "backend"
$ScriptsDir = Join-Path $ProjectRoot "scripts"
$LogDir = Join-Path $ProjectRoot "logs"

function Write-Step { param([string]$M) Write-Host "`n>>> $M" -ForegroundColor Cyan }
function Write-Success { param([string]$M) Write-Host "  OK  $M" -ForegroundColor Green }
function Write-ErrorExit { param([string]$M) Write-Host "  ERR $M" -ForegroundColor Red; exit 1 }

function Show-Help {
Write-Host @"
OnlineChat IM 构建脚本

用法: ./build.ps1 <command> [options]

Commands:
  clean       清理构建目录
  build       编译后端（Maven compile）
  package     清理、编译并打包 fat jar
  up          启动中间件（MySQL + Redis + MinIO）
  down        停止全部服务
  run         启动后端 jar（需先 package）
  dev         启动中间件 + 本地开发提示
  logs        实时查看后端日志（tail -f，Ctrl+C 退出）
  install     下载 Maven 依赖

Options:
  -Help        显示帮助信息
  -SkipTests   打包时跳过测试（默认跳过）
  -TailLines   日志显示行数（默认 100，仅 logs 命令）
  -LogFile     指定日志文件路径（仅 logs 命令）

示例:
  ./build.ps1 up           # 启动中间件
  ./build.ps1 run          # 启动后端 jar
  ./build.ps1 dev          # 开发模式
  ./build.ps1 logs         # 实时查看后端日志
  ./build.ps1 build        # 仅编译后端
"@
    exit 0
}

if ($Help -or $Command -eq '') { Show-Help }

# ==================== 清理 ====================
function Invoke-Clean {
    Write-Step "清理后端构建"
    Push-Location $BackendDir
    mvn clean -B
    if ($LASTEXITCODE -ne 0) { Pop-Location; Write-ErrorExit "Maven clean 失败" }
    Pop-Location

    if (Test-Path $LogDir) { Remove-Item -Recurse $LogDir -Force; Write-Success "已清理日志目录" }
    Write-Success "清理完成"
}

# ==================== 安装依赖 ====================
function Invoke-Install {
    Write-Step "下载 Maven 依赖"
    Push-Location $BackendDir
    mvn dependency:resolve -B
    if ($LASTEXITCODE -ne 0) { Pop-Location; Write-ErrorExit "Maven 依赖下载失败" }
    Pop-Location
    Write-Success "Maven 依赖下载完成"
}

# ==================== 编译 ====================
function Invoke-Build {
    Write-Step "编译后端"
    Push-Location $BackendDir
    mvn compile -B
    if ($LASTEXITCODE -ne 0) { Pop-Location; Write-ErrorExit "后端编译失败" }
    Pop-Location
    Write-Success "后端编译完成"
}

# ==================== 打包 ====================
function Invoke-Package {
    Write-Step "打包 fat jar"
    Push-Location $BackendDir
    $mvnArgs = "clean package -pl onlinechat-server -am -B -DskipTests"
    Invoke-Expression "mvn $mvnArgs"
    if ($LASTEXITCODE -ne 0) { Pop-Location; Write-ErrorExit "后端打包失败" }
    Pop-Location
    Write-Success "onlinechat-server/target/onlinechat-server-1.0.0.jar"
}

# ==================== 启动中间件 ====================
function Invoke-Up {
    Write-Step "启动中间件（MySQL + Redis + MinIO）"
    Push-Location $ScriptsDir
    docker compose up -d mysql redis minio
    if ($LASTEXITCODE -ne 0) { Pop-Location; Write-ErrorExit "中间件启动失败" }
    Pop-Location

    Write-Success "中间件已启动"
    Write-Host "`n  MySQL:   localhost:3306"
    Write-Host "  Redis:   localhost:6379"
    Write-Host "  MinIO:   http://localhost:9001 (console)`n"
}

# ==================== 启动后端 ====================
function Invoke-Run {
    $JarPath = Join-Path $BackendDir "onlinechat-server\target\onlinechat-server-1.0.0.jar"
    if (-not (Test-Path $JarPath)) { Invoke-Package }
    Write-Step "启动后端"
    java "-Dfile.encoding=UTF-8" -jar $JarPath
}

# ==================== 停止 ====================
function Invoke-Down {
    Write-Step "停止全部服务"
    Push-Location $ScriptsDir
    docker compose down
    if ($LASTEXITCODE -ne 0) { Pop-Location; Write-ErrorExit "停止失败" }
    Pop-Location
    Write-Success "全部服务已停止"
}

# ==================== 开发模式 ====================
function Invoke-Dev {
    Invoke-Up
    Write-Host "`n  然后在两个终端分别启动："
    Write-Host "  终端1 — 后端:  cd backend && mvn spring-boot:run -pl onlinechat-server"
    Write-Host "  终端2 — 前端:  cd frontend && pnpm install && pnpm dev:client`n"
    Write-Host "  API:  http://localhost:8080/doc.html"
    Write-Host "  Web:  http://localhost:3000`n"
}

# ==================== 日志查看 ====================
function Invoke-Logs {
    # 优先使用 -LogFile 参数，其次自动查找 onlinechat.log
    $targetLog = $LogFile
    if (-not $targetLog) {
        $springLog = Join-Path $LogDir "onlinechat.log"
        if (Test-Path $springLog) {
            $targetLog = $springLog
        } else {
            # 日志文件不存在，尝试 docker compose logs
            Write-Host "  本地日志文件未找到，尝试 docker compose logs..." -ForegroundColor Yellow
            Write-Host ""
            Push-Location $ScriptsDir
            docker compose logs -f --tail $TailLines backend
            Pop-Location
            return
        }
    }

    Write-Step "实时日志: $targetLog（Ctrl+C 退出）"
    Write-Host ""
    Get-Content $targetLog -Wait -Tail $TailLines
}

# ==================== 主流程 ====================
switch ($Command) {
    'clean'   { Invoke-Clean }
    'build'   { Invoke-Build }
    'package' { Invoke-Package }
    'up'      { Invoke-Up }
    'down'    { Invoke-Down }
    'run'     { Invoke-Run }
    'dev'     { Invoke-Dev }
    'logs'    { Invoke-Logs }
    'install' { Invoke-Install }
    default   { Write-ErrorExit "未知命令: $Command" }
}
