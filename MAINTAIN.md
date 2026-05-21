# Maintain Guide

本文件记录当前项目的模块划分和后续维护约定，避免 Android UI、核心业务逻辑和持久化代码混在一起。

## 项目结构

```text
hackathon/
  android/
    app/                  Android UI 模块
    social-core/          可复用的纯 Java 社交核心模块
```

Android 迁移后的核心结构：

```text
android/
  app/
    src/main/java/com/example/comp2100miniproject/
      MainActivity.java        # 单 Activity 宿主 + BottomNavigationView
      TabHost.java             # Fragment 与宿主之间的接口
      FeedFragment.java        # 4 个 Tab 各一个 Fragment
      TrendsFragment.java
      ProfileFragment.java
      SettingsFragment.java
      PostViewerActivity.java  # 深页面（点进帖子）
      AdminReportsActivity.java
      FrozenUsersActivity.java
      LoginActivity.java
      src/
        MessageAdapter.java
        PostAdapter.java
    src/main/res/

  social-core/
    src/main/java/
      dao/
      dao/model/
      moderation/
      persistentdata/
      sorteddata/
      userstate/
```

## 模块职责

### android/app

只放 Android 相关代码：

- Activity、Fragment、Adapter
- XML layout、drawable、string、theme
- Android 权限、Intent、RecyclerView 等 UI 逻辑

不要在这里新增核心社交业务类，例如举报、隐藏、点赞、关注、通知、私信等。

### android/social-core

放纯 Java 业务逻辑，不依赖 Android SDK。

当前包含：

- `dao`：用户、帖子、消息的数据访问
- `dao.model`：`User`、`Post`、`Message` 等领域模型
- `moderation`：举报、隐藏消息、查看举报消息
- `persistentdata`：CSV 持久化管线和序列化
- `sorteddata`：排序数据结构
- `userstate`：用户状态

以后新增社交功能优先放在这里，例如：

```text
social/
  reaction/       点赞、收藏、表情
  relation/       关注、好友、拉黑
  notification/   通知
  messaging/      私信
  feed/           信息流排序和过滤
```

## 依赖方向

依赖只能这样走：

```text
android/app  ->  android/social-core
```

`social-core` 不能 import Android 包，例如：

```java
import android.content.Context;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
```

如果核心逻辑需要读取 Android 文件或数据库，应先定义接口，再由 `android/app` 提供 Android 版本实现。

## 构建验证

在迁移后的 Android 项目目录运行：

```powershell
cd D:\IntellJ_Project\Hackthon\hackathon\android
.\gradlew.bat build
```

## Profile appearance maintenance

Profile avatar and profile background are both user-facing appearance settings owned by the Android app layer. They do not modify `User`, `Post`, or `Message` domain models.

Current entry point:

- `ProfileFragment` -> `Edit profile`
- `Change avatar` -> default avatar or album image
- `Change profile background` -> default background or album image
- `Change display name` and `Change password` remain in the same menu

Storage:

- Avatar state is stored in `users.json` as `avatarSource` and `avatarValue`.
- Profile background state is stored in `users.json` as `profileBackgroundSource` and `profileBackgroundValue`.
- Public profile visibility is stored in `users.json` as `publicPosts` and `publicReplies`.
- Existing users without background fields fall back to `profile_background_default_1`.
- Existing users without visibility fields fall back to public posts and public replies.
- Album images are copied into app-private storage before saving the file URI, because Photo Picker URIs are temporary.

Code ownership:

- `AvatarManager` handles default/gallery avatar display and persistence calls.
- `ProfileBackgroundManager` handles default/gallery profile background display and persistence calls.
- `AuthManager` owns reading/writing the user JSON appearance fields.
- `ProfileFragment` owns the menu flow and screen refresh.
- `SettingsFragment` owns the user-facing visibility toggles in Account -> `Profile visibility`.
- `UserProfileActivity` reads `AuthManager.getProfileVisibility(...)` and hides another user's posts/replies when those sections are not public. The owner can still see their own content in the normal Profile tab.

