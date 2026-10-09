# Nocturne

A Firebase-backed Android app for managing student academic life: attendance,
timetable, subjects and syllabus, notes, flashcards, previous-year papers, and
a career side that ranks internships against the student's skills, tracks
applications and builds learning plans.

**Status:** Student-side features are complete and working. The Faculty module
is future scope (see Known limitations).

---

## Tech stack

- **Language:** Java, XML layouts, Material 3
- **Build system:** Gradle (Kotlin DSL, `build.gradle.kts`)
- **compileSdk / targetSdk:** 34 (Android 14) · **minSdk:** 24 (Android 7.0)
  *(unchanged from the original README; confirm against `app/build.gradle.kts`)*
- **Backend:** Firebase, no custom server
  - Authentication (Email/Password + Google Sign-In)
  - Cloud Firestore (per-student documents, per-user security rules)
- **File storage:** Cloudinary (unsigned uploads) for notes and papers.
  Firestore stores only the file's link. This replaces the original plan to use
  Firebase Cloud Storage, which needs the Blaze billing plan.
- **AI:** Groq API (OpenAI-compatible) for flashcard generation, timetable
  extraction (text, or a vision model for scanned PDFs) and learning plans.
  Internship matching, the Life Score and CV import run on the device, with
  no model and no server.
- **Other libraries:** Firebase BoM, `play-services-auth` (Google Sign-In),
  Glide (splash GIF), Markwon (render notes), PDFBox-Android (read PDF text),
  Cloudinary Android SDK

## Getting started

1. Clone the repo and open it in Android Studio.
2. Put `google-services.json` (Firebase Console → Project Settings → Your apps →
   Android app) in `app/google-services.json`.
