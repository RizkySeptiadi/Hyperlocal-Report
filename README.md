# HyperLocal Report

HyperLocal Report is an Android application designed to bridge the gap between citizens (Warga) and local authorities (Admins at the RT/RW and Kelurahan levels). The app empowers citizens to easily report local infrastructure issues, public facility damages, and other community concerns, while providing admins with a streamlined dashboard to manage and resolve these reports.

## Features

*   **Role-Based Access Control**: Different views and capabilities for Warga, Admin RT/RW, and Admin Kelurahan.
*   **Real-Time Reporting**: Submit reports with photos, descriptions, categories, and precise location data.
*   **Interactive Maps**: View reports dynamically on an integrated map to easily spot problem areas.
*   **Upvoting System**: Citizens can upvote issues they also experience, helping admins prioritize the most pressing problems.
*   **Admin Dashboard**: A comprehensive dashboard featuring statistical cards (New, Verified, Processing, Done) to track the status of all community reports.
*   **Global Visibility**: Unrestricted access to reports ensuring transparency and broader awareness across areas.

## Tech Stack

*   **Language**: Kotlin
*   **Architecture**: MVVM (Model-View-ViewModel) with Coroutines & StateFlow
*   **Backend & Database**: Firebase Authentication, Cloud Firestore, Firebase Storage
*   **UI Components**: Material Design Components, XML Layouts, RecyclerView, ViewBinding
*   **Maps**: Google Maps SDK / Location Services

## Releases

### v1.0.0 - Initial Global Release
*   **Feature**: Complete Warga and Admin reporting flow implemented.
*   **Feature**: Added interactive statistical cards on the Admin Dashboard for quick overview of report statuses (Baru, Terverifikasi, Diproses, Selesai).
*   **Update**: Removed `area_id` restrictions across the app. Reports are now globally visible on the dashboard and map without regional limitations, ensuring transparency and comprehensive data access for all users.
*   **Bug Fix**: Resolved `NullPointerException` crashes related to lifecycle management during role switching.
*   **Bug Fix**: Corrected data binding references in `AdminDashboardFragment` and fixed compilation errors in view models.

## Getting Started

1.  Clone the repository.
2.  Open the project in **Android Studio**.
3.  Ensure you have added your `google-services.json` file to the `app/` directory to connect to your Firebase project.
4.  Build and run the app on an emulator or physical Android device.
