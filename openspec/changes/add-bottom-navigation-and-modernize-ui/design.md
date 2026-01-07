# Design Document: 底部导航和现代化界面

## Overview

本文档详细描述 SafeVault 密码管理器底部导航菜单和现代化界面设计的架构决策、技术方案和实现细节。

## Architecture Changes

### 导航架构重构

#### 当前导航结构
```
MainActivity (单一容器)
└── NavHostFragment
    └── startDestination: passwordListFragment
        ├── → passwordDetailFragment
        ├── → editPasswordFragment
        └── → settingsFragment
```

#### 新导航结构
```
MainActivity (底部导航容器)
├── BottomNavigationView (3个顶级目的地)
└── NavHostFragment
    ├── startDestination: passwordListFragment (密码库)
    │   ├── → passwordDetailFragment
    │   └── → editPasswordFragment
    │
    ├── generatorFragment (生成器) [新增]
    │
    └── settingsFragment (设置)
```

### 组件层次结构

```
MainActivity
├── AppBarLayout
│   └── MaterialToolbar (动态标题)
├── FragmentContainerView
│   └── NavHostFragment (三个顶级 Fragment)
└── BottomNavigationView (底部导航栏)
```

## Technical Decisions

### 1. 底部导航实现方案

**选择**：使用 `BottomNavigationView` + Navigation Component

**理由**：
- Navigation Component 原生支持底部导航
- 自动处理返回栈和导航状态
- 符合 Android 推荐的最佳实践

**实现要点**：
```xml
<!-- activity_main.xml -->
<BottomNavigationView
    android:id="@+id/bottom_navigation"
    app:menu="@menu/bottom_nav_menu" />
```

```java
// MainActivity.java
NavigationUI.setupWithNavController(
    bottomNavigation,
    navController
);
```

### 2. 多个顶级目的地配置

**挑战**：Navigation Component 默认只支持一个起点

**解决方案**：
- 创建一个隐藏的导航容器 Fragment（`MainNavFragment`）
- 或者使用多个 NavHostFragment（更复杂的方案）
- **推荐方案**：使用嵌套导航图

```xml
<!-- main_nav_graph.xml -->
<navigation app:startDestination="@id/mainTabsFragment">
    <fragment
        android:id="@+id/mainTabsFragment"
        android:name="com.ttt.safevault.ui.MainTabsFragment">
        <!-- 包含底部导航逻辑 -->
    </fragment>
</navigation>
```

**简化方案**（推荐）：
- 保持单层导航图
- 将三个顶级 Fragment 设为同一层级
- 在 MainActivity 中手动处理底部导航切换

### 3. 密码生成器 Fragment 设计

**架构**：
```java
public class GeneratorFragment extends BaseFragment {
    private GeneratorViewModel viewModel;

    // UI 组件
    private TextInputEditText generatedPasswordText;
    private Slider lengthSlider;
    private SwitchMaterial uppercaseSwitch;
    private SwitchMaterial lowercaseSwitch;
    private SwitchMaterial numbersSwitch;
    private SwitchMaterial symbolsSwitch;
    private Button regenerateButton;
    private Button copyButton;
    private Button saveButton;

    // 生成历史
    private RecyclerView historyRecyclerView;
    private GeneratedPasswordsAdapter historyAdapter;
}
```

**功能**：
- 实时密码生成和预览
- 生成历史记录（最近 10 个）
- 预设配置（快速选择：PIN 码、强密码、记忆密码）
- 一键复制和保存到密码库

### 4. Material Design 3 主题实现

**主题层级**：
```xml
<!-- themes.xml -->
<style name="Theme.SafeVault" parent="Theme.Material3.DayNight.NoActionBar">
    <!-- 主色调 -->
    <item name="colorPrimary">@color/md_theme_light_primary</item>
    <item name="colorOnPrimary">@color/md_theme_light_onPrimary</item>
    <item name="colorPrimaryContainer">@color/md_theme_light_primaryContainer</item>

    <!-- 动态配色（Android 12+）-->
    <item name="android:colorBackground">@android:color/system_neutral1_500</item>
    <item name="android:colorBackgroundFallback">@android:color/system_neutral1_500</item>
</style>
```

**配色方案**（固定配色，Android 10-11）：
```
主色（Primary）:   #6750A4 (紫色)
主色容器:         #EADDFF (浅紫)
次色（Secondary）: #625B71 (灰紫)
次色容器:         #E8DEF8 (浅灰紫)
表面色:           #FEF7FF (几乎白色)
错误色:           #B3261E (红色)
```

**圆角规范**：
- 小组件（按钮、输入框）：8dp
- 中等组件（卡片、对话框）：12dp
- 大型组件（底部表单、全屏对话框）：16dp
- 特大型组件：24dp

## Implementation Details

### 布局调整

