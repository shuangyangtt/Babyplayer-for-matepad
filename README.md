# 宝宝视频 BabyPlayer

一个给小孩用的极简 Android 视频播放器：**打开就全屏播放，上下滑动换视频，其他操作一律无效。**

小孩拿平板时经常乱抓乱点屏幕，普通播放器很容易被点到暂停、退出、调亮度、弹出广告。这个软件把这些都去掉了，只留下孩子需要的部分。

[English](#english)

## 功能

- 打开后自动全屏、横屏播放存储卡上 `Baby` 文件夹里的视频
- 当前视频**一直重复播放**
- **往上滑**：下一个视频；**往下滑**：上一个视频（到头会循环）
- 点击、长按、多指乱抓都**不起作用**
- 返回键无效，退不出去
- 屏幕常亮，不会自动熄屏
- 画面上没有任何按钮、文字、进度条
- 不联网、没有广告、不需要登录、不收集任何数据
- 安装包只有几十 KB

刻意**没有**的功能：暂停、进度条、调音量、调亮度、显示文件名、设置界面。音量请用平板侧面的实体按键调。

## 使用教程

### 1. 安装

从 [Releases](../../releases) 页面下载最新的 `BabyPlayer.apk`，在平板上打开安装。

如果平板提示"禁止安装未知来源应用"，按提示允许即可。华为/鸿蒙设备如果提示被**纯净模式**拦截，到 **设置 → 系统和更新 → 纯净模式** 里关掉。

系统要求：Android 8.0 及以上（鸿蒙 2/3/4 也可以）。

### 2. 放入视频

在存储卡（或手机/平板内部存储）的根目录建一个名为 **`Baby`** 的文件夹，把视频放进去：

```
存储卡/
└── Baby/
    ├── 01 小星星.mp4
    ├── 02 两只老虎.mp4
    └── 03 Baby Shark.mp4
```

- 支持的格式：mp4、m4v、mkv、webm、3gp、mov、avi、ts、flv（能否播放取决于设备的解码能力，mp4 最稳）
- 播放顺序按**文件名**排序，想调整顺序就在文件名前加 `01`、`02`……
- 存储卡和内部存储都有 `Baby` 文件夹时，优先用存储卡里的
- 加了新视频后，重新打开软件就能看到

### 3. 第一次打开

第一次打开会询问是否允许读取存储，点 **允许**。之后就会直接开始播放。

### 4. 防止孩子退出（强烈建议）

软件能屏蔽返回键，但**从屏幕底部上滑回桌面**这类系统手势，普通软件挡不住。请使用系统自带的**屏幕固定**功能：

1. 打开 **设置**，搜索 **"屏幕固定"**，把它打开
   - 华为/鸿蒙：一般在 **设置 → 安全 → 更多安全设置 → 屏幕固定**
   - 原生 Android：一般在 **设置 → 安全 → 应用固定**
2. 打开"宝宝视频"
3. 进入多任务界面（最近使用的应用），在"宝宝视频"的卡片上点 **图钉** 图标（或点应用图标后选"固定"）
4. 固定后孩子就退不出去了。家长退出时按系统提示的方式解除（通常需要输入锁屏密码）

## 自己编译

本项目**不用 Gradle**，只用 Android SDK 自带的命令行工具，一个脚本就能编译。

### 需要准备

- JDK 17 或更新版本
- Android SDK，包含：
  - `build-tools;35.0.0`
  - `platforms;android-35`
- `zip` 命令

用 Android SDK 命令行工具安装所需组件：

```bash
sdkmanager "build-tools;35.0.0" "platforms;android-35"
```

### 编译

```bash
git clone https://github.com/<你的用户名>/BabyPlayer.git
cd BabyPlayer
./build.sh
```

编译完成后会生成 `BabyPlayer.apk`。

SDK 不在 `~/Android/Sdk` 时，或者想用别的版本，可以用环境变量指定：

```bash
ANDROID_HOME=/path/to/sdk BUILD_TOOLS=34.0.0 PLATFORM=android-34 ./build.sh
```

### 签名

第一次编译时会自动生成签名文件 `release.keystore`（默认密码 `babyplayer`，可以用 `KEYSTORE_PASS` 环境变量修改）。

> ⚠️ **请保管好 `release.keystore`，不要上传到 GitHub。** 以后发布新版本必须用同一个签名文件，否则用户无法覆盖升级。`.gitignore` 已经把它排除了。

### 用数据线安装到设备

设备打开 USB 调试后：

```bash
adb install -r --no-incremental BabyPlayer.apk
# 可选：直接授予读取权限，省掉第一次打开的弹窗
adb shell pm grant com.pepsi.babyplayer android.permission.READ_EXTERNAL_STORAGE
```

## 项目结构

```
BabyPlayer/
├── AndroidManifest.xml          应用清单（权限、横屏、全屏主题）
├── build.sh                     编译脚本
├── res/
│   ├── drawable/ic_launcher.xml 应用图标（矢量图）
│   └── values/strings.xml       应用名称
└── src/com/pepsi/babyplayer/
    └── MainActivity.java        全部代码（约 250 行）
```

想改动的话，常见的修改点都在 `MainActivity.java` 开头：

| 想改什么 | 改哪里 |
|---|---|
| 视频文件夹名称 | `FOLDER = "Baby"` |
| 支持的视频格式 | `EXTENSIONS` |
| 两次切换的最短间隔 | `SWITCH_COOLDOWN_MS` |
| 改成竖屏 | `AndroidManifest.xml` 里的 `screenOrientation` 改成 `sensorPortrait` |

## 许可证

[GPL-3.0](LICENSE)。你可以自由使用、修改和分发，但发布修改后的版本时也必须以 GPL-3.0 开源。

---

## English

**BabyPlayer** is a minimal, distraction-proof Android video player for toddlers.

- Opens straight into full-screen playback of the videos in a `Baby` folder (SD card first, then internal storage)
- The current video loops forever; **swipe up** for the next video, **swipe down** for the previous one
- Every other touch, multi-finger grab and the Back key is ignored; the screen stays on
- No buttons, no overlays, no network, no ads, no account, no tracking

**Usage:** install the APK from [Releases](../../releases), put videos in a folder named `Baby` at the root of the SD card or internal storage, open the app and allow storage access. Use your system's *screen pinning* / *app pinning* feature to stop the child from leaving the app with system gestures.

**Build:** needs JDK 17+ and the Android SDK (`build-tools;35.0.0`, `platforms;android-35`). No Gradle; just run `./build.sh`. A signing key `release.keystore` is created on first build. Keep it private and out of git, since every future release must be signed with it.

License: [GPL-3.0](LICENSE).
