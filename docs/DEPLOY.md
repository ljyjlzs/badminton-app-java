# 部署指南

本文档说明如何将小程序后端部署到公网，让其他用户可以访问。

## 目录

- [环境要求](#环境要求)
- [步骤一：启动 Java 后端](#步骤一启动-java-后端)
- [步骤二：内网穿透](#步骤二内网穿透)
- [步骤三：修改小程序配置](#步骤三修改小程序配置)
- [步骤四：微信公众平台配置](#步骤四微信公众平台配置)
- [步骤五：测试验证](#步骤五测试验证)
- [常见问题](#常见问题)

---

## 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 已安装在 `D:\work\jdk-17.0.12` |
| Maven | 3.8+ | 构建工具 |
| MySQL | 8.0+ | 已安装在 `D:\work\mysql8.0` |
| natapp | 最新版 | 内网穿透工具 |
| 微信开发者工具 | 最版 | 小程序开发调试 |

---

## 步骤一：启动 Java 后端

### 1. 打开终端，进入项目目录

```bash
cd D:\project\badminton-app-java
```

### 2. 设置 Java 环境变量

```bash
set JAVA_HOME=D:\work\jdk-17.0.12
set PATH=%JAVA_HOME%\bin;%PATH%
```

### 3. 启动应用

```bash
mvn spring-boot:run
```

### 4. 验证启动成功

看到以下日志表示启动成功：

```
Started BadmintonApplication in X.XX seconds
```

访问 http://localhost:8049/doc.html 能打开 API 文档即表示成功。

### 5. 保持终端运行

**不要关闭此终端**，后端服务需要持续运行。

---

## 步骤二：内网穿透

使用 natapp 将本地服务暴露到公网。

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

### 5. 启动 natapp

打开新的终端，执行：

```bash
C:\Users\你的用户名\natapp.exe -authtoken=你的authtoken
```

示例：

```bash
C:\Users\浪\natapp.exe -authtoken=be735939c52f74c5
```

### 6. 获取公网地址

启动成功后会显示：

```
Forwarding    http://xxxxxx.natappfree.cc -> http://127.0.0.1:8049
```

记下这个地址 `http://xxxxxx.natappfree.cc`，后面需要用到。

### 7. 保持终端运行

**不要关闭此终端**，natapp 需要持续运行。

---

## 步骤三：修改小程序配置

### 1. 打开配置文件

用编辑器打开：

```
D:\project\badminton-app-java\miniprogram\app.js
```

### 2. 修改 baseUrl

找到第 4 行，将地址改为 natapp 提供的公网地址：

```javascript
// 修改前
this.globalData.baseUrl = 'http://localhost:8049';

// 修改后（替换为你的 natapp 地址）
this.globalData.baseUrl = 'http://xxxxxx.natappfree.cc';
```

### 3. 保存文件

---

## 步骤四：微信公众平台配置

> ⚠️ 注意：natapp 免费版只有 HTTP，微信小程序正式环境要求 HTTPS。
> 以下配置仅用于开发测试阶段。

### 1. 登录微信公众平台

访问 https://mp.weixin.qq.com ，用微信扫码登录

### 2. 进入服务器域名配置

左侧菜单 → **开发** → **开发设置** → **服务器域名**

### 3. 添加 request 域名

在 **request 合法域名** 中添加：

```
http://xxxxxx.natappfree.cc
```

（替换为你的 natapp 地址）

### 4. 保存并提交

点击 **保存** → **提交**

---

## 步骤五：测试验证

### 1. 打开微信开发者工具

导入项目目录：

```
D:\project\badminton-app-java\miniprogram
```

### 2. 配置开发者工具

- 点击右上角 **详情** → **本地设置**
- **取消勾选**「不校验合法域名」（因为已经在公众平台配置了）

### 3. 编译运行

点击顶部 **编译** 按钮

### 4. 测试功能

- 测试登录功能
- 测试创建活动
- 测试报名功能

### 5. 手机测试

- 微信开发者工具中点击 **预览** 或 **真机调试**
- 手机微信扫码即可体验

---

## 常见问题

### Q1: natapp 地址变了怎么办？

natapp 免费版每次重启地址会变，需要：

1. 查看新的地址
2. 修改 `miniprogram/app.js` 中的 `baseUrl`
3. 在微信公众平台更新域名
4. 重新编译小程序

### Q2: 微信提示 "不在以下 request 合法域名列表中"

原因：微信公众平台未配置该域名

解决：
1. 登录微信公众平台
2. 添加 request 域名
3. 重新编译小程序

### Q3: 后端启动失败

检查：
1. MySQL 是否启动
2. 数据库用户名密码是否正确
3. 端口 8049 是否被占用

```bash
# 检查 MySQL
sc query MySQL

# 检查端口
netstat -ano | findstr 8049
```

### Q4: 小程序无法登录

检查：
1. 微信 appId 和 appSecret 是否正确
2. 后端日志是否有错误信息
3. natapp 是否正常运行

### Q5: 如何停止服务

1. 停止 Java 后端：在后端终端按 `Ctrl+C`
2. 停止 natapp：在 natapp 终端按 `Ctrl+C`

### Q6: 如何后台运行（不占用终端）

使用 `javaw` 或 `start` 命令：

```bash
# 后台启动 Java 后端
start mvn spring-boot:run

# 后台启动 natapp
start C:\Users\浪\natapp.exe -authtoken=xxx
```

---

## 完整启动流程（每次使用）

按以下顺序启动：

### 1. 启动 MySQL

确保 MySQL 服务已运行：

```bash
sc query MySQL
# 如果未运行，启动它
net start MySQL
```

### 2. 启动 Java 后端

```bash
cd D:\project\badminton-app-java
set JAVA_HOME=D:\work\jdk-17.0.12
mvn spring-boot:run
```

### 3. 启动 natapp

打开新终端：

```bash
C:\Users\浪\natapp.exe -authtoken=be735939c52f74c5
```

### 4. 修改小程序地址（如果地址变了）

编辑 `miniprogram/app.js`，更新 `baseUrl`

### 5. 微信开发者工具编译运行

---

## 一键启动脚本

创建 `start.bat` 文件：

```batch
@echo off
echo ===========================
echo 羽毛球小程序后端启动脚本
echo ===========================

echo.
echo [1/3] 启动 Java 后端...
cd /d D:\project\badminton-app-java
set JAVA_HOME=D:\work\jdk-17.0.12
start "Java后端" cmd /k "mvn spring-boot:run"

echo [2/3] 等待后端启动...
timeout /t 30

echo [3/3] 启动 natapp...
start "natapp" cmd /k "C:\Users\浪\natapp.exe -authtoken=be735939c52f74c5"

echo.
echo ===========================
echo 启动完成！
echo 请查看 natapp 窗口获取公网地址
echo ===========================
pause
```

双击 `start.bat` 即可一键启动所有服务。

---

## 技术支持

如有问题，请查看：
- API 文档：http://localhost:8049/doc.html
- 后端日志：查看 Java 后端终端输出
- natapp 管理：http://127.0.0.1:4040
