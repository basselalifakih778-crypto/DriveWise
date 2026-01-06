# DriveWise

Android car rental app backed by Firebase (Auth + Firestore + Storage). It supports two roles:

- **Client**: browse cars, manage profile + upload documents for verification, request bookings, upload pickup/return photos.
- **Admin**: manage cars, manage bookings, review customer profiles + documents, and verify customers.

> UI: **XML layouts + ViewBinding** (Compose is disabled).

---

## Tech stack

- Android (Kotlin)
- Gradle + Android Gradle Plugin
- Firebase
  - Authentication
  - Firestore
  - Storage

---

## Repository structure

- `app/` – Android application module
  - `app/google-services.json` – Firebase Android config (must match your Firebase project)
- `firestore.rules` – Firestore security rules (publish to Firebase)
- `storage.rules` – Storage security rules (publish to Firebase)
- `firestore.indexes.json` – Firestore composite indexes (deploy to Firebase)

---

## Prerequisites

- **Android Studio** (recent stable)
- **JDK 11** (project targets Java 11)
- A **Firebase project** with:
  - Firestore Database
  - Firebase Authentication
  - Firebase Storage
---

## Android project configuration

From `app/build.gradle.kts`:

- `compileSdk = 34`
- `minSdk = 28`
- `targetSdk = 34`
- Java/Kotlin target: **11**

---

## Firebase setup

### 1) Create / select a Firebase project

1. Go to Firebase Console
2. Create a project (or reuse an existing one)
3. Enable:
   - **Authentication** → Sign-in method: Email/Password
   - **Firestore Database**
   - **Storage**

### 2) Add an Android app in Firebase

1. Firebase Console → Project Settings → Your Apps → Add app (Android)
2. Package name **must match**:
   - `com.example.drivewise`
3. Download `google-services.json`
4. Put it here:
   - `app/google-services.json`

> This repo currently includes a `google-services.json`. If you’re using your own Firebase project, replace it.

---

## Firestore data model (collections)

This project uses these main collections:

### `users/{uid}`
Fields used by the app include:

- `uid` (string)
- `email` (string)
- `role` (string): `"admin"` or `"client"`
- `fullName` (string)
- `phone` (string)
- `address` (string)
- `licenseNumber` (string)
- `profileImageUrl` (string)
- `licenseImageUrl` (string)
- `idDocumentUrl` (string)
- `isVerified` (boolean)
- `createdAt` (number/long)

### `cars/{carId}`
Typical fields:

- `brand`, `model`, `year`
- `transmission`, `fuel`
- `dayPrice`
- `available`
- `imageUrl`
- `features` (array)
- `createdAt`

### `bookings/{bookingId}`
Typical fields:

- `carId`, `userId`
- `userName`, `userEmail`
- `carName`
- `startDate`, `endDate`
- `totalPrice`
- `status` (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED`)
- `pickupPhotos` (array of URLs)
- `returnPhotos` (array of URLs)
- `createdAt`
- `adminNotes`

---

## Security rules

### Firestore rules

The repository provides Firestore rules in:

- `firestore.rules`

Publish them in Firebase Console:

1. Firebase Console → Firestore Database → **Rules**
2. Replace the rules with the contents of `firestore.rules`
3. Click **Publish**

Important behavior:
- Users can read/update their own `users/{uid}` document
- Admin can read/update all users
- Bookings are readable by owners and admins

### Storage rules

The repository provides Storage rules in:

- `storage.rules`

Publish them in Firebase Console:

1. Firebase Console → Storage → **Rules**
2. Replace with `storage.rules`
3. Click **Publish**

Storage paths used by the app:

- `cars/*` – car images (admin write)
- `documents/{userId}/*` – license/ID uploads (user write; admin read)
- `profile/{userId}/*` – profile photos (user write; authenticated read)
- `bookings/{bookingId}/{pickup|return}/*` – booking photos (authenticated write)

---

## Firestore indexes

Some queries require composite indexes.

This repo includes a suggested `firestore.indexes.json`.

### Option A: Deploy via Firebase CLI (recommended)

1. Install Firebase CLI
2. Login
3. Deploy indexes

Example (run from repo root):

```bash
firebase login
firebase use <your-project-id>
firebase deploy --only firestore:indexes
```

> To use CLI deployment, you also typically add a `firebase.json` file. If you don’t have one yet, you can still deploy indexes via the Console when Firestore prompts you with an index creation link.

### Option B: Create indexes via Firebase Console

When the app runs a query that needs an index, Firestore will return an error with a **direct link** to create the index. Click it and create the index.

---

## Creating an admin user

Admins are identified by a `users/{uid}` document with:

- `role = "admin"`

Recommended flow:

1. Register a new account in the app
2. In Firebase Console → Firestore → `users/{uid}`
3. Set:
   - `role`: `"admin"`

After that, log in again and you’ll see the admin UI.

---

## Running the app

1. Open the repo in Android Studio
2. Let Gradle sync
3. Select an emulator or physical device
4. Run the `app` configuration

---

## Booking verification requirement

DriveWise blocks car booking for unverified clients.

Before a client can place a booking request:

- The user must have `isVerified = true` in Firestore
- Admin can toggle verification from the admin customer profiles screen

---

## Troubleshooting

### “Missing or insufficient permissions”

- Ensure Firestore rules are published (`firestore.rules`)
- Ensure your user document exists at `users/{uid}`
- Ensure admin users have `role = "admin"`

### Index required errors

- Create the composite index via the link in the error, **or** deploy `firestore.indexes.json`.

### Google Services / package mismatch

- Ensure Firebase app package name is `com.example.drivewise`
- Ensure `app/google-services.json` is from the same Firebase project

