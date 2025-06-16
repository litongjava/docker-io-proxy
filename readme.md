# docker-io-proxy

一个轻量级的 Docker Registry V2 HTTP 代理，用于支持 Docker 和 Podman 镜像拉取加速与自定义镜像源配置。

## 功能特点

* 完整支持 Docker Registry V2 的 `/v2/` 接口（包括 manifests、blobs）
* 支持 multi-platform manifest list（多平台清单）
* 可配置 registry 镜像加速源
* 支持 Docker 和 Podman，无需重启服务即可生效
* 简单处理公开镜像的 JWT Token 转发

## 前提要求

* Java 8 或更高版本
* Maven 3.6+
* 可访问上游镜像仓库（如 Docker Hub）的网络

## 构建项目

```bash
# 克隆项目
git clone https://github.com/litongjava/docker-io-proxy.git
cd docker-io-proxy

# 构建生产环境 JAR 包（跳过测试与 GPG 签名）
mvn clean package -DskipTests -Dgpg.skip -Pproduction
```

## 启动代理服务

```bash
# 默认监听 8004 端口
java -jar target/docker-io-proxy-1.0.0.jar --server.port=8004
```

如需指定端口，可替换 `--server.port` 参数值。

## 使用systemctl管理docker-io-proxy

mkdir -p ~/.config/systemd/user
新建服务文件 ~/.config/systemd/user/docker-io-proxy.service，内容如下：
```
[Unit]
Description=docker-io-proxy Java Web Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/data/apps/docker-io-proxy
ExecStart=/usr/java/jdk1.8.0_211/bin/java -jar target/docker-io-proxy-1.0.0.jar --server.port=8004
Restart=on-failure
RestartSec=5s

[Install]
WantedBy=default.target
```
- WorkingDirectory：服务启动前切换到的目录
- ExecStart：完整的 Java 可执行路径与 JAR 包路径
- WantedBy=default.target：对应 systemctl --user enable 时的目标
重新加载用户级服务配置：
```
systemctl --user daemon-reload
```

启动服务：
```
systemctl --user start docker-io-proxy.service
```
查看运行状态：
```
systemctl --user status docker-io-proxy.service
```
设置为开机启动
```
systemctl --user enable docker-io-proxy.service
``

## 使用配置

### Docker（daemon.json 配置）

编辑 `/etc/docker/daemon.json` 文件，配置你的代理地址：

```json
{
  "registry-mirrors": ["http://docker.max-kb.com"],
  "insecure-registries": ["docker.max-kb.com"]
}
```

然后重启 Docker 服务：

```bash
sudo systemctl restart docker
```

### Podman（registries.conf 配置）

Podman 每次执行 pull/run 时会自动读取 `/etc/containers/registries.conf` 文件，无需重启。

```toml
# 启用宽松的短名称解析
short-name-mode = "permissive"

# 设置搜索顺序，优先使用私有代理
unqualified-search-registries = [
  "docker.max-kb.com",
  "registry.access.redhat.com",
  "registry.redhat.io",
  "docker.io"
]

# 为 docker.max-kb.com 设置代理配置
[[registry]]
prefix = "docker.max-kb.com"
location = "docker.max-kb.com"
insecure = false
```

保存配置后立即生效。

### Nginx 反向代理配置

如果你希望通过域名 `docker.max-kb.com` 暴露服务并监听标准 HTTP 端口 80，可以使用如下 Nginx 配置：

```nginx
server {
  listen 80;
  server_name docker.max-kb.com;

  location / {
    proxy_pass http://127.0.0.1:8004;
    proxy_pass_header Set-Cookie;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto https;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    error_log  /var/log/nginx/backend.error.log;
    access_log  /var/log/nginx/backend.access.log;
  } 
}
```

配置好后，重启 Nginx：

```bash
sudo systemctl restart nginx
```

确保你的 DNS 或 hosts 文件已将 `docker.max-kb.com` 指向对应服务器 IP。

## 镜像拉取完整流程

1. **获取索引清单（multi-platform manifest list）**

   ```http
   GET /v2/<repo>/manifests/<tag>
   ```
2. **选择平台清单**

   ```http
   GET /v2/<repo>/manifests/<digest>
   ```
3. **拉取镜像配置文件**

   ```http
   GET /v2/<repo>/blobs/<config-digest>
   ```
4. **依次拉取各层 layer**

   ```http
   GET /v2/<repo>/blobs/<layer-digest>
   ```

示例命令：

```bash
docker pull 1panel/openresty:1.27.1.2-0-1-focal
```

## Token 认证机制

当拉取公开镜像时，客户端首先会请求 JWT Token：

```http
GET https://auth.docker.io/token?scope=repository:1panel/openresty:pull&service=registry.docker.io
```

你也可以在代理中暴露 `/token` 接口：

```http
GET https://docker.max-kb.com/token?scope=repository:1panel/openresty:pull&service=registry.docker.io
```

但需要注意：Token 请求不会被 \[\[registry]] 或 \[\[mirror]] 捕获，仍会默认走官方 auth.docker.io。

## 测试示例

* **Docker 测试**：

  ```bash
  docker pull 1panel/openresty:1.27.1.2-0-1-focal
  ```
* **Podman 测试**：

  ```bash
  podman pull docker.max-kb.com/1panel/openresty:1.27.1.2-0-1-focal
  ```

观察 proxy 日志确认是否有 `/v2/` 请求即代表代理工作正常。

## 许可证

基于 MIT 开源许可证，详情请查阅 `LICENSE` 文件。
