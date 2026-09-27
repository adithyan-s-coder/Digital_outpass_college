# Digital Outpass & Campus Gate Security Management System 🎓🚪

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF.svg?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4.svg?style=flat&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room Database](https://img.shields.io/badge/Room-SQLite%20Persistence-3DDC84.svg?style=flat&logo=android&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![CameraX](https://img.shields.io/badge/CameraX-ZXing%20QR-FF9800.svg?style=flat&logo=google&logoColor=white)](https://developer.android.com/training/camerax)
[![Gemini AI](https://img.shields.io/badge/Gemini%20AI-2.5%20Flash-8E24AA.svg?style=flat&logo=google&logoColor=white)](https://ai.google.dev/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An end-to-end digital campus ERP and gate security Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, **Android Room Database**, **CameraX (Z-Xing)**, and **Google Gemini AI**.

Designed for colleges, universities, and residential campuses to eliminate paper outpasses, automate multi-tier staff and HOD approvals, enable instant gate validation via dynamic QR codes, and generate AI-powered audit reports.

---

## 📋 Table of Contents
- [🌟 Key Highlights](#-key-highlights)
- [📱 Module-by-Module Workflows](#-module-by-module-workflows)
  - [1. Student Module](#1-student-module)
  - [2. Staff Advisor Module](#2-staff-advisor-module)
  - [3. Head of Department (HOD) Module](#3-head-of-department-hod-module)
  - [4. Security Officer (Main Gate) Module](#4-security-officer-main-gate-module)
  - [5. System Administrator Module](#5-system-administrator-module)
- [🔐 Authentication & Local Persistence](#-authentication--local-persistence)
- [🏗️ Architecture & Technology Stack](#️-architecture--technology-stack)
- [📁 Project Directory Structure](#-project-directory-structure)
- [🚀 Getting Started & Build Instructions](#-getting-started--build-instructions)
- [🐙 Pushing to GitHub](#-pushing-to-github)
- [🔒 Permissions & Privacy](#-permissions--privacy)
- [📄 License](#-license)

---

## 🌟 Key Highlights

- **Dual-Layer Offline Persistence**: Built with Android Room (SQLite) and automatic JSON backup storage to ensure all registered students, faculty accounts, outpasses, and gate logs remain permanently stored on device across app closures and reboots.
- **Multi-Tier Hierarchical Approvals**: Pass lifecycle advances strictly: `Applied` ➔ `Staff Advisor Vetted` ➔ `HOD Authorized` ➔ `Active Dynamic QR Pass` ➔ `Checked-Out` ➔ `Checked-In (Completed)`.
- **Dynamic Single-Use QR Pass**: Hardware-scanned QR code with animated visual laser reticle. The pass is automatically invalidated and marked `USED` once processed at the gate to prevent pass sharing.
- **Hardware CameraX QR Scanner**: Real-time camera preview supporting instant front/rear camera switching, torch/flashlight toggle, and fallback manual alphanumeric pass verification (`PASS-XXXX`).
- **Gemini 2.5 AI Analytics Engine**: On-demand departmental outpass audits summarizing peak movement times, approval turnaround, top outing reasons, and discipline anomalies.
- **Automated 4:10 PM HOD Dispatch Service**: Android `AlarmManager` exact scheduler that automatically packages daily outpass logs and prepares dispatch reports for the Head of Department.
- **Zero-Permission Photo Integration**: Complies with Google Play developer policies using Android Photo Picker with an interactive avatar cropping and transformation tool.

---

## 📱 Module-by-Module Workflows

### 1. Student Module
- **Self-Service Pass Creation**: Apply for outpasses with category selection (*Emergency*, *General*, *Weekend*, *Day Outing*), specific departure time, destination, and expected return time.
- **Real-Time Stage Timeline**: Visual progress tracker showing approval status at each level (Staff Advisor approval and HOD authorization).
- **Dynamic Encrypted QR Pass**: Renders high-contrast QR code vector graphic upon complete approval for gate scanning.
- **Pass History & Filtering**: Filter and review all previous requests with approval remarks and gate check-out/check-in timestamps.
- **Profile Customization**: Interactive avatar photo upload with zoom, rotate, and pan framing.

### 2. Staff Advisor Module
- **Department Scoping**: Advisors automatically view pending requests submitted by students from their designated academic department.
- **One-Tap Actions**: Approve with personalized advisor remarks or reject with explicit feedback.
- **Departmental Analytics**: Filter outpass activity across presets (*Today*, *Yesterday*, *This Week*, *This Month*, or *Custom Range*).
- **AI Audit Report**: Trigger Google Gemini AI to analyze approval turnaround and detect recurring outing trends.

### 3. Head of Department (HOD) Module
- **Executive Authorization**: Final approval authority over passes already vetted by the Staff Advisor.
- **Live Perimeter Oversight**: Real-time tally of students currently outside campus grounds.
- **Automated 4:10 PM Daily Report**: Background scheduling service automatically generates the day's student movement report and opens the system email client pre-addressed to the HOD.
- **AI Department Intelligence**: On-demand AI outpass audits and safety metrics.

### 4. Security Officer (Main Gate) Module
- **CameraX QR Code Reader**: High-performance camera scanning with ZXing engine integration.
- **Lens & Flash Controls**: Instant front/back camera flip and flashlight toggle for day/night gate duty.
- **Manual Verification Fallback**: Alphanumeric code entry (`PASS-XXXX`) for cracked or unreadable screens.
- **One-Tap Check-Out & Check-In**:
  - *Check-Out*: Records departure timestamp and updates state to `CHECKED_OUT`.
  - *Check-In*: Logs return timestamp, marks pass `CHECKED_IN`, and automatically flags late arrivals.
- **Active Off-Campus Roster**: Live roster displaying all students currently outside campus bounds with emergency contact triggers.

### 5. System Administrator Module
- **Complete Campus Audit Log**: Searchable and filterable master ledger of all gate logs, timestamps, and officer IDs.
- **User Directory**: View registered faculty and student credentials across academic departments (CSE, AIDS, ECE, Mechanical, Civil).
- **Password Administration**: Reset and recover credentials for students or faculty members.

---

## 🔐 Authentication & Local Persistence

- **Streamlined Role Selection**: Login interface featuring a clean **Selected Role** dropdown selector.
- **Role Enforcement & Security**: Ensures entered credentials match the chosen role portal to prevent unauthorized cross-module access with clear user guidance.
- **Permanent Account Storage**: Newly registered students and staff are written immediately to persistent storage (`Room SQLite Database` + `LocalBackupStorage` JSON backup). Registered accounts survive application restarts and device reboots.
- **Self-Service Credential Reset**: Built-in "Forgot Password" modal for resetting account passwords securely.

---

## 🏗️ Architecture & Technology Stack

```text
       ┌────────────────────────────────────────────────────────┐
       │               Jetpack Compose UI (M3)                  │
       │  (StudentDashboard, ApprovalsScreen, SecurityGateScreen)│
       └───────────────────────────▲────────────────────────────┘
                                   │ StateFlow / Events
       ┌───────────────────────────┴────────────────────────────┐
       │                   OutpassViewModel                     │
       │       (State Holder & Business Logic Coordinator)       │
       └───────────────────────────▲────────────────────────────┘
                                   │
       ┌───────────────────────────┴────────────────────────────┐
       │                   OutpassRepository                    │
       │            (Single Source of Truth / SSOT)             │
       └──────────────┬──────────────────────────┬──────────────┘
                      │                          │
       ┌──────────────▼─────────────┐ ┌──────────▼──────────────┐
       │   Android Room (SQLite)    │ │ LocalBackupStorage      │
       │   AppDatabase & DAOs       │ │ (JSON File Fallback)    │
       └────────────────────────────┘ └─────────────────────────┘
```

| Component | Technology | Description |
|---|---|---|
| **Language** | Kotlin 1.9+ | Modern, concise, and type-safe Android development |
| **UI Framework** | Jetpack Compose (M3) | Declarative UI following Material Design 3 guidelines |
| **Architecture** | MVVM + Clean Architecture | Clear separation of concerns with unidirectional data flow |
| **State Management** | Kotlin Coroutines & `StateFlow` | Asynchronous programming and reactive UI updates |
| **Data Persistence** | Android Room Database & SQLite | Embedded local SQLite database with JSON backup redundancy |
| **Hardware Scanning** | AndroidX CameraX & Google ZXing | Ultra-fast barcode/QR code analysis with lifecycle awareness |
| **Artificial Intelligence**| Google Gemini 2.5 Flash API | Automated report generation and student movement analysis |
| **Scheduling** | Android `AlarmManager` | Reliable exact alarm scheduling for 4:10 PM daily reports |
| **Image Loading** | Coil Compose | Smooth, asynchronous image decoding and caching |

---

## 📁 Project Directory Structure

```text
app/src/main/java/com/example/
├── MainActivity.kt                       # Entry point, role navigation & Scaffold shell
├── data/
│   ├── models/
│   │   ├── Outpass.kt                    # Outpass data model & OutpassStatus enums
│   │   ├── User.kt                       # User model, UserRole, & Department enums
│   │   ├── GateLog.kt                    # Gate check-in/out audit records
│   │   └── AiReportModels.kt             # Gemini AI analytics data structures
│   ├── repository/
│   │   └── OutpassRepository.kt          # Dual-layer persistence, Room DB & StateFlows
│   └── local/
│       ├── AppDatabase.kt                # Room database definition & type converters
│       ├── OutpassDao.kt                 # Database access objects for passes & logs
│       └── LocalBackupStorage.kt         # File-based JSON backup fallback
├── ui/
│   ├── components/
│   │   ├── CameraQrScannerView.kt        # CameraX QR scanner with camera switch & torch
│   │   ├── AiReportDialog.kt             # High-contrast AI audit report modal & charts
│   │   ├── QrCodeCanvas.kt               # Custom vector QR code generator & renderer
│   │   ├── ImageCropperDialog.kt         # Zoom/rotate/pan avatar photo crop dialog
│   │   └── StatusBadge.kt                # Material 3 status chips
│   ├── screens/
│   │   ├── LoginScreen.kt                # Streamlined role-based authentication screen
│   │   ├── RegisterScreen.kt             # Student & staff registration with photo picker
│   │   ├── StudentDashboardScreen.kt     # Pass card, dynamic QR code, stage timeline
│   │   ├── ApplyOutpassScreen.kt         # Outpass application form with date/time pickers
│   │   ├── ApprovalsScreen.kt            # Staff Advisor & HOD multi-tier approval queue
│   │   ├── SecurityGateScreen.kt         # Gate verification scanner & gate logs
│   │   ├── ActiveOutsideScreen.kt        # Roster of students currently off-campus
│   │   └── PassHistoryScreen.kt          # Searchable, filterable outpass history
│   ├── viewmodels/
│   │   └── OutpassViewModel.kt           # Business logic, validation, and reactive UI state
│   └── theme/
│       ├── Color.kt                      # Emerald & Slate campus color palette
│       ├── Type.kt                       # Material 3 typography
│       └── Theme.kt                      # Dynamic theme configuration
└── util/
    ├── GeminiAiReportService.kt          # Gemini API client for automated outpass audit
    ├── DailyHodReportScheduler.kt        # Exact Alarm 4:10 PM automated report dispatcher
    └── OutpassStatisticsCalculator.kt    # In-memory statistical aggregator
```

---

## 🚀 Getting Started & Build Instructions

### Prerequisites
- **Android Studio Ladybug (2024.2+)** or newer
- **JDK 17** or **JDK 21**
- **Android SDK Platform 34 / 35**
- Physical Android device or emulator running **Android 8.0 (API level 26)** or higher

### Build Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/<your-username>/<your-repo-name>.git
   cd <your-repo-name>
   ```

2. **Open in Android Studio**:
   - Launch Android Studio, click **File ➔ Open**, and select the project root directory.
   - Allow Gradle to download dependencies and sync the project.

3. **Configure API Keys (Optional)**:
   - For live Google Gemini AI reporting, add your Gemini API key in `gradle.properties` or environment variables:
     ```properties
     GEMINI_API_KEY=your_gemini_api_key_here
     ```
   - *Note: If no API key is provided, the app seamlessly falls back to the built-in local statistical audit engine without error.*

4. **Build and Run**:
   - Connect an Android device with USB debugging enabled, or start an emulator.
   - Run the debug build:
     ```bash
     ./gradlew assembleDebug
     ```
   - Click the green **Run ▶** button in Android Studio or install the APK directly:
     ```bash
     adb install -r app/build/outputs/apk/debug/app-debug.apk
     ```

---

## 🐙 Pushing to GitHub

Follow these steps to initialize and push this project to your GitHub repository:

### Step 1: Initialize Git in your project
```bash
git init
```

### Step 2: Verify `.gitignore`
Ensure `.gitignore` contains standard Android exclusions:
```gitignore
*.iml
.gradle
/local.properties
/.idea/
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
*.apk
*.aab
```

### Step 3: Stage all project files
```bash
git add .
```

### Step 4: Commit your changes
```bash
git commit -m "Initial commit: Complete Digital Outpass & Campus Gate Security System"
```

### Step 5: Link your remote repository and push
```bash
# Rename default branch to main
git branch -M main

# Add your GitHub repository as remote
git remote add origin https://github.com/<your-username>/<your-repo-name>.git

# Push the codebase to GitHub
git push -u origin main
```

---

## 🔒 Permissions & Privacy

The application adheres strictly to the Android security model and Google Play Developer Program policies:

| Permission | Usage Description |
|---|---|
| `android.permission.CAMERA` | Used exclusively when opening the Gate Scanner to read student QR passes. |
| `android.permission.SCHEDULE_EXACT_ALARM` | Powers the daily 4:10 PM automated departmental outpass report dispatch to the HOD. |
| `android.permission.POST_NOTIFICATIONS` | Delivers local status notifications for outpass approvals. |
| `android.permission.INTERNET` | Enables Gemini AI reporting and report email dispatch. |

- **Zero Broad Storage Permissions**: The app uses the privacy-first Android Photo Picker (`PickVisualMedia`) for profile picture selection, requiring zero external storage permissions.
- **Local-First Architecture**: Sensitive student movement and gate log data are stored locally on the device SQLite database.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) - see the LICENSE file for details.
