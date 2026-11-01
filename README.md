# Hamenda

轻量、快速、完全本地的安卓课程表。

名字来自校园平台 **Ham** 与 **agenda** 的结合。v1 只做课程表，课外时间规划留给后续版本。

## 功能

- 周视图整屏铺开，不用滚动；今天高亮
- 长按空白处新建；长按课程拖动（15 分钟吸附，可跨天）；长按课程弹出「编辑 / 删除这节课 / 删除这门课」；短按查看详情
- 左右滑动切换周；顶栏箭头可在设置中关闭
- 左侧刻度点击切换：整点 → 第几节 → 上下课时间
- 周数按开学日期推算，单双周、1–25 周
- 跟随系统 / 浅色 / 深色主题
- 周起始日（周日/周一）、5 天/7 天显示、每日时间范围可调
- JSON 导出 / 导入备份

## 下载

到 [Releases](https://github.com/hh12356/Hamenda/releases) 下载 APK，支持 Android 8.0（API 26）及以上。

## 构建

需要 JDK 17、Android SDK 35（装 Android Studio 最省事）。

```bash
# local.properties 里写：
# sdk.dir=D:/dev/android-sdk

./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

发布版签名需要 `keystore.properties` 与 `.jks` 文件，两者都不进仓库。

## 项目结构

```
app/src/main/java/io/github/hh12356/hamenda/
├── MainActivity.kt        入口、主题与页面切换
├── data/
│   ├── Models.kt          数据模型、节次表、色板
│   └── Store.kt           单文件 JSON 持久化
├── ui/
│   ├── WeekScreen.kt      周视图、手势、课程菜单
│   ├── EditScreen.kt      新建 / 编辑课程
│   ├── SettingsScreen.kt  设置与备份
│   └── theme/Theme.kt     浅色 / 深色主题
└── util/TimeUtils.kt      日期、周次、时间工具
```

完整设计见 [DESIGN.md](DESIGN.md)。

## 数据与隐私

课程数据保存在应用私有目录的 `hamenda.json`，不联网、不上传。没有通知和后台服务。

## License

[MIT](LICENSE)
