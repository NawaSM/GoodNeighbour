GoodNeighbour - Volunteer Connection Platform

GoodNeighbour is an Android application that connects volunteers with organizations offering volunteer opportunities in Penang. The app facilitates easy discovery, application, and management of volunteer opportunities.


Prerequisites

- Android Studio (latest version)
- Minimum SDK: API 24 (Android 7.0)
- Java Development Kit (JDK) 8 or newer
- Android device or emulator running Android 7.0 or higher

Setup

1. Clone the repository

2. Open Android Studio:
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned repository
   - Click "OK" to open the project

3. Firebase Setup:
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project or use an existing one
   - Add an Android app to the project
   - Download the `google-services.json` file
   - Place `google-services.json` in the `app/` directory
   - Set up Realtime Database:
     - Go to Build → Realtime Database
     - Create database
     - Choose location (asia-southeast1)
     - Start in test mode

4. Build the project:
   - Click "Build → Make Project" or press Ctrl+F9 (Windows/Linux) or Cmd+F9 (macOS)
   - Wait for the build to complete

Running the App

1. Open Android Virtual Device (AVD) Manager in Android Studio
2. Create a new virtual device if none exists
3. Select a device definition (e.g., Pixel 4)
4. Select a system image (minimum API 24)
5. Click "Run" (▶) or press Shift+F10 (Windows/Linux) or Control+R (macOS)

Testing the App

1. Register an Account:
   - Launch the app
   - Click "Register"
   - Fill in required information

2. Organization Setup:
   - Go to Profile tab
   - Click "Edit Profile"
   - Add organization name
   - Upload profile picture

3. Create an Opportunity:
   - Go to Opportunities tab
   - Click "+" button
   - Fill in opportunity details
   - Upload opportunity image
   - Click "Create"

4. Apply for Opportunities:
   - Browse opportunities
   - Click on an opportunity
   - Click "Apply"
   - Submit application

Troubleshooting

- Build Errors
  - Clean Project (Build → Clean Project)
  - Rebuild Project (Build → Rebuild Project)
  - Sync project with Gradle files

- Image Upload Issues
  - Verify internet connection
  - Check Firebase Realtime Database rules
  - Verify image size is reasonable

- Authentication Issues
  - Enable Email/Password authentication in Firebase Console
  - Check email format
  - Verify password meets requirements


Notes

- The app requires an active internet connection
- Profile pictures and opportunity images are stored in Firebase Realtime Database
- Organization names can only be changed once every 30 days
- All dates and times are in the device's local timezone

Known Issues

- Image upload may take some time depending on internet speed
