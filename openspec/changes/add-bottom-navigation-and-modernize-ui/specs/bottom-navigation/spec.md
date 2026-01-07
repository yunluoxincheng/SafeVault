# bottom-navigation Specification

## Purpose
定义 SafeVault 应用底部导航菜单的 UI 结构和交互行为规范，为用户提供清晰、直观的功能导航体验。

## ADDED Requirements

### Requirement: 底部导航菜单结构
应用 MUST 在 MainActivity 中实现底部导航菜单，包含三个顶级目的地。

#### Scenario: 底部导航菜单显示
**Given** 用户打开 SafeVault 应用
**When** MainActivity 加载完成
**Then** 系统 SHALL 在屏幕底部显示 BottomNavigationView
**And** 底部导航 SHALL 包含三个选项卡：
  - 密码库（导航图标：保险柜图标）
  - 生成器（导航图标：密码生成图标）
  - 设置（导航图标：设置图标）
**And** 每个选项卡 SHALL 包含图标和文字标签
**And** 默认选中项 SHALL 为"密码库"

#### Scenario: 底部导航菜单高度和布局
**Given** 用户查看应用界面
**When** 底部导航菜单可见
**Then** 导航栏高度 SHALL 为 80dp
**And** 图标尺寸 SHALL 为 24dp x 24dp
**And** 图标和标签 SHALL 垂直排列
**And** 选中状态的选项卡 SHALL 使用主色调高亮显示

### Requirement: 底部导航选项卡切换
用户 MUST 能够通过底部导航在三个主要功能之间快速切换。

#### Scenario: 切换到密码库选项卡
**Given** 用户在生成器或设置页面
**When** 用户点击底部导航的"密码库"选项卡
**Then** 系统 SHALL 导航到 PasswordListFragment
**And** 工具栏标题 SHALL 显示"密码列表"
**And** 密码列表 SHALL 保持之前的滚动位置
**And** 如果有进行中的搜索，搜索状态 SHALL 保持

#### Scenario: 切换到生成器选项卡
**Given** 用户在密码库或设置页面
**When** 用户点击底部导航的"生成器"选项卡
**Then** 系统 SHALL 导航到 GeneratorFragment
**And** 工具栏标题 SHALL 显示"密码生成器"
**And** 如果之前生成过密码，生成的密码 SHALL 保持显示
**And** 生成历史记录 SHALL 可见

#### Scenario: 切换到设置选项卡
**Given** 用户在密码库或生成器页面
**When** 用户点击底部导航的"设置"选项卡
**Then** 系统 SHALL 导航到 SettingsFragment
**And** 工具栏标题 SHALL 显示"设置"
**And** 设置选项 SHALL 以列表形式展示

### Requirement: 底部导航状态保持
应用 MUST 在选项卡切换时保持各页面的状态和滚动位置。

#### Scenario: 密码库列表滚动位置保持
**Given** 用户在密码库页面滚动到某个位置
**When** 用户切换到其他选项卡后再切换回密码库
**Then** 密码列表 SHALL 恢复到之前的滚动位置
**And** 搜索状态 SHALL 保持
**And** 任何正在加载的操作 SHALL 继续

#### Scenario: 生成器页面状态保持
**Given** 用户在生成器页面配置了密码生成选项
**When** 用户切换到其他选项卡后再切换回生成器
**Then** 之前生成的密码 SHALL 保持显示
**And** 滑块、开关等控件的值 SHALL 保持不变
**And** 生成历史记录 SHALL 保持

### Requirement: 底部导航视觉反馈
底部导航 MUST 提供清晰的视觉反馈以指示当前选中的选项卡。

#### Scenario: 选项卡选中状态显示
**Given** 用户查看底部导航菜单
**When** 某个选项卡被选中
**Then** 该选项卡的图标 SHALL 使用主色调填充显示
**And** 该选项卡的文字标签 SHALL 使用主色调显示
**And** 未选中的选项卡图标 SHALL 使用轮廓样式或降低透明度显示

#### Scenario: 点击波纹效果
**Given** 用户点击底部导航的某个选项卡
**When** 点击动作发生
**Then** 系统 SHALL 显示 Material Design 的波纹效果
**And** 波纹颜色 SHALL 使用主色调的半透明版本

### Requirement: 底部导航菜单资源定义
底部导航菜单 MUST 通过 XML 资源文件定义。

#### Scenario: 菜单资源文件结构
**Given** 开发者创建底部导航菜单资源
**When** 在 res/menu/bottom_nav_menu.xml 中定义菜单
**Then** 菜单 SHALL 包含三个 item 元素
**And** 每个 item SHALL 定义：
  - android:id（导航目标 Fragment 的 ID）
  - android:icon（选项卡图标引用）
  - android:title（选项卡文字标签字符串资源）

### Requirement: 底部导航与系统集成
底部导航 MUST 与 Navigation Component 和返回栈正确集成。

#### Scenario: 返回键行为
**Given** 用户在密码库或生成器的子页面（如详情页）
**When** 用户按下返回键
**Then** 系统 SHALL 先返回到父级 Fragment
**And** 如果已经在顶级 Fragment（密码库、生成器、设置）
**Then** 再按返回键 SHALL 退出应用或返回登录页

#### Scenario: 底部导航选项卡不可用
**Given** 应用处于某种特殊状态（如正在加载数据）
**When** 某个选项卡暂时不可用
**Then** 该选项卡 SHOULD 显示为禁用状态
**Or** 该选项卡 SHOULD 保持可见但点击时显示加载提示
