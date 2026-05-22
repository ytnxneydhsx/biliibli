本文档详细阐述 bilibili_web 用户端应用的路由架构与页面体系设计。作为基于 Vue 3 + Vue Router 5 构建的单页应用，bilibili_web 采用声明式路由配置、导航守卫认证保护以及模块化页面组织，为视频社区平台提供完整的前端页面框架。通过分析路由定义、页面组件结构和导航逻辑，开发者能够深入理解用户端的页面流转机制与功能边界。

## 路由架构概览

bilibili_web 的路由系统基于 Vue Router 5 构建，采用 **HTML5 History 模式** 提供干净的 URL 路径。路由配置集中在 `src/router.ts` 文件中，通过 `createRouter` 工厂函数创建路由实例，并配置了全局滚动行为和认证守卫。

**路由实例创建与基础配置**：路由实例使用 `createWebHistory()` 创建，启用了 History 模式以消除 URL 中的 `#` 符号。全局滚动行为配置为每次导航后自动滚动到页面顶部，确保用户在不同页面间切换时获得一致的视觉体验。

**路由守卫机制**：`router.beforeEach` 全局前置守卫实现了认证保护逻辑。当用户访问需要认证的页面（`meta.requiresAuth: true`）且本地无有效 token 时，系统会自动重定向到认证页面，并携带 `redirect` 查询参数以便登录后返回原目标页面。

