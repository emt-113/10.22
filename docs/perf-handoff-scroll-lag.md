# 交接文档：首页纵向滑动卡顿（PhonePulse / Redmi K70 Pro）

> 交接时间：2026-10-21 14:30（距数媒竞赛截止 2026-10-23 23:59 还有 2 天）
> 工程目录：`D:\Dsektop\PhonePulse`
> 交接人：WorkBuddy。下面是**已核实的事实**，不是猜测，请勿推翻已验证结论。

---

## 1. 问题描述

**用户主诉**：App "感觉卡卡的"，具体是**上下滑动卡**（纵向滚动）。

**复现环境**：真机 Redmi K70 Pro（23117RK66C / manet，Android 16 HyperOS，屏幕 override 1080×2400，物理 1440×3200）。

**当前主观状态**：优化后用户仍反馈"就是上下滑动卡"，**未解决**，需要继续排查。

---

## 2. 已完成并验证的修复（不要重复做）

### 2.1 星球主视觉渲染卡顿（已根治，勿回退）
- **原症状**：首页 hero 星球每帧重绘约 20 层渐变（径向/线性渐变 + clipPath + 数十个小圆），任何重组都触发整颗星球重画。
- **最终方案**（当前代码状态，勿改回 `drawWithCache`）：
  - `app/src/main/java/com/aizeek/phonepulse/ui/components/PlanetHomeHub.kt`
  - 绘制拆成两层：`drawPlanetBackdrop(stage, screenOn)`（静态）/ `drawPlanetProgress(progress, over)`（动态进度弧 + 珍珠珠点）。
  - 静态层用 `remember(canvasSize, stage, screenOn) { buildPlanetLayer(...) }` 缓存成 `ImageBitmap`，每帧只 `drawImage(layer)` + 画进度弧。
  - 验证：logcat 计数显示 **启动构建 1 次、滑动期间 0 次**。

- **关键教训（写在这里防止重犯）**：
  1. `Modifier.drawWithCache` **不等于缓存**——它的 cache block 每次重组都因 lambda 实例变化而失效重建，实测导致重组阶段涨到 75–124ms。必须用 `remember` 按 key 缓存。
  2. `CanvasDrawScope.draw(density, layoutDirection, canvas, size) { ... }`：第一个参数要传 `this`（CacheDrawScope 实现了 Density），因为 `Density.density` 是 Float 缩放比而非 Density 对象；绘制要写在 trailing lambda 里。

### 2.2 星球详情浮层悬在别的页面上（已修）
- **根因**：`showPlanetDetail` 是独立 state，详情页画在 Scaffold 内容层之上，切 tab 不会清掉它。
- **修法**（`ui/MainScreen.kt`）：底部导航 `onClick` 里先 `showPlanetDetail = false` 再切 tab；`ui/screens/PlanetDetailScreen.kt` 加 `BackHandler { onClose() }`（系统返回键现在能关详情页，之前会直接退出 App）。
- 验证：真机实拍 `final-tab_battery.png`（开星球 → 点电量统计 → 正常显示电量统计页）。

### 2.3 星球详情"最近 7 天"日期全显示今天（已修）
- **根因**：`records.indexOf(record)` 在多条 null 记录时全部返回第一个 null 的下标。
- **修法**：改用 `records.forEachIndexed { index, record -> ... shiftKey(today, -index) }`。
- 验证：真机/模拟器实拍显示 10-21 → 10-15 正确。

---

## 3. 客观性能数据（真机，Android 16）

### 3.1 测量方法（重要，方法错了会得出错误结论）
```powershell
$adb = "D:\Dsektop\PhonePulse\.workbuddy\sdk\platform-tools\adb.exe"
& $adb shell "am start -n com.aizeek.phonepulse/.MainActivity"   # 只用 am start 拉起，不要模拟解锁/输入
& $adb shell "dumpsys window displays | grep -i mCurrentFocus"     # 确认前台是我们的 App
# 先滚回顶部
for ($i=0;$i -lt 3;$i++){ & $adb shell "input swipe 540 700 540 1900 200" }
& $adb shell "dumpsys gfxinfo com.aizeek.phonepulse reset"          # 必须 reset 后再测
# 连续滑动
for ($i=0;$i -lt 8;$i++){
  & $adb shell "input swipe 540 1700 540 700 280"
  & $adb shell "input swipe 540 700 540 1700 280"
}
& $adb shell "dumpsys gfxinfo com.aizeek.phonepulse"               # 摘要：Total frames / Janky frames / percentile
& $adb shell "dumpsys gfxinfo com.aizeek.phonepulse framestats" > "$out\_fs.txt"  # 逐帧阶段耗时
```
**测量纪律**：
- 必须先 `reset` 再测，否则统计窗口会混入启动首帧（曾因此误判 18.6% 卡顿）。
- 必须先滚回顶部再测，否则测的是列表中段/另一个状态。
- `adb` 走 PowerShell 通道（bash 里跑 adb shell 会被沙箱拦截）。
- PowerShell `>` 写出的文件是 **UTF-16**，Python 解析要 `encoding='utf-16'`。
- Android 16 的 framestats 是 **24 列新格式**（见下表），字段名与旧版不同，别用旧索引。

