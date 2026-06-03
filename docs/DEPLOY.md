# 部署指南

本文档说明如何将小程序后端部署到公网，让其他用户可以访问。

## 目录

- [环境要求](#环境要求)
- [当前配置信息](#当前配置信息)
- [启动步骤（每次使用）](#启动步骤每次使用)
- [停止服务](#停止服务)
- [natapp 地址变化处理](#natapp-地址变化处理)
- [微信公众平台配置](#微信公众平台配置)
- [微信开发者工具配置](#微信开发者工具配置)
- [手机测试](#手机测试)
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

## 启动步骤（每次使用）

需要打开 **两个终端窗口**，按顺序启动：

### 终端 1：启动 Java 后端

```bash
# 1. 进入项目目录
cd D:\project\badminton-app-java

# 2. 设置 Java 环境变量
set JAVA_HOME=D:\work\jdk-17.0.12

# 3. 启动后端
mvn spring-boot:run
```

等待看到以下日志表示启动成功：

```
Started BadmintonApplication in X.XX seconds
```

**不要关闭此终端**，保持运行。

---

### 终端 2：启动 natapp

```bash
# 启动 natapp 内网穿透
C:\Users\浪\natapp.exe -authtoken=be735939c52f74c5
```

启动成功后会显示：

```
Forwarding    http://c9d5f36d.natappfree.cc -> http://127.0.0.1:8049
```

记下这个公网地址 `http://c9d5f36d.natappfree.cc`。

**不要关闭此终端**，保持运行。

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

## natapp 地址变化处理

natapp 免费版每次重启地址会变，需要更新小程序配置：

### 1. 查看新地址

启动 natapp 后查看终端输出的地址：

```
Forwarding    http://新地址.natappfree.cc -> http://127.0.0.1:8049
```

### 2. 修改小程序配置

编辑文件 `miniprogram\app.js`，修改第 4 行：

```javascript
// 将地址改为 natapp 显示的新地址
this.globalData.baseUrl = 'http://新地址.natappfree.cc';
```

### 3. 更新微信公众平台域名

登录微信公众平台 → 开发 → 开发设置 → 服务器域名 → 修改 request 域名

### 4. 重新编译小程序

在微信开发者工具中点击 **编译**

---

## 微信公众平台配置

> ⚠️ 仅在首次部署或地址变化时需要配置

### 1. 登录

访问 https://mp.weixin.qq.com ，微信扫码登录

### 2. 配置域名

左侧菜单 → **开发** → **开发设置** → **服务器域名**

在 **request 合法域名** 中添加：

```
http://你的natapp地址.natappfree.cc
```

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

- 如果已在微信公众平台配置了域名 → **取消勾选**「不校验合法域名」
- 如果未配置域名 → **勾选**「不校验合法域名」（仅开发测试用）

### 3. 编译运行

点击顶部 **编译** 按钮

---

## 手机测试

### 方式一：预览

微信开发者工具 → 点击 **预览** → 手机微信扫码

### 方式二：真机调试

微信开发者工具 → 点击 **真机调试** → 手机微信扫码

> 注意：手机和电脑需在同一网络，或使用 natapp 公网地址

---

## 常见问题

### Q1: 后端启动失败，提示数据库连接错误

检查 MySQL 是否启动：

```bash
sc query MySQL
```

如果未启动：

```bash
net start MySQL
```

### Q2: 端口 8049 被占用

```bash
# 查看占用端口的进程
netstat -ano | findstr 8049

# 结束进程（替换 PID）
taskkill //F //PID 进程ID
```

### Q3: 微信提示 "不在以下 request 合法域名列表中"

原因：微信公众平台未配置该域名

解决：
1. 登录微信公众平台
2. 开发 → 开发设置 → 服务器域名
3. 添加 natapp 地址到 request 合法域名
4. 重新编译小程序

### Q4: 小程序无法登录

检查：
1. 后端是否正常运行（访问 http://localhost:8049/doc.html）
2. natapp 是否正常运行
3. 小程序 baseUrl 是否正确
4. 后端终端是否有错误日志

### Q5: natapp 显示 offline

原因：natapp 服务异常或网络问题

解决：
1. 重启 natapp（Ctrl+C 后重新运行）
2. 检查网络连接
3. 检查 authtoken 是否正确

---

## 快速参考

### 启动命令

```bash
# 终端 1：Java 后端
cd D:\project\badminton-app-java && set JAVA_HOME=D:\work\jdk-17.0.12 && mvn spring-boot:run

# 终端 2：natapp
C:\Users\浪\natapp.exe -authtoken=be735939c52f74c5
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
