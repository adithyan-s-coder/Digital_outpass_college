# Smart Campus Outpass & Security Management System 🎓🚪

An end-to-end digital campus ERP and gate security Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, **CameraX (Z-Xing)**, **Room Database**, and **Gemini 2.5 AI**.

Designed for colleges and universities to eliminate paper outpasses, automate multi-tier approvals, facilitate ultra-fast gate check-in/out via dynamic QR codes, and generate AI-powered audit reports.

---

## 🌟 Key Features & Role-Based Workflows

### 1. 🧑‍🎓 Student Module
- **Self-Service Pass Application**: Request Outpasses by selecting type (*Emergency*, *General*, *Weekend*, *Day Outing*), destination, reason, departure time, and expected return time.
- **Dynamic Profile & Avatars**: Upload, adjust, and view student profile photos alongside registration numbers and department tags.
- **Multi-Tier Live Tracking**: Real-time stage progression:
  1. `Applied` ➔ 2. `Staff Advisor Approval` ➔ 3. `HOD Sign-Off` ➔ 4. `Approved & QR Active`
- **Dynamic Single-Use QR Pass**:
  - Encrypted, tamper-proof QR code with laser scanning animation.
  - Automatically marked as **EXPIRED & USED** once scanned at the security gate to prevent pass sharing.
- **Student History**: Filterable list of previous outpasses with approval remarks and gate timestamps.

---

### 2. 👨‍🏫 Staff Advisor Module
- **Class/Department Scope**: Advisors only see pending requests from their assigned department.
- **1-Tap Decision Making**: Review student reasons, destination, past record, and approve or reject with custom advisor remarks.
- **AI-Powered Departmental Analytics**:
  - Filter passes by date preset (*Today*, *Yesterday*, *This Week*, *This Month*, or *Custom Range*).
  - One-click **Gemini AI Outpass Report** analyzing departure peak hours, approval turnaround times, top outing reasons, and key disciplinary observations.
- **Automated 4:10 PM HOD Dispatch Status**: Status indicator displaying scheduled automated daily reporting to the HOD.

---

### 3. 👨‍💼 Head of Department (HOD) Module
- **Final Tier Authorization**: Review passes already vetted by the Staff Advisor.
- **Executive Audit Dashboard**: Real-time overview of students currently out of campus and historical department passes.
- **Automated 4:10 PM Daily Report Delivery**: Background alarm service automatically compiles the day's outpass audit at 4:10 PM and prepares report dispatch to the HOD's email.
- **Custom AI Intelligence**: Generate custom periodic analysis directly with Gemini AI.

---

### 4. 👮 Security Officer (Main Gate) Module
- **High-Speed CameraX QR Scanner**:
  - Instant hardware scanning powered by CameraX and ZXing.
  - **Front & Back Camera Toggle**: Switch effortlessly between the rear gate camera and front selfie camera.
  - Built-in flashlight/torch toggle and visual laser reticle.
  - Fallback manual pass code entry (`PASS-XXXX`) if a student's phone screen is cracked or low battery.
- **1-Tap Gate Check-Out & Check-In**:
  - **Check-Out**: Logs departure timestamp and sets status to `CHECKED_OUT`.
  - **Check-In**: Logs return timestamp, marks pass `CHECKED_IN`, and automatically flags late returns.
- **Active Outside Roster**: Dedicated live tab showing all students currently outside the campus perimeter.
- **Categorized Gate Logs & History**: Dynamic tab filtering with dedicated views for *Staff Approval Pending*, *Pending HOD Sign-off*, *Approved Passes*, *Outside Campus*, and *Returned/Completed*.

---

### 5. 🛠️ System Administration Module
- **Full Campus Audit Trail**: Unified searchable log of all gate check-outs, check-ins, and late returns.
- **Multi-Role Quick Switcher**: Easily test and demonstrate the app as Student, Staff Advisor, HOD, Security Officer, or Administrator.
- **User Directory**: View registered faculty and students across Computer Science, AI & Data Science, Electronics, Mechanical, and Civil Engineering.

---

## 🏗️ Architecture & Technology Stack