How to add a default profile background:

1. Add a drawable under `android/app/src/main/res/drawable/`.
2. Add a label string in `android/app/src/main/res/values/strings.xml`.
3. Register it in `ProfileBackgroundManager.DEFAULT_BACKGROUNDS`.
4. Keep the option key stable because saved user records refer to it.

How to show a user's profile background elsewhere:

```java
ProfileBackgroundManager manager = new ProfileBackgroundManager(authManager);
manager.displayBackground(user, imageView);
```

## User profile navigation

Users can open another user's read-only profile by tapping that user's avatar in the feed, trends results, or comment threads. Post detail also lets the author line open the same profile because the detail header does not include a separate avatar.

Entry points:

- `PostAdapter` exposes `OnUserClick` for post-card avatars.
- `MessageAdapter` exposes `OnUserClick` for comment avatars.
- `FeedFragment`, `TrendsFragment`, `PostViewerActivity`, and `ProfileFragment` route those callbacks to `UserProfileActivity`.
- `UserProfileActivity` receives `UserProfileActivity.EXTRA_PROFILE_USER_ID` plus the current user extras from `AuthManager`.

Read-only profile behavior:

- `UserProfileActivity` uses `AvatarManager` and `ProfileBackgroundManager` to render the target user's current avatar/background.
- It lists the target user's visible posts and visible replies using existing `PostDAO` data and `MessageDeletionRegistry`.
- It does not edit `User`, `Post`, or `Message`, and does not create any new per-message state.

How to add this navigation somewhere else:

```java
Intent intent = new Intent(context, UserProfileActivity.class);
intent.putExtra(UserProfileActivity.EXTRA_PROFILE_USER_ID, targetUser.getUUID().toString());
intent.putExtra(AuthManager.EXTRA_USER_ID, currentUser.getUUID().toString());
intent.putExtra(AuthManager.EXTRA_IS_ADMIN, currentUser.role() == User.Role.Admin);
startActivity(intent);
```

## Composer format options

Post creation and reply composition share a small "more options" composer menu. The entry point is a plus button:

- In `CreatePostActivity`, the plus button inserts formatting into the post body or attaches a preview image.
- In `PostViewerActivity`, the plus button sits to the right of `Send` in the bottom reply bar.
- Reply-to-message dialogs also expose the same plus menu.

Current options:

- `Add image`: opens Android Photo Picker, copies the selected image into app-private storage, and inserts an internal `[[image:file-uri]]` token into the text.
- `Add emoji`: inserts the selected emoji or saved sticker at the cursor. The picker includes defaults, saved text emojis, and images saved as stickers.
- The emoji/sticker picker is a flat grid. Saved stickers render as thumbnail-only cells.
- Tapping a rendered image opens a full-screen preview. The preview has a top-right overflow menu with `Save image to gallery` and `Save image as emoji`.
- Tapping text that contains emojis opens a small chooser. A text emoji can be saved to the app emoji list or rendered as an image and saved to the gallery.
- Compact previews such as Profile -> My replies must call `ComposerFormatManager.previewText(...)` so internal image tokens appear as `[image]`, not as file paths.

Architecture:

- `ComposerFormatManager` owns the formatting tokens, image copy logic, and rendering helper.
- `ComposerFormatManager` also owns saved emoji/sticker extraction and persistence. It stores emoji glyphs and image token refs in `SharedPreferences`, not per-message state.
- `Post` and `Message` models are not modified. Rich content is encoded inside existing text fields and rendered by the Android UI layer.
- `PostViewerActivity.renderPost` and `MessageAdapter.ViewHolder.display` call `ComposerFormatManager.bindContent(...)` so image tokens render as an `ImageView` while plain text remains in the `TextView`.
- Image saving uses Android `MediaStore`. On Android 10+ it writes to `Pictures/Social Moderation` using scoped storage; older devices use the manifest's `WRITE_EXTERNAL_STORAGE` permission limit.

