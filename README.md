# DrugEatAPP（安卓吃药记录）

这是一个使用 **Jetpack Compose** 开发的安卓应用，用来记录每日服药情况。

## 已实现功能

1. **按月日历浏览**
   - 可切换上个月 / 下个月，查看不同年月。
2. **简洁添加药物**
   - 可自定义药物名称。
   - 可选择药物颜色（用于日历展示）。
3. **日历彩色点展示**
   - 日历每天格子显示已勾选药物的颜色小点（最多显示 3 个）。
   - 例如：红色=奥沙西泮，蓝色=帕罗西汀，某日会显示红蓝双色点。
4. **每日备注**
   - 支持记录当日服药原因和身体情况。

## 目前不足  
> 不能删除已有药物
> 不能实现滑动屏幕跳动至下一月份
> 添加药物的按钮基本只需要一次，主页不需要长时间显示
> 界面UI过于单一
> 没有提醒的功能，接入APP提醒

## 项目结构

- `app/src/main/java/com/example/drugeatapp/MainActivity.kt`：入口。
- `app/src/main/java/com/example/drugeatapp/DrugTrackerApp.kt`：主要 UI（日历、药物管理、当日记录）。
- `app/src/main/java/com/example/drugeatapp/MedicationViewModel.kt`：状态管理与业务逻辑。
- `app/src/main/java/com/example/drugeatapp/MedicationRepository.kt`：本地持久化（SharedPreferences + JSON）。
- `app/src/main/java/com/example/drugeatapp/Models.kt`：数据模型。

## 说明

- 数据本地存储在 SharedPreferences，卸载 App 后数据会被清空。
- 最低支持 Android 8.0（API 26）。
