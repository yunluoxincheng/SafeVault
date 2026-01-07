# material-design-3 Specification

## Purpose
定义 SafeVault 应用采用 Material Design 3 设计规范的视觉风格、主题系统和组件样式，提供现代化的用户体验。

## ADDED Requirements

### Requirement: Material Design 3 主题配置
应用 MUST 采用 Material Design 3 (Material You) 设计规范作为基础主题。

#### Scenario: 主题基础样式
**Given** 应用启动
**When** 主题加载
**Then** 主题 MUST 继承自 Theme.Material3.DayNight.NoActionBar
**And** 应用 MUST 支持亮色和深色模式
**And** 系统 SHALL 根据系统设置自动切换主题

#### Scenario: 动态配色支持（Android 12+）
**Given** 用户使用 Android 12 或更高版本
**When** 应用启动
**Then** 系统 SHALL 尝试使用系统动态配色（Material You）
**And** 主色调 SHALL 从用户的壁纸提取
**And** 如果动态配色不可用，系统 SHALL 回退到固定配色方案

#### Scenario: 固定配色方案（Android 10-11）
**Given** 用户使用 Android 10 或 11
**When** 应用启动
**Then** 系统 SHALL 使用预定义的固定配色方案
**And** 主色（Primary）SHALL 为 #6750A4（紫色）
**And** 主色容器 SHALL 为 #EADDFF（浅紫色）
**And** 次色（Secondary）SHALL 为 #625B71（灰紫色）
**And** 表面色 SHALL 为 #FEF7FF（几乎白色）

### Requirement: 颜色系统定义
应用 MUST 使用 Material Design 3 的完整颜色系统。

#### Scenario: 颜色角色定义
**Given** 开发者定义应用颜色
**When** 在 colors.xml 中声明
**Then** 系统 MUST 定义以下颜色角色：
  - Primary / OnPrimary / PrimaryContainer
  - Secondary / OnSecondary / SecondaryContainer
  - Tertiary / OnTertiary / TertiaryContainer
  - Error / OnError / ErrorContainer
  - Background / OnBackground
  - Surface / OnSurface / SurfaceVariant
**And** 每个颜色角色 MUST 为亮色和深色模式分别定义值

#### Scenario: 语义化颜色使用
**Given** UI 组件需要使用颜色
**When** 应用颜色样式
**Then** 主要操作按钮 SHALL 使用 Primary 颜色
**And** 次要操作 SHALL 使用 Secondary 颜色
**And** 错误状态 SHALL 使用 Error 颜色
**And** 卡片和对话框 SHALL 使用 Surface 颜色

### Requirement: 圆角和形状规范
应用 MUST 使用 Material Design 3 推荐的圆角半径。

#### Scenario: 组件圆角规范
**Given** 开发者设计 UI 组件
**When** 应用圆角样式
**Then** 小型组件（按钮、输入框、芯片）SHALL 使用 8dp 圆角
**And** 中型组件（卡片、对话框）SHALL 使用 12dp 圆角
**And** 大型组件（底部表单、全屏对话框）SHALL 使用 16dp 圆角
**And** 特大型组件（全屏布局）SHALL 使用 24dp 圆角

#### Scenario: 密码列表卡片圆角
**Given** 用户查看密码列表
**When** 列表项卡片显示
**Then** 卡片圆角 SHALL 为 12dp
**And** 卡片 SHALL 使用 Material 3 卡片样式（Elevation 2dp）

### Requirement: 字体排版系统
应用 MUST 使用 Material Design 3 的字体排版系统。

#### Scenario: 字体样式定义
**Given** 应用显示文本
**When** 应用字体样式
**Then** 标题（Headline）SHALL 使用 Material 3 Headline 样式
**And** 正文（Body）SHALL 使用 Material 3 Body 样式
**And** 标签（Label）SHALL 使用 Material 3 Label 样式
**And** 所有文本 MUST 支持字体缩放（无障碍功能）

#### Scenario: 密码预览字体
**Given** 密码文本显示
**When** 显示生成的密码或密码字段
**Then** 密码文本 SHALL 使用等宽字体（monospace）
**And** 字体大小 SHALL 为 18sp 或更大
**And** 字体 SHALL 支持区分易混淆字符（如 0 和 O）

### Requirement: 图标系统
应用 MUST 使用 Material Design 3 的填充式图标。

#### Scenario: 图标样式选择
**Given** 应用显示图标
**When** 选择图标资源
**Then** 主要操作图标 MUST 使用填充式图标（Filled Icons）
**And** 未选中状态可使用轮廓图标（Outlined Icons）
**And** 所有图标 SHALL 遵循 Material Icons 设计规范

