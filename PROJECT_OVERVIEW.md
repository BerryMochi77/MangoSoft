# MangoSoft — 项目综合说明文档

---

## 一、项目定位与服务对象

**MangoSoft** 是一款面向社区用户的 Android 原生社交论坛 App，核心场景是**帖子发布、评论、举报与内容审核**。

项目背景为 ANU COMP2100 课程第二次 Hackathon，目标是：
1. 将课程第一次 Hackathon 实现的内容审核后端逻辑移植到完整的 Android UI；
2. 在此基础上设计并实现一个具备一定技术深度的新功能——**AI 个性化 Feed 策展**。

服务的两类用户：
- **普通用户**：浏览帖子、发帖、评论、点赞、举报、定制个人主页、使用 AI 推送。
- **管理员（Admin）**：审核举报、冻结账号、查看数据分析仪表盘和 AI 过滤统计。

---

## 二、具体实现的功能

### 2.1 账号与认证

| 功能 | 说明 |
|------|------|
| 登录 | 用户名 + 密码登录；设备自动记住上次用户名；可选"记住密码" |
| 角色区分 | `User.Role.Admin` / `User.Role.User`，全局权限控制 |
| 密码修改 | 在 Profile 页 Edit profile 菜单中修改 |
| 登出 | Settings → 登出，清除会话状态 |

### 2.2 信息流（Feed Tab）

- 以时间倒序或"热度"（按浏览量 + 反应数排序）展示全部帖子。
- 关键词搜索：实时过滤帖子标题和正文。
- 发帖：支持富文本（Markdown 风格内部 Token）和图片附件（通过 Android Photo Picker 选取）。
- 管理员专属入口：Feed 右上角直达举报管理、分析仪表盘。
- 普通用户被封禁（冻结）时会进入只读模式。

### 2.3 帖子详情（PostViewerActivity）

- 显示帖子标题、正文、图片附件、Hashtag 和 Emoji 反应。
- 帖子头部支持向下折叠（滑动评论时头部收起为紧凑态）。
- 评论（Message）系统：
  - **层级嵌套**（Reddit 风格，最多 3 层视觉缩进）：点击回复按钮后底部输入框自动切换为"Replying to …" 提示。
  - 发送后评论按 `likeCount` 降序 + 时间升序排列。
  - 点击评论正文折叠/展开其子回复。
  - `@用户名` 提及：底部工具栏 `@` 按钮弹出 Follow/Friend 选择器，插入 mention；对应用户收到通知。
  - 图片附件支持全屏预览，可"保存到相册"或"保存为 Emoji"。
- 评论交互：
  - 👍 / 👎 点赞/踩（`MessageReactionRegistry`，per-message × per-user）。
  - 书签收藏（`MessageBookmarkRegistry`）。
  - 编辑（`MessageEditRegistry`，编辑后原始内容不再显示）。
  - 软删除（`MessageDeletionRegistry`，管理员或自己可删）。
- 举报：普通用户可举报任意评论；管理员可隐藏/取消隐藏。

### 2.4 Trending（标签趋势 Tab）

- 默认展示按"热度"（浏览量 + 帖子反应数）排名的推荐帖子。
- Hashtag 芯片列表（最多 6 个，点击"Show more"可展开全部）。
- 点击任一 Tag 在同页面内联展示过滤结果。
- `PostSearchStrategy` 接口，当前实现：`HashtagSearchStrategy`（按 Tag 过滤）和 `KeywordSearchStrategy`（按关键词）。
- 跨 Tab 导航：FeedFragment / PostViewerActivity 点击 `#tag` → 跳转到 TrendsFragment 并预填过滤。

### 2.5 AI 策展（AI Tab）⭐ 新功能

- **偏好编辑器**：用户用自然语言描述自己感兴趣的话题（每用户独立存储于 `SharedPreferences`）。
- **Curate My Feed**：将当前全部可见帖子（含标题、正文、Hashtag、回复数）连同用户偏好一次性发给 DeepSeek LLM；模型返回 JSON 数组，包含每篇帖子的 `worth_reading`（boolean）、`score`（0–10）和 `summary`（原因摘要）。
- 结果展示：
  - 推荐帖子按 score 降序排列，配 AI 摘要。
  - 未推荐帖子折叠到"Filtered by AI"区域，用户可点击"Show anyway"强制打开。
- 用户反馈：推荐帖子可标记"Not relevant"；"Show anyway"和"Not relevant"操作均记录到 `AIAnalyticsRepository`，供管理员统计。
- 离线降级：网络/API 不可用时自动切换为 `OfflinePostCurationStrategy`（关键词匹配评分）。
- AI 摘要卡片：显示 Top Topic、Top 3 Tags 和推荐数量。

### 2.6 消息与通知（Messages Tab）

- **Replies 文件夹**：收到对自己评论的回复时产生通知。
- **Mentions 文件夹**：被 `@提及` 时产生通知。
- 各文件夹未读数以红色 Badge 显示；底部导航栏也显示汇总未读数（最大 `99+`）。
- 点击通知卡片跳转到对应帖子详情，并定位到目标评论。
- 打开文件夹后自动标记已读并刷新 Badge。

