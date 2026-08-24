# FM 项目 — Agent 指南

> FM 小程序后端 API 服务。本文档供 AI coding agent 和开发者阅读，描述项目结构、约定和开发规范。

## 技术栈

| 项 | 版本/说明 |
|---|---|
| Java | 17（`JAVA_HOME` 已在 `~/.zshrc` 固定为 openjdk@17） |
| Spring Boot | 3.2.5 |
| ORM | Spring Data JPA（Hibernate） |
| 数据库 | 生产 PostgreSQL（Supabase）/ 开发 H2 内存库 |
| 认证 | JWT（jjwt 0.12.5），AuthInterceptor 拦截 |
| 构建 | Maven |
| 工具库 | Lombok、Jakarta Validation |

## 启动与构建

```bash
# 开发环境（H2 内存库，自动建表）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 生产环境（需要 Supabase 环境变量）
SUPABASE_HOST=xxx SUPABASE_USER=xxx SUPABASE_PASSWORD=xxx SUPABASE_DB=xxx mvn spring-boot:run

# 编译检查
mvn compile

# 跑测试
mvn test
```

- 默认端口 `8080`
- 开发环境 H2 Console：`http://localhost:8080/h2-console`（JDBC URL: `jdbc:h2:mem:fmdb`）

## 包结构

```
src/main/java/com/wyy/fm/
├── FmApplication.java          # 启动类
├── common/                     # 公共基础设施
│   ├── Result.java             # 统一响应封装 Result<T>
│   ├── ErrorCode.java          # 错误码枚举（所有业务错误码集中定义）
│   ├── BusinessException.java  # 业务异常（携带 ErrorCode）
│   ├── GlobalExceptionHandler.java  # 全局异常处理 @RestControllerAdvice
│   └── JwtUtil.java            # JWT 工具类
├── config/                     # 配置类
│   ├── AuthInterceptor.java    # JWT 鉴权拦截器
│   ├── WebMvcConfig.java       # 注册拦截器、配置拦截路径
│   ├── CorsConfig.java         # 跨域配置
│   └── RestTemplateConfig.java # RestTemplate 配置
├── controller/                 # REST 接口层（只负责接收请求、调用 Service、返回 Result）
├── service/                    # 业务接口层
│   ├── XxxService.java         # 接口定义
│   └── impl/                   # 实现层
│       └── XxxServiceImpl.java # 具体实现（@Service）
├── repository/                 # 数据访问层（Spring Data JPA Repository）
├── model/                      # JPA 实体（对应数据库表）
│   └── BaseEntity.java         # 基类（id + createdAt + updatedAt）
└── dto/                        # 请求/响应 DTO（与前端交互的数据结构）
```

## 核心约定

### 1. API 响应格式

所有接口统一返回 `Result<T>`：

```json
{ "code": 200, "message": "success", "data": { ... } }
```

- 成功：`Result.ok(data)` 或 `Result.ok()`
- 失败：`Result.fail(ErrorCode.XXX)` 或 `Result.fail(code, message)`

### 2. 异常处理

**禁止**直接抛 `IllegalArgumentException` / `RuntimeException`。必须用 `BusinessException` + `ErrorCode`：

```java
// ✅ 正确
throw new BusinessException(ErrorCode.USER_NOT_FOUND);
throw new BusinessException(ErrorCode.NO_PERMISSION, "该记录属于其他用户");

// ❌ 禁止
throw new RuntimeException("用户不存在");
throw new IllegalArgumentException("参数错误");
```

新增业务错误时：
1. 先在 `ErrorCode` 枚举中添加错误码和默认提示
2. 再在 Service 中 `throw new BusinessException(ErrorCode.XXX)`

### 3. 认证与鉴权

- `AuthInterceptor` 拦截所有 `/api/**` 路径
- 白名单（不需要 token）：`/api/user/wx-login`、`/health`
- 新增公开接口时，必须在 `WebMvcConfig.addInterceptors()` 中添加 `excludePathPatterns`
- 获取当前用户 ID：`request.getAttribute(AuthInterceptor.CURRENT_USER_ID)`

### 4. 实体与数据库

- 所有实体继承 `BaseEntity`（自动获得 `id`、`createdAt`、`updatedAt`）
- 使用 `@Data` + `@EqualsAndHashCode(callSuper = true)`
- 表名用 `@Table(name = "xxx")`，字段名用 `@Column(name = "xxx")`
- 关联关系用 `userId` 字段（逻辑外键），不用 `@ManyToOne`
- 生产环境 `ddl-auto: validate`（不会自动建表），开发环境 `ddl-auto: create-drop`

### 5. Service 层

- 接口定义在 `service/XxxService.java`
- 实现类在 `service/impl/XxxServiceImpl.java`，标注 `@Service`
- 依赖注入用构造器注入（`@RequiredArgsConstructor` + `private final`）
- 写操作加 `@Transactional`，只读查询不加

### 6. Controller 层

- 标注 `@RestController` + `@RequestMapping("/api/xxx")`
- 只负责：接收参数 → 提取 userId → 调用 Service → 包装 Result
- 不包含业务逻辑
- 请求体 DTO 加 `@Valid` 注解触发校验

### 7. DTO 规范

- 请求 DTO 命名：`CreateXxxRequest` / `UpdateXxxRequest` / `QueryXxxRequest`
- 响应 DTO 命名：`XxxResponse`
- 校验注解：`@NotNull`、`@NotBlank`、`@Size` 等（Jakarta Validation）
- 使用 Lombok `@Data` / `@Builder`

### 8. RESTful 路径

| 操作 | 方法 | 路径示例 |
|---|---|---|
| 创建 | POST | `/api/diet` |
| 查询 | GET | `/api/diet/query` |
| 更新 | PUT | `/api/diet/{id}` |
| 删除 | DELETE | `/api/diet/{id}` |
| 用户信息 | GET/PUT | `/api/user/info` |
| 登录 | POST | `/api/user/wx-login` |

## 新增功能 Checklist

添加一个新功能模块时，按以下顺序创建文件：

1. **model** — 定义 JPA 实体，继承 `BaseEntity`
2. **repository** — 继承 `JpaRepository<Entity, Long>`，添加自定义查询方法
3. **dto** — 定义请求/响应 DTO，加校验注解
4. **service** — 定义接口 + impl 实现类
5. **controller** — 定义 REST 接口，返回 `Result<T>`
6. **ErrorCode** — 如有新错误，在枚举中添加
7. **WebMvcConfig** — 如有公开接口（不需要登录），添加白名单

## 注释规范

本项目同时是**学习项目 + 生产代码**，注释要写得充分：

- **类注释**：说明这个类的作用、职责边界、和其他类的关系
- **方法注释**：说明输入输出、业务含义、为什么这样写（不只是做了什么）
- **关键逻辑**：分支判断、边界处理、设计取舍要解释原因
- **实体字段**：每个字段说明业务含义、枚举值含义、约束条件
- **复杂算法/统计**：分步骤注释，让初学者能跟上思路

参考现有代码风格：`BaseEntity.java`、`User.java`、`DietRecord.java` 的注释密度就是标准。

## 敏感信息

- JWT secret、数据库密码等通过环境变量注入，**不写入代码或配置文件**
- `application.yml` 中使用 `${ENV_VAR:default}` 占位符
- 生产部署时通过环境变量传入 `SUPABASE_HOST` / `SUPABASE_USER` / `SUPABASE_PASSWORD` / `SUPABASE_DB` / `JWT_SECRET`
