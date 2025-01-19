# AI Calendar

AI Calendar是一个基于Android原生开发的智能日历应用,它可以通过自然语言处理(NLP)将用户的文本描述自动转换为标准的日历事件。

## 一般性介绍

### 1. 用户界面
- 主界面采用Material Design 3设计风格
- 中心区域提供简洁的文本输入框,支持多行输入
- 底部配备"生成日历"按钮
- 右上角提供设置入口
- 侧边栏包含API设置和其他配置选项
- 支持浅色/深色主题自动切换

### 2. 工作流程
1. 首次使用时,用户需要在侧边栏设置DeepSeek API密钥
2. 在主界面文本框中输入自然语言描述的事件
3. 点击"生成日历"按钮,应用会:
   - 显示进度指示器
   - 调用DeepSeek API进行文本解析
   - 生成标准日历文件
   - 弹出成功提示和保存位置
4. 用户可以选择直接导入到系统日历或保存.ics文件

### 3. 数据流转
- 本地存储: 使用Room数据库保存历史记录
- 网络请求: 通过Retrofit调用DeepSeek API
- 文件处理: 生成标准格式的.ics日历文件
- 系统集成: 支持与Android系统日历应用交互

## 项目概述

本项目是[python-kivy版本](link-to-original-project)的Android原生重构版本,使用Kotlin语言和Jetpack Compose开发,采用MVVM架构模式。

### 技术栈
- Kotlin + Jetpack Compose
- Material Design 3
- Kotlin Flow & Coroutines
- Room Database
- Hilt依赖注入
- DeepSeek API集成

## 核心功能

### 1. 自然语言事件创建
- 支持中英文双语输入
- 智能识别时间表达式
- 自动提取事件要素(标题、地点、参与者等)
- 支持模糊时间表达("下周一"、"五一节前一天"等)

### 2. 日历管理
- 生成标准的日历事件
- 支持导出.ics文件
- 支持添加提醒
- 支持在线/线下会议自动识别

### 3. 智能分析
- 智能提取会议参与者信息
- 自动识别地点信息
- 智能设置提醒时间
- 自动处理时区转换

## DeepSeek API使用说明

### API配置
1. 在项目根目录下创建`local.properties`文件(已被.gitignore忽略)
2. 添加API密钥配置:
```properties
DEEPSEEK_API_KEY=your_api_key_here
```

### API调用示例
```kotlin
// 示例代码将在实现时提供
```

## 项目结构
```
app/
├── data/
│   ├── api/          # DeepSeek API相关
│   ├── db/           # Room数据库
│   └── repository/   # 数据仓库
├── di/               # Hilt依赖注入
├── domain/           # 业务逻辑
├── ui/               # UI层
└── util/             # 工具类
```

## 开发环境要求
- Android Studio Hedgehog | 2023.1.1
- Kotlin 1.9.0+
- Minimum SDK: 24
- Target SDK: 34
- JDK 11

## 构建与运行
1. 克隆项目
2. 在local.properties中配置DeepSeek API密钥
3. 使用Android Studio打开项目
4. Sync Project with Gradle Files
5. 运行应用

## 使用示例

### 基础用法
输入文本示例:
- "明天下午2点开产品评审会"
- "下周一上午10点到11点半在3楼会议室开项目进展会"
- "五一节前一天下午和张总开预算会议"

### 高级功能
- 在线会议自动识别
- 智能提醒时间设置
- 多人会议参与者提取
- 节假日智能识别

## 待办功能
- [ ] 语音输入支持
- [ ] 日历小部件
- [ ] 智能日程推荐
- [ ] 多设备同步

## 注意事项
1. API密钥安全性
2. 网络请求权限
3. 日历读写权限
4. 数据备份

## 贡献指南
欢迎提交Issue和Pull Request

## 许可证
MIT License

## 联系方式
[Your Contact Information]

