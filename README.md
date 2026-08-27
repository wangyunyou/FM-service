# FM 小程序后端服务

FM 小程序的后端 API 服务，提供用户管理、微信登录、饮食记录等功能。

## 技术栈

- **Java 17** + **Spring Boot 3.2.5**
- **Spring Data JPA** (Hibernate)
- **数据库**：PostgreSQL（开发用本地实例，生产用 Supabase）
- **认证**：JWT（jjwt 0.12.5）
- **构建**：Maven
- **工具库**：Lombok、Jakarta Validation

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- PostgreSQL 14+（开发和测试都连真实 PG，`brew services start postgresql@16`）

### 开发环境启动

```bash
# 克隆项目
git clone https://github.com/wangyunyou/FM-service.git
cd FM-service

# 首次准备数据库（库名需与 application-dev.yml 一致）
createdb fmdb

# 启动（dev profile：本地 PG + 自动建表 + data.sql 种子数据）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动后访问：
- API 服务：`http://localhost:8080`
- 接口文档：`http://localhost:8080/swagger-ui.html`（仅 dev profile 开启）
- 健康检查：`http://localhost:8080/health`

连上本地库后可直接查数据：

```bash
psql -d fmdb -c 'SELECT id, openid, nickname FROM users;'
```

> dev profile 默认打开了微信登录 mock（`wx.miniapp.mock-enabled=true`），
> 没有真实小程序密钥也能调通 `/api/user/wx-login` 拿 token：任意 `code` 会映射成
> 一个稳定的 `mock-openid-xxx` 用户。该开关默认关闭，且**即使误设为 true，
> 非 dev profile 启动也会被 `StartupSafetyCheck` 直接报错拦住**（见下文“启动自检”）。

### 生产环境启动

需要配置 Supabase PostgreSQL 数据库和微信小程序密钥：

```bash
# 设置环境变量
export SUPABASE_HOST=your-supabase-host
export SUPABASE_USER=postgres
export SUPABASE_PASSWORD=your-password
export SUPABASE_DB=postgres
export JWT_SECRET=your-jwt-secret-key

# 微信小程序配置（环境变量名与 application.yml 保持一致）
export WX_APPID=your-appid
export WX_SECRET=your-secret

# 跨域来源（逗号分隔，仅 H5 / 浏览器调试需要；小程序原生请求不受同源策略限制）
# 留空表示不放行任何浏览器跨域来源；绝对不要为了省事填 *
export CORS_ALLOWED_ORIGIN_PATTERNS=https://fm.example.com

# 接口文档：生产默认关闭，临时排查问题才设 true，用完关掉
# 微信登录 mock：生产必须保持 false（默认值）
export SWAGGER_ENABLED=false
export WX_MOCK_LOGIN=false

# 启动
mvn spring-boot:run
```

### 启动自检

`config/StartupSafetyCheck.java` 在启动时强校验下面三项，非 dev profile 命中就直接抛
`IllegalStateException` 阻止启动（而不是只打一条容易被忽略的 WARN）：

| 开关 | 非 dev 时的行为 |
|------|----------------|
| `wx.miniapp.mock-enabled=true` | 拒绝启动（该开关会让任意 code 换到有效 token，等同关闭鉴权） |
| `app.cors.allowed-origin-patterns` 含通配符或 `null` | 拒绝启动（生产只允许完整的协议+域名，或留空） |
| `jwt.secret` 为空/仍是文档示例值/长度 < 32 | 拒绝启动（弱密钥可被伪造任意用户 token） |

因此本地调试请始终用 `-Dspring-boot.run.profiles=dev`，不要把 dev 的配置值抄到默认（生产）profile。

> 接口文档（`SWAGGER_ENABLED`）不在硬性拦截范围内：它默认 false，但线上临时排查需要显式打开的能力，
> 因此只做“默认关闭 + 用完关回去”的约定，不设启动红线。

生成合格密钥：`openssl rand -base64 48`，通过环境变量 `JWT_SECRET` 传入，**不要把真实密钥写进代码或配置文件**。

## API 接口

