

<div align="center">

<img src="./design_assets/icon.png" width="38" height="38" alt="软件图标">

# NuBook Minimalist Ledger

A modern minimalist Android accounting application developed based on Clean Architecture and MVVM architecture.

---
</div>

<div align="center">
    
[中文](README.zh.md) | English | [日本語](README.ja.md)

</div>

## Direct App Download Experience

You can directly download the latest compiled and released `NuBook_v2.1.0_universal.apk` installation package from the **[Releases]** panel on the right side of this GitHub repository. Download the APK to your phone and install it directly without any configuration to experience it.

## Why Choose NuBook? (Comparison with Mainstream Accounting Apps)

Accounting software on the market often becomes increasingly bloated, filled with advertisements, social circles, and mandatory cloud sync restrictions. For those who simply want to record personal financial transactions, NuBook achieves extreme simplification and fundamental innovation across four core dimensions:

### 1. Absolute Data Sovereignty & Multi-Layer Fault-Tolerant Import
*   **Pain Points of Competitors**: Data hostage. Many applications encrypt ledgers within the app sandbox, export them into hard-to-parse proprietary formats, or even package "Export to Excel/CSV" as a paid VIP feature.
*   **NuBook's Disruption**: 100% free flow of data. With just one click, you can instantly generate JSON abstract tree dictionaries, JSONL line-by-line formats, or human-readable standard CSV tables, all saved directly in the system-public `Documents/NuBook` directory. Most powerfully, we have implemented a **Three-Stage Intelligent Detection System** within the import module. Scenario example: You can arbitrarily modify fields in the exported CSV on your computer, or mix in JSON fragments using Notepad and transfer them back to your phone. The system engine will automatically filter out corrupted or invalid rows and forcibly extract standard data to write into the Room database for restoration.

### 2. Real-time Algebraic Parsing, Say Goodbye to Popup Calculators
*   **Pain Points of Competitors**: To record "groceries $38, plus two bottles of water for $4, minus a $2 store refund," you have to open a separate calculator, calculate, and then move or enter the result manually.
*   **NuBook's Disruption**: Input is not limited to just numbers. The underlying input layer integrates the powerful `mXparser` library. Scenario example: When the above amount fragments come to mind, you can simply type `38+(2*2)-2` freely in the input field. The system will parse it in real-time, stripping it into an Abstract Syntax Tree (AST), resolving the floating-point result, and storing it. Experience the pleasure of advanced digital entry for geeks.

### 3. Deep Pixel-Level Theme Adaptation, Say Goodbye to Templates
*   **Pain Points of Competitors**: Interfaces offer a dry "Light Mode/Dark Mode" or require purchasing skins. Even with skins, there are often many unadapted corners and heavy drop shadows.
*   **NuBook's Disruption**: Embraces an ultra-flat design philosophy (banishing card elevation shadows across the app, with global `elevation="0dp"`), complemented by a globally embedded `ColorEngine`. Once you select a color as the primary theme, the system will, through underlying lifecycle hooks, meticulously recolor elements like the three icons on the system navigation bar, the subtle mask strip on the left of each entry in the bill list, and even the sector proportions on the statistical pie charts—all rendered in that main color pixel by pixel. This provides a seamlessly integrated immersive aesthetic.

### 4. Data Firewall: Vacuum Runtime Environment
*   **Pain Points of Competitors**: Registration requires a phone number; background services frequently wake up to push financial product courses or even access your location and photo gallery.
*   **NuBook's Disruption**: Absolute zero disruption and a vacuum operation mechanism. We have not only completely abandoned background daemons and alarm wake-ups (AlarmManager) but also have not requested even a single line of internet (INTERNET) permission in the application's underlying configuration file, `AndroidManifest.xml`. Without an internet connection base, NuBook can only, and will only, interact securely with the local SQLite (Room) database. No social features, no splash screens; this is simply a place for recording numbers.

## Tech Stack & Architecture

This project follows the latest Android development architecture standards recommended by Google:
- Language: Kotlin 1.9+
- Asynchronous Framework: Kotlin Coroutines + Flow
- Lifecycle Components: Jetpack Lifecycle, ViewModel, LiveData
- Persistence: Jetpack Room
- UI Component Library: Material Components for Android (lightly customized)
- Charting: MPAndroidChart (shadow removal with soft color fills)
- Dependency Management / Build Engine: Gradle (KTS) / AGP 8.2.2

## Project Structure Guide

```text
com.nubook/
├── NuBookApplication.kt  # Global application entry declaration
├── data/
│   ├── export/       # Three-stage intelligent data import & multi-protocol export parsing engine
│   └── local/        # Room Database, Entity table structures, and Dao retrieval interfaces
├── domain/           
│   └── usecase/      # Clean business logic processing units (e.g., core calculation, filtering, statistics use cases)
└── ui/
    ├── base/         # BaseActivity (provides theme-level override engine)
    ├── home/         # Home aggregation panel and multi-bill feed
    ├── input/        # Accounting entry interface and mXparser math algorithm bridge
    ├── ledger/       # Detailed transaction history for a specific ledger
    ├── search/       # Flow-based global keyword real-time filtering
    ├── settings/     # Main entry for color engine switching
    ├── statistics/   # MPAndroidChart presentation logic
    └── theme/        # Core renderer for the ColorEngine
```
## Build & Run
Environment Requirements: JDK 17+, Android Studio Iguana / Jellyfish (or later).

Clone this repository, download the ZIP, or pull it locally.

Open the project in Android Studio.

If the Android SDK is not configured, the necessary configuration will be generated in the local.properties file.

Wait for Gradle Sync to finish resolving dependencies, then click Run 'app' (the green triangle) to compile and run on a physical device or emulator (requires Android API >= 24).

## License & Open Source
This project is open source and complies with the MIT License. You are free to use and explore the source code for further development and modification.