How to add another format option:

1. Add the option label in `strings.xml`.
2. Add a branch in `showComposerMenu(...)` for both `CreatePostActivity` and `PostViewerActivity`.
3. Keep storage either inside existing text tokens or in a sidecar registry if it becomes separate per-message state.
4. Update `ComposerFormatManager` if the new format needs parsing or rendering.

## Demo content seeding

`RandomContentGenerator` is only for demo feed data. It must never assign generated posts or replies to real registered users.

- Demo authors are limited to the fixed usernames in `RandomContentGenerator.DEMO_USERNAMES`.
- Do not replace this with "all member users" from `UserDAO`; that makes a newly registered user appear to have written seeded comments.
- User-created posts/replies should only be created through explicit UI actions such as `FeedFragment.createPost(...)` and `PostViewerActivity.addReplyMessage(...)`.
- Seeded posts and replies may include composer-format content such as emoji and `[[image:demo:*]]` image tokens. `ComposerFormatManager` resolves those demo image tokens to bundled drawable resources, so `social-core` still has no Android imports.
- `RandomContentGenerator.repairSeededData()` runs on app entry to hide old generated replies that were accidentally assigned to non-demo users, and to backfill rich demo bodies onto old seeded posts.
- Feed cards use `ComposerFormatManager.bindContent(...)` for post body previews, so demo images and emoji are visible before opening the post detail page.

## Rounded media

Use `RoundedImageView` for rectangular user-controlled media such as profile backgrounds and post/reply attachments. It clips the bitmap to an 8dp radius so media cards do not show square image corners against rounded UI surfaces. Keep `CircleAvatarImageView` only for avatars.
`CircleAvatarImageView` draws a thin theme border around avatars so they remain legible on image backgrounds.

当前已验证：

```text
BUILD SUCCESSFUL
```

如果提示找不到 Android SDK，需要在 `android/local.properties` 中配置：

```properties
sdk.dir=C\:\\Users\\52734\\AppData\\Local\\Android\\Sdk
```

`local.properties` 已被 `.gitignore` 忽略，不应提交。

## 新功能添加规则

1. 先判断功能属于 UI 还是核心逻辑。
2. 核心逻辑放入 `android/social-core/src/main/java`。
3. Android 页面只调用 core 暴露出来的方法。
4. 能用纯 Java 测试覆盖的逻辑，不写进 Activity。
5. 修改消息可见性时，优先使用 `Post.getVisibleMessages(isAdmin)`，不要直接遍历 `post.messages` 给普通用户展示。

## 顶层导航

应用是**单 Activity + 多 Fragment** 结构。`MainActivity` 是唯一持有
`BottomNavigationView` 的宿主，4 个 Tab 各对应一个 Fragment：

| Tab            | Fragment            | 职责                              |
|----------------|---------------------|-----------------------------------|
| `navFeed`      | `FeedFragment`      | 帖子列表 + 发帖 + 管理员审核入口 |
| `navTrending`  | `TrendsFragment`    | Trending tags + 按 tag 过滤的内联结果 |
| `navProfile`   | `ProfileFragment`   | 个人资料、头像、我的帖子/回复    |
| `navSettings`  | `SettingsFragment`  | 主题、登出                        |

Tab 切换通过 `FragmentTransaction.show()` / `hide()` 实现 — **不是** 启动新
Activity。这样底部栏始终留在原地、每个 Tab 的 scroll 和分页 state 也会
保留。"进入-退出" 式的 Activity 跳转只用于"深页面"（PostViewerActivity、
AdminReportsActivity、FrozenUsersActivity 等），它们有左上角箭头 `ic_arrow_back`，
没有底部栏。

Fragment 通过 `TabHost` 接口与宿主通信：