Sources: [router.ts](bilibili_web/src/router.ts#L1-L84)

## 页面体系与路由映射

bilibili_web 的页面体系包含 **10 个核心视图组件**，分为公开页面和受保护页面两大类。公开页面无需登录即可访问，而受保护页面需要用户完成身份认证。

**公开页面**包括首页、认证页、搜索页、视频详情页和用户空间页。这些页面构成了平台的基础浏览体验，允许未登录用户浏览视频、搜索内容和查看公开用户信息。

**受保护页面**包括创作中心、个人资料设置、隐私设置和消息中心。这些页面涉及用户数据修改、内容创作和私密通信，因此需要严格的认证保护。

以下表格详细列出了所有页面的路由配置：

| 路由路径 | 路由名称 | 组件文件 | 认证要求 | 功能描述 |
|---------|---------|---------|---------|---------|
| `/` | `home` | `HomeView.vue` | 否 | 首页，展示推荐视频和热门排行榜 |
| `/auth` | `auth` | `AuthView.vue` | 否 | 认证页面，包含登录和注册功能 |
| `/search` | `search` | `SearchView.vue` | 否 | 搜索页面，支持视频和用户搜索 |
| `/video/:id` | `video` | `VideoDetailView.vue` | 否 | 视频详情页，展示视频播放、评论和互动功能 |
| `/user/:uid` | `user` | `UserSpaceView.vue` | 否 | 用户空间页，展示用户公开信息和视频列表 |
| `/studio` | `studio` | `StudioView.vue` | **是** | 创作中心，视频上传和管理功能 |
| `/profile` | `profile` | `ProfileView.vue` | **是** | 个人资料页，包含嵌套路由 |
| `/profile` | `profile` | `SettingsView.vue` | **是** | 资料设置子页面（默认子路由） |
| `/profile/privacy` | `profile-privacy` | `ProfilePrivacyView.vue` | **是** | 隐私设置子页面 |
| `/settings` | - | - | - | 重定向到 `/profile` 的别名路由 |
| `/messages` | `messages` | `MessagesView.vue` | **是** | 消息中心，即时通信功能 |

Sources: [router.ts](bilibili_web/src/router.ts#L10-L80)

## 认证保护与路由守卫

bilibili_web 的认证保护通过路由守卫和元数据标记实现。需要认证的页面在路由配置中设置 `meta.requiresAuth: true`，全局前置守卫在每次导航时检查认证状态。

**认证状态管理**：认证状态通过响应式对象 `authState` 管理，包含 `token`、`uid`、`username` 和 `profile` 等关键字段。该状态与 `localStorage` 持久化，确保页面刷新后认证状态得以保留。

**守卫执行流程**：当用户访问受保护页面时，守卫首先检查 `to.meta.requiresAuth` 标记。若标记为 `true` 且 `authState.token` 为空，则重定向到认证页面，并将原目标路径作为 `redirect` 查询参数传递。认证页面登录成功后会解析该参数并自动跳转回原目标页面。

**测试验证**：路由守卫逻辑通过单元测试验证，确保未认证用户无法直接访问受保护页面，且认证后能够正确解析嵌套路由。

Sources: [router.ts](bilibili_web/src/router.ts#L66-L84)
Sources: [router.spec.ts](bilibili_web/src/router.spec.ts#L1-L50)

## 应用布局与导航结构

bilibili_web 采用统一的布局结构，由 `App.vue` 作为根组件，包含固定的顶部导航栏和动态内容区域。

**根组件结构**：`App.vue` 定义了 `.app-shell` 容器，包含 `SiteHeader` 组件和 `RouterView` 路由出口。这种布局确保了导航栏在所有页面中保持一致，同时允许每个页面组件独立定义自己的内容结构。

**顶部导航栏**：`SiteHeader` 组件实现了平台的全局导航，包含品牌标识、搜索表单、导航链接和用户状态区域。导航栏采用响应式设计，在桌面端显示完整的导航链接，在移动端可能需要折叠处理。

**导航链接逻辑**：导航栏根据用户的认证状态动态显示不同的链接。已登录用户可以看到"私信"、"创作中心"和"个人"等链接，而未登录用户则显示"登录/注册"按钮。用户头像和昵称以可点击的芯片形式展示，点击后跳转到用户个人空间。

Sources: [App.vue](bilibili_web/src/App.vue#L1-L13)
Sources: [SiteHeader.vue](bilibili_web/src/components/SiteHeader.vue#L1-L237)

## 页面功能详解

### 首页（HomeView）

首页是平台的入口页面，采用 **双栏布局**：左侧为今日推荐视频和最新公开视频列表，右侧为今日热榜排行榜。首页通过并行请求加载视频列表和排行榜数据，确保快速展示核心内容。

**推荐视频展示**：首个视频作为"今日推荐"以大卡片形式展示，包含视频封面、标题、播放次数、发布时间和作者信息。推荐卡片提供"立即观看"、"去搜索"和"创作中心"三个快捷操作按钮。

**视频网格布局**：推荐位之外的视频以响应式网格布局展示，使用 `VideoCard` 组件渲染每个视频卡片。网格布局根据屏幕宽度自动调整列数，确保在不同设备上都有良好的展示效果。

Sources: [HomeView.vue](bilibili_web/src/views/HomeView.vue#L1-L331)

### 认证页面（AuthView）

认证页面采用 **左右分栏布局**：左侧为社区介绍和欢迎信息，右侧为登录/注册表单。页面支持登录和注册两种模式切换，通过 `tab` 状态控制显示哪个表单。

**登录功能**：登录表单收集用户名和密码，调用 `/users/login` API 获取认证 token。登录成功后调用 `applyLogin` 函数更新认证状态，并根据 `redirect` 查询参数跳转到目标页面。

**注册功能**：注册表单收集用户名、昵称、密码和确认密码，调用 `/users/register` API 创建账户。注册成功后自动切换到登录模式，并预填用户名和密码以便用户直接登录。

**已登录状态处理**：当用户已登录时，认证页面显示欢迎信息和快捷操作链接，包括"去首页看看"、"进入创作中心"和"退出登录"选项。

Sources: [AuthView.vue](bilibili_web/src/views/AuthView.vue#L1-L313)

### 视频详情页（VideoDetailView）

视频详情页展示单个视频的完整信息，包括视频播放器、作者信息、互动功能、评论区等。页面通过动态路由参数 `:id` 获取视频 ID，并并行加载视频详情和评论数据。

**视频播放器**：页面集成 HTML5 视频播放器，支持视频播放、暂停、进度控制等基本功能。视频 URL 从 `VideoDetailVO.videoUrl` 获取。

**互动功能**：页面提供点赞、关注作者等互动功能。点赞和关注状态通过独立的 API 调用更新，并实时更新本地状态以提供即时反馈。

**评论系统**：评论区使用 `CommentList` 组件展示评论列表，支持回复、点赞和删除操作。评论数据通过 `/videos/:id/comments` API 获取，支持分页加载。

Sources: [VideoDetailView.vue](bilibili_web/src/views/VideoDetailView.vue#L1-L318)

### 搜索页面（SearchView）

搜索页面提供 **视频搜索和用户搜索** 两种模式，通过选项卡切换。页面支持关键词搜索、搜索历史记录和结果展示。

**搜索功能实现**：搜索页面使用 URL 查询参数 `q` 和 `tab` 同步搜索状态，确保搜索结果可以通过 URL 分享和书签保存。搜索请求通过 `/search/videos` 和 `/search/users` API 执行。

**搜索历史**：已登录用户的搜索历史通过 `/search/videos/history` API 获取，并以标签形式展示在侧边栏。点击历史标签可快速执行搜索。

**结果展示**：视频搜索结果使用 `VideoCard` 组件以网格布局展示，用户搜索结果使用 `UserCard` 组件以列表形式展示。

Sources: [SearchView.vue](bilibili_web/src/views/SearchView.vue#L1-L227)

### 用户空间页（UserSpaceView）

用户空间页展示用户的公开信息、视频列表、社交关系等。页面通过动态路由参数 `:uid` 获取用户 ID，并并行加载用户资料、视频列表、粉丝列表、关注列表和好友列表。

**用户资料展示**：页面顶部展示用户头像、昵称、签名和粉丝/关注/好友数量。已登录用户可以执行关注操作和发送私信。

**视频列表**：用户公开的视频以网格布局展示，使用 `VideoCard` 组件渲染。视频数据通过 `/users/:uid/videos` API 获取，支持分页。

**社交关系**：页面侧边栏展示用户的粉丝、关注和好友列表，使用 `UserCard` 组件渲染每个用户卡片。

Sources: [UserSpaceView.vue](bilibili_web/src/views/UserSpaceView.vue#L1-L234)

### 创作中心（StudioView）

创作中心提供视频上传和管理功能，包括视频文件上传、封面上传、标题和简介编辑等。页面采用分步上传流程，确保大文件上传的可靠性和进度反馈。

**上传流程**：视频上传采用 **分片上传** 策略，包括初始化上传会话、获取签名 URL、分片上传和完成上传四个步骤。上传过程中实时更新进度条，提供良好的用户体验。

**表单设计**：上传表单包含标题、简介、封面上传和视频文件选择。封面支持预览，视频文件支持时长探测。

Sources: [StudioView.vue](bilibili_web/src/views/StudioView.vue#L1-L294)

### 个人资料页（ProfileView + SettingsView）

个人资料页采用 **侧边栏导航 + 内容区域** 的布局，左侧为导航菜单，右侧为对应的内容页面。页面包含两个子路由：资料设置和隐私设置。

**资料设置**：允许用户修改头像、昵称和签名。头像上传通过 `/me/uploads/avatar` API 实现，资料更新通过 `/me/profile` API 实现。

**隐私设置**：允许用户配置私信接收策略，包括"所有人都可以私信我"、"仅联系人可以私信我"、"陌生人只能先发一条"和"不接受私信"四个选项。

Sources: [ProfileView.vue](bilibili_web/src/views/ProfileView.vue#L1-L101)
Sources: [SettingsView.vue](bilibili_web/src/views/SettingsView.vue#L1-L254)
Sources: [ProfilePrivacyView.vue](bilibili_web/src/views/ProfilePrivacyView.vue#L1-L195)

### 消息中心（MessagesView）

消息中心提供即时通信功能，支持私人聊天和群组聊天。页面采用 **三栏布局**：左侧为会话列表侧边栏，中间为消息流区域，右侧为群组设置抽屉（仅群组聊天时显示）。

**WebSocket 连接**：消息中心通过 WebSocket 实现实时消息传输，支持连接状态管理、消息推送、历史消息加载等功能。

**会话管理**：会话列表分为私人聊天和群组聊天两个选项卡，显示未读消息数量和最后消息预览。点击会话可打开对应的聊天窗口。

**消息功能**：支持文本消息和图片消息的发送，消息气泡根据发送方向（自己/对方）采用不同的样式。群组聊天支持群设置、成员管理和禁言功能。

Sources: [MessagesView.vue](bilibili_web/src/views/MessagesView.vue#L1-L321)

## 认证状态管理

bilibili_web 的认证状态通过 `auth.ts` 模块集中管理，使用 Vue 3 的 `reactive` API 创建响应式状态对象。

**状态结构**：`authState` 包含 `token`（JWT token）、`uid`（用户 ID）、`username`（用户名）、`profile`（用户资料）和 `ready`（初始化完成标记）五个字段。

**持久化机制**：token、uid 和 username 三个字段持久化到 `localStorage`，确保页面刷新后认证状态得以保留。用户资料通过 `/users/:uid` API 动态获取。

**认证操作**：`applyLogin` 函数处理登录成功后的状态更新，包括解析 JWT token 提取 uid、更新 localStorage 和刷新用户资料。`logout` 函数清除所有认证状态和 localStorage 数据。

Sources: [auth.ts](bilibili_web/src/lib/auth.ts#L1-L89)

## API 集成层

bilibili_web 使用 Axios 库封装了统一的 API 客户端，位于 `src/lib/api.ts` 文件中。该客户端提供了请求拦截、响应处理和错误标准化功能。

**请求拦截**：自动从 `localStorage` 读取 token 并添加到请求头的 `Authorization` 字段，格式为 `Bearer ${token}`。

**响应处理**：统一处理 API 响应格式，当响应包含 `code` 字段时，检查 `code` 是否为 0（成功），否则抛出包含 `message` 的错误。支持 `json-bigint` 处理大整数，避免精度丢失。

**API 方法**：提供 `get`、`post`、`put`、`delete` 四个方法，返回类型化 Promise，支持泛型参数指定响应数据类型。

Sources: [api.ts](bilibili_web/src/lib/api.ts#L1-L73)

## 开发与构建配置

bilibili_web 使用 Vite 作为构建工具，TypeScript 作为开发语言，支持热模块替换和类型检查。

**开发服务器配置**：开发服务器监听 `0.0.0.0:5173`，配置了 API 代理规则，将 `/users`、`/me`、`/videos`、`/search`、`/ws` 开头的请求代理到后端服务。代理目标可通过 `VITE_API_PROXY_TARGET` 环境变量配置。

**构建配置**：生产构建执行 TypeScript 类型检查和 Vite 打包，输出到 `dist` 目录。

**测试配置**：使用 Vitest 作为测试框架，支持单元测试和快照测试。路由守卫等核心逻辑通过单元测试验证。

Sources: [vite.config.ts](bilibili_web/vite.config.ts#L1-L23)
Sources: [package.json](bilibili_web/package.json#L1-L30)

## 页面间导航流

bilibili_web 的页面导航遵循清晰的逻辑流，用户可以通过多种方式在不同页面间切换。

**主导航路径**：用户可以通过顶部导航栏的链接在首页、搜索、私信、创作中心和个人页面间切换。未登录用户访问受保护页面时会被重定向到认证页面。

**内容驱动导航**：视频卡片、用户卡片等组件通过 `RouterLink` 提供到视频详情页和用户空间页的导航链接。搜索结果、推荐视频、排行榜等区域都包含可点击的导航元素。

**认证后重定向**：用户在认证页面登录后，系统根据 `redirect` 查询参数自动跳转到原目标页面，实现无缝的认证流程。

**嵌套路由导航**：个人资料页使用嵌套路由，侧边栏导航链接通过 `RouterLink` 的 `name` 属性指定子路由，实现资料设置和隐私设置页面的切换。

## 下一步阅读

了解了用户端路由与页面体系后，建议继续阅读以下文档以深入理解特定功能模块：

- **[视频浏览与弹幕交互](5-shi-pin-liu-lan-yu-dan-mu-jiao-hu)**：深入了解视频播放、弹幕系统和评论互动的前端实现
- **[即时通信（IM）前端集成](6-ji-shi-tong-xin-im-qian-duan-ji-cheng)**：详细分析 WebSocket 连接管理、消息收发和群组聊天的前端架构
- **[管理端功能与权限设计](7-guan-li-duan-gong-neng-yu-quan-xian-she-ji)**：了解管理端的路由体系和权限控制机制