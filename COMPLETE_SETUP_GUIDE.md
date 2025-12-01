# DriveWise Fleet - Complete Setup Guide

## ✅ What's Already Done

### 1. Project Configuration ✅
- Firebase dependencies added
- Google Services plugin configured
- ViewBinding enabled
- All necessary Android libraries included
- Permissions added to AndroidManifest.xml

### 2. Architecture Implemented ✅
- **MVVM Pattern**: ViewModels, Repositories, State management
- **Domain Layer**: User, Post, Role models with Firestore compatibility
- **Data Layer**: Firebase implementations for Auth and Posts
- **UI State**: Comprehensive state classes for all screens
- **Navigation**: Route definitions ready

### 3. XML Layouts Created ✅
All Material Design 3 layouts are ready:
- Login screen (email, password, role toggle)
- Register screen (with confirm password)
- Admin Home (with FAB for create post)
- Client Home (read-only posts)
- Create Post screen
- Post Details screen
- Post RecyclerView item layout

### 4. Firebase Integration ✅
- FirebaseAuthRepository with role-based auth
- FirebasePostsRepository with real-time updates
- Proper error handling
- Coroutines integration

## 🔧 Setup Instructions

### Step 1: Replace Firebase Configuration File

**CRITICAL**: Replace the placeholder `google-services.json`

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select existing one
3. Click "Add app" → Android icon
4. Package name: `com.example.drivewise`
5. Download `google-services.json`
6. Replace the file at: `app/google-services.json`

### Step 2: Enable Firebase Services

**In Firebase Console:**

1. **Authentication**:
   - Go to Authentication → Sign-in method
   - Enable "Email/Password"
   - Save