```java
public interface TabHost {
    User currentUser();
    void showTrendsForTag(String tag);   // 跨 Tab 跳转：点击 #tag 切到 Trends 并预过滤
    void requestLogout();                 // SettingsFragment 触发登出
}
```

资源约定：
- 菜单：`res/menu/bottom_nav_menu.xml`
- Tab 图标：`res/drawable/ic_tab_*.xml`
- 选中态色选择器：`res/color/bottom_nav_item_tint.xml`

新增顶层 Tab 的步骤：

1. 在 `bottom_nav_menu.xml` 加一个 item，配套加 `ic_tab_*.xml`。
2. 创建 `XxxFragment extends Fragment`，在 `onAttach` 里把 `context` 转成
   `TabHost` 缓存起来。
3. 在 `MainActivity.setupFragments` 里 `add()` 这个 fragment（初始 `hide()`），
   并在 `showTab` 的分支里把它对应到新 menu id。

跨 Tab 跳转走 `TabHost`。例如点击一个 #hashtag 切到 Trends，FeedFragment 调用
`host.showTrendsForTag(tag)`，MainActivity 内部做 `applyTagFilter` + 改
`selectedItemId`，让选中态高亮跟上。

**从深页面（PostViewerActivity 等）跳回 Tab**：深页面不是 Fragment，拿不到 `TabHost`。
做法是发一个 intent 回 MainActivity：

```java
Intent intent = new Intent(this, MainActivity.class);
intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
intent.putExtra(MainActivity.EXTRA_TRENDS_TAG, tag);
startActivity(intent);
finish();
```

`CLEAR_TOP | SINGLE_TOP` 让 MainActivity 走 `onNewIntent` 而不是重建，所以其他 Tab
的状态保留。MainActivity 在 `handleTrendsIntent` 里读 extra、用完立即 `removeExtra`
以避免配置变更时重复触发。新增类似的"深页面 → Tab"指令时沿用这个 pattern：新加一个
`EXTRA_*` 常量、在 `handleTrendsIntent` 同级加一个 handler。

页面顶部如需返回按钮，使用左上角箭头 `ImageButton`（参考
`activity_post_viewer.xml` 的 `buttonBack`，drawable 为 `ic_arrow_back.xml`），
不要再放整宽的底部 Back 按钮。系统返回手势已自动可用。

## 主题模式

`ThemeModeManager` 管理浅色 / 深色 / 跟随系统三种模式。状态写在
SharedPreferences，进程启动时由 `SocialModerationApplication.onCreate`
调用 `applySavedMode` 应用到 `AppCompatDelegate`。

- UI 入口：`SettingsFragment` 的 "Theme" 行，点开调
  `ThemeModeManager.showModeChooser(activity)`
- 颜色资源：浅色 `res/values/colors.xml`，深色 `res/values-night/colors.xml`，
  **同名** — layout 只引用 `@color/text_primary` 这类共享名，不要在 XML 里写
  死颜色
- 增加模式：在 `ThemeModeManager.Mode` 加 enum 项 + 在 `values/strings.xml`
  加 label

## 字体偏好

字体样式和大小属于 UI preference，和主题、语言、时区一样由 Settings 入口管理，不修改 `User`、`Post`、`Message`。

- UI 入口：`SettingsFragment` 的 `Font style` 和 `Font size` 行。
- 存储：`AppPreferencesRepository` 的 `font_family` 和 `font_size`，保存在 `ui_preferences`。
- 应用：`SocialModerationApplication` 注册 Activity lifecycle callback，在 Activity 创建和恢复时调用 `UiFontManager.applyToActivity(...)`。
- 字体样式当前支持 `System`、`Serif`、`Monospace`。
- 字体大小当前支持 `Small`、`Default`、`Large`、`Extra large`，通过原始 `TextView` px size 乘缩放比例实现。
- `UiFontManager` 用 `WeakHashMap` 记住每个 `TextView` 的原始字号和 typeface，避免反复进入页面时重复放大。