### 3.2 Android 16 framestats 列序（PROFILEDATA 段）
```
0 Flags
1 FrameTimelineVsyncId
2 IntendedVsync        ← 总帧耗时 = FrameCompleted(17) - IntendedVsync(2)
3 Vsync
4 InputEventId
5 HandleInputStart
6 AnimationStart       ← process 起点
7 PerformTraversalsStart
8 DrawStart
9 FrameDeadline
10 FrameStartTime
11 FrameInterval
12 WorkloadTarget
13 SyncQueued
14 SyncStart
15 IssueDrawCommandsStart
16 SwapBuffers
17 FrameCompleted      ← 总帧耗时终点
18 DequeueBufferDuration
19 QueueBufferDuration
20 GpuCompleted
21 SwapBuffersCompleted
22 DisplayPresentTime
23 CommandSubmissionCompleted
```
阶段定义（Android 官方口径）：
- prepare = p[6] − p[5]（输入处理）
- **process = p[7] − p[6]（= Compose 重组 + 动画回调，主要卡顿来源看这里）**
- execute = p[8] − p[7]（measure/layout）
- draw = p[17] − p[8]（绘制 + GPU 等待）

### 3.3 实测结果
| 场景 | 帧数 | Janky | process p50/p90/p99 | draw p50 |
|---|---|---|---|---|
| 修复前（星球每帧重绘） | 120 | 18.6% | — / 26.6 / 35.6（max 75.8） | 10.4ms |
| 修复后（位图缓存 + remember） | 303 | **0.99%** | 12.4 / 20.5 / 36.1 | 6.3ms |

冷启动：`am start -W` → **TotalTime 664ms**（启动不是瓶颈）。

**结论**：官方 Janky 指标已到 0.99%（几乎无超时帧），但**用户主观仍觉得卡**。这是本任务的核心矛盾点，也是下一位接手者要解决的重点。

---

## 4. 尚未定位的疑点（按可能性排序）

### 疑点 A：整个干预区被塞进**一个** LazyColumn item（最可疑）
`ui/screens/OverviewScreen.kt`：
```kotlin
LazyColumn(...) {
    item { planetHeader() }          // ← 整个 hero 区（星球+预算+提示卡+今日卡+健康卡+欢迎卡）是一个巨型 item
    if (companion != null) item { CompanionHomeEntry(...) }
    item { BentoStatCards(...) }     // 4 个指标卡
    item { ActivityBarChart(hourlyStats = hourlyStats) }  // 24 根柱的 Canvas 图表
    item { /* 最近状态日志 标题 */ }
    items(displaySessions.size, key = { displaySessions[it].id }) { ... }
}
```
问题点：
1. `planetHeader` 是 MainScreen 传进来的 **lambda**，每次 MainScreen 重组都是新实例 → 该 item 内容不稳定，**整个巨型 item 会被整体重组/重新测量**。
2. 静态 item **没有 key** → LazyColumn 按下标复用槽位。
3. 巨型 item 的高度很大，滚动时它的测量/重组成本被放大。

**建议动作**：把 hero 区**拆成多个独立 item**（星球/预算行/提示卡/今日卡/健康卡/欢迎卡各一个），给每个 item 加稳定 `key`，并把 `planetHeader` 的内容改为**直接参数**（如 `planetHeaderContent: @Composable () -> Unit` 换成在 OverviewScreen 内部直接调用 PlanetHomeSection），消除跨文件 lambda 实例不稳定。

### 疑点 B：每秒刷新的计时器在列表项内部
`ui/components/PlanetHomeHub.kt` 的 `LiveSessionTimer`：
```kotlin
val elapsedFlow = remember(startTimeMs) { flow { while (true) { emit(...); delay(1000) } } }
val elapsedMs by elapsedFlow.collectAsStateWithLifecycle(...)
```
它在 LazyColumn item 内部，每秒重组一次该子树。若它在巨型 hero item 里 → **每秒触发一次巨型 item 的重组**。拆成独立 item 后此项也会被隔离。
另外检查 `TimeFormatter.formatDigitalClock` 输出长度是否随时间变化（若长度变化 → 文本宽度变化 → 行宽变化 → 列表内容高度变化 → **滚动位置跳动**，主观上极像"卡"）。

### 疑点 C：每张卡的 Compose 模糊阴影（GPU 离屏层）
`PlanetHomeHub.kt:1180` 的 `bevelCard` 使用 `.shadow(1.5.dp, shape)`，Android 12+ 会为每张卡生成一个离屏渲染层 + 模糊阴影。首页同时可见约 6–8 张卡 → 每帧多个模糊通道。
**建议动作**：A/B 测试——把阴影换成 1dp 描边 + 更浅的背景，看主观是否改善；有 `Modifier.border` 兜底，视觉损失很小。