| Component | Technology |
|---|---|
| **Language** | Kotlin 1.9+ |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM (Model-View-ViewModel) + Clean Architecture |
| **Asynchronous** | Kotlin Coroutines & `StateFlow` / `SharedFlow` |
| **Local Persistence** | Android Room Database & SQLite |
| **Image Loading** | Coil Compose |
| **QR Code Engine** | Google ZXing (MultiFormatReader & QRCodeWriter) |
| **Camera Integration** | AndroidX CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`) |
| **Artificial Intelligence** | Google Gemini API (2.5 Flash) via Server-Side REST integration |
| **Background Scheduling** | Android `AlarmManager` with `BroadcastReceiver` (`SCHEDULE_EXACT_ALARM`) |

---

## 📁 Project Directory Structure

```text
app/src/main/java/com/example/
├── MainActivity.kt                       # App entry point, role-based navigation, top bar
├── data/
│   ├── models/
│   │   ├── Outpass.kt                    # Outpass data model & OutpassStatus enums
│   │   ├── User.kt                       # User model, UserRole, & Department enums
│   │   ├── GateLog.kt                    # Gate check-in/out log entries
│   │   └── AiReportModels.kt             # Gemini AI analytics data structures
│   ├── repository/
│   │   └── OutpassRepository.kt          # Room DB queries, state flows, & demo data
│   └── local/
│       └── AppDatabase.kt                # Room database definition & type converters
├── ui/
│   ├── components/
│   │   ├── CameraQrScannerView.kt        # CameraX QR scanner with front/back camera switch
│   │   ├── AiReportDialog.kt             # High-contrast AI audit report modal & charts
│   │   ├── QrCodeCanvas.kt               # Custom vector QR code generator & renderer
│   │   └── StatusBadge.kt                # Material 3 status chips
│   ├── screens/
│   │   ├── StudentDashboardScreen.kt     # Student pass card, dynamic QR, active status
│   │   ├── ApplyOutpassScreen.kt         # Pass application form with date/time pickers
│   │   ├── ApprovalsScreen.kt            # Staff Advisor & HOD multi-tier approval queue
│   │   ├── SecurityGateScreen.kt         # Main Gate verification scanner & gate logs
│   │   ├── ActiveOutsideScreen.kt        # Roster of students currently off-campus
│   │   ├── PassHistoryScreen.kt          # Filterable outpass history by status
│   │   ├── LoginScreen.kt                # Authentication & demo account switcher
│   │   └── RegisterScreen.kt             # New member registration & photo capture
│   ├── viewmodels/
│   │   └── OutpassViewModel.kt           # Central business logic and reactive UI state
│   └── theme/
│       ├── Color.kt                      # Modern Emerald & Slate color scheme
│       └── Theme.kt                      # Material 3 Theme configuration
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
- Android device or emulator running **Android 8.0 (API 26)** or higher

### Installation
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/campus-outpass-erp.git
   cd campus-outpass-erp
   ```

2. **Open in Android Studio**:
   - Open Android Studio, select **Open**, and navigate to the project directory.
   - Wait for Gradle sync to complete.

3. **Configure API Keys (Optional for AI Reports)**:
   - To enable live Gemini AI reporting, add your Gemini API key in `BuildConfig` or through the AI Studio environment secrets.
   - If an API key is not present, the app automatically uses the built-in **Calculated Audit Engine** to generate statistics and insights without crashing.

4. **Run the App**:
   - Select your target device or emulator.
   - Click **Run** (`Shift + F10`) or execute via Gradle:
     ```bash
     ./gradlew assembleDebug
     ```

---

## 🔒 Permissions & Privacy
- `android.permission.CAMERA`: Used exclusively for the live gate QR code scanner.
- `android.permission.SCHEDULE_EXACT_ALARM`: Used for the daily 4:10 PM automated departmental report scheduling.
- `android.permission.POST_NOTIFICATIONS`: Delivers local alerts when outpasses are approved or dispatched.
- **Zero Mock Storage Policy**: Uses zero-permission Android Photo Picker for profile photos and local Room database for student data protection.

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
