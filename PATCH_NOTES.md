# Cite Circle — Release Patch Notes & Changelog

All notable changes, configuration updates, and feature additions for the **Cite Circle** academic collaboration platform are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.3] — 2026-09-20 (Build 3)

### 🚀 Highlights
This release overhauls the researcher profile into a modern, fully functional **LinkedIn-style academic profile interface**. It fixes previous alignment bugs, adds working image pickers for cover and avatar, persistent profile editing via DataStore, live interactive skill endorsements, private analytics insights, and structured academic appointment timelines.

### 💼 LinkedIn-Style Academic Profile Overhaul
- **Authentic LinkedIn Architecture**: Redesigned layout featuring an overlapping left-aligned circular avatar (106dp with 3.5dp surface ring border), 140dp gradient cover banner, and prominent scholar name, headline, institution, and location.
- **Working Photo Pickers**: Integrated Android photo selection (`ActivityResultContracts.GetContent()`) with Coil `AsyncImage` for both avatar and cover photos, persisting URI changes directly into `UserSessionManager`.
- **Interactive 'Edit Intro' Modal**: Built a full-screen editing sheet allowing scholars to update their display name, professional headline, university/institution, research domain, location, and open-to status in real time.
- **'Contact Info' Sheet**: One-tap modal revealing the scholar's verified email, personal/lab website, and ORCID identifier with built-in clipboard copying.
- **'Open To' Sheet**: Added LinkedIn-style status badges allowing researchers to broadcast availability for Research Collaborations, Peer Review, or Postdoc / Lab Openings.
- **Private Analytics Card**: Dedicated 'Analytics (Private to you)' dashboard displaying live counts for profile views, search appearances, and post impressions.
- **Interactive Skill Endorsements**: Dynamic skills list (Deep Learning, Quantum Computing, Distributed Systems, Peer Review Ethics) with real-time upvoting and endorsement count increments.
- **Academic Timeline Cards**: Structured sections for Experience (Stanford AI Lab, MIT CSAIL, DeepMind) and Education (MIT Ph.D., UC Berkeley B.S.).
- **Activity Filter Tabs**: LinkedIn-style activity feed tabs ('Posts', 'Preprints', 'Reviews', 'Figures') linked directly to live manuscripts and reviews.
- **Cleaned Up Architecture**: Modularized profile into its own dedicated `ProfileScreen.kt`, reducing `MainActivity.kt` by 583 lines.



## [1.2.0] — 2026-09-20 (Build 2)

### 🚀 Highlights
This major update integrates Google Gemini AI with real-time academic search grounding, live multi-client WebSocket notification badges, and a smart in-app APK version checker with automated download capabilities.

### 🤖 Gemini AI Research Assistant
- **Upgraded to 2026 Models**: Set `gemini-2.5-flash` as default high-speed intelligence model, with toggleable chips for `gemini-3.5-flash` (complex reasoning & synthesis) and `gemini-3.1-flash-lite-preview` (instant summarization).
- **Live Google Search Grounding**: Enabled dynamic web search grounding via Gemini API to ground answers in real-time academic literature, verifying claims against recent publications and preprints.
- **Multimodal Vision Analysis**: Added full camera capture and gallery upload support for diagrams, scientific plots, mathematical notation, and manuscript excerpts.
- **Resilient Network Retry**: Implemented automatic 3-attempt exponential backoff retry for transient `503 Service Unavailable` and `429 Resource Exhausted` API spikes.
- **Manual Retry Action**: Added an interactive "Retry" button on failed message bubbles, allowing one-tap retry without re-typing lengthy academic prompts.
- **Role-Alternation Sync**: Resolved multi-turn desynchronization where failed network requests previously corrupted the user/model message sequence.
- **Hardware Bitmap Safety**: Replaced hardware bitmap compression with `Bitmap.Config.ARGB_8888` and `ALLOCATOR_SOFTWARE` to prevent native Skia memory crashes on Android 9+ devices.
- **Auto-Scroll UI Fix**: Corrected Compose LazyList auto-scrolling to reliably pin the latest AI response during streaming generation.

