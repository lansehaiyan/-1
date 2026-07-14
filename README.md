# 手机时间管家 (Phone Time Manager)

孩子玩手机的时间管理工具。家长设定每日使用时长，时间到了自动锁定手机。

## 功能

- 儿童计时界面（大倒计时 + 进度条）
- 后台服务跟踪使用时长，App 关掉也继续计
- 时间到自动全屏锁屏（无法返回/跳过）
- 家长 4-6 位数字密码解锁
- 额外加时（+15/+30/+60 分钟）
- 每日自动重置
- 开机自启
- 设备管理员防卸载

## 编译 APK

### 方法一：GitHub Actions（推荐，免费，无需本地装任何东西）

1. 在 GitHub 上新建一个仓库
2. 把项目文件 push 上去：
   ```bash
   git init
   git add .
   git commit -m "init"
   git remote add origin https://github.com/你的用户名/手机时间管家.git
   git push -u origin main
   ```
3. 打开 GitHub 仓库 → **Actions** 标签 → 自动开始编译
4. 编译完成后，在 Action 运行结果里下载 **PhoneTimeManager-APK** 压缩包

### 方法二：Android Studio

1. 下载 [Android Studio](https://developer.android.com/studio)
2. 打开项目：`File → Open → 选择本项目目录`
3. 菜单：`Build → Build Bundle(s) / APK(s) → Build APK(s)`
4. APK 生成在 `app/build/outputs/apk/debug/`

## 首次使用

1. 安装 APK 到手机
2. 打开 App → 设置家长密码（4-6位数字）
3. 设置每日使用时长（30分钟 ~ 3小时）
4. 按提示激活设备管理员权限（防卸载）
5. 允许通知权限和忽略电池优化
