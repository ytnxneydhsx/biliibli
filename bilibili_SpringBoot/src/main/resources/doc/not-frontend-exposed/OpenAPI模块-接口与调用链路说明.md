# OpenAPI模块接口与调用链路说明（springdoc / Swagger UI / Knife4j）

## 1. 范围

本文覆盖项目中 OpenAPI 文档能力相关配置：

- `com.bilibili.config.openapi.OpenApiSecurityConfig`
- `com.bilibili.config.security.SecurityConfig`（文档路径放行）
- `application.yaml`、`application-dev.yaml`（springdoc 开关）
- 控制器上的 OpenAPI 注解（`@Tag`、`@Operation`、`@Parameter`）

## 2. 模块总览

| 层级 | 组件 | 职责 |
| --- | --- | --- |
| 依赖层 | `springdoc-openapi-starter-webmvc-ui`、`knife4j-openapi3-ui` | 生成并展示 OpenAPI 文档 |
| 规范层 | `OpenApiSecurityConfig` | 声明 `bearerAuth` 安全方案 |
| 安全层 | `SecurityConfig` | 控制文档 URL 是否匿名可访问 |
| 配置层 | `application*.yaml` | 按环境启停 API docs 与 Swagger UI |
| 业务注解层 | Controller `@Tag/@Operation` | 定义接口分组和摘要 |

## 3. 文档生成与访问链路

```mermaid
flowchart TD
    A[Controller 方法注解 @Tag/@Operation] --> B[springdoc 扫描]
    B --> C[生成 OpenAPI JSON]
    C --> D[/v3/api-docs]
    C --> E[Swagger UI /swagger-ui/index.html]
    C --> F[Knife4j /doc.html]
    D --> G[SecurityConfig 文档路径放行判定]
    E --> G
    F --> G
```

说明：

1. 只要控制器有 OpenAPI 注解，springdoc 会参与建模并输出到 `/v3/api-docs`。
2. `OpenApiSecurityConfig` 给规范增加全局 `bearerAuth`（HTTP Bearer + JWT）。
3. 是否允许匿名访问文档，由 `app.docs.public` 与 `DOC_PATHS` 放行规则共同决定。

## 4. 配置明细

## 4.1 OpenAPI 安全方案（`OpenApiSecurityConfig`）

核心配置：

1. `SecurityScheme` 名称：`bearerAuth`
2. 类型：`HTTP`
3. `scheme`：`bearer`
4. `bearerFormat`：`JWT`
5. 通过 `addSecurityItem` 全局挂载到 OpenAPI 文档

效果：Swagger/Knife4j 页面可使用 `Authorize` 输入 JWT。

## 4.2 springdoc 开关（`application*.yaml`）

`application.yaml`（默认）：

- `springdoc.api-docs.enabled: false`
- `springdoc.swagger-ui.enabled: false`
- `app.docs.public: false`

`application-dev.yaml`（开发环境）：

- `springdoc.api-docs.enabled: true`
- `springdoc.swagger-ui.enabled: true`
- `app.docs.public: true`

结论：

1. 默认配置下，文档服务不启用且不对外公开。
2. 开发配置下，文档启用并允许匿名访问。

## 4.3 文档路径放行（`SecurityConfig`）

`DOC_PATHS`：

- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs`
- `/v3/api-docs/**`
- `/v3/api-docs.yaml`
- `/doc.html`
- `/webjars/**`

仅当 `docsPublic=true` 时执行 `permitAll`。否则文档路径走 `anyRequest().authenticated()`。

## 5. 控制器注解使用现状

当前各控制器已普遍使用：

1. `@Tag`：接口分组（如 `User`、`Video`、`Comment`、`Search`、`Me Video Upload`）
2. `@Operation`：方法摘要
3. `@Parameter(hidden = true)`：隐藏 `@AuthenticationPrincipal` 这类非外部输入参数

这保证了文档对前端更清晰，不会暴露内部注入参数。

## 6. 访问入口与鉴权关系

常用入口：

1. OpenAPI JSON：`/v3/api-docs`
2. Swagger UI：`/swagger-ui/index.html`（兼容 `/swagger-ui.html`）
3. Knife4j：`/doc.html`

鉴权关系：

1. 文档页面是否可访问由 `SecurityConfig` 决定。
2. 业务接口是否需要登录仍由接口本身安全规则决定（`permitAll`、`authenticated`、`@PreAuthorize`）。
3. 即使文档页面公开，受保护 API 仍需要有效 JWT 才能调用成功。

## 7. 维护建议

1. 生产环境保持 `springdoc.*.enabled=false` 与 `app.docs.public=false`。
2. 仅在开发/测试环境开启文档匿名访问。
3. 新增控制器时同步补 `@Tag/@Operation`，保持文档可读性一致。