#### activity_main.xml
```xml
<androidx.constraintlayout.widget.ConstraintLayout>
    <com.google.android.material.appbar.AppBarLayout>
        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar" />
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host_fragment"
        app:layout_constraintTop_toBottomOf="@id/app_bar_layout"
        app:layout_constraintBottom_toTopOf="@id/bottom_navigation" />

    <!-- 移除 FAB -->
    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_navigation"
        app:layout_constraintBottom_toBottomOf="parent"
        app:menu="@menu/bottom_nav_menu" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

#### bottom_nav_menu.xml（新）
```xml
<menu>
    <item
        android:id="@+id/nav_passwords"
        android:icon="@drawable/ic_vault_key"
        android:title="@string/nav_passwords" />
    <item
        android:id="@+id/nav_generator"
        android:icon="@drawable/ic_generate"
        android:title="@string/nav_generator" />
    <item
        android:id="@+id/nav_settings"
        android:icon="@drawable/ic_settings"
        android:title="@string/nav_settings" />
</menu>
```

### Fragment 生命周期处理

**问题**：底部导航切换时 Fragment 重建

**解决方案**：
```java
// 使用 FragmentManager 保留实例
// 或使用 ViewModel 保持状态
bottomNavigation.setOnItemSelectedListener(item -> {
    // 手动切换 Fragment 而非使用导航
    Fragment selectedFragment = getFragmentForItemId(item.getItemId());
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.nav_host_fragment, selectedFragment)
        .commit();
    return true;
});
```

### 状态保持

**使用 ViewModel**：
```java
public class PasswordListViewModel extends ViewModel {
    private final MutableLiveData<Integer> scrollPosition = new MutableLiveData<>();

    public void saveScrollPosition(int position) {
        scrollPosition.setValue(position);
    }

    public LiveData<Integer> getScrollPosition() {
        return scrollPosition;
    }
}
```

## UI Design Specifications

### 底部导航栏
- **高度**：80dp
- **图标尺寸**：24dp
- **选中状态**：图标 + 标签（12dp 字体）
- **未选中状态**：图标 + 标签（降低透明度）
- **波纹效果**：使用 Material 3 的波纹颜色
- **标签字体**：Material 3 Label Large

### 密码库页面
- **移除 FAB**
- **添加工具栏操作**：搜索、添加
- **列表项卡片**：
  - 圆角：12dp
  - 间距：8dp（列表项之间）
  - 内边距：16dp
  - 阴影：elevation 2dp
- **空状态**：居中显示插画和引导文案

### 生成器页面
- **布局**：垂直滚动的 ConstraintLayout
- **预览区**：
  - 顶部显示生成的密码
  - 大字体、居中对齐
  - 复制按钮紧贴右侧
- **控制区**：
  - 滑块：密码长度（8-32）
  - 开关组：大写、小写、数字、符号
- **操作按钮**：重新生成、保存到密码库
- **历史记录**：底部显示最近的生成记录

### 设置页面
- **列表式布局**：使用 `PreferenceFragmentCompat` 或 `RecyclerView`
- **分组**：安全设置、外观设置、备份设置、关于
- **每个设置项**：
  - 图标（左侧）
  - 标题和描述
  - 切换开关或箭头（右侧）

## Migration Strategy

### 阶段 1：准备工作
1. 创建新的菜单和布局资源
2. 实现 Material 3 主题
3. 创建 GeneratorFragment 的基础结构

### 阶段 2：底部导航集成
1. 修改 activity_main.xml
2. 更新 MainActivity 代码
3. 配置导航图

### 阶段 3：Fragment 迁移
1. 调整 PasswordListFragment（移除 FAB 依赖）
2. 完善 GeneratorFragment
3. 更新 SettingsFragment

### 阶段 4：UI 现代化
1. 更新所有布局以符合 Material 3 规范
2. 调整颜色、圆角、间距
3. 测试不同 Android 版本的表现

## Testing Considerations

### 功能测试
- [ ] 底部导航三个选项卡都可以正常切换
- [ ] 切换后状态保持（滚动位置、输入内容）
- [ ] 密码生成器功能完整
- [ ] 设置功能正常

### 兼容性测试
- [ ] Android 10（API 29）：固定主题正常显示
- [ ] Android 12（API 31）：动态配色生效
- [ ] Android 13+：最新的 Material 3 组件正常

### 用户体验测试
- [ ] 动画流畅，无卡顿
- [ ] 触摸反馈及时
- [ ] 无障碍功能正常（TalkBack、放大手势）

## Open Questions

1. **状态保存机制**：是使用 Fragment 的 `setRetainInstance` 还是 ViewModel？
   - 建议：使用 ViewModel + SavedStateHandle

2. **底部导航动画**：是否需要自定义切换动画？
   - 建议：使用默认的淡入淡出动画

3. **生成器页面状态**：切换出去后是否保持生成的密码？
   - 建议：保持，方便用户复制

4. **深色模式适配**：是否需要手动调整深色模式下的颜色？
   - 建议：使用 Material 3 的自动深色模式
