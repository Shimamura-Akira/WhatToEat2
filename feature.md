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
  - **摇一摇抽选 (Shake to Spin)**：借助手机加速度传感器，用户用力摇晃手机即可直接启动转盘，无需手动点击“开始”按钮。
  - **隔空抽选 (Wave to Spin)**：借助手机距离传感器，手在屏幕上方（听筒附近）悬停然后移开挥过即可启动转盘，避免手上有油污时弄脏屏幕。
- **自定义控制**：
  - **地图导航联动 (Map Integration)**：列表每一项带有地图图标按钮，点击即可呼起设备中安装的任一地图应用（如高德地图、百度地图或 Google Maps），并自动以该选项名称作为目的地进行位置检索。
  - **盲选附近美食 (LBS 定位)**：采用 Overpass API 接口与设备定位服务。点击“盲选附近”按钮即可获取用户方圆 2 公里内的真实餐厅、快餐店或咖啡馆，并自动排重后加入转盘备选菜单。
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
- **JSON 序列化/反序列化**：使用 `com.google.code.gson:gson:2.10.1`，把用户自定义对象列表转成 JSON 字符串保存到本地，并自带原生 `org.json` 处理网络回调数据。
- **网络与并发**：通过 `HttpURLConnection` 在 `ExecutorService` （单线程池）下发起并发 RESTful POST 请求，通过回调 `Handler` 更新主线程 UI。
- **应用分层**：分为 UI 层与通过 `DataManager` 封装的简单 Repository/Data 层。

### 工具和动画类
- 动画库: Android 原生 `ValueAnimator`（控制转盘速率）与 `DecelerateInterpolator`。
- Material 过渡动画: `MaterialSharedAxis`（控制页面切换）。
- 触摸反馈与传感器: `HapticFeedbackConstants`（调用手机震动马达），`SensorManager`，`Sensor.TYPE_ACCELEROMETER`（实现摇一摇功能）以及 `Sensor.TYPE_PROXIMITY`（实现隔空抽选功能）。

---

## 📝 更新日志 (Changelog)

### [新功能] 添加网络支持 - 盲选附近真实餐厅
- **AndroidManifest.xml**:
  - 声明了网络访问权限 (`INTERNET`) 及定位权限 (`ACCESS_FINE_LOCATION` 和 `ACCESS_COARSE_LOCATION`)。
- **activity_main.xml**:
  - 在列表管理页的右下侧悬浮按钮布局处，添加了新的“盲选附近”对应的 `ExtendedFloatingActionButton` (ID: `fabNearby`)。
- **MainActivity.java**:
  - 新增 `fetchNearbyRestaurants` 方法。加入动态权限申请拦截。
  - 获取上次已知设备的最后 GPS/网络 位置信息（经纬度）。
  - 利用 `Executors.newSingleThreadExecutor()` 在子线程调度原生的 `HttpURLConnection` 去访问 `Overpass API`，搜索约 2000 米内的 restaurants / fast_food 设施节点。
  - 通过 原生 `JSONObject` 完成 JSON 数据清洗过滤后再回到主 UI `Handler` 去重更新食物列表阵列。

### [新功能] 添加列表到地图的联动跳转机制
- **item_food.xml / FoodAdapter.java**:
  - 为列表中每个单独选项加入了“地图检索” (`ic_menu_mapmode`) 的图标及点击监听接口。
- **MainActivity.java**:
  - 新增 `openMap` 方法，通过拼装 `geo:0,0?q=` 规范的隐式意图 (`Intent.ACTION_VIEW`) 泛型跳转至系统的第三方地图应用定位商铺，增强 O2O 操作体验。

### [新功能] 添加传感器支持 - 隔空抽选（挥手）
- **MainActivity.java**:
  - 引入了 `Sensor.TYPE_PROXIMITY` 距离传感器并初始化。
  - 在 `SensorEventListener` 中监听距离变化，当检测到手先靠近屏幕上方听筒区域，然后离开时（模拟挥手动作），配合原有的 1 秒防抖机制触发 `startRolling()` 开始抽选。
  - 在 `onResume` 中同步注册距离传感器的监听，重用共有的解绑逻辑以统一管理生命周期。
- **feature.md**:
  - 在文档中补充了关于距离传感器和“隔空抽选”的功能描述。

### [新功能] 添加传感器支持 - 摇一摇抽选
- **MainActivity.java**:
  - 引入了 `SensorManager` 和 `Sensor` 组件获取手机的加速度传感器 (`TYPE_ACCELEROMETER`)。
  - 新增 `SensorEventListener` 监听加速度变化。当三轴加速度向量和剔除地球重力后超过设定的阈值（`SHAKE_THRESHOLD = 15.0f`）时，自动触发 `startRolling()`。
  - 添加了1秒的防抖机制，避免一次摇动触发多次抽选。
  - 重写了 `onResume` 和 `onPause` 生命周期方法，确保应用在进入前台时注册传感器，切入后台时注销传感器，节省设备电量。

### [优化] 限制抽取逻辑范围
- **MainActivity.java**:
  - 在 `startRolling()` 方法内追加校验逻辑：仅当底部导航栏 (`bottomNavigation`) 选中 `nav_home` (“吃什么”转盘页) 时，方可执行轮盘抽取操作。
  - 此举避免了用户在“列表”页面录入或管理条目时，因距离传感器挥手防误触或加速度传感器摇晃操作而意外打断列表操作流并后台开启抽取事件的问题。
