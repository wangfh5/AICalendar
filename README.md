# AI Calendar

AI Calendar是一个基于Android原生开发的智能日历应用，它可以通过自然语言处理(NLP)将用户的文本描述自动转换为标准的日历事件。

## 项目概述

本项目使用Kotlin和Jetpack Compose开发，采用MVVM架构模式，主要特点：

### 技术架构
- Kotlin + Jetpack Compose：现代Android UI开发框架
- Material Design 3：Google最新设计规范
- Kotlin Flow & Coroutines：响应式编程和异步操作
- Room Database：本地数据持久化
- Hilt：依赖注入框架
- Retrofit：网络请求处理

### 项目结构
```
app/
├── data/
│   ├── api/          # API相关
│   ├── db/           # Room数据库
│   └── repository/   # 数据仓库
├── di/               # Hilt依赖注入
├── domain/          # 业务逻辑
├── ui/              # UI层
└── util/            # 工具类
```

### 开发环境
- Android Studio Hedgehog | 2023.1.1
- Kotlin 1.9.0+
- Minimum SDK: 26
- Target SDK: 34
- JDK 17

## 使用说明

### 基本设置
1. 首次使用时，在侧边栏设置API密钥
2. 确保应用有网络访问和日历读写权限

### 创建日历事件
1. 在主界面文本框中输入自然语言描述，例如：
   - "明天下午2点开产品评审会"
   - "下周一上午10点到11点半在3楼会议室开项目进展会"
   - "五一节前一天下午和张总开预算会议"
2. 点击"生成日历"按钮
3. 在弹出的对话框中选择：
   - 添加到系统日历
   - 或保存为.ics文件

### 界面说明
- 主界面：Material Design 3风格
- 输入区：支持多行文本输入
- 侧边栏：包含设置选项
- 状态栏：显示处理进度和结果

## 核心功能

### 自然语言处理
- 支持中英文双语输入
- 智能识别时间表达式
- 自动提取事件要素(标题、地点、参与者等)
- 支持模糊时间表达("下周一"、"五一节前一天"等)
- 智能识别在线/线下会议

### 日历管理
- 生成标准的日历事件
- 支持导出.ics文件
- 支持添加提醒（使用相对时间格式，兼容各种日历应用）
- 完整的时区支持：
  - 自动使用系统时区
  - 正确处理夏令时
  - 兼容主流日历应用（Outlook、Apple Calendar、华为日历、vivo日历等）
  - 支持X-WR-TIMEZONE扩展属性

### 数据处理
- 本地历史记录存储
- 标准日历文件格式
- 系统日历集成
- 安全的API密钥管理

## 许可证
MIT License

