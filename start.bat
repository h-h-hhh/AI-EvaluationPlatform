@echo off
echo ============================================
echo   代码作业智慧评价平台 - 一键启动脚本
echo ============================================

docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误：未检测到Docker，请先安装Docker Desktop
    echo 下载地址：https://www.docker.com/products/docker-desktop/
    pause
    exit /b 1
)

docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误：未检测到Docker Compose
    pause
    exit /b 1
)

echo.
echo 正在启动平台...
echo.

docker-compose up -d

echo.
echo 平台启动完成！
echo.
echo 访问地址：
echo   前端应用：http://localhost
echo   后端API：http://localhost:8080/api
echo   数据库：localhost:5432
echo.
echo 管理命令：
echo   停止服务：docker-compose down
echo   查看日志：docker-compose logs -f
echo   重启服务：docker-compose restart
echo.
pause