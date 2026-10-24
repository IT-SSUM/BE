#!/bin/bash
# EC2(Ubuntu 24.04) 최초 부팅 시 실행하는 사용자 데이터. Docker, Nginx(80 -> 8080), 2GB 스왑, KST 시간대를 세팅한다.
# 이미 만든 인스턴스에는 한 번만 sudo bash 로 실행한다 (두 번 돌리면 스왑 단계에서 실패).
# 콘솔 사용자 데이터에 파일로 올리면 한글 주석이 깨져 보이지만 주석이라 실행에는 영향 없다.
set -euxo pipefail
export DEBIAN_FRONTEND=noninteractive

timedatectl set-timezone Asia/Seoul

# 스왑 2GB — Spring Boot 기동 시 메모리 여유
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

# Docker + compose 플러그인. ubuntu 사용자를 docker 그룹에 넣어 CD가 sudo 없이 docker 를 쓴다.
curl -fsSL https://get.docker.com | sh
usermod -aG docker ubuntu

# Nginx: 80 -> 127.0.0.1:8080 (STOMP WebSocket 업그레이드 포함)
apt-get update -y
apt-get install -y nginx
cat > /etc/nginx/sites-available/itssum <<'EOF'
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}

server {
    listen 80;
    server_name _;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
    }
}
EOF
ln -sf /etc/nginx/sites-available/itssum /etc/nginx/sites-enabled/itssum
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl restart nginx

# CD가 docker-compose.yml과 .env를 올릴 폴더
mkdir -p /home/ubuntu/itssum-app
chown ubuntu:ubuntu /home/ubuntu/itssum-app
