# FM 小程序后端服务

FM 小程序的后端 API 服务，提供用户管理、微信登录、饮食记录等功能。

## 技术栈

- **Java 17** + **Spring Boot 3.2.5**
- **Spring Data JPA** (Hibernate)
- **数据库**：H2（开发环境）/ PostgreSQL（生产环境，Supabase）
- **认证**：JWT（jjwt 0.12.5）
- **构建**：Maven
- **工具库**：Lombok、Jakarta Validation

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+

### 开发环境启动

```bash
# 克隆项目
git clone https://github.com/wangyunyou/FM-service.git
cd FM-service

# 使用 H2 内存数据库启动（无需额外配置）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动后访问：
- API 服务：`http://localhost:8080`
- H2 控制台：`http://localhost:8080/h2-console`
  - JDBC URL：`jdbc:h2:mem:fmdb`
  - 用户名：`sa`
  - 密码：（空）

### 生产环境启动

需要配置 Supabase PostgreSQL 数据库和微信小程序密钥：

```bash
# 设置环境变量
export SUPABASE_HOST=your-supabase-host
export SUPABASE_USER=postgres
export SUPABASE_PASSWORD=your-password
export SUPABASE_DB=postgres
export JWT_SECRET=your-jwt-secret-key

# 微信小程序配置（通过 application.yml 或环境变量）
export WX_MINIAPP_APPID=your-appid
export WX_MINIAPP_SECRET=your-secret

# 启动
mvn spring-boot:run
```

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

### 健康检查

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 服务健康检查 |

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
    url: jdbc:h2:mem:fmdb  # H2 内存数据库
  jpa:
    hibernate:
      ddl-auto: create-drop  # 自动建表，重启清空
  h2:
    console:
      enabled: true
```

## 常用命令

```bash
# 编译
mvn compile

# 运行测试
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
3. **微信配置**：配置 `WX_MINIAPP_APPID`、`WX_MINIAPP_SECRET`
4. **服务器**：部署 jar 包，使用 `java -jar` 启动
5. **域名/SSL**：配置 HTTPS（小程序强制要求）

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
    gender SMALLINT DEFAULT 0,
    status SMALLINT DEFAULT 0,
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