### 疑点 D：ActivityBarChart 每帧 Canvas 绘制
24 根柱子如果每根都用渐变/圆角/描边，在滚动可见时是持续的 GPU 负担。检查是否用了 `remember` 缓存成 ImageBitmap（可参考星球的做法）。

### 疑点 E：帧率模型与主观感受不一致
设备 `FrameInterval` = 17ms（59Hz），而官方 Janky 指标可能按更高的刷新率模型判定。`Number High input latency` 曾达 1153（`adb input swipe` 会灌入大量 motion event，该指标被合成输入放大，**不代表真人滑动**）。
**建议动作**：用 `adb shell screenrecord` 录 10 秒真人速度拖动（或让用户录），用 `ffmpeg`/播放器逐帧看是否有可见的顿挫；比合成 swipe 更接近主观体验。

---

## 5. 环境与工具（务必遵守，否则会踩坑）

| 项 | 值 / 规则 |
|---|---|
| 编译 | `JAVA_HOME="D:\Program Files\Android\Android Studio\jbr"`；`GRADLE_USER_HOME="D:\Dsektop\PhonePulse\.workbuddy\gradle-home"` |
| 构建 | `.\gradlew.bat :app:assembleDebug --console=plain -q`（约 80–110s） |
| 测试 | `.\gradlew.bat :app:testDebugUnitTest`（约 95s，当前 **255 例 0 失败**）；构建前把 `app/build/reports/tests/testDebugUnitTest` 改名绕过目录锁 |
| 真机 adb | `D:\Dsektop\PhonePulse\.workbuddy\sdk\platform-tools\adb.exe`，**只能走 PowerShell 通道** |
| 真机纪律 | **只用 `am start` 拉起 App；绝不模拟解锁/输入**（曾把锁屏密码打进用户微信悬浮窗输入框，已清理并记录进记忆文件） |
| 模拟器 | AVD `pulse`（API 35 x86_64，**无 GPU 加速，吃满 CPU**）。**禁止与 Gradle 编译并行**（曾导致整机假死关机）。验证脚本 `.workbuddy/verify_planet.ps1` / `verify_perf.ps1` 首尾自动 `adb emu kill` |
| 脚本编码 | **所有 .ps1 只写 ASCII**（Windows PowerShell 5.1 按 GBK 读 .ps1，中文会破坏引号导致 ParserError）；中文说明写进 md |
| 已有脚本 | `.workbuddy/verify_final.ps1`（装机+截图+切 tab 流程）、`.workbuddy/measure_final.ps1`（干净帧率测量）、`.workbuddy/profile_frames.ps1`（framestats 抓取）、`.workbuddy/compare_tabs.ps1`（多 tab 对照） |
| 截图取证 | `adb shell screencap -p /sdcard/x.png` + `adb pull`（PowerShell `>` 会破坏二进制） |

---

## 6. 建议的下一步（按性价比）

1. **先做疑点 A 的拆分**（结构改动，收益最大，风险可控）：把 hero 巨型 item 拆成多个带 key 的 item → 顺带隔离每���刷新的计时器。改完立刻用 `measure_final.ps1` 复测 + 让用户主观确认。
2. **再 A/B 疑点 C**（去阴影）：一版构建就能测出差异，视觉代价小。
3. **用 screenrecord 做客观主观对齐**：录 10 秒滑动，逐帧看顿挫点，判断是"掉帧"还是"滚动位置跳动/内容高度变化"（后者指向疑点 B 的文本宽度问题）。
4. 复测口径固定为：`am start` 拉起 → 滚回顶部 → `gfxinfo reset` → 8 次滑动 → 读 Janky + framestats 的 process/draw 分位。

---

## 7. 当前工程状态（接手基线）

- 编译：✅ 通过（`assembleDebug`）
- 单测：✅ 255 例 0 失败
- 真机：已装最新 debug 包（`app/build/outputs/apk\debug\app-debug.apk`），首页/应用活跃/电量统计/状态明细/系统设置均已实拍复验，浅色珍珠主题在 HyperOS 上成立
- **代码已提交（安全）**：commit `847f404` "feat: add digital health intervention hub with pearl planet hero"，包含 intervention/ 新模块、珍珠浅色主题、星球 hero + 详情页、教练卡、位图缓存优化、浮层与日期标签修复。基线是上游 `1629ece`。
  → **接手者请勿 `git checkout`/`git reset --hard`**，如需试验请先 `git branch backup/pre-perf-fix`。
- 未纳入版本管理：`.workbuddy/`（工程记忆、验证脚本、截图取证）被 git 忽略，但**排查现场全在里面**，别删。
- 相关工程记忆：`.workbuddy/memory/2026-10-21.md`（当日完整排查日志）、`.workbuddy/memory/MEMORY.md`（长期纪律）
- 踩坑手册：`.workbuddy/踩坑手册.md` #7（DrawScope API）#8（模拟器与 Gradle 并行）#9（ps1 必须 ASCII）
