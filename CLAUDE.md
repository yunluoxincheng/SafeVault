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
- **MainActivity**: Main container with Navigation Component
- **PasswordListFragment**: Display password entries with search
- **PasswordDetailFragment**: Show individual password details
- **EditPasswordFragment**: Create/edit password entries
- **AutofillServiceImpl**: Android AutofillService integration
- **BackendService**: Interface defining all backend operations

### Technology Stack
- **Min SDK**: 23 (Android 6.0)
- **Target SDK**: 35
- **Language**: Java 8
- **Architecture**: MVVM with Android Jetpack
- **UI**: Material Components + ConstraintLayout
- **Navigation**: Android Navigation Component
- **Security**: Biometric authentication, FLAG_SECURE

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
Single navigation graph with start destination at `passwordListFragment`:
- List → Detail (pass `passwordId` as argument)
- List → Edit (pass `passwordId`, use `-1` for new items)
- List → Settings

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
- Material Components for consistent UI
- Biometric authentication support