新增字体选项时，先在 `AppPreferencesRepository` 增加稳定 key 和 label，再在 `SettingsFragment` 的 picker 中注册。

## 用户头像

`AvatarManager` 管业务（默认头像、读写、URI），`CircleAvatarImageView` 管圆形
显示，`AvatarCropActivity` 提供裁剪 UI。

- 存储：用户 JSON 里 `avatarSource` ∈ `default` / `gallery`；`avatarValue` 是默
  认头像 key 或本地文件 URI。已存用户没有这两个字段时回退到第一个默认头像
- 默认头像：`drawable/avatar_default_{1..4}.xml`，在 `AvatarManager.DEFAULT_AVATARS`
  注册。key 字符串必须保持稳定（用户记录靠它识别）
- 从相册选图：`PickVisualMedia` → `AvatarManager.setGalleryAvatar(context, user, uri)`
  把字节拷到 `filesDir/avatars/<uuid>` 并存本地 URI。**不要**回到 `OpenDocument` —
  Photo Picker 返回的 URI 不能 `takePersistableUriPermission`，所以拷贝是必须的
- 在别的页面显示某个用户的头像：`new AvatarManager(authManager).displayAvatar(user, imageView)`
- 增加默认头像：加 drawable + 加 string label + 在 `DEFAULT_AVATARS` 里注册一项

## Per-message state（sidecar pattern）

Hackathon 2 brief 的硬约束：

> "New features that introduce additional per-message state (e.g., reactions) should avoid modifying the `Message` data model (e.g., be SOLID)."

也就是说，[`Message`](android/social-core/src/main/java/dao/model/Message.java) 只保留 Hackathon 1 时就有的字段（id / poster / thread / timestamp / message / hidden），所有 Hackathon 1 之后新增的 per-message 状态都走 **sidecar registry**，放在 social-core 的 `messagestate/` 包里。

当前已有五个 sidecar：

| Registry | 职责 | 例子 |
|---|---|---|
| [`MessageEditRegistry`](android/social-core/src/main/java/messagestate/MessageEditRegistry.java) | 记录哪些 message 被编辑过 + 当前内容 | `recordEdit(id, newContent)` / `currentContent(id, original)` |
| [`MessageDeletionRegistry`](android/social-core/src/main/java/messagestate/MessageDeletionRegistry.java) | 软删除标记 | `markDeleted(id)` / `isDeleted(id)` |
| [`MessageThreadRegistry`](android/social-core/src/main/java/messagestate/MessageThreadRegistry.java) | Reddit 风格嵌套回帖的父子关系 | `setParent(child, parent)` / `depthOf(id)` / `flatten(list, idOf)` |
| [`MessageReactionRegistry`](android/social-core/src/main/java/messagestate/MessageReactionRegistry.java) | 评论级别的 thumbs-up / thumbs-down，按 (message × user) 记录 | `toggle(messageId, userId, LIKE)` / `likeCount(id)` |
| [`MessageBookmarkRegistry`](android/social-core/src/main/java/messagestate/MessageBookmarkRegistry.java) | 用户收藏的消息 | `toggle(userId, messageId)` / `isBookmarked(userId, messageId)` |

**约定**：

