# password-generator Specification

## Purpose
定义 SafeVault 密码生成器独立页面的 UI 结构、交互行为和功能规范，为用户提供便捷、安全的密码生成服务。

## ADDED Requirements

### Requirement: 密码生成器页面布局
应用 MUST 提供一个独立的密码生成器页面（GeneratorFragment），通过底部导航可访问。

#### Scenario: 生成器页面显示
**Given** 用户点击底部导航的"生成器"选项卡
**When** GeneratorFragment 加载完成
**Then** 系统 SHALL 显示密码生成器界面
**And** 界面 SHALL 包含以下区域：
  - 预览区：显示当前生成的密码
  - 控制区：密码长度滑块和字符类型开关
  - 操作区：重新生成、复制、保存按钮
  - 历史区：显示最近生成的密码记录

#### Scenario: 密码预览区布局
**Given** 用户查看密码生成器页面
**When** 预览区可见
**Then** 生成的密码 SHALL 以大字体显示在屏幕中央
**And** 密码文本 SHALL 使用等宽字体
**And** 密码右侧 SHALL 显示复制按钮
**And** 预览区背景 SHALL 使用卡片样式（圆角、轻微阴影）

### Requirement: 密码生成控制
用户 MUST 能够配置密码生成的参数。

#### Scenario: 调整密码长度
**Given** 用户在密码生成器页面
**When** 用户拖动长度滑块
**Then** 密码长度显示 SHALL 实时更新
**And** 系统 SHALL 自动重新生成密码
**And** 滑块范围 SHALL 为 8-32 个字符
**And** 默认值 SHALL 为 16 个字符

#### Scenario: 选择字符类型
**Given** 用户在密码生成器页面
**When** 用户切换字符类型开关（大写、小写、数字、符号）
**Then** 系统 SHALL 自动重新生成密码
**And** 至少需要选择一种字符类型
**And** 如果用户取消所有选项，系统 SHALL 显示错误提示

#### Scenario: 生成强密码配置
**Given** 用户需要生成强密码
**When** 用户启用所有字符类型开关并设置长度为 16 或以上
**Then** 系统 SHALL 生成包含所有字符类型的强密码
**And** 密码 SHALL 确保至少包含每种选中类型的一个字符

### Requirement: 密码生成操作
用户 MUST 能够通过按钮触发密码生成操作。

#### Scenario: 重新生成密码
**Given** 用户在密码生成器页面
**When** 用户点击"重新生成"按钮
**Then** 系统 SHALL 使用当前配置生成新密码
**And** 新密码 SHALL 立即显示在预览区
**And** 生成过程 SHALL 伴有轻微的动画效果

#### Scenario: 复制密码到剪贴板
**Given** 用户在密码生成器页面
**When** 用户点击"复制"按钮或复制图标
**Then** 系统 SHALL 将生成的密码复制到剪贴板
**And** 系统 SHALL 显示"已复制"的提示消息
**And** 剪贴板 SHALL 在 30 秒后自动清除（如系统支持）

#### Scenario: 保存密码到密码库
**Given** 用户生成了一个满意的密码
**When** 用户点击"保存到密码库"按钮
**Then** 系统 SHALL 导航到 EditPasswordFragment
**And** 生成的密码 SHALL 自动填充到密码字段
**And** 用户可以继续填写其他信息（标题、用户名等）

### Requirement: 密码生成历史
应用 MUST 记录用户最近生成的密码，方便用户回顾和使用。

#### Scenario: 显示生成历史
**Given** 用户在密码生成器页面
**When** 用户已生成多个密码
**Then** 底部 SHALL 显示最近 10 次的生成历史
**And** 每条历史记录 SHALL 显示：
  - 生成的密码
  - 生成时间
  - 生成配置（长度和字符类型）
**And** 点击历史记录 SHALL 将该密码恢复到预览区

#### Scenario: 清除生成历史
**Given** 用户在密码生成器页面
**When** 用户长按某条历史记录或点击清除按钮
**Then** 系统 SHALL 显示确认对话框
**And** 确认后该条历史记录 SHALL 被删除
**And** 系统 SHALL 提供"清除全部历史"选项

### Requirement: 密码生成预设配置
应用 MUST 提供常用密码配置的快速选择功能。

#### Scenario: 选择 PIN 码预设
**Given** 用户需要生成简单的数字 PIN 码
**When** 用户点击"PIN 码"预设按钮
**Then** 系统 SHALL 配置：仅数字、长度 4-6 位
**And** 系统 SHALL 自动生成相应的密码

#### Scenario: 选择强密码预设
**Given** 用户需要生成强密码
**When** 用户点击"强密码"预设按钮
**Then** 系统 SHALL 配置：所有字符类型、长度 16 位
**And** 系统 SHALL 自动生成相应的密码

#### Scenario: 选择记忆密码预设
**Given** 用户需要生成容易记忆的密码
**When** 用户点击"记忆密码"预设按钮
**Then** 系统 SHALL 配置：大小写字母、数字、长度 12 位
**And** 系统 SHALL 自动生成相应的密码
**And** 生成的密码 SHALL 避免容易混淆的字符（如 0 和 O）

### Requirement: 密码强度指示
生成器页面 MUST 显示生成密码的强度评估。

#### Scenario: 实时强度显示
**Given** 用户在密码生成器页面
**When** 密码生成或配置更改
**Then** 系统 SHALL 计算并显示密码强度
**And** 强度 SHALL 分为三个级别：弱、中等、强
**And** 强度指示 SHALL 使用颜色和文字显示
**And** 颜色 SHALL 对应：弱（红色）、中等（橙色）、强（绿色）

### Requirement: 生成器页面状态保持
应用 MUST 在用户切换选项卡时保持生成器的状态。

#### Scenario: 切换后状态恢复
**Given** 用户在生成器页面配置了参数并生成了密码
**When** 用户切换到其他选项卡后再切换回来
**Then** 生成的密码 SHALL 保持显示
**And** 滑块和开关的值 SHALL 保持不变
**And** 生成历史记录 SHALL 保持

### Requirement: 密码生成安全性
密码生成器 MUST 使用安全的随机数生成方法。

#### Scenario: 使用加密安全的随机数
**Given** 系统需要生成密码
**When** 密码生成逻辑执行
**Then** 系统 MUST 使用 java.security.SecureRandom
**And** 系统 SHALL NOT 使用普通的 java.util.Random
**And** 生成的密码 SHALL 具有足够的随机性和不可预测性