3. Add your Groq key to `local.properties` (get one at https://console.groq.com/keys):
```
   GROQ_API_KEY=your_key_here
   GROQ_MODEL=openai/gpt-oss-120b
   GROQ_VISION_MODEL=qwen/qwen3.8-27b
```
   Groq retires models from time to time. If a request says a model isn't
   available, change these two values; no code change is needed.
4. Cloudinary: create an **unsigned** upload preset named `studentlifeos_unsigned`.
   The cloud name is set in `StudentLifeOSApp.java`.
5. Sync Gradle, then run on an emulator or device with API 24+.

Auth providers and Firestore rules are configured in the Firebase Console, not
in code. The internship data ships inside the app (`assets/jobs.json`).

## Features

**Core**
- **Splash, Welcome, Login & SignUp:** branded GIF intro that routes by session
  state; email/password and Google Sign-In, "Remember Me", forgot-password email.
- **Navigation:** bottom bar with Home, Subjects, Career and Profile.
- **Profile:** live profile, edit profile, dark mode toggle (app-level override), log out.

**Academics**
- **Subjects & syllabus tracker:** add subjects, tick off syllabus units, see completion.
- **Notes:** per-unit notes with Markdown, file upload (Cloudinary) and a viewer.
- **Flashcards:** write your own, or generate draft cards from a unit's notes
  with the Groq model; then study them.
- **Attendance:** mark attendance by hand per subject, with an overview and a log.
- **Timetable:** add classes by hand, or upload a timetable PDF. Text PDFs are
  read on the device, and scanned or photographed ones are read with a vision
  model. You review and edit the result before it is saved.
- **Previous-year papers (PYQ):** library opened from the Home screen card.

**Career**
- **Skills profile:** add skills from an autocomplete list of 188 skills drawn
  from real internship listings, rate each Beginner / Intermediate / Advanced,
  and optionally pick a target role. Suggestions come from your subject names
  and syllabus units.
- **CV import:** pick a PDF CV, which is read on the device and matched against
  the skill catalogue and aliases. A dialog lets you untick anything you don't
  know before adding. Skills mentioned 3+ times start at Intermediate, the rest
  at Beginner.
- **Internship intelligence:** 392 cleaned listings, scored 0–100 on the
  device. Fit is the share of required skills you have, weighted by level
  (Beginner 0.5, Intermediate 0.8, Advanced 1.0), half credit for closely related
  skills, +10 for your target role. Verdicts: Apply (70+), Wait (45–69),
  Skip (<45), Avoid for listings that ask for money or whose skills don't fit
  the title. Filters: verdict, remote, stipend ₹10k+.
- **Job detail:** fit score, plain-English explanation, skills you have and lack,
  listing check, and a link to the original listing.
- **Skill gap:** ranks the skills you don't have by how many listings would move
  up a level (Skip → Wait → Apply) if you learned them.
- **Learning plans:** a Groq-written plan for a listing or a skill: ordered
  skills, a reason, three actions, a time estimate and a free resource for each,
  plus a weekend project. Plans are cached on the device.
- **Saved & applied tracker:** save any listing and move it through Saved,
  Applied, Interviewing, Offer, Rejected. The Tracker screen (button on the Jobs
  tab) lists them with status filters, job cards carry a status tag, and the app
  asks "Did you apply?" when you come back from a listing link.

**Overview**
- **Home:** profile summary, credits, today's classes, attendance, top internship
  match, Life Score and quick access cards.
- **Life Score (0–100):** attendance 40%, syllabus completion 30%, CGPA 20%,
  skills readiness 10% (average fit of your five best-matching internships).
  With no skills recorded, the total is capped at 90. A breakdown screen shows
  each component and its weight.

## Firestore data model

| Collection | Purpose |
|---|---|
| `students/{uid}` | Profile, plus the `skills` map (items, target role) and the `tracker` map (jobId → status, dates) |
| `subjects/{id}` | A student's subjects (`studentId`, name, code, credits, …) |
| `units/{id}` | Syllabus units (`studentId`, subject, title, completed) |
| `notes/{id}` | Notes and uploaded file links |
| `flashcards/{id}` | Flashcards per unit |
| `attendance/{id}` | Attendance log entries (`studentId`, subject, date, status) |
| `timetable_entries/{id}` | Weekly class entries |
| `papers/{id}` | Previous-year paper library |

The internship listings are not in Firestore. They ship with the app in
`assets/jobs.json`: listings, the skill catalogue and an alias table.

## Known limitations

- **Faculty module not built.** This version is student-only.
- **Internship data is a snapshot.** The 393 listings are bundled, and there are
  no live updates. Filters for work preference other than remote, and for
  duration, are future scope.
- **Groq key is inside the APK** (read from `BuildConfig`), so it can be
  extracted. Fine for personal and demo use; for wider release, call Groq through
  a small backend such as a Cloud Function that holds the key.
- **Cloudinary cloud name is hardcoded** in `StudentLifeOSApp.java`, and uploads
  are unsigned.
- **CV import reads text PDFs only.** A scanned or photographed CV shows a
  message asking for a PDF exported from Word or Docs. Matching is
  keyword-based, so the review dialog is where false matches get removed.
- **Soft skills and projects are not tracked.** Growth graphs, projects and
  certifications are planned additions to the Life Score.
- **No reminders** for tracked applications.
- **No automated UI tests.** The scoring and tracker logic is plain Java with no
  Android dependencies, so it can be unit-tested directly.

## Project structure

```
StudentLifeOS/
├── app/
│   ├── google-services.json            ← Firebase config goes here
│   ├── build.gradle.kts
│   └── src/main/
│       ├── assets/jobs.json            (listings, skill catalogue, aliases)
│       ├── java/com/example/studentlifeos/
│       │   ├── App & auth      StudentLifeOSApp, Splash/Welcome/Login/SignUp/Dashboard/Main
│       │   ├── Home & profile  HomeFragment, ProfileFragment, EditProfileActivity, NotificationsActivity
│       │   ├── Academics       Subjects*/Syllabus*/Notes*/Flashcard*/Attendance*/Timetable*/Papers*
│       │   ├── Files & AI      CloudinaryUploader, PdfTextExtractor, PdfImageRenderer,
│       │   │                   GroqApiClient, LearningPlanClient, LearningPlan(Prompt)
│       │   ├── Skills          SkillsProfile(+Activity/Store), StudentSkill, SkillNormalizer,
│       │   │                   SkillSuggester, CvSkillExtractor, SkillGapAnalyzer, SkillGapActivity
│       │   ├── Matching        Job, JobRepository, Matching, JobMatcher, MatchResult, MatchText,
│       │   │                   JobsFragment, JobMatchAdapter, JobDetailActivity
│       │   ├── Tracker         TrackedJob, JobTracker, TrackerStore, TrackerUi, TrackerActivity
│       │   └── Life Score      LifeScoreCalculator, LifeScoreUtil, LifeScoreActivity,
│       │                       SkillsReadiness, SkillsScoreLoader, ScoreRingView
│       └── res/
│           ├── layout/                 (activity_*.xml, fragment_*.xml, item_*.xml)
│           ├── drawable/               (custom shapes, icons)
│           └── values/ , values-night/ (colors, strings, themes: light/dark)
├── build.gradle.kts                    (project-level)
└── settings.gradle.kts
```