2. **Firestore Database**:
   - Go to Firestore Database
   - Click "Create database"
   - Choose "Test mode" (for development) or use these rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    match /posts/{postId} {
      allow read: if request.auth != null;
      allow create, update, delete: if request.auth != null && 
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
  }
}
```

### Step 3: Build the Project

```powershell
cd C:\Users\basse_4ixzt7j\OneDrive\Desktop\DriveWise\DriveWise
.\gradlew clean build
```

## 📱 Current State

### ✅ What Works
1. All domain models are defined
2. Repositories are implemented
3. ViewModels handle all business logic
4. XML layouts are complete and ready
5. Firebase integration is configured
6. State management is robust
7. Error handling is comprehensive

### ⏳ What Needs Activities
You need to create 6 Activity classes to connect the XML layouts with ViewModels:

1. **LoginActivity**
2. **RegisterActivity**
3. **AdminHomeActivity**
4. **ClientHomeActivity**
5. **CreatePostActivity**
6. **PostDetailsActivity**

Plus:
- **PostsAdapter** (RecyclerView adapter)
- Update **MainActivity** to handle initial routing

## 📝 Activity Implementation Template

Here's a template for implementing activities:

```kotlin
// Example: LoginActivity.kt
class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel (you'll need to create repository instances)
        val authRepository = FirebaseAuthRepository(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance()
        )
        viewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(authRepository)
        )[AuthViewModel::class.java]
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // Handle role selection
        binding.chipGroupRole.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedRole = when(checkedIds.firstOrNull()) {
                binding.chipAdmin.id -> Role.ADMIN
                else -> Role.CLIENT
            }
            viewModel.onRoleSelected(selectedRole)
        }
        
        // Handle login button
        binding.btnLogin.setOnClickListener {
            viewModel.onEmailChanged(binding.etEmail.text.toString())
            viewModel.onPasswordChanged(binding.etPassword.text.toString())
            viewModel.login()
        }
        
        // Navigate to register
        binding.btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun observeViewModel() {
        // Observe state
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.tvError.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE
                binding.tvError.text = state.errorMessage
            }
        }
        
        // Observe events
        lifecycleScope.launch {
            viewModel.event.collect { event ->
                when (event) {
                    is AuthEvent.LoggedIn -> {
                        when (event.user.getRoleEnum()) {
                            Role.ADMIN -> {
                                startActivity(Intent(this@LoginActivity, AdminHomeActivity::class.java))
                                finish()
                            }
                            Role.CLIENT -> {
                                startActivity(Intent(this@LoginActivity, ClientHomeActivity::class.java))
                                finish()
                            }
                        }
                    }
                    AuthEvent.RoleMismatch -> {
                        Toast.makeText(this@LoginActivity, "Role mismatch", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
}
```

## 🏗️ Architecture Diagram

```
┌─────────────────────────────────────────┐
│           UI Layer (Activities)          │
│  - LoginActivity      - AdminHomeActivity│
│  - RegisterActivity   - ClientHomeActivity│
│  - CreatePostActivity - PostDetailsActivity│
└─────────────────┬───────────────────────┘
                  │ Uses
┌─────────────────┴───────────────────────┐
│        ViewModels (State Management)     │
│  - AuthViewModel                         │
│  - PostsViewModel                        │
└─────────────────┬───────────────────────┘
                  │ Calls
┌─────────────────┴───────────────────────┐
│      Repositories (Business Logic)       │
│  - FirebaseAuthRepository                │
│  - FirebasePostsRepository               │
└─────────────────┬───────────────────────┘
                  │ Uses
┌─────────────────┴───────────────────────┐
│          Firebase Services               │
│  - Firebase Auth                         │
│  - Cloud Firestore                       │
└─────────────────────────────────────────┘
```

## 📊 Data Flow

### Registration Flow
```
User fills form → ViewModel validates → Repository creates auth user
→ Repository creates Firestore user doc → Success/Error event
```

### Login Flow
```
User enters credentials → ViewModel calls login → Repository authenticates
→ Repository fetches user doc → Check role match → Navigate or error
```

### Posts Flow
```
ViewModel observes posts → Repository listens to Firestore
→ Real-time updates → UI updates automatically
```

### Create Post Flow (Admin only)
```
Admin fills form → ViewModel validates → Repository creates Firestore doc
→ Success → Navigate back → List auto-updates via listener
```

## 🎯 Testing Checklist

Once activities are implemented, test:

1. ✅ Register as Admin
2. ✅ Logout
3. ✅ Register as Client
4. ✅ Try login as Admin with Client account (should fail)
5. ✅ Try login as Client with Admin account (should fail)
6. ✅ Login correctly as Admin
7. ✅ Create a post as Admin
8. ✅ View posts list as Admin
9. ✅ Logout
10. ✅ Login as Client
11. ✅ View posts list as Client
12. ✅ Verify FAB not visible for Client
13. ✅ Click post to see details
14. ✅ Test SwipeRefreshLayout

## 🔐 Security Notes

1. **Never commit real `google-services.json`** to public repos
2. Always use Firestore security rules in production
3. Validate user roles server-side (Firestore rules do this)
4. Use HTTPS only (Firebase handles this)

## 📚 Documentation Files

- **FIREBASE_SETUP.md**: Detailed Firebase Console setup
- **IMPLEMENTATION_SUMMARY.md**: Complete feature checklist
- **This file**: Quick start guide

## 🚀 Quick Commands

```powershell
# Clean build
.\gradlew clean

# Build debug
.\gradlew assembleDebug

# Install on connected device
.\gradlew installDebug

# Check for errors
.\gradlew check
```

## 💡 Tips

1. Use **ViewBinding** instead of findViewById
2. Use **lifecycleScope** for coroutines in activities
3. Use **ViewModelProvider** with custom factory if needed
4. Remember to call `viewModel.clearEvent()` after handling events
5. Always check Firebase Console for auth/database issues

## 🆘 Troubleshooting

### App crashes on start
- Check if google-services.json is the real file
- Verify Firebase Auth is enabled
- Check Logcat for specific error

### Login fails
- Verify email/password in Firebase Console → Authentication → Users
- Check internet connection
- Verify Firestore rules allow read on users collection

### Posts don't appear
- Check Firestore console → posts collection
- Verify user is authenticated
- Check Firestore rules allow read on posts

### Role mismatch not detected
- Verify user document in Firestore has correct "role" field
- Check it's lowercase "admin" or "client"

## 📞 Next Steps

1. Create the 6 Activity classes using the template above
2. Create PostsAdapter for RecyclerView
3. Update MainActivity for initial routing
4. Replace google-services.json with real config
5. Test all flows
6. Deploy and celebrate! 🎉

---

**Ready to Code!** All the hard architectural work is done. Just need to wire up the activities. Good luck! 🚀