### 🔔 Real-Time Activity Alerts & Badges
- **Supabase Realtime Stream**: Wired active WebSocket channel listening to `public.notifications` table for instant push alerts.
- **Dynamic Badge Counters**: Added live notification count badges on the bottom navigation bar and menu drawers that update in real time.
- **Automated PostgreSQL Triggers**:
  - `notify_on_post_like`: Instant alert when another researcher endorses your manuscript.
  - `notify_on_comment`: Instant notification when a peer leaves a review comment.
  - `notify_on_message`: Real-time notification for conversation participants when a direct message arrives.
- **Audio & Haptic Feedback**: Optional sound and vibration alerts when in-app alerts fire during reading sessions.

### 🔄 Intelligent In-App Version Checker & Updater
- **Silent Startup Verification**: Background version check on app launch is completely silent when the app is up to date (`update_available: false`), preventing user fatigue.
- **Conditional Alert Modal**: Displays an interactive `InAppUpdateDialog` with formatted release notes only when an update is genuinely available.
- **Manual Check Controls**: Added "Check for Updates" actions in both `SettingsScreen` and `MenuScreen` with instant confirmation toasts ("Cite Circle is up to date").
- **Background APK Downloader**: One-tap download pipeline using Android `DownloadManager` with real-time progress notification and automated package installer trigger.
- **Release Notes Viewer**: Built-in "Release Notes & Patch Changelog" modal in Settings allows users to inspect current and historical features anytime.

### 🛡️ Storage, Database & Platform Hardening
- **Cloudflare R2 Decentralized Vault**: Fast CDN distribution for open-access PDFs and manuscript supplements via `r2.dev` bucket.
- **Room v5 SQLite Offline Cache**: Robust local persistence ensuring bookmarked preprints and draft reviews remain accessible during offline fieldwork.
- **Anti-Malware File Guards**: Enforced magic-byte inspection (blocking disguised MZ/ELF binaries) and strict file extension filtering for paper uploads.

---

## [1.1.0] — 2026-09-19 (Build 2)

### 🚀 Highlights
Full migration from legacy mock backends to Supabase PostgreSQL with strict Row Level Security (RLS), real-time researcher messaging, and CrossRef DOI resolution.

### Added
- **Supabase Cloud Infrastructure**: Provisioned 10 core PostgreSQL tables (`profiles`, `posts`, `comments`, `post_likes`, `bookmarks`, `notifications`, `conversations`, `conversation_participants`, `messages`, `circles`) with Row Level Security.
- **CrossRef DOI Resolver**: Serverless edge function endpoint (`action: "resolve_doi"`) that queries CrossRef REST API to fetch clean title, author list, journal, publication year, and citation counts.
- **Instant Citation Formatter**: Generates formatted citations on demand in four international standards: BibTeX, APA, IEEE, and MLA.
- **Lounge Researcher Chat**: Real-time multi-user discussion room with persistent message history and participant presence.
- **Serverless Edge Runtime**: Deployed `cite-server` Deno Edge Function for backend coordination, version checking, and citation formatting.

### Security
- **Strict RLS Policies**: Enforced authenticated user verification for paper uploads, peer reviews, and conversation access.
- **Secure Token Management**: Encrypted session storage using Android `EncryptedSharedPreferences`.

---

## [1.0.0] — 2026-09-18 (Build 1)

### 🚀 Highlights
Initial public release of Cite Circle: An academic social platform for researchers and students.

### Features
- **Meta / Facebook Style Architecture**: Clean, non-distracting academic feed, reading circles, and peer discussion messenger.
- **Preprint Discovery & Reading**: View research papers directly with fast local PDF rendering.
- **Peer Review Commentary**: Leave structured comments and feedback on published manuscripts.
- **Author Profiles & Affiliations**: Showcase academic institution, field of study, and published papers.
- **Offline Vault**: Bookmark and store papers in local SQLite Room database.
- **100% Solid Flat Surfaces**: Modern design system adhering to strict readability and contrast guidelines.
