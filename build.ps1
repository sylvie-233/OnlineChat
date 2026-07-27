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
#   install     - 下载 Maven 依赖
# ==================================================

param(
    [Parameter(Position = 0)]
    [ValidateSet('clean', 'build', 'package', 'up', 'down', 'run', 'dev', 'install')]
    [string]$Command = '',

    [Parameter()]
    [switch]$Help,

    [Parameter()]
    [switch]$SkipTests
)

# 显示帮助信息
if ($Help -or $Command -eq '') {
    Write-Host @"
OnlineChat IM 构建脚本

用法: ./build.ps1 <command> [options]

Commands:
  clean       清理构建目录
  build       编译后端（Maven compile）
  package     清理、编译并打包 fat jar
  up          启动中间件（MySQL + Redis + MinIO）
  down        停止全部服务
  dev         启动中间件，提示本地开发方式
  install     下载 Maven 依赖

Options:
  -Help       显示帮助信息
  -SkipTests  打包时跳过测试（默认跳过）

示例:
  ./build.ps1 up           # 启动中间件
  ./build.ps1 run          # 启动后端 jar
  ./build.ps1 package      # 打包 fat jar
  ./build.ps1 dev          # 启动中间件，提示本地开发方式
  ./build.ps1 build        # 仅编译后端
  ./build.ps1 down         # 停止全部
"@
    exit 0
}

# 项目根目录（脚本所在目录）
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = Join-Path $ProjectRoot "backend"
$ScriptsDir = Join-Path $ProjectRoot "scripts"
$BackendLogDir = Join-Path $BackendDir "logs"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host ">>> $Message" -ForegroundColor Cyan
    Write-Host "==================================================" -ForegroundColor DarkGray
}

function Write-Success {
    param([string]$Message)
    Write-Host "  OK  $Message" -ForegroundColor Green
}

function Write-ErrorExit {
    param([string]$Message)
    Write-Host "  ERR $Message" -ForegroundColor Red
    exit 1
}

# ==================== 清理 ====================
function Invoke-Clean {
    Write-Step "清理后端构建目录"
    Push-Location $BackendDir
    mvn clean -B
    if ($LASTEXITCODE -ne 0) { Pop-Location; Write-ErrorExit "Maven clean 失败" }
    Pop-Location

    if (Test-Path $BackendLogDir) {
        Remove-Item -Recurse $BackendLogDir -Force
        Write-Success "已清理日志目录"
    }

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
    Write-Step "打包后端 fat jar"
    Push-Location $BackendDir
    $mvnArgs = "clean package -pl onlinechat-server -am -B"
    if ($SkipTests) {
        $mvnArgs += " -DskipTests"
    } else {
        $mvnArgs += " -DskipTests"   # 默认跳过测试
    }
    Invoke-Expression "mvn $mvnArgs"
    if ($LASTEXITCODE -ne 0) { Pop-Location; Write-ErrorExit "后端打包失败" }
    Pop-Location
    Write-Success "打包完成 -> onlinechat-server/target/onlinechat-server-1.0.0.jar"
}

# ==================== 启动中间件 ====================
function Invoke-Up {
    Write-Step "启动中间件（MySQL + Redis + MinIO）"
    Push-Location $ScriptsDir
    docker compose up -d mysql redis minio
    if ($LASTEXITCODE -ne 0) { Pop-Location; Write-ErrorExit "中间件启动失败" }
    Pop-Location

    Write-Success "中间件已启动"
    Write-Host ""
    Write-Host "  MySQL:   localhost:3306" -ForegroundColor Green
    Write-Host "  Redis:   localhost:6379" -ForegroundColor Green
    Write-Host "  MinIO:   http://localhost:9001 (console) / :9000 (API)" -ForegroundColor Green
    Write-Host ""
    Write-Host "  启动后端: cd backend\onlinechat-server\target" -ForegroundColor Yellow
    Write-Host "            java -Dfile.encoding=UTF-8 -jar onlinechat-server-1.0.0.jar" -ForegroundColor Yellow
    Write-Host ""
}

# ==================== 启动后端 jar ====================
function Invoke-Run {
    $JarPath = Join-Path $BackendDir "onlinechat-server\target\onlinechat-server-1.0.0.jar"

    if (-not (Test-Path $JarPath)) {
        Write-Host "  Jar 不存在，先执行打包..." -ForegroundColor Yellow
        Invoke-Package
    }

    Write-Step "启动后端"
    Write-Host "  Jar: $JarPath" -ForegroundColor Gray
    Write-Host "  编码: UTF-8" -ForegroundColor Gray
    Write-Host ""
    java "-Dfile.encoding=UTF-8" -jar $JarPath
}

# ==================== 停止 ====================
function Invoke-Down {
    Write-Step "停止全部服务"
    Push-Location $ScriptsDir
    docker compose down
    if ($LASTEXITCODE -ne 0) { Pop-Location; Write-ErrorExit "服务停止失败" }
    Pop-Location
    Write-Success "全部服务已停止"
}

# ==================== 开发模式 ====================
function Invoke-Dev {
    Write-Step "启动中间件 + 开发环境"
    Push-Location $ScriptsDir
    docker compose up -d mysql redis minio
    if ($LASTEXITCODE -ne 0) { Pop-Location; Write-ErrorExit "中间件启动失败" }
    Pop-Location

    Write-Success "MySQL + Redis + MinIO 已启动"
    Write-Host ""
    Write-Host "  然后在两个终端分别启动：" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  # 终端 1 — 后端（Spring Boot 热重载）" -ForegroundColor Cyan
    Write-Host "  cd backend" -ForegroundColor White
    Write-Host "  mvn spring-boot:run -pl onlinechat-server" -ForegroundColor White
    Write-Host ""
    Write-Host "  # 终端 2 — 前端（Vite 热重载）" -ForegroundColor Cyan
    Write-Host "  cd frontend" -ForegroundColor White
    Write-Host "  pnpm install && pnpm dev:client" -ForegroundColor White
    Write-Host ""
    Write-Host "  后端 API:  http://localhost:8080" -ForegroundColor Green
    Write-Host "  API 文档:  http://localhost:8080/doc.html" -ForegroundColor Green
    Write-Host "  WebSocket: ws://localhost:9090/ws" -ForegroundColor Green
    Write-Host "  前端页面:  http://localhost:3000" -ForegroundColor Green
}

# ==================== 主流程 ====================

Write-Host "==================================================" -ForegroundColor DarkGray
Write-Host "  OnlineChat IM 系统 — 构建脚本" -ForegroundColor Cyan
Write-Host "  工作目录: $ProjectRoot" -ForegroundColor Gray
Write-Host "==================================================" -ForegroundColor DarkGray

switch ($Command) {
    'clean'   { Invoke-Clean }
    'build'   { Invoke-Build }
    'package' { Invoke-Package }
    'up'      { Invoke-Up }
    'down'    { Invoke-Down }
    'run'     { Invoke-Run }
    'dev'     { Invoke-Dev }
    'install' { Invoke-Install }
    default   { Write-ErrorExit "未知命令: $Command" }
}
