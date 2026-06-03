# 部署指南

本文档说明如何将小程序后端部署到公网，让其他用户可以访问。

## 目录

- [环境要求](#环境要求)
- [当前配置信息](#当前配置信息)
- [部署方式一：内网部署（同一 WiFi）](#部署方式一内网部署同一-wifi)
- [部署方式二：公网部署（natapp）](#部署方式二公网部署natapp)
- [部署方式三：公网部署（域名 + HTTPS）](#部署方式三公网部署域名--https)
- [停止服务](#停止服务)
- [微信公众平台配置](#微信公众平台配置)
- [微信开发者工具配置](#微信开发者工具配置)
- [清除缓存](#清除缓存)
- [常见问题](#常见问题)

---

## 环境要求

| 软件 | 版本 | 安装位置 |
|------|------|----------|
| JDK | 17 | `D:\work\jdk-17.0.12` |
| Maven | 3.8 | `D:\work\maven\apache-maven-3.8.5` |
| MySQL | 8.0 | `D:\work\mysql8.0` |
| natapp | 最新版 | `C:\Users\浪\natapp.exe` |

---

## 当前配置信息

| 配置项 | 值 |
|--------|-----|
| 数据库用户 | `root` |
| 数据库密码 | `123456` |
| 数据库名 | `badminton` |
| 后端端口 | `8049` |
| 微信 appId | `wxb0369110c3c788ef` |
| 微信 appSecret | `fc27c8b8639894dea311c706dd2a2640` |
| AI API Key | `tp-c8yy59jtcd38ksv6hicz0yq0mofymzxlijbhqxg68gnanpld` |
| natapp authtoken | `be735939c52f74c5` |

---

## 部署方式一：内网部署（同一 WiFi）

适用于：手机和电脑连接同一个 WiFi，本地测试。

### 1. 查看电脑局域网 IP

```bash
ipconfig | findstr "IPv4"
```

记下 IP 地址，例如：`192.168.31.91`

### 2. 启动 Java 后端

打开终端：

```bash
# 进入项目目录
cd D:\project\badminton-app-java

# 设置 Java 环境变量
set JAVA_HOME=D:\work\jdk-17.0.12

# 启动后端
mvn spring-boot:run
```

等待看到 `Started BadmintonApplication in X.XX seconds` 表示启动成功。

**不要关闭此终端**。

### 3. 添加防火墙规则

```bash
netsh advfirewall firewall add rule name="Badminton App" dir=in action=allow protocol=TCP localport=8049
```

### 4. 修改小程序配置

编辑 `miniprogram/app.js`，修改第 4 行：

```javascript
// 将地址改为你的局域网 IP
this.globalData.baseUrl = 'http://192.168.31.91:8049';
```

### 5. 微信开发者工具配置

打开微信开发者工具 → 右上角 **详情** → **本地设置**：

✅ **勾选**「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」

### 6. 编译运行

点击顶部 **编译** 按钮

### 7. 手机测试

确保手机和电脑连接 **同一 WiFi**

在微信开发者工具中点击 **预览** 或 **真机调试** → 手机扫码

### 优点

- ✅ 免费
- ✅ 速度快
- ✅ 地址固定

### 缺点

- ❌ 只能在同一 WiFi 下使用
- ❌ 无法分享给其他网络的人

---

## 部署方式二：公网部署（natapp）

适用于：分享给其他人测试，不需要域名。

### 1. 注册 natapp 账号

访问 https://natapp.cn 注册账号

### 2. 实名认证

免费版需要实名认证：
- 登录后进入 **个人中心**
- 完成 **实名认证**

### 3. 购买免费隧道

- 进入 **我的隧道** → **购买隧道**
- 选择 **免费隧道**
- 配置：
  - 协议：`HTTP`
  - 本地端口：`8049`
- 购买后复制 **authtoken**

### 4. 下载 natapp 客户端

访问 https://natapp.cn/#download 下载 Windows 版本

### 5. 启动 Java 后端

打开终端 1：

```bash
cd D:\project\badminton-app-java
set JAVA_HOME=D:\work\jdk-17.0.12
mvn spring-boot:run
```

**不要关闭此终端**。

### 6. 启动 natapp

打开终端 2：

```bash
C:\Users\浪\natapp.exe -authtoken=be735939c52f74c5
```

启动成功后会显示：

```
Forwarding    http://c9d5f36d.natappfree.cc -> http://127.0.0.1:8049
```

记下公网地址 `http://c9d5f36d.natappfree.cc`。

**不要关闭此终端**。

### 7. 修改小程序配置

编辑 `miniprogram/app.js`，修改第 4 行：

```javascript
// 将地址改为 natapp 提供的公网地址
this.globalData.baseUrl = 'http://c9d5f36d.natappfree.cc';
```

### 8. 微信开发者工具配置

打开微信开发者工具 → 右上角 **详情** → **本地设置**：

✅ **勾选**「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」

### 9. 编译运行

点击顶部 **编译** 按钮

### 10. 分享测试

点击 **预览** → 手机扫码 → 分享给其他人

### 优点

- ✅ 免费
- ✅ 可以分享给任何人

### 缺点

- ❌ 地址每次重启会变
- ❌ 只有 HTTP，没有 HTTPS
- ❌ 无法在微信公众平台配置域名

---

## 部署方式三：公网部署（域名 + HTTPS）

适用于：正式发布，需要在微信公众平台配置域名。

### 1. 购买域名

推荐平台：

| 平台 | 价格 | 网址 |
|------|------|------|
| 阿里云万网 | .top 一年 ¥9 | https://wanwang.aliyun.com |
| 腾讯云 | .top 一年 ¥9 | https://cloud.tencent.com/act/pro/domain |
| Namesilo | .top 一年 $1 | https://www.namesilo.com |

### 2. 注册 Cloudflare

访问 https://dash.cloudflare.com/sign-up 注册账号

### 3. 添加域名到 Cloudflare

1. 登录 Cloudflare
2. 点击 **Add a Site**
3. 输入你的域名
4. 按提示修改域名的 DNS 服务器

### 4. 安装 cloudflared

下载地址：https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/

### 5. 登录 Cloudflare

```bash
cloudflared tunnel login
```

按提示在浏览器中授权

### 6. 创建隧道

```bash
cloudflared tunnel create badminton-app
```

记下隧道 ID

### 7. 配置隧道

创建配置文件 `~/.cloudflared/config.yml`：

```yaml
tunnel: 你的隧道ID
credentials-file: C:\Users\浪\.cloudflared\你的隧道ID.json

ingress:
  - hostname: api.你的域名.com
    service: http://localhost:8049
  - service: http_status:404
```

### 8. 添加 DNS 记录

在 Cloudflare 控制台 → 你的域名 → DNS → 添加记录：

| 类型 | 名称 | 内容 |
|------|------|------|
| CNAME | api | 你的隧道ID.cfargotunnel.com |

### 9. 启动服务

```bash
# 终端 1：Java 后端
cd D:\project\badminton-app-java
set JAVA_HOME=D:\work\jdk-17.0.12
mvn spring-boot:run

# 终端 2：Cloudflare 隧道
cloudflared tunnel run badminton-app
```

### 10. 修改小程序配置

编辑 `miniprogram/app.js`：

```javascript
this.globalData.baseUrl = 'https://api.你的域名.com';
```

### 11. 微信公众平台配置

登录 https://mp.weixin.qq.com → 开发 → 开发设置 → 服务器域名

添加 request 合法域名：`https://api.你的域名.com`

### 12. 微信开发者工具配置

打开微信开发者工具 → 右上角 **详情** → **本地设置**：

❌ **取消勾选**「不校验合法域名」

### 13. 编译运行

点击顶部 **编译** 按钮

### 优点

- ✅ 免费（除了域名费用）
- ✅ HTTPS 支持
- ✅ 地址固定
- ✅ 可以在微信公众平台配置域名
- ✅ 可以发布正式版

### 缺点

- ❌ 需要购买域名（一年十几块）
- ❌ 配置较复杂

---

## 停止服务

### 停止 Java 后端

在 Java 后端终端按 `Ctrl+C`

### 停止 natapp

在 natapp 终端按 `Ctrl+C`

### 强制停止（如果终端已关闭）

```bash
# 停止 Java 进程
taskkill //F //IM java.exe

# 停止 natapp
taskkill //F //IM natapp.exe
```

---

## 微信公众平台配置

> ⚠️ 仅在公网部署（方式二、三）时需要配置

### 1. 登录

访问 https://mp.weixin.qq.com ，微信扫码登录

### 2. 配置域名

左侧菜单 → **开发** → **开发设置** → **服务器域名**

在 **request 合法域名** 中添加：

```
https://你的域名.com
```

> ⚠️ 微信公众平台要求 HTTPS，HTTP 域名会报错「协议头非法」

### 3. 保存

点击 **保存** → **提交**

---

## 微信开发者工具配置

### 1. 导入项目

打开微信开发者工具 → 导入项目 → 选择目录：

```
D:\project\badminton-app-java\miniprogram
```

AppID 填写：`wxb0369110c3c788ef`

### 2. 配置域名校验

点击右上角 **详情** → **本地设置**：

- 内网部署 → ✅ **勾选**「不校验合法域名」
- natapp 部署 → ✅ **勾选**「不校验合法域名」
- 域名 + HTTPS 部署 → ❌ **取消勾选**「不校验合法域名」

### 3. 编译运行

点击顶部 **编译** 按钮

---

## 清除缓存

如果小程序显示异常，需要清除缓存：

### 方法一：清除全部缓存

点击顶部 **清缓存** → **清除全部缓存**

### 方法二：手动清除

1. 点击 **调试器** → **Console**
2. 输入以下代码并回车：

```javascript
wx.clearStorageSync();
```

3. 重新编译

---

## 常见问题

### Q1: 首页一直转圈圈

原因：小程序缓存了旧的登录信息

解决：清除缓存（见上方）

### Q2: 登录失败

原因：
1. 微信 appId 或 appSecret 配置错误
2. 微信开发者工具未登录微信号
3. 网络问题无法访问微信 API

解决：
1. 检查 `application.yml` 中的 appId 和 appSecret
2. 在微信开发者工具中登录微信号
3. 检查网络连接

### Q3: 后端启动失败，提示数据库连接错误

检查 MySQL 是否启动：

```bash
sc query MySQL
```

如果未启动：

```bash
net start MySQL
```

### Q4: 端口 8049 被占用

```bash
# 查看占用端口的进程
netstat -ano | findstr 8049

# 结束进程（替换 PID）
taskkill //F //PID 进程ID
```

### Q5: 微信提示 "不在以下 request 合法域名列表中"

原因：微信公众平台未配置该域名

解决：
1. 登录微信公众平台
2. 开发 → 开发设置 → 服务器域名
3. 添加域名到 request 合法域名
4. 重新编译小程序

或者：在微信开发者工具中勾选「不校验合法域名」

### Q6: natapp 地址变了怎么办？

natapp 免费版每次重启地址会变，需要：

1. 查看新的地址
2. 修改 `miniprogram/app.js` 中的 `baseUrl`
3. 重新编译小程序

### Q7: 小程序无法登录

检查：
1. 后端是否正常运行（访问 http://localhost:8049/doc.html）
2. natapp 是否正常运行
3. 小程序 baseUrl 是否正确
4. 后端终端是否有错误日志

---

## 快速参考

### 内网部署命令

```bash
# 终端 1：Java 后端
cd D:\project\badminton-app-java && set JAVA_HOME=D:\work\jdk-17.0.12 && mvn spring-boot:run

# 防火墙
netsh advfirewall firewall add rule name="Badminton App" dir=in action=allow protocol=TCP localport=8049

# 小程序配置
this.globalData.baseUrl = 'http://192.168.31.91:8049';
```

### natapp 部署命令

```bash
# 终端 1：Java 后端
cd D:\project\badminton-app-java && set JAVA_HOME=D:\work\jdk-17.0.12 && mvn spring-boot:run

# 终端 2：natapp
C:\Users\浪\natapp.exe -authtoken=be735939c52f74c5

# 小程序配置（地址会变）
this.globalData.baseUrl = 'http://xxxxxx.natappfree.cc';
```

### 停止命令

```bash
taskkill //F //IM java.exe
taskkill //F //IM natapp.exe
```

### 验证命令

```bash
# 检查后端
curl http://localhost:8049/doc.html

# 检查进程
tasklist | findstr java
tasklist | findstr natapp
```

---

## 配置文件位置

| 文件 | 说明 |
|------|------|
| `src\main\resources\application.yml` | 主配置 |
| `src\main\resources\application-dev.yml` | 开发环境配置 |
| `miniprogram\app.js` | 小程序配置（baseUrl） |
| `docs\DEPLOY.md` | 本文档 |
