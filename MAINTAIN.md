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
      MainActivity.java
      PostViewerActivity.java
      MessageFragment.java
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
