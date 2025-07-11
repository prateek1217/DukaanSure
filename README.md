
# DukaanSure

DukaanSure is a modern Android app for shop management, built with Jetpack Compose and Firebase. It empowers small business owners and their staff to efficiently manage inventory, sales, and team members with a clean, intuitive interface.

## Features

### Authentication & Role-Based Dashboards
- **Owner Login:** Email/password authentication.
- **Staff Login:** Shop code and staff ID authentication.
- **Role-Based Navigation:** Owners and staff see separate dashboards and features.
- **Secure Logout:** With confirmation dialog and proper Firebase sign-out.

### Stock Management
- **View, Add, Edit, Delete Stocks:** Manage inventory in real time.
- **Autocomplete Search:** Quickly find and select products.
- **Confirmation Dialogs:** Prevent accidental deletions.
- **Stock History:** View all sales for a product, including customer, quantity, date/time, and who sold it.

### Sales Management
- **Sell Products:** Owners and staff can record sales, with real-time stock updates.
- **Sales History:** View all sales, filterable by date range.
- **WhatsApp Integration:** Share sales details directly via WhatsApp.

### Staff Management (Owner Only)
- **View Staff List:** See all staff and their active/inactive status.
- **Toggle Staff Status:** Activate or deactivate staff accounts.
- **Permissions:** Only active staff can edit stocks; staff cannot change their own status.

### UI/UX
- **Jetpack Compose UI:** Fast, modern, and responsive.
- **Tab Navigation:** Switch between Stocks, Staff, Sell Product, and Sales History.
- **State Reset:** Switching tabs resets search and selection for clarity.
- **Material 3 Theming:** Light and dark mode support.

### Robustness
- **Real-Time Firestore Listeners:** Instant updates for stocks, sales, and staff.
- **Error Handling:** User-friendly error messages and loading indicators.



## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android device or emulator (API 24+)
- Firebase project (Firestore, Authentication enabled)

### Setup

1. **Clone the repository:**
   ```sh
   git clone https://github.com/yourusername/dukaansure.git
   cd dukaansure
   ```

2. **Firebase Configuration:**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/).
   - Enable **Email/Password** and **Anonymous** authentication.
   - Enable **Cloud Firestore**.
   - Download `google-services.json` from your Firebase project and place it in `app/`.

3. **Build the project:**
   - Open in Android Studio.
   - Let Gradle sync and download dependencies.

4. **Run the app:**
   - Select a device or emulator.
   - Click **Run**.

## Tech Stack

- **Kotlin** & **Jetpack Compose** for UI
- **Firebase Auth** & **Firestore** for backend
- **Material 3** for theming
- **Accompanist** for system UI control
- **Coroutines** for async operations

## Project Structure

```
app/
  └── src/
      └── main/
          ├── java/com/example/dukaaan/
          │   ├── MainActivity.kt
          │   └── ui/
          │       ├── NavGraph.kt
          │       ├── OwnerDashboard.kt
          │       ├── StaffDashboard.kt
          │       ├── LoginScreen.kt
          │       └── ... (other UI components)
          └── res/
              ├── layout/
              ├── values/
              └── ... (icons, themes)
```

## Firestore Data Model

- **shops**: `{ shop_id, shop_code, owner_id, ... }`
- **staff**: `{ staff_id, shop_id, name, active_status, last_login, ... }`
- **stocks**: `{ stock_id, shop_id, name, quantity, ... }`
- **sales**: `{ sale_id, shop_id, stock_id, customer_name, quantity, datetime, sold_by, ... }`

## Customization

- Update app name and branding in `res/values/strings.xml` and launcher icons.
- Adjust theme in `ui/theme/Theme.kt`.

## Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.



---

**DukaanSure** – Shop management made simple, fast, and reliable.

Made with ❤️ by Prateek Khandelwal
---
