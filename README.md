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

- JDK 11 或更高版本
- Android Studio Hedgehog 或更新版本
- Xcode 14+ (仅 iOS 开发)

### 构建项目

```bash
# Android
./gradlew :composeApp:assembleDebug

# iOS
./gradlew :composeApp:iosSimulatorArm64Build
```

## 📱 功能说明

### NFC 打卡

**Android 写入记录:**
- URI: `piggydance://open?q=1`
- External: `io.piggydance.checkinn`

External 的作用是直接打开 APP，无需展示系统提示框。
URI 的作用是携带打开参数，包括打卡类别、地点等信息。

1. **写入 NFC 标签**
   - 点击「写入上班卡」或「写入下班卡」
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