- Registry 是单例（`getInstance()`），key 永远是 `Message.id()`（按 user 维度切分时再加 userId）
- UI 显示消息时调 `MessageEditRegistry.currentContent(id, msg.message())`，不要直接用 `msg.message()`
- 过滤可见消息走 [`Post.getVisibleMessages(isAdmin)`](android/social-core/src/main/java/dao/model/Post.java)，它内部会查 `MessageDeletionRegistry`
- 渲染线程时先用 `MessageThreadRegistry.flatten(timeSorted, Message::id)` 重排成深度优先列表，再交给 `MessageAdapter`；adapter 让 `ThreadIndentView` 按 `depthOf` 占宽度，而连接父子头像的 L 线由 `ThreadConnectorDecoration`（一个 `RecyclerView.ItemDecoration`）在 RecyclerView 画布上跨行绘制 —— View 不能画到自己边界外，所以这部分用 ItemDecoration
- 点赞/收藏按钮的状态在 `MessageAdapter.bindReactions` 里查 `MessageReactionRegistry`、`MessageBookmarkRegistry` 重新绘制 —— 用 `notifyItemChanged(index)` 单独刷新一行，避免触发整个列表重建
- `Message` 模型保持 6 字段 + `hidden`，**不要加新字段**

**新增 per-message 特性**（置顶、私聊、举报详情、…）一律按同款 sidecar 模板：social-core 下加一个新的 `*Registry` singleton，key 是 `Message.id()`（per-user 切分就再加 userId），UI 层在渲染时按需查。Message 模型保持不变。

> 注：post 层级的 emoji 反应仍然走 [`ReactionManager`](android/app/src/main/java/com/example/comp2100miniproject/ReactionManager.java)（key 是 post UUID），跟评论 thumbs-up/down 是两套独立机制——前者是帖子级别的情绪聚合，后者是评论级别的投票。

## Moderation 当前入口

核心审核功能入口：

```java
ModerationTools.addReport(messageId, userId, timestamp);
ModerationTools.removeReport(messageId, userId, timestamp);
ModerationTools.hasReported(messageId, userId);
ModerationTools.setHidden(messageId, adminUserId, hidden);
ModerationTools.getReportedMessages(strategy, amount);
```

Android UI 后续接入建议：

- 普通用户长按消息或点击菜单后调用 `addReport`
- 管理员页面调用 `getReportedMessages("MOST", amount)` 或 `getReportedMessages("OLDEST", amount)`
- 管理员隐藏/取消隐藏消息时调用 `setHidden`
- 普通用户展示消息时调用 `post.getVisibleMessages(false)`
- 管理员展示消息时调用 `post.getVisibleMessages(true)`

## 提交注意事项

不要提交这些文件或目录：

```text
android/.gradle/
android/app/build/
android/social-core/build/
android/build/
android/local.properties
.idea/
*.iml
out/
```

提交前建议检查：

```powershell
git status --short
cd android
.\gradlew.bat build
```

## Login and reactions maintenance

Login credential memory is intentionally split into two behaviors:

- The login screen always remembers the last successful username in `SharedPreferences` and pre-fills it next time.
- Password memory is opt-in only. The `Remember password` checkbox stores or clears the password while keeping the last username.
- Do not store this state in `User` records; it is device-local UI convenience state owned by `LoginActivity`.

Post reactions are per-post, per-user emoji toggles:

- Each emoji can be selected once per user. Tapping the same emoji again removes that user's reaction.
- Selecting one emoji must not clear other selected emojis from the same user.
- The `+` chip expands a quick emoji tray directly on the post detail page; avoid adding an extra submit dialog for quick reactions.
- `ReactionManager` currently lives in the Android app module as historical post-level state. If reactions become persistent or per-message later, move that logic into a social-core sidecar registry instead of adding fields to `Post` or `Message`.

Threaded replies use one composer:

- Message reply buttons should not open a second dialog composer.
- Tapping a message reply button sets the bottom reply field's parent message id and changes the hint to `Replying to <name>...`.
- Tapping outside the bottom reply bar clears the reply target only when the input is empty; typed text keeps the target so accidental taps do not lose context.
- Sending through the bottom reply bar records the parent in `MessageThreadRegistry`, then clears the reply target back to the post-level default.
- Plain post replies keep the normal `Write a reply` hint and use a null parent id.
- Thread indentation is visually capped at three levels with an 18dp step so nested replies keep enough width for content and owner action buttons.
