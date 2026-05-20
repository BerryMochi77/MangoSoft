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