#### Scenario: 图标尺寸规范
**Given** 图标显示在界面中
**When** 测量图标尺寸
**Then** 导航栏图标 SHALL 为 24dp x 24dp
**And** 列表项图标 SHALL 为 24dp x 24dp
**And** 按钮图标 SHALL 根据按钮大小调整（通常 20-24dp）
**And** FAB 图标 SHALL 为 24dp x 24dp

### Requirement: 组件间距规范
应用 MUST 使用统一的间距系统以保持视觉一致性。

#### Scenario: 间距基础单位
**Given** 设计界面布局
**When** 定义组件间距
**Then** 基础间距单位 SHALL 为 8dp
**And** 所有间距 SHALL 为 8dp 的倍数（8dp, 16dp, 24dp, 32dp）

#### Scenario: 密码列表间距
**Given** 用户查看密码列表
**When** 列表项显示
**Then** 列表项之间 SHALL 有 8dp 的垂直间距
**And** 列表项内边距 SHALL 为 16dp
**And** 列表左右边距 SHALL 为 16dp

### Requirement: 阴影和高度系统
应用 MUST 使用 Material Design 3 的阴影和高度系统。

#### Scenario: 组件高度定义
**Given** 组件显示在界面中
**When** 应用阴影效果
**Then** 底部导航栏 SHALL 使用 elevation 3dp
**And** 工具栏 SHALL 使用 elevation 2dp
**And** 列表卡片 SHALL 使用 elevation 1dp（默认）或 2dp（悬浮）
**And** 对话框 SHALL 使用 elevation 6dp
**And** FAB SHALL 使用 elevation 6dp

### Requirement: 按钮样式规范
应用 MUST 使用 Material Design 3 的按钮样式。

#### Scenario: 按钮类型使用
**Given** 界面需要显示按钮
**When** 选择按钮样式
**Then** 主要操作按钮 MUST 使用填充按钮（Filled Button）
**And** 次要操作按钮 SHALL 使用文本按钮（Text Button）
**And** 确认对话框按钮 SHALL 使用填充按钮（主操作）和文本按钮（取消）
**And** 密码可见性切换 SHALL 使用图标按钮（Icon Button）

#### Scenario: 按钮圆角和高度
**Given** 按钮显示在界面中
**When** 测量按钮尺寸
**Then** 填充按钮高度 SHALL 为 40dp
**And** 文本按钮高度 SHALL 为 40dp
**And** 按钮圆角 SHALL 为 20dp（全高度圆角）

### Requirement: 输入框样式规范
应用 MUST 使用 Material Design 3 的文本输入框样式。

#### Scenario: 输入框布局
**Given** 用户输入信息
**When** 显示输入框
**Then** 输入框 MUST 使用 TextInputLayout 包裹 TextInputEditText
**And** 输入框 SHALL 支持显示标签（Hint）和辅助文本
**And** 错误状态 SHALL 显示错误文本和红色边框
**And** 输入框圆角 SHALL 为 8dp

#### Scenario: 密码输入框
**Given** 用户输入密码
**When** 显示密码输入框
**Then** 输入框 SHALL 在右侧显示可见性切换按钮
**And** 切换按钮 SHALL 使用眼睛图标（显示/隐藏）
**And** 默认状态 SHALL 为隐藏密码

### Requirement: 深色模式适配
应用 MUST 正确适配深色模式。

#### Scenario: 深色模式颜色
**Given** 用户设备处于深色模式
**When** 应用以深色模式运行
**Then** 背景色 SHALL 为深色（如 #1C1B1F）
**And** 表面色 SHALL 为深色（如 #1C1B1F）
**And** 文本色 SHALL 为浅色（如 #E6E1E5）
**And** 主色调 SHALL 使用深色版本的 Primary 颜色

#### Scenario: 深色模式开关
**Given** 用户在设置中
**When** 用户切换深色模式选项
**Then** 系统 SHALL 提供"跟随系统"、"亮色"、"深色"三个选项
**And** 选择后应用 SHALL 立即应用相应主题
**And** 主题选择 SHALL 被持久化保存

### Requirement: 动画和过渡效果
应用 MUST 使用 Material Design 3 的动画规范。

#### Scenario: 标准动画时长
**Given** 界面元素需要动画
**When** 应用过渡效果
**Then** 标准动画时长 SHALL 为 200-300ms
**And** 快速反馈动画 SHALL 为 100-150ms
**And** 缓慢动画 SHALL 为 400-500ms

#### Scenario: 缓动曲线
**Given** 动画播放
**When** 应用缓动效果
**Then** 标准动画 SHALL 使用标准缓动曲线（Standard Easing）
**And** 进入动画 SHALL 使用强调缓动曲线（Emphasized Easing）
**And** 退出动画 SHALL 使用强调减速曲线（Emphasized Decelerate）