### 2.7 个人主页（Profile Tab）

- 展示头像、用户名、自定义 Profile 背景图。
- 分页浏览自己的帖子和回复（每页 3 条，支持翻页）。
- **Edit Profile 菜单**：
  - 修改显示名 / 密码。
  - 更换头像（4 个内置矢量头像 或 从相册选图，支持裁剪）。
  - 更换 Profile 背景（3 个内置背景 或 从相册选图）。
- **Profile Visibility**（Settings → Account）：分别控制"我的帖子"和"我的回复"是否对他人可见。
- **他人主页（UserProfileActivity）**：点击任意帖子/评论的头像可进入只读主页；显示该用户头像、背景，并受 visibility 控制。

### 2.8 社交关系

- **关注（Following）**：单向；在他人主页点击 Follow / Following 切换。
- **好友（Friends）**：双向互加；在他人主页点击 Add friend / Friends 切换。
- 关系数据存储于 `SharedPreferences`，通过 `RelationshipStore` 访问。
- `@提及` 的自动补全候选来源于 Follow / Friends 列表。

### 2.9 帖子 Emoji 反应

- 每篇帖子显示 Emoji 反应条（👍 ❤️ 😂 😡 + 自定义 Emoji）。
- 每个用户每个 Emoji 可选中/取消，选中态有强调边框。
- `+` 芯片在当前反应行内联展开更多 Emoji 选项（不弹 Dialog）。
- 用户可将任意文字 Emoji 保存为"已保存 Emoji"，也可将帖子/评论附图中的图片保存为 Emoji。

### 2.10 Settings（设置 Tab）

| 设置项 | 细节 |
|--------|------|
| 主题 | 浅色 / 深色 / 跟随系统，品牌色为芒果橘（accent） |
| 字体样式 | System / Serif / Monospace |
| 字体大小 | Small / Default / Large / Extra large |
| 语言 | 通过 `AppCompatDelegate.setApplicationLocales` 切换 |
| 时区 | 影响时间戳显示格式 |
| 账号安全 | Profile visibility 控制 |
| 登出 | 清除会话 |

### 2.11 管理员功能（Admin）

| 功能 | Activity |
|------|----------|
| 举报管理（消息级别） | `AdminReportsActivity` |
| 帖子举报管理 | `AdminPostReportsActivity` |
| 用户列表 | `AdminUserListActivity`（所有用户 / 活跃用户排行） |
| 帖子列表 | `AdminPostListActivity`（全部 / 今日 / 最多浏览 / 最多反应） |
| 回复列表 | `AdminReplyListActivity` |
| 冻结用户管理 | `FrozenUsersActivity` |
| 数据分析仪表盘 | `AdminAnalyticsDashboardActivity`（点击各指标行可直接跳转对应列表） |
| AI 过滤分析 | `AdminAiAnalyticsActivity`（AI 概览 / 过滤模式 / 用户反馈三节） |

---

## 三、设计思想

### 3.1 单 Activity + 多 Fragment

`MainActivity` 是唯一顶层宿主，持有 `BottomNavigationView`；五个顶层 Tab 均为 Fragment（`show()` / `hide()` 切换，不重建），保留各 Tab 的滚动和分页状态。深页面（帖子详情、管理员页等）以 Activity 形式叠加，拥有返回箭头但无底部导航栏。Fragment 通过 `TabHost` 接口与宿主通信，实现跨 Tab 的导航指令（如 FeedFragment 点击 #tag → TrendsFragment 预筛选）。

### 3.2 双模块分层（社交核心 vs. Android UI）

```
android/app        ← 只放 UI 代码（Activity / Fragment / Adapter / View）
android/social-core ← 纯 Java 业务逻辑，零 Android SDK 依赖
```

`social-core` 可独立用 JUnit 测试，`app` 仅通过 `social-core` 暴露的接口访问核心逻辑。新增社交功能优先在 `social-core` 建包，UI 层只调用接口。

### 3.3 Sidecar Registry 模式（SOLID 约束）

Hackathon 要求**不修改 `Message` 数据模型**（OCP 原则）。所有新增的 per-message 状态均以"旁挂注册表"（Sidecar Registry）存储，以 `Message.id()` 为键：

| Registry | 职责 |
|----------|------|
| `MessageEditRegistry` | 编辑记录 + 当前内容 |
| `MessageDeletionRegistry` | 软删除标记 |
| `MessageThreadRegistry` | 嵌套父子关系 + 深度 |
| `MessageReactionRegistry` | 评论级点赞/踩（per-message × per-user） |
| `MessageBookmarkRegistry` | 用户收藏标记 |

帖子级别的 Emoji 反应走独立的 `ReactionManager`（key 为 post UUID），与评论级点赞是两套完全独立的机制。

### 3.4 Strategy 模式（AI 策展）

`PostCurationStrategy` 接口定义 `curate(posts, viewerHint, preferences, callback)` 契约。当前有两个实现：

