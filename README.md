# 羽毛球报名小程序后端

基于 Spring Boot 的羽毛球活动报名小程序后端服务。

## 技术栈

- **框架**: Spring Boot 3.2.5
- **ORM**: MyBatis-Plus 3.5.5
- **数据库**: MySQL 8.0
- **认证**: JWT (jjwt 0.12.5)
- **微信**: WxJava 4.5.0 (微信小程序 SDK)
- **API 文档**: Knife4j 4.4.0
- **构建工具**: Maven
- **Java 版本**: JDK 17

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+

### 本地开发

1. 克隆项目
```bash
git clone <repository-url>
cd badminton-app-java
```

2. 配置环境变量（可选，有默认值）
```bash
export DB_PASSWORD=your_password
export WX_APP_SECRET=your_wx_secret
export JWT_SECRET=your_jwt_secret_32chars!
export AI_API_KEY=your_ai_key
```

3. 启动应用
```bash
mvn spring-boot:run
```

应用默认运行在 `http://localhost:8049`

### Docker 部署

1. 使用 Docker Compose 一键启动
```bash
# 配置环境变量
cp .env.example .env
# 编辑 .env 文件，填入实际配置

# 启动服务
docker-compose up -d
```

2. 查看日志
```bash
docker-compose logs -f app
```

3. 停止服务
```bash
docker-compose down
```

## API 文档

启动应用后访问 Knife4j API 文档：

- 开发环境: `http://localhost:8049/doc.html`
- 生产环境: `https://yourdomain.com/doc.html`

### 主要接口

| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 用户 | `/api/user` | 登录、用户信息 |
| 活动 | `/api/activity` | 活动 CRUD、报名 |
| 比赛 | `/api/match` | 比赛管理、比分提交 |
| 排名 | `/api/ranking` | 排行榜 |
| 挑战 | `/api/challenge` | 挑战赛 |
| 球队 | `/api/team` | 球队管理 |
| 规则 | `/api/rules` | 规则查询 |
| AI | `/api/ai` | AI 对话 |
| 管理后台 | `/api/admin` | 后台管理 |

## 配置说明

### 环境变量

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| `SERVER_PORT` | 否 | 8049 | 服务端口 |
| `SPRING_PROFILES_ACTIVE` | 否 | dev | 运行环境 (dev/prod) |
| `DB_URL` | 否 | jdbc:mysql://localhost:3306/badminton | 数据库连接地址 |
| `DB_USERNAME` | 否 | root | 数据库用户名 |
| `DB_PASSWORD` | 是 | - | 数据库密码 |
| `WX_APP_ID` | 否 | wxb0369110c3c788ef | 微信小程序 AppID |
| `WX_APP_SECRET` | 是 | - | 微信小程序 AppSecret |
| `JWT_SECRET` | 是 | - | JWT 签名密钥 (至少 32 位) |
| `JWT_EXPIRATION` | 否 | 604800000 | JWT 过期时间 (毫秒，默认 7 天) |
| `AI_API_KEY` | 否 | - | AI 服务 API Key |
| `AI_API_URL` | 否 | https://token-plan-cn.xiaomimimo.com/v1/chat/completions | AI 服务地址 |
| `LOG_LEVEL_APP` | 否 | info | 应用日志级别 |
| `LOG_LEVEL_WEB` | 否 | info | Web 日志级别 |

### 配置文件

- `application.yml` - 主配置
- `application-dev.yml` - 开发环境配置
- `application-prod.yml` - 生产环境配置

## 项目结构

```
src/main/java/com/badminton/
├── BadmintonApplication.java    # 启动类
├── common/                      # 公共类
│   ├── BusinessException.java
│   ├── GlobalExceptionHandler.java
│   └── Result.java
├── config/                      # 配置类
│   ├── MyBatisPlusConfig.java
│   ├── WebMvcConfig.java
│   └── WxMaConfig.java
├── controller/                  # 控制器
├── dto/                         # 数据传输对象
├── entity/                      # 实体类
├── interceptor/                 # 拦截器
├── mapper/                      # MyBatis Mapper
└── service/                     # 业务逻辑
```

## 部署说明

> 📖 详细部署文档请查看 [docs/DEPLOY.md](docs/DEPLOY.md)

### 生产环境部署

1. 构建镜像
```bash
docker-compose build
```

2. 配置生产环境变量
```bash
# .env 文件
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://your-db-host:3306/badminton
DB_USERNAME=your_username
DB_PASSWORD=your_password
WX_APP_ID=your_app_id
WX_APP_SECRET=your_app_secret
JWT_SECRET=your_production_jwt_secret_at_least_32_chars!
AI_API_KEY=your_ai_key
```

3. 启动服务
```bash
docker-compose -f docker-compose.yml up -d
```

### 数据库初始化

应用启动时会自动执行 `schema.sql` 和 `data.sql` 初始化数据库。生产环境建议关闭自动初始化，手动执行 SQL 脚本。

## 许可证

[MIT](LICENSE)