### 用户模块

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/user/wx-login` | 微信登录 | ❌ |
| GET | `/api/user/info` | 获取用户信息 | ✅ |
| PUT | `/api/user/info` | 更新用户信息 | ✅ |

### 饮食记录模块

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/diet` | 新增饮食记录 | ✅ |
| PUT | `/api/diet/{id}` | 更新饮食记录 | ✅ |
| DELETE | `/api/diet/{id}` | 删除饮食记录 | ✅ |
| GET | `/api/diet/query` | 查询记录+统计 | ✅ |

### 系统与文档

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/health` | 服务健康检查 | ❌ |
| GET | `/version` | 版本信息 | ❌ |
| GET | `/internal/actuator/health` | Actuator 健康检查（只暴露 health） | ❌ |
| GET | `/swagger-ui.html` | 接口文档（仅 dev / 显式开启） | ❌ |

> `/health`、`/version` 不在 `/api/**` 下，因此不经过 JWT 拦截器。新增公开接口请放在 `/api/**` 内并在 `WebMvcConfig` 里显式 exclude，避免漏配鉴权。

### 认证方式

需要认证的接口，在请求头中添加：

```
Authorization: Bearer <token>
```

token 通过 `/api/user/wx-login` 接口获取。

## 项目结构

```
src/main/java/com/wyy/fm/
├── FmApplication.java          # 启动类
├── common/                     # 公共组件
│   ├── Result.java             # 统一响应封装
│   ├── ErrorCode.java          # 错误码枚举
│   ├── BusinessException.java  # 业务异常
│   ├── GlobalExceptionHandler.java  # 全局异常处理
│   └── JwtUtil.java            # JWT 工具类
├── config/                     # 配置类
│   ├── AuthInterceptor.java    # JWT 鉴权拦截器
│   ├── WebMvcConfig.java       # 拦截器注册
│   ├── CorsConfig.java         # 跨域配置
│   └── RestTemplateConfig.java # RestTemplate 配置
├── controller/                 # REST 接口层
├── service/                    # 业务接口层
│   └── impl/                   # 业务实现层
├── repository/                 # 数据访问层 (JPA)
├── model/                      # JPA 实体
│   └── BaseEntity.java         # 实体基类
└── dto/                        # 请求/响应 DTO
```

## 数据模型

### User（用户）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| openid | String | 微信 openid（唯一） |
| unionId | String | 微信 unionId |
| nickname | String | 昵称 |
| avatarUrl | String | 头像 URL |
| phone | String | 手机号 |
| gender | Integer | 性别（0-未知 1-男 2-女） |
| status | Integer | 状态（0-正常 1-禁用） |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

### DietRecord（饮食记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| userId | Long | 用户 ID |
| recordDate | LocalDate | 记录日期 |
| mealType | Integer | 餐次（1-早餐 2-午餐 3-晚餐 4-加餐） |
| foodName | String | 食物名称 |
| calories | Integer | 热量（千卡） |
| remark | String | 备注 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

## 配置说明

### application.yml（默认/生产）

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://${SUPABASE_HOST}:${SUPABASE_PORT}/${SUPABASE_DB}
    username: ${SUPABASE_USER}
    password: ${SUPABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # 生产环境不自动建表

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24 小时
```

### application-dev.yml（开发环境）

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fmdb  # 本地 PostgreSQL
  jpa:
    hibernate:
      ddl-auto: update                          # 自动建表/改表
    defer-datasource-initialization: true       # data.sql 在建表后执行
  sql:
    init:
      mode: always                              # 执行 data.sql 种子数据（幂等）

app:
  cors:
    allowed-origin-patterns: "http://localhost:*,http://127.0.0.1:*,null"

springdoc:
  api-docs:
    enabled: true                               # 开发环境开放接口文档

wx:
  miniapp:
    mock-enabled: true                          # 无需真实微信密钥即可登录
```

> 上面两个脚本相关配置是**成套的强制依赖**：缺 `mode: always` 则在 PostgreSQL 上根本不执行 data.sql
> （默认值 embedded 仅对内存库生效）；缺 `defer-datasource-initialization: true` 则脚本跑在建表前，
> 空库首次启动报“表不存在”。另外 data.sql 必须写成幂等（`WHERE NOT EXISTS`）：dev 用 `ddl-auto: update`
> 会保留数据，脚本每次启动都重跑。生产 profile 是 `sql.init.mode: never` + `ddl-auto: validate`，不碰这份脚本。

## 环境变量一览

| 变量 | 作用 | 默认值 |
|------|------|--------|
| `SUPABASE_HOST` / `SUPABASE_PORT` / `SUPABASE_DB` / `SUPABASE_USER` / `SUPABASE_PASSWORD` | PG 连接 | 仅生产需要 |
| `JWT_SECRET` | JWT 签名密钥（≥ 32 字符） | 无默认值，不配则启动失败（dev 有专用默认值） |
| `WX_APPID` / `WX_SECRET` | 小程序凭证（映射到配置项 `wx.miniapp.appid` / `wx.miniapp.secret`） | 占位值，真实登录必须配 |
| `WX_MOCK_LOGIN` | 本地 mock 登录 | `false`（非 dev 开启会直接启动失败） |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | 允许的跨域来源 | 空（不放行）；含通配符或 `null` 时非 dev 启动失败 |
| `SWAGGER_ENABLED` | 是否开放接口文档 | `false` |

## 常用命令

```bash
# 编译
mvn compile

# 运行测试（需先启动本地 PostgreSQL，测试连 dev profile 的真实库，数据自动回滚）
mvn test

# 打包
mvn package

# 运行打包后的 jar
java -jar target/fm-0.0.1-SNAPSHOT.jar

# 清理构建产物
mvn clean
```

## 部署

### 生产环境部署清单

1. **数据库**：在 Supabase 创建 PostgreSQL 数据库，手动建表（参考数据模型）
2. **环境变量**：配置 `SUPABASE_HOST`、`SUPABASE_USER`、`SUPABASE_PASSWORD`、`SUPABASE_DB`、`JWT_SECRET`
3. **微信配置**：配置 `WX_APPID`、`WX_SECRET`，并确认 `WX_MOCK_LOGIN` 未开启
4. **跨域与文档**：按实际前端域名配 `CORS_ALLOWED_ORIGIN_PATTERNS`；确认 `SWAGGER_ENABLED=false`
5. **启动自检**：确认服务正常启动即可——非 dev profile 下若开着 mock 登录或 CORS 填了 `*`/`null`，
   `StartupSafetyCheck` 会直接报错阻止启动（不依赖人工看日志告警）
6. **服务器**：部署 jar 包，使用 `java -jar` 启动
7. **域名/SSL**：配置 HTTPS（小程序强制要求）

### 数据库建表 SQL（PostgreSQL）

```sql
-- 用户表
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    openid VARCHAR(64) NOT NULL UNIQUE,
    union_id VARCHAR(64),
    nickname VARCHAR(64),
    avatar_url VARCHAR(512),
    phone VARCHAR(20),
    gender INTEGER DEFAULT 0,
    status INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_openid ON users(openid);
CREATE INDEX idx_phone ON users(phone);

-- 饮食记录表
CREATE TABLE diet_records (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    record_date DATE NOT NULL,
    meal_type INTEGER NOT NULL,
    food_name VARCHAR(200) NOT NULL,
    calories INTEGER NOT NULL,
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_date ON diet_records(user_id, record_date);
```

> **已有库注意（破坏性变更）**：早期版本把 `gender`/`status` 建成了 `SMALLINT`，而实体字段是 `Integer`，
> 生产 `ddl-auto: validate` 会报 `wrong column type encountered in column [gender]` 且服务无法启动。
> 实体侧已统一为 `Integer`（不再写 `columnDefinition`），**部署本版本前必须先对目标库执行一次**：
>
> ```sql
> ALTER TABLE users ALTER COLUMN gender TYPE integer,
>                   ALTER COLUMN status TYPE integer;
> ```

## 开发规范

详见 [AGENTS.md](./AGENTS.md)，包含：
- 包结构约定
- API 响应格式（Result 统一封装）
- 异常处理规范（ErrorCode + BusinessException）
- 认证与鉴权机制
- 实体与数据库规范
- 注释规范

## License

MIT