- `AiPostCurationStrategy`：调用 DeepSeek Chat Completions API，一次批量请求处理所有帖子。
- `OfflinePostCurationStrategy`：本地关键词匹配，无需网络，作为降级 fallback。

`AiFragment` 在 `onCreate` 注入具体策略，UI 对策略实现完全透明。未来换模型或增加策略只需新增实现类，无需改动 UI。

### 3.5 Repository 模式

- `AiUserPreferences`：封装 AI 偏好的 SharedPreferences 读写。
- `AppPreferencesRepository`：封装字体、语言、时区等 UI 偏好的读写。
- `AIAnalyticsRepository`：存储 AI 分析记录和用户反馈，供 `AIAnalyticsService` 读取计算报告。

### 3.6 关注点分离（Service vs. Activity）

- `AdminAnalyticsService.computeStats()`、`AIAnalyticsService.computeSummary()` 负责所有计算，Activity 只负责渲染。
- `ComposerFormatManager` 独立管理富文本 Token 解析、图片拷贝、Emoji 持久化；`Post` 和 `Message` 模型不变，富内容编码在现有文本字段内，由 Android UI 层负责渲染。

### 3.7 其他设计决策

- **异步模型**：AI 网络请求在 `ExecutorService` 单线程上执行，结果通过 `Handler(Looper.getMainLooper())` 回调到主线程，避免 ANR。
- **主题系统**：浅色/深色颜色资源同名放在 `values/` 和 `values-night/`，所有 Layout 只引用语义色名（如 `@color/text_primary`），不硬编码颜色值；进程启动时由 `SocialModerationApplication` 应用保存的主题模式。
- **字体系统**：`UiFontManager` 用 `WeakHashMap` 记住每个 `TextView` 的原始字号，避免多次进入页面时重复放大。
- **线程回复渲染**：`MessageThreadRegistry.flatten()` 将时间排序列表重排为深度优先列表，`ThreadIndentView` 按 depth 控制缩进宽度，父子连线由 `ThreadConnectorDecoration`（`RecyclerView.ItemDecoration`）在画布级别跨行绘制。

---

## 四、技术支持

### 4.1 Android 平台

| 技术 | 用途 |
|------|------|
| Android SDK（Java，非 Kotlin） | 全应用基础平台 |
| Fragment + BottomNavigationView | 单 Activity 多 Tab 导航 |
| RecyclerView + LinearLayoutManager | 帖子列表、评论列表、AI 推荐列表 |
| RecyclerView.ItemDecoration | 评论线程父子连线 |
| ActivityResultContracts.PickVisualMedia | Android Photo Picker（头像、背景、附图） |
| MediaStore | 图片保存至系统相册（Pictures/MangoSoft） |
| InputMethodManager | 软键盘管理 |
| AppCompatDelegate | 动态深色/浅色主题切换；语言切换 |
| SharedPreferences | AI 偏好、UI 偏好、社交关系、Login 状态 |
| EdgeToEdge + WindowInsetsCompat | 沉浸式状态栏 / 导航栏适配 |

### 4.2 Material Design Components

| 组件 | 用途 |
|------|------|
| `com.google.android.material` | Material 3 主题、颜色系统 |
| ChipGroup / Chip | Trending Hashtag 芯片、Emoji 反应芯片 |
| AlertDialog | 确认对话框、选择器 |
| Toast | 轻量提示 |

### 4.3 AI / 网络

| 技术 | 用途 |
|------|------|
| **DeepSeek Chat Completions API** | AI 个性化 Feed 策展（LLM 判断帖子相关性） |
| `HttpURLConnection` | 原生 HTTP 客户端，无第三方网络库依赖 |
| `org.json`（Android 内置） | JSON 构造与解析，包含防御性 markdown fence 剥离 |
| `ExecutorService`（单线程） | 后台 HTTP 请求线程 |

### 4.4 数据层（social-core）

| 技术 / 模式 | 用途 |
|-------------|------|
| DAO 模式（`PostDAO`, `UserDAO`） | 帖子、用户数据访问，单例 |
| CSV 持久化管线 | 核心数据（帖子、消息、用户、举报）的文件持久化 |
| JSON（`users.json`, `posts_seed.json`） | 用户记录持久化、演示数据种子 |
| AVL Tree（`sorteddata`） | 有序数据集合，支持范围查询和排序迭代 |
| `SortedData` / `SortedDataSlice` | 分页和切片抽象 |
| Strategy 模式（`PostSearchStrategy`） | Hashtag 搜索 / 关键词搜索可换策略 |
| Iterator 模式（举报列表） | `MostReportedMessageIterator` / `OldestReportedMessageIterator` |
| Singleton 注册表 | 所有 Sidecar Registry、`MentionNotificationRegistry`、`ReactionManager` |

### 4.5 构建工具

| 工具 | 说明 |
|------|------|
| Gradle（Kotlin DSL） | 多模块构建：`app` + `social-core` |
| Android Gradle Plugin | APK 打包 |
| ProGuard | Release 混淆规则（`proguard-rules.pro`） |

---

> 文档生成时间：2026-06-03。基于当前代码仓库分析，不含外部部署或运行时状态。