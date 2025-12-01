# DriveWise Fleet - Firebase Configuration Guide

## Firebase Setup Instructions

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" or select an existing project
3. Follow the setup wizard to create your project

### Step 2: Add Android App to Firebase

1. In Firebase Console, click on the Android icon to add an Android app
2. Enter your package name: `com.example.drivewise`
3. (Optional) Enter app nickname: "DriveWise Fleet"
4. (Optional) Add SHA-1 certificate for debug/release builds
5. Click "Register app"

### Step 3: Download google-services.json

1. After registering the app, Firebase will provide a `google-services.json` file
2. **IMPORTANT**: Download this file and replace the placeholder file at:
   ```
   app/google-services.json
   ```
3. The current `google-services.json` file is a **PLACEHOLDER** with dummy values
4. You **MUST** replace it with your actual Firebase configuration file

### Step 4: Enable Firebase Authentication

1. In Firebase Console, navigate to "Authentication" → "Sign-in method"
2. Enable "Email/Password" authentication
3. Click "Save"

### Step 5: Create Firestore Database

1. In Firebase Console, navigate to "Firestore Database"
2. Click "Create database"
3. Choose "Start in test mode" (or production mode with security rules)
4. Select your preferred region
5. Click "Enable"

### Step 6: Set Up Firestore Security Rules (Optional but Recommended)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection - users can only read/write their own document
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Posts collection
    match /posts/{postId} {
      // Anyone authenticated can read posts
      allow read: if request.auth != null;
      // Only admins can create/update/delete posts
      allow create, update, delete: if request.auth != null && 
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
  }
}
```

### Step 7: Build and Run

1. After replacing `google-services.json`, sync your Gradle project
2. Build and run the app:
   ```bash
   ./gradlew clean assembleDebug
   ```

### Testing Accounts

Create test accounts with different roles:

1. **Admin Account**:
   - Register with role "Admin"
   - Can create, view posts
   - Access Admin Home screen

2. **Client Account**:
   - Register with role "Client"
   - Can view posts only
   - Access Client Home screen

## Troubleshooting

### Common Issues

1. **"Default FirebaseApp is not initialized"**
   - Ensure `google-services.json` is in the correct location
   - Sync Gradle project
   - Clean and rebuild

2. **Authentication fails**
   - Verify Email/Password is enabled in Firebase Console
   - Check internet connection
   - Verify API keys in `google-services.json`

3. **Firestore permission denied**
   - Update Firestore security rules
   - Ensure user is authenticated
   - Check user role in Firestore

### Firebase Console Links

- Project Overview: https://console.firebase.google.com/project/YOUR_PROJECT_ID
- Authentication: https://console.firebase.google.com/project/YOUR_PROJECT_ID/authentication
- Firestore: https://console.firebase.google.com/project/YOUR_PROJECT_ID/firestore

## Architecture Overview

### Data Models

```kotlin
// User model stored in Firestore users/{uid}
data class User(
    val uid: String = "",
    val email: String = "",
    val role: Role = Role.CLIENT // "admin" or "client"
)

// Post model stored in Firestore posts/{postId}
data class Post(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "" // uid of the admin
)
```

### Firestore Collections

1. **users**: Stores user profiles with roles
   - Document ID: Firebase Auth UID
   - Fields: uid, email, role

2. **posts**: Stores posts/announcements
   - Document ID: Auto-generated
   - Fields: id, title, description, imageUrl, createdAt, createdBy

## Stage 1 Features Checklist

- [x] Firebase Authentication (Email/Password)
- [x] Role-based access (Admin/Client)
- [x] User registration with role selection
- [x] Login with role validation
- [x] Admin can create posts
- [x] Admin/Client can view posts list
- [x] Post details screen
- [x] XML-based UI layouts
- [x] MVVM architecture with repositories
- [x] Coroutines for async operations
- [x] Real-time Firestore listeners

## Next Steps

After Stage 1 is working:
- Add profile management
- Add car/fleet management
- Add booking system
- Add image upload for posts
- Add push notifications
- Add offline support with Room database

