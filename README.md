# Nocturne

A Firebase-backed Android app for managing student academic life — profile,
subjects, syllabus tracking, notes, and previous-year papers.

**Status:** Phase 1 in progress. Auth, navigation, Dashboard, and Profile are
functional. Subjects/Syllabus/Notes/Papers screens are not yet built (data
model exists; UI is pending).

---

## Tech stack

- **Language:** Java
- **Build system:** Gradle (Kotlin DSL — `build.gradle.kts`)
- **compileSdk / targetSdk:** 34 (Android 14)
- **minSdk:** 24 (Android 7.0)
- **Backend:** Firebase
  - Authentication (Email/Password + Google Sign-In)
  - Firestore (Standard edition)
  - Cloud Storage — *not yet enabled* (requires Firebase's Blaze billing
    plan; deferred, see Known Limitations below)
- **Key libraries:** Firebase BoM `33.1.2`, `play-services-auth:21.2.0`
  (Google Sign-In), Glide (splash screen GIF rendering)

## Getting started

1. Clone the repo and open it in Android Studio.
2. Get `google-services.json` from Firebase Console → Project Settings →
   Your apps → Android app, and place it in `app/google-services.json`
   (see **Project structure** below for the exact spot).
3. Sync Gradle.
4. Run on an emulator or device with API 24+.

No `.env` or extra config needed beyond `google-services.json` — everything
else (Auth providers, Firestore rules) is configured on the Firebase Console
side, not in code.

## Features implemented so far

- **Splash screen** — branded GIF intro, then routes based on session state:
  - Logged in → straight to Dashboard
  - First-ever launch → Welcome screen
  - Returning, logged-out → straight to Login
- **Welcome screen** — first-launch-only entry point into Login/SignUp
- **Login & SignUp** — email/password and Google Sign-In, password
  visibility toggle, "Remember Me", "Forgot Password" (Firebase's built-in
  reset email flow)
- **Dashboard** — bottom navigation (Home / Profile / Notes / Papers)
  - Home: shows live profile data (name, semester, CGPA) and a computed
    Credits total (summed from enrolled subjects); "Today's Classes" is
    currently a placeholder list
  - Profile: live Firestore-backed profile view, dark mode toggle
    (app-level override, not just system setting), log out
  - Notes / Papers tabs: not yet built (placeholders)

## Firestore data model (current)

| Collection | Purpose |
|---|---|
| `users/{uid}` | Login identity + student profile |
| `subjects/{id}` | Subject catalog (code, name, credits, faculty, semester) |
| `subjects/{id}/syllabusUnits/{id}` | Syllabus tracker (subcollection) |
| `enrollments/{id}` | Student ↔ Subject join table |
| `notes/{id}` | Notes repository |
| `papers/{id}` | Previous-year paper library |

A richer, denormalized dataset (`nocturne_dataset`) has been prepared for a
future phase and is not yet imported — see the team's notes on that
migration before assuming this schema is final.

## Known limitations

- **Cloud Storage is not enabled** — Firebase now requires linking a Blaze
  (pay-as-you-go) billing plan even for Storage's free tier. `notes`/`papers`
  currently use placeholder file URLs rather than real uploads.
- **Profile fields are mostly empty for real users** — sign-up only collects
  name, email, and password; roll number/CGPA/phone/etc. have no input
  screen yet.
- **No attendance data model** — Home's Attendance stat is a placeholder;
  there is no attendance-tracking collection yet.
- **No automated test suite** for the Android app. A synthetic data seed
  script exists (`firebase-admin` + `@faker-js/faker`) for exercising the
  schema against the Firebase Local Emulator Suite.

## Project structure (where things live)

```
StudentLifeOS/
├── app/
│   ├── google-services.json          ← Firebase config goes here
│   ├── build.gradle.kts
│   └── src/main/
│       ├── java/com/example/studentlifeos/
│       │   ├── StudentLifeOSApp.java      (Application subclass — theme init)
│       │   ├── SplashActivity.java
│       │   ├── WelcomeActivity.java
│       │   ├── LoginActivity.java
│       │   ├── SignUpActivity.java
│       │   ├── DashboardActivity.java
│       │   ├── HomeFragment.java
│       │   └── ProfileFragment.java
│       └── res/
│           ├── layout/                    (activity_*.xml, fragment_*.xml)
│           ├── drawable/                  (custom shapes, icons)
│           └── values/ , values-night/    (colors, strings, themes — light/dark)
├── build.gradle.kts                  (project-level)
└── settings.gradle.kts
```
