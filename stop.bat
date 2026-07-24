@echo off
chcp 65001 >nul
echo ============================================
echo   停止代码作业智慧评价平台服务
echo ============================================
echo.

echo 停止占用8080端口的进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080"') do (
    if not "%%a"=="0" (
        taskkill /f /pid %%a >nul 2>&1
        echo 已停止进程 PID: %%a
    )
)

echo.
echo 停止占用5173端口的进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173"') do (
    if not "%%a"=="0" (
        taskkill /f /pid %%a >nul 2>&1
        echo 已停止进程 PID: %%a
    )
)

echo.
echo 停止所有Java进程...
taskkill /f /im java.exe >nul 2>&1
echo Java进程已停止

echo.
echo 停止所有Node.js进程...
taskkill /f /im node.exe >nul 2>&1
echo Node.js进程已停止

echo.
echo ============================================
echo   所有服务已停止！
echo ============================================
pause