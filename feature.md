# WhatToEat (今天吃什么) - 功能与技术栈文档

## 📱 项目简介
这是一个运行在 Android 平台的应用程序，旨在帮助有“选择困难症”的用户决定今天吃什么。通过一个可以交互的自定义幸运转盘，用户可以在他们自己的候选菜单中随机抽取结果。

## ✨ 主要功能 (Features)

### 1. 幸运大转盘随机抽取 (Roulette Wheel)
- **自定义 UI 转盘**：转盘中心有质感高光，扇形按比例均匀分割，选项文字自动适配缩放（过长显示省略号）。转盘颜色采用 Material 3 标准浅色调（Pastel）。
- **动效与交互**：
  - 支持随转动发出的触觉反馈（震动滴答声），模拟真实的物理转盘刻度。
  - 启动后自动计算目标位置，并在减速后停止。
  - **趣味彩蛋（暗箱操作）**：有 20% 的概率触发彩蛋。转盘假装停留在某一项，悬停 0.5 秒后突然“滑”向下一个选项，并弹出“有黑幕！”的趣味对话框。

### 2. 候选菜单管理 (Menu Management)
- **菜单列表视图**：利用 `RecyclerView` 显示所有菜单，支持长列表滚动。
- **自定义控制**：
  - 开关/启用 (Enable/Disable)：可勾选或取消勾选某些菜单，未勾选的项不会出现在转盘中。
  - 添加 (Add)：输入名字添加新选项，并在输入过程中拦截空字符与重名。
  - 编辑 (Edit)：点击已有选项修改名称。
  - 删除 (Delete)：从列表中移除不再想吃的食物选项。
- **本地持久化保存**：对菜单的增删改查操作均会实时保存，下次打开应用依然存在。持久化使用的是 `SharedPreferences` + `Gson` 序列化方案。

### 3. 页面导航与过渡
- 底部导航栏 (Bottom Navigation)：提供在“首页(拉转盘)”和“列表(管理菜单)”之间的无缝切换。
- **Material 切换动画**：点击底部 Tab 时，采用 Material Shared Axis (轴向过渡) 展现顺滑的左右滑动效果，带来流畅的用户体验。

### 4. 沉浸式视觉体验
- 支持 Android 沉浸式边缘到边缘 (Edge-to-Edge) 布局显示。
- 全面适配 Material You 的动态取色 (Dynamic Colors) 以搭配系统主题。

---

## 🛠 技术栈 (Tech Stack)

### 编程语言与核心
- **编程语言**：Java 11 
- **Minimum SDK**：24 (Android 7.0)
- **Target SDK**：36
- **构建系统**：Gradle (AGP 9.1.1)，并采用最新的 Kotlin DSL (`build.gradle.kts`) 和版本目录 (`libs.versions.toml`) 来管理依赖。

### 用户界面 / UI 框架
- **原生 View 系统**：不使用 Compose，全面基于 XML Layouts。
- **ViewBinding**：代替 `findViewById` 绑定页面视图。
- **自定义 View (Custom View)**：转盘组件 (`RouletteView`) 使用 Canvas 绘制几何路径（Arc, Circle, Path），计算三角函数，并结合 `ValueAnimator` 提供动画支撑。
- **AndroidX & Material Design**：
  - `androidx.appcompat:appcompat:1.6.1`
  - `com.google.android.material:material:1.10.0`
  - `androidx.constraintlayout:constraintlayout:2.1.4` 

### 架构与数据存储
- **本地存储持久化**：使用系统的 `SharedPreferences` 进行键值对轻量级存储。
- **JSON 序列化/反序列化**：使用 `com.google.code.gson:gson:2.10.1`，把用户自定义对象列表转成 JSON 字符串保存到本地。
- **应用分层**：分为 UI 层与通过 `DataManager` 封装的简单 Repository/Data 层。

### 工具和动画类
- 动画库: Android 原生 `ValueAnimator`（控制转盘速率）与 `DecelerateInterpolator`。
- Material 过渡动画: `MaterialSharedAxis`（控制页面切换）。
- 触摸反馈: `HapticFeedbackConstants`（调用手机震动马达）。