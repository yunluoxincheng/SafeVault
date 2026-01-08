<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---
## 项目放在com.ttt.safevault软件包目录下

##目前这个项目做的是前端部分

##项目的目标Android版本是Android10及以上（最小SDK 29，目标SDK 36)一定要兼容Android10+

## SafeVault Android Password Manager

A native Android password manager application built with Java using MVVM architecture. The frontend handles UI and user interactions only, while all encryption and data persistence is handled by a backend service.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Install debug build to connected device
./gradlew installDebug

# Check dependencies
./gradlew dependencies
```

## High-Level Architecture

### Frontend-Only Approach
- **UI Layer**: Activities/Fragments handle user interaction and display
- **ViewModel Layer**: Manages UI state and business logic
- **Backend Interface**: All cryptographic operations and data storage through `BackendService`

### Key Principle
The frontend **never** handles:
- Encryption/decryption operations
- Direct database access
- Plain-text password storage
- Sensitive data persistence

### Package Structure
```
com.ttt.safevault/
├── ui/                      # UI components (Activities/Fragments)
├── viewmodel/               # MVVM ViewModels
├── model/                   # Data models and BackendService interface
├── autofill/                # Android AutofillService implementation
├── security/                # Security utilities and configuration
├── utils/                   # Helper classes
└── adapter/                 # RecyclerView adapters
```

### Core Components
- **LoginActivity**: App entry point with authentication
- **MainActivity**: Main container with Navigation Component and Bottom Navigation
- **PasswordListFragment**: Display password entries with search
- **PasswordDetailFragment**: Show individual password details
- **EditPasswordFragment**: Create/edit password entries
- **GeneratorFragment**: Standalone password generator with strength indicator and history
- **SettingsFragment**: App settings and preferences
- **AutofillServiceImpl**: Android AutofillService integration
- **BackendService**: Interface defining all backend operations

### Technology Stack
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36
- **Language**: Java 8
- **Architecture**: MVVM with Android Jetpack
- **UI**: Material Design 3 + ConstraintLayout
- **Navigation**: Android Navigation Component with Bottom Navigation
- **Security**: Biometric authentication, FLAG_SECURE
- **Clipboard**: Custom ClipboardManager with 30-second auto-clear

## Development Guidelines

### BackendService Implementation
All data operations go through the `BackendService` interface. The frontend receives:
- `PasswordItem` objects (already decrypted)
- `List<PasswordItem>` for search results
- Primitive types for operation results

### Security Implementation
- Activities use `FLAG_SECURE` to prevent screenshots
- Clipboard manager auto-clears sensitive data
- Auto-lock when app goes to background
- Biometric authentication support

### Autofill Service
- Configured in `AndroidManifest.xml`
- Service implementation in `autofill/AutofillServiceImpl.java`
- Configuration file: `res/xml/autofill_service_configuration.xml`

### Navigation Flow
Bottom navigation with three top-level destinations:
- **密码库** (Vault): PasswordListFragment - Display password entries with search
  - List → Detail (pass `passwordId` as argument)
  - List → Edit (pass `passwordId`, use `-1` for new items)
- **生成器** (Generator): GeneratorFragment - Standalone password generator
  - Features: Password strength indicator, generation history, preset configurations
  - Supports: PIN codes, strong passwords, memorable passwords
- **设置** (Settings): SettingsFragment - App settings and preferences

### Material Design 3 Implementation
The app uses Material Design 3 components throughout:
- **TextInputLayout**: Outlined box style with Material 3
- **MaterialButton**: Filled, outlined, and text button variants
- **MaterialCardView**: 12dp corner radius for cards
- **MaterialSwitch**: For toggle controls
- **LinearProgressIndicator**: For password strength and loading states
- **BottomNavigationView**: Three-tab navigation with Material 3 styling
- **Dynamic Colors**: Android 12+ devices use system colors
- **Fixed Colors**: Android 10-11 use purple-based theme

## Important Notes

### Namespace Mismatch
- Package name: `com.ttt.safevault`
- Build namespace: `com.safevault`
- Maintain consistency when creating new components

### Backend Dependency
The frontend codebase is incomplete without a backend implementation of `BackendService`. Current placeholders exist for:
- Repository pattern
- Security manager
- Backend service locator

### Sprint Development Structure
1. **Sprint 1**: UI framework, login, password list
2. **Sprint 2**: Edit/create UI, password generator
3. **Sprint 3**: Autofill service, search functionality
4. **Sprint 4**: Settings, animations, bug fixes

### Build Configuration
- ViewBinding enabled for type-safe view references
- Room database included for repository access only
- Material Design 3 components for modern UI
- Biometric authentication support
- Custom ClipboardManager for secure clipboard handling

### Password Generator Features
The GeneratorFragment provides comprehensive password generation:
- **Length Control**: Slider from 8-32 characters
- **Character Types**: Uppercase, lowercase, numbers, symbols toggles
- **Strength Indicator**: Visual bar with color-coded strength levels (weak/medium/strong/very strong)
- **Presets**: Quick-select configurations for PIN codes, strong passwords, memorable passwords
- **Generation History**: Recent passwords stored locally (max 10 items)
- **Clear History**: Button to remove all stored history
- **Secure Clipboard**: 30-second auto-clear after copying
