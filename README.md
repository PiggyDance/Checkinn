# Checkinn - NFC 智能打卡应用

一款使用 Kotlin Multiplatform 构建的现代化打卡应用，支持 NFC 标签打卡和手动打卡。

## ✨ 特性

- 📱 **Kotlin Multiplatform** - 支持 Android 和 iOS
- 🎨 **现代 UI** - 使用 Compose Multiplatform 构建的精美界面
- 🏷️ **NFC 支持** - 支持读写 NFC 标签进行打卡
- 📊 **数据统计** - 周视图和月视图查看打卡记录
- 🌍 **多语言支持** - 支持 10 种语言
- 🎯 **工作目标** - 每日 10 小时工作目标追踪

## 🌐 支持的语言

- 简体中文 (zh-CN)
- 繁体中文 (zh-TW)
- 日语 (ja)
- 韩语 (ko)
- 泰语 (th)
- 越南语 (vi)
- 英语 (en)
- 法语 (fr)
- 西班牙语 (es)
- 俄语 (ru)

应用会自动根据系统语言设置显示对应的语言界面。

## 🏗️ 技术栈

- **Kotlin Multiplatform** - 跨平台代码共享
- **Compose Multiplatform** - 声明式 UI 框架
- **Material 3** - Material Design 3 组件
- **Lottie (Compottie)** - 动画效果
- **Haze** - 高斯模糊效果
- **Android NFC** - NFC 标签读写

## 📦 项目结构

```
Checkinn/
├── composeApp/
│   ├── src/
│   │   ├── androidMain/        # Android 平台特定代码
│   │   │   ├── kotlin/
│   │   │   └── res/
│   │   │       ├── values/           # 简体中文资源
│   │   │       ├── values-zh-rTW/    # 繁体中文资源
│   │   │       ├── values-ja/        # 日语资源
│   │   │       ├── values-ko/        # 韩语资源
│   │   │       ├── values-th/        # 泰语资源
│   │   │       ├── values-vi/        # 越南语资源
│   │   │       ├── values-en/        # 英语资源
│   │   │       ├── values-fr/        # 法语资源
│   │   │       ├── values-es/        # 西班牙语资源
│   │   │       └── values-ru/        # 俄语资源
│   │   ├── commonMain/         # 共享代码
│   │   │   └── kotlin/
│   │   └── iosMain/            # iOS 平台特定代码
│   │       └── kotlin/
│   └── build.gradle.kts
├── iosApp/                     # iOS 应用
└── gradle/
```

## 🚀 快速开始

### 前置要求

- JDK 17 或更高版本
- Android Studio Hedgehog 或更新版本
- Xcode 14+ (仅 iOS 开发)

### 构建项目

```bash
# Android
./gradlew :composeApp:assembleDebug

# iOS
./gradlew :composeApp:iosSimulatorArm64Build
```

Android Debug 使用 `io.piggydance.checkinn.debug`，名称为「Checkinn（测试版）」，可与正式版并存。测试版新写入的 NFC 标签关联测试包名；正式版标签关联 `io.piggydance.checkinn`。测试冷启动时请使用专用标签，正式版已有标签可在测试版前台读取。

## 📱 功能说明

### 页面与设置

主页聚焦工作状态、上班／下班和当日工作时段，底部「记录」保留周／月历史。右上角齿轮进入独立设置页，每日目标、工作日和 NFC 写卡集中在该页；NFC 的使用说明通过问号浮层按需查看。点击「保存」应用工作设置；返回时若有未保存的更改，可选择保存或放弃。原有配色、打卡动画和数据存储方式保留。

### 发行优化

release 启用 R8 优化、混淆、代码缩减及资源缩减，使用 `proguard-android-optimize.txt` 与依赖自带的 consumer rules。没有用全库 keep 或全局禁用选项抵消优化；debug 保留正常调试构建。AGP 8.11.2 与 Gradle 8.14.3 用于兼容项目的 Kotlin 2.2，wrapper 保留官方分发校验值。

2026-09-30 的发行 AAB 中 DEX 合计从 47,542,416 bytes 降到 2,368,692 bytes，减少 95.02%。Google 从 2027 年 2 月开始对 DEX 超过 10 MB 的 App 设置优化、混淆和缩减门槛；本地 DEX 减幅不等同于三项 Play Console 指标，上传后仍以 Console 的分析为准。要求见 [Google Play 技术质量要求](https://support.google.com/googleplay/android-developer/answer/17492799)。`mapping.txt` 等优化输出保留在构建目录供发行归档和崩溃还原，不提交到源码。

已在本任务 API 33 / arm64 隔离模拟器验证真实 release AAB 经 bundletool 生成的临时 debug 签名 APK：启动、设置、NFC 引导与写卡等待/取消、打卡动画、周历史以及设置保存后重启读取均正常。临时签名仅用于该模拟器验收，发行 AAB 保持未签名；真实 NFC 标签和 Android 16 行为仍需对应设备验收。

### NFC 打卡

**Android 写入记录:**
- URI: `piggydance://checkinn?s=clock_in`（或 `clock_out`）
- AAR: `io.piggydance.checkinn`

标签包含场景 URI（`piggydance://checkinn?s=clock_in` 或 `clock_out`）和 Android Application Record（AAR，`io.piggydance.checkinn`），用于关联应用和打卡类别。
Android 16 起，用户可以在系统的「通过 NFC 启动」设置中禁止标签启动本应用。应用检测到该设置关闭时会提供设置入口；手动打卡仍可使用。

1. **写入 NFC 标签**
   - 在设置页点击「写入上班卡」或「写入下班卡」
   - 将空白 NFC 标签贴近手机背面
   - 写入成功后，该标签即可用于打卡

2. **NFC 打卡**
   - 将已写入的 NFC 标签贴近手机
   - 自动识别并完成打卡
   - 显示打卡成功动画

### 手动打卡

- 点击「上班打卡」按钮开始计时
- 点击「下班打卡」按钮结束计时
- 支持同一天多次打卡
- 支持跨午夜下班；整段工时保留在上班日期，不拆分或丢失分钟。重新打开应用也可恢复此前未结束的工作段。

### 数据统计

- **今日视图** - 实时显示当前工作状态和累计时长
- **周视图** - 查看本周每日打卡记录和统计
- **月视图** - 以日历形式展示整月打卡情况

## 🌍 国际化开发

### 添加新语言

1. 在 `composeApp/src/androidMain/res/` 下创建对应的 values 目录:
   ```
   values-<language-code>/strings.xml
   ```

2. 复制 `values/strings.xml` 的内容并翻译

3. 对于 iOS，当前使用默认英语，可通过扩展 `IosStringResources` 类实现完整的多语言支持

### 使用字符串资源

在 Composable 函数中:
```kotlin
val strings = remember { getStringResources() }
Text(text = strings.clockIn())
```

## 📄 开源协议

本项目采用 MIT 协议开源。
