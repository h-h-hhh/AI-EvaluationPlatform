#!/bin/bash
echo "================================================"
echo "   代码作业智慧评价平台 - 一键启动脚本"
echo "================================================"

if ! command -v docker &> /dev/null; then
    echo "错误：未检测到Docker，请先安装Docker"
    echo "下载地址：https://www.docker.com/products/docker-desktop/"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "错误：未检测到Docker Compose"
    exit 1
fi

echo ""
echo "正在启动平台..."
echo ""

docker-compose up -d

echo ""
echo "平台启动完成！"
echo ""
echo "访问地址："
echo "  前端应用：http://localhost"
echo "  后端API：http://localhost:8080/api"
echo "  数据库：localhost:5432"
echo ""
echo "管理命令："
echo "  停止服务：docker-compose down"
echo "  查看日志：docker-compose logs -f"
echo "  重启服务：docker-compose restart"