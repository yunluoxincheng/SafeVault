# Implementation Tasks

## 阶段 1：准备工作

### 1.1 创建底部导航资源文件
- [ ] 创建 `res/menu/bottom_nav_menu.xml` 定义底部导航菜单项
- [ ] 添加底部导航图标资源（ic_vault_key.xml, ic_generate.xml, ic_settings.xml 可使用现有的）
- [ ] 在 `strings.xml` 中添加导航标签字符串：
  - `nav_passwords`: "密码库"
  - `nav_generator`: "生成器"
  - `nav_settings`: "设置"

### 1.2 实现 Material Design 3 主题
- [ ] 在 `res/values/themes.xml` 中创建 `Theme.Material3` 基础主题
- [ ] 在 `res/values/colors.xml` 中定义 Material 3 完整颜色系统（亮色）
- [ ] 在 `res/values-night/colors.xml` 中定义 Material 3 完整颜色系统（深色）
- [ ] 创建 `res/values-v31/themes.xml` 支持动态配色（Android 12+）
- [ ] 测试主题在不同 Android 版本的表现

### 1.3 创建 GeneratorFragment 基础结构
- [ ] 创建 `GeneratorFragment.java` 类文件
- [ ] 创建 `GeneratorViewModel.java` 类文件
- [ ] 创建 `fragment_generator.xml` 布局文件
- [ ] 创建 `GeneratedPasswordsAdapter.java` 用于显示历史记录
- [ ] 创建 `item_generated_password.xml` 历史记录项布局

## 阶段 2：底部导航集成

### 2.1 更新 MainActivity 布局
- [ ] 修改 `activity_main.xml`：
  - 移除 FloatingActionButton（FAB）
  - 添加 BottomNavigationView 组件
  - 调整 NavHostFragment 的约束（底部预留导航栏空间）
- [ ] 确保布局在不同屏幕尺寸下正常显示

### 2.2 更新 MainActivity 代码
- [ ] 在 `MainActivity.java` 中初始化 BottomNavigationView
- [ ] 实现 `setupWithNavController` 集成导航
- [ ] 配置 `AppBarConfiguration` 支持多个顶级目的地
- [ ] 实现选项卡切换时的状态保持逻辑
- [ ] 测试返回键行为和导航栈管理

### 2.3 更新导航图配置
- [ ] 修改 `main_nav_graph.xml`：
  - 将 `passwordListFragment`、`generatorFragment`、`settingsFragment` 设为顶级目的地
  - 移除从 passwordListFragment 到 settingsFragment 的导航（通过底部导航访问）
  - 确保子页面导航（详情、编辑）正确配置

## 阶段 3：密码生成器页面实现

### 3.1 实现生成器 UI 布局
- [ ] 完成 `fragment_generator.xml`：
  - 预览区：显示生成的密码（TextInputEditText 或 TextView）
  - 控制区：Slider（长度）和 4 个 SwitchMaterial（字符类型）
  - 操作区：重新生成、复制、保存按钮
  - 历史区：RecyclerView 显示历史记录
- [ ] 应用 Material Design 3 样式（圆角、间距、颜色）
- [ ] 添加密码强度指示器

### 3.2 实现生成器业务逻辑
- [ ] 在 `GeneratorViewModel` 中实现：
  - `generatePassword()` 方法使用 SecureRandom
  - `copyToClipboard()` 方法处理剪贴板操作
  - `savePassword()` 方法导航到编辑页面
  - 生成历史的存储和查询逻辑
- [ ] 在 `GeneratorFragment` 中绑定 ViewModel 和 UI
- [ ] 实现滑块和开关的实时响应
- [ ] 实现预设配置按钮（PIN 码、强密码、记忆密码）

### 3.3 实现生成历史功能
- [ ] 创建 `GeneratedPassword` 数据模型
- [ ] 实现 `GeneratedPasswordsAdapter`
- [ ] 在 ViewModel 中添加历史记录管理方法
- [ ] 实现历史记录点击恢复功能
- [ ] 实现清除历史功能

## 阶段 4：界面现代化改造

### 4.1 更新密码列表页面
- [ ] 修改 `fragment_password_list.xml`：
  - 应用 Material 3 卡片样式（12dp 圆角）
  - 调整间距为 8dp 基础单位
  - 更新列表项布局样式
- [ ] 在工具栏添加"添加"按钮（替代 FAB）
- [ ] 更新空状态显示样式
- [ ] 测试列表项动画

### 4.2 更新密码详情页面
- [ ] 修改 `fragment_password_detail.xml`：
  - 应用 Material 3 卡片和按钮样式
  - 更新密码预览区域样式
  - 调整操作按钮布局
- [ ] 确保可见性切换按钮使用 Material 图标

### 4.3 更新编辑页面
- [ ] 修改 `fragment_edit_password.xml`：
  - 所有输入框使用 TextInputLayout 包裹
  - 应用 Material 3 输入框样式（8dp 圆角）
  - 统一按钮样式
  - 添加密码生成器入口按钮

### 4.4 更新设置页面
- [ ] 修改 `fragment_settings.xml`：
  - 使用列表式布局
  - 每个设置项包含图标、标题、描述
  - 应用 Material 3 样式
- [ ] 实现设置分组（安全、外观、备份、关于）
- [ ] 添加深色模式设置选项

### 4.5 更新其他页面
- [ ] 更新 `activity_login.xml` 样式
- [ ] 更新 `dialog_generate_password.xml` 样式（如果保留）
- [ ] 统一所有对话框使用 MaterialAlertDialogBuilder
- [ ] 更新 `item_password.xml` 列表项样式

## 阶段 5：功能整合和测试

### 5.1 导航和状态管理
- [ ] 测试底部导航三个选项卡切换
- [ ] 验证各页面状态保持（滚动位置、输入内容）
- [ ] 测试返回键行为
- [ ] 测试从生成器保存到密码库的流程

### 5.2 剪贴板功能
- [ ] 测试密码复制功能（生成器、详情页）
- [ ] 实现 30 秒自动清除（如果系统支持）
- [ ] 添加复制成功提示

### 5.3 主题适配测试
- [ ] 测试亮色模式显示
- [ ] 测试深色模式显示
- [ ] 测试 Android 10-11 固定配色
- [ ] 测试 Android 12+ 动态配色
- [ ] 测试字体缩放功能

### 5.4 兼容性测试
- [ ] 在 Android 10 (API 29) 上测试
- [ ] 在 Android 11 (API 30) 上测试
- [ ] 在 Android 12 (API 31) 上测试
- [ ] 在 Android 13+ 上测试
- [ ] 测试不同屏幕尺寸（手机、平板）

### 5.5 无障碍测试
- [ ] 测试 TalkBack 导航
- [ ] 测试所有图标和按钮的 contentDescription
- [ ] 测试触摸目标尺寸（至少 48dp）
- [ ] 测试高对比度模式

## 阶段 6：文档和清理

### 6.1 代码清理
- [ ] 移除不再使用的 FAB 相关代码
- [ ] 移除或更新 GeneratePasswordDialog（如果不再需要）
- [ ] 清理未使用的资源文件
- [ ] 统一代码格式和注释

### 6.2 文档更新
- [ ] 更新 CLAUDE.md 中的架构描述
- [ ] 更新 README.md 中的功能列表
- [ ] 添加新功能的截图（如需要）
- [ ] 更新项目文档

### 6.3 最终验证
- [ ] 运行 `./gradlew assembleDebug` 确保编译成功
- [ ] 运行所有单元测试
- [ ] 运行 `openspec validate` 确保规范正确
- [ ] 进行完整的手动功能测试

## 任务依赖关系

- **阶段 1** 可独立进行
- **阶段 2** 依赖阶段 1.1 和 1.2 完成
- **阶段 3** 依赖阶段 1.3 完成
- **阶段 4** 可与阶段 2、3 并行进行（独立页面）
- **阶段 5** 依赖前面所有阶段完成
- **阶段 6** 依赖阶段 5 完成

## 并行化建议

可以并行进行的工作：
- 阶段 1.1、1.2、1.3 可并行（资源文件创建）
- 阶段 4 中的各子任务可并行（不同页面独立）
- UI 布局调整和业务逻辑实现可部分并行
