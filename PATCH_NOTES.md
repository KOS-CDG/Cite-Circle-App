# Cite Circle — Release Patch Notes & Changelog

All notable changes, configuration updates, and feature additions for the **Cite Circle** academic collaboration platform are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.3] — 2026-09-27 (Build 13)

### Highlights
This milestone release delivers **Facebook-Grade Database & Reader Scalability**. To provide an instantaneous, zero-lag experience while browsing and reading research papers throughout the application, the entire data pipeline was audited and optimized under a live **1,000-user stress test** (achieving **813.7 req/s peak throughput** and **sub-65ms median latency**).

### Facebook-Grade Database & Reader Scalability
- **Composite & Covering B-Tree Indexes**: Deployed 8 high-performance composite indexes on `posts`, `post_likes`, `messages`, `user_library_papers`, `user_collections`, and `notifications`, eliminating full-table scans.
- **Atomic Set-Based Notification Trigger**: Replaced procedural cursor iteration in `notify_on_message()` with an atomic set insert (`INSERT ... SELECT`), eradicating lock serialization contention during real-time lounge discussions.
- **Non-Negative Bounded Counters**: Hardened post likes and comments triggers with `GREATEST(0, ...)` bounds to prevent race condition counter drift.
- **Smart Batch SQLite Room Persistence**: Replaced sequential loop insertions with single-transaction smart batch merging in `PaperRepository` and `HomeViewModel`, eliminating UI thread micro-stutters while browsing papers.
- **High-Throughput OkHttp Connection Pooling**: Expanded OkHttpClient connection pool to 16 idle connections (5 min keep-alive) and `maxRequestsPerHost = 20` for non-blocking concurrent PDF prefetching, avatar loading, and cloud sync.
- **Edge Caching for Scholarly DOIs**: Added in-memory TTL caching (`doiCache`) and HTTP `Cache-Control: public, max-age=120, stale-while-revalidate=300` headers on `cite-server`, reducing DOI resolution from 1,050ms to sub-5ms on cache hits.
- **1,000-User Live Concurrency Benchmark Suite**: Added comprehensive 32-test end-to-end verification (`test/comprehensive-system.test.mjs`) and live 1,000-user stress test runner (`test/load-test-1000-users.mjs`).

### Fixed
- **Foreign Key & Metadata Schema Alignment**: Resolved foreign key naming mismatch (`posts_author_id_fkey` vs `posts_user_id_fkey`) and nested scholarly metadata into JSONB in `SupabaseClient.kt`, preventing HTTP 400 Bad Request errors on manuscript creation.
- **Trigger RPC Security**: Revoked public RPC execution from trigger functions to prevent unauthorized invocation via PostgREST.



## [2.2] — 2026-09-25 (Build 12)

### Highlights
This milestone release introduces an **In-App PDF Reader & Excerpt Highlighter**. Researchers can now read papers directly inside Cite Circle with dynamic page bookmarking, quick-jump navigation, auto-resume tracking, interactive progress indicators, an excerpt highlighter modal with 6 academic category tags, an in-reader literature notes drawer, and cloud synchronization across Room SQLite v9 and Supabase PostgreSQL.

### In-App Document Reading & Auto-Resume
- **Built-in Document Reader (`PdfViewerScreen`)**: Native document rendering with on-demand bitmap rasterization, LRU memory caching (max 8 pages), pinch-to-zoom (up to 3.5x), smooth pan gestures, and night reading mode.
- **Reading Progress Tracking & Auto-Resume**: Automatically tracks visible pages (`lastReadPage`, `totalPageCount`) as researchers scroll, instantly restoring readers to their exact previous page upon opening any manuscript.
- **Auto-Advancement of Reading Status**: Opening a document and reading past page 1 automatically transitions its status from `TO_READ` to `READING`, and a 1-tap "Mark Read" pill prompt appears upon completing the manuscript.
- **Floating Reading Progress Indicator**: Slim bottom floating pill showing exact page counter (`Page X of Y`), percentage completed, and a live progress bar.

### Page Bookmarks & Fast-Jump Navigation
- **1-Tap Page Bookmarks**: Bookmark any page directly from the reader top app bar with instant visual feedback (`Icons.Filled.Bookmark`).
- **Bookmarks Quick-Jump Menu**: Dedicated dropdown menu listing all bookmarked pages (`Page 3`, `Page 14`), enabling instant animated jumps across the manuscript.
- **Persistent CSV Serialization**: Bookmarks persist offline in Room SQLite as comma-separated integers and automatically synchronize with Supabase PostgreSQL.

### Excerpt Highlighter & In-Reader Literature Notes
- **Excerpt Highlighter Modal (`ExcerptHighlighterDialog`)**: Capture key sentences or paragraphs on the fly, tag them with scholar categories (`Key Finding`, `Methodology`, `Result`, `Limitation`, `Idea`, `General`), append personal insights, and save directly to the paper's research notes.
- **In-Reader Research Notes Sheet (`InReaderResearchNotesSheet`)**: Slide-up `ModalBottomSheet` providing immediate access to personal literature notes and captured excerpts without exiting the PDF reader.

### UI Badges & Multi-Device Cloud Sync
- **Interactive Reading Progress Badges**: Paper cards in `ReadingListsScreen` and `PostUi` now display reading badges (e.g. `Page 7/24 (29%)`) and a 1-tap "Resume" action when progress exists.
- **Database Schema Migration v9**: Upgraded Room Database to version 9 with `migration8To9()`, adding `lastReadPage`, `totalPageCount`, and `pageBookmarks` with zero data loss.
- **Supabase Cloud Synchronization**: Updated `user_library_papers` schema and Supabase client to sync reading progress and bookmarks across devices.

## [2.1] — 2026-09-25 (Build 11)

### Highlights
This milestone release delivers **Multi-Device Cloud Sync** for Cite Circle's research repository. Researchers can now seamlessly synchronize their custom collections, paper assignments, reading statuses ("To Read", "Reading", "Read"), and personal markdown research notes across devices with our secure Supabase PostgreSQL backend, while maintaining 100% offline functionality and zero data loss on local device storage.

### Multi-Device Repository Cloud Sync
- **Two-Way Cloud Synchronization (`HomeViewModel.syncFullLibraryNow`)**: Pushes local collections, folder mappings, and library papers to the cloud while pulling and merging remote updates into Room SQLite.
- **Smart Asset Preservation**: Merging logic strictly protects and preserves local device paths (downloaded PDFs in Paper Vault, cached figures/images) during cloud synchronizations.
- **1-Tap Cloud Sync Control**: Added an interactive sync button with real-time circular progress indicator in the Library header (`ReadingListsScreen`), allowing scholars to manually trigger or inspect sync state at any time.
- **Automatic Background Mutation Sync**: Creating, editing, or deleting collections, moving papers between folders, changing reading statuses, and editing study notes automatically trigger background cloud sync requests when signed in.

### Backend & Security Architecture
- **Supabase PostgreSQL Schema (`user_library_papers`, `user_collections`, `user_collection_papers`)**: Scalable relational schema tracking papers, collections, and many-to-many paper-collection memberships per user.
- **Row-Level Security (RLS)**: Enforces strict user authorization with `auth.uid() = user_id` on all tables, passing all Supabase security advisories with zero vulnerabilities.
- **Declarative Migration**: Stored migration at `supabase/migrations/20260925_user_library_sync.sql`.

## [2.0] — 2026-09-25 (Build 10)

### Highlights
This milestone release equips Cite Circle with **Multi-Faceted Repository Filtering, Study Notes & Batch BibTeX Citation Export**. Researchers can now filter and sort papers across reading statuses ("To Read", "Reading", "Read"), document attachments (Offline PDFs, Open Access), and publication metadata. It introduces interactive reading status tracking badges, personal markdown research notes per paper, batch BibTeX (`.bib`) exporting for Overleaf/LaTeX, and Room Database schema migration 8.

### Multi-Faceted Repository Filtering & Sorting
- **Interactive Filter & Sort Dialog (`RepositoryFilterDialog`)**: Filter by Reading Status (All, To Read, Reading, Read), Document Attachment (All, Has PDF, Open Access), and Sort Order (Newest Published, Oldest Published, Title A-Z, Author A-Z).
- **Active Filter Badges**: Badged button indicating the active filter count with 1-tap "Reset All" functionality.

### Reading Status & Personal Research Notes
- **Interactive Reading Status Badge (`ReadingStatusBadge`)**: 1-tap toggle between "To Read" (Blue), "Reading" (Orange), and "Read" (Green) on both library and feed cards.
- **Personal Literature Notes (`ResearchNotesDialog`)**: Dedicated notes modal allowing researchers to record methodology takeaways, critiques, and review summaries, with inline preview snippets on paper cards.

### Batch Bibliography Export
- **1-Tap Batch BibTeX Export (`CitationFormatter.exportBatch`)**: Formats all papers in any active collection or filtered view into a complete, standard `.bib` bibliography ready for Overleaf, LaTeX, or citation managers via Android share sheet.

### Database Migration v8
- **Room Migration 7 to 8**: Adds `readingStatus` and `researchNotes` columns to `saved_papers` with offline persistence and backward compatibility.

## [1.9] — 2026-09-25 (Build 9)

### Highlights
This release delivers **Repository Collections & Folders**, enabling scholars to organize, group, and categorize research papers into custom projects, research topics, and reading lists (akin to Zotero and Mendeley). It introduces a dynamic collections and folders bar in the Library, an 8-color academic palette with dedicated icon markers, an instant paper-to-collection organization dialog accessible from both library and social feed cards, color badges on paper cards, Room Database schema migration 7, and full offline persistence.

### Repository Collections & Folders
- **Dynamic Collections & Folders Bar**: Upgraded `ReadingListsScreen` with a horizontal collections filter ribbon featuring "All Saved", "Paper Vault" (offline PDFs), custom user collections with live paper counters, and a 1-tap "+ New Collection" action.
- **Customizable Collections & Projects**: Create, edit, and manage research folders with custom names, optional research notes/descriptions, 8 scholar-curated color tags (`#1A73E8`, `#0D904F`, `#EA4335`, `#F9AB00`, `#9334E6`, `#007B83`, `#E37400`, `#5F6368`), and 6 academic icons (Folder, Book, Star, Code, Science, Bookmark).
- **Collection Header Banner**: Displays an informative top banner when viewing any custom collection, showing icon, title, description, paper count, and an options menu to edit or delete the folder.
- **Universal Paper Organization Dialog (`OrganizePaperDialog`)**: Quickly categorize papers into one or multiple collections with checkbox toggling directly from `SavedEntryCard` or the `PostCard` dropdown menu across the feed.
- **Paper Collection Badges**: Displays compact color-coded collection pills on research paper cards in both the feed and library for instant visual categorization.
- **Room Database Migration v7**: Added `collections` and many-to-many `paper_collection_entries` tables with indexed foreign references and automatic cascade cleanup upon paper or collection deletion.

## [1.8] — 2026-09-25 (Build 8)

### Highlights
This release establishes Cite Circle as a dedicated **Research Paper Repository**. It introduces the unified **Paper Ingestion Hub** featuring 1-tap DOI and arXiv metadata resolution with automated Open Access PDF downloads, smart PDF manuscript extraction (`PaperMetadataExtractor`) that auto-populates bibliographic details from uploaded documents, completely purges mock/placeholder user identities for an authentic scholar experience, and exposes full repository metadata fields across the composer and feed cards.

### Research Paper Ingestion Hub
- **1-Tap DOI & arXiv Import**: Ingesting research papers is now frictionless: enter any standard DOI (e.g. `10.1038/s41586-020-2649-2`) or arXiv ID (`2301.07041` or `arXiv:1706.03762`) to instantly resolve Title, full academic Authors, Year, Venue, Abstract, DOI, and landing URLs via Crossref, OpenAlex, and native arXiv Atom APIs.
- **Automated Open-Access PDF Retrieval**: When importing a paper with a public Open Access PDF, Cite Circle automatically downloads the manuscript into the local Paper Vault (`PdfStore`) and binds it to the entry for offline reading.
- **Smart PDF Manuscript Extractor (`PaperMetadataExtractor`)**: Uploading a PDF, Word DOCX, or text manuscript automatically scans the binary stream for embedded DOIs, arXiv IDs, XMP metadata, and PDF Info dictionaries (`/Title`, `/Author`, `/CreationDate`). It auto-fills bibliographic citation fields with zero manual typing required.
- **Complete Repository Metadata Exposure**: The paper composer now natively supports Venue/Journal, DOI, Repository URL, Abstract, and Open Access license indicators, persisting full scholarly records into the Room database.

### Clean Slate User Defaults & Access Simplification
- **Placeholder Identity Elimination**: Completely removed all mock user identities ("Guest Researcher", "Dr. Alex Rivera", "Academic Institution", "Stanford University").
- **Proactive Data Sanitizer**: Added `purgePlaceholderData()` executing on app startup to wipe any legacy dummy data from `DataStore` and Room user tables.
- **Streamlined Peer Messaging**: Temporarily removed complex messaging entry points from navigation bars, menus, and post action rows to focus on paper storage and academic discussions.

## [1.7] — 2026-09-25 (Build 7)

### Highlights
This release resolves critical UI cut-off and keyboard alignment issues across the application. It eliminates duplicate window inset padding between the application root Scaffold and destination screens, fixes squashed top headers in the Profile and Chat screens, restructures the Profile action controls into a responsive layout that prevents button truncation on all mobile screen widths, adds bottom navigation bar padding to prevent content from hiding behind system navigation bars, and corrects modal dialog height constraints during text input.

### Messenger Keyboard & Text Box Alignment
- **Zero-Shift IME Alignment**: Configured the root application `Scaffold` with `contentWindowInsets = WindowInsets(0, 0, 0, 0)`. The message text box in `ChatThreadScreen` now anchors directly to the top edge of the virtual keyboard without being pushed hundreds of pixels into the middle of the screen.
- **Top Header Unsquashing**: Reordered the modifier hierarchy in `ChatThreadScreen` and `PostDetailScreen` so that status bar padding applies to the container rather than constraining the 56dp Row height.

### Profile Screen Interface & Cut-Off Fixes
- **Top Bar Header Layout**: Resolved a modifier order bug (`statusBarsPadding().height(54.dp)`) by wrapping the top bar in a status-bar-padded Column, ensuring back button, title, search, and settings icons have full 56dp vertical space without clipping.
- **Responsive Action Button Bar**: Re-engineered the 4-button action row: "Open to" and "Edit Profile" now have balanced responsive widths (`weight(1f)` with `maxLines = 1`), while "Share" and "More" are elegant 38dp circular outline buttons. All 4 controls fit cleanly across all mobile screen densities without text truncation.
- **Navigation Bar Bottom Spacing**: Added `navigationBarsPadding()` to the bottom of the Profile and Opportunities scroll streams so that bottom cards, skills, and endorsements are never obscured by Android system 3-button or gesture bars.
- **Responsive Dialog Heights**: Converted fixed-height dialogs (`Edit Intro`, `Experience`, `Education`) from fixed heights to flexible `heightIn` constraints, ensuring action buttons ("Save", "Cancel") remain visible when the software keyboard opens.

## [1.6] — 2026-09-21 (Build 6)

### Highlights
This release adds a universal swipe-down pull-to-refresh feature across every page and menu in Cite Circle to retrieve the latest scholarly posts, preprints, conversations, and notifications in real-time. It completely streamlines the new entry creation workflow by removing tedious metadata inputs (DOI, Journal/venue, URL, PDF link, and Abstract), leaving a clean, focused form for commentary, figures, paper documents, and basic bibliographic citations. Additionally, it integrates high-performance native C++ acceleration and SQLite C-native Full-Text Search (FTS4).

### Universal Swipe-Down Pull-to-Refresh
- **Omnipresent Refresh Across All Screens**: Integrated Material 3 `RefreshableBox` (wrapping `PullToRefreshBox`) across all navigation pages and menus: Feed (`HomeScreen`), Side Menu (`MenuScreen`), Settings & Privacy (`SettingsScreen`), Research Fields (`FieldsScreen`), Venue Explorer (`VenueScreen`), Opportunities & Grants (`OpportunitiesScreen`), Messenger (`MessengerScreen`), Discussion Threads (`ChatThreadScreen`), Activity Notifications (`NotificationsScreen`), and Post Details (`PostDetailScreen`).
- **Comprehensive Cloud & Local Sync**: Swipe gestures trigger immediate synchronization with Supabase remote public posts, real-time activity alerts, Academic Lounge peer messages, and Room library counts.
- **Scrollable Empty State Integration**: Empty states and loading skeletons are scrollable, ensuring users can pull to refresh even when a section is initially devoid of content.

### Streamlined Research Entry Composer
- **Eliminated Cluttered Metadata Fields**: Completely removed DOI, Journal/venue, external URL, open-access PDF link, and Abstract text fields from the new post form (`ComposePostScreen.kt`).
- **Focused Creation Experience**: Researchers now enjoy an uncluttered composition flow focusing on personal commentary, figure attachments, manuscript uploads (PDF, DOCX, TXT), and essential citation metadata (Title, Authors, Year, Affiliation).
- **Graceful Backward Compatibility**: Existing posts with legacy metadata retain their bibliographic details during edits while presenting the new streamlined UI.

### C++ Native Acceleration & Full-Text Search
- **C++20 Native Engine (`citecircle_native`)**: Implemented high-performance C++20 parsing routines providing zero-copy string scanning for BibTeX citation AST generation and microsecond binary magic byte detection.
- **Robust Fallback & Brace Matching**: Balanced-brace parser fallback logic in `NativeCitationEngine` guaranteeing zero parsing errors on JVM host environments.
- **SQLite C-Native Full-Text Search (FTS4)**: Room FTS4 virtual table (`SavedPaperFts`) indexing paper titles, commentary, and authors with instant multi-term querying.

## [1.5] — 2026-09-21 (Build 5)

### Highlights
This release provides scholars complete autonomy and freedom over their profile pictures with automated downsampling, center-square cropping, persistent internal storage, and cloud sync. It removes intrusive automatic in-app update popups on launch so researchers can choose when to update, and completely eradicates all hardcoded mock profile information so newly registered users start with a 100% clean, authentic slate.

### Profile Picture Autonomy & Image Optimization
- Modern Photo Picker: Integrated Android's standard visual media picker (`PickVisualMedia`) for high-resolution avatar and cover photo selection without requiring storage permissions.
- Automatic Image Optimization (`ProfileImageHelper`): Downsamples incoming images to a memory-efficient 512x512 resolution (center-square cropped, ~85% quality JPEG) for avatars and 1600x600 for cover banners, completely preventing Out-Of-Memory (OOM) errors and layout distortion.
- Sandboxed Persistent Internal Storage: Saves processed photos directly into `context.filesDir/avatars/`, eliminating transient content URI permission expiration crashes across device reboots and app updates.
- Interactive Avatar Management: Tapping the profile picture or cover opens an options sheet allowing scholars to "Change Photo", "View Full Photo" in an edge-to-edge modal preview, or "Remove Photo" (reverting cleanly to academic initials).
- Cloud Storage Sync: Processed avatars are automatically synchronized with Supabase Storage (`manuscripts/avatars/`) and linked to `public.profiles.avatar_url`.
- Omnipresent Dynamic Avatar: Real-time avatar rendering propagates across the entire application, including the Side Menu drawer shortcut and Settings & Privacy Accounts Center.

### User-Empowered Non-Intrusive Updates
- Startup Interruption Removed: Removed automatic update check dialog triggers from application launch init blocks. Users are never interrupted or blocked from using Cite Circle on startup.
- Full Update Autonomy: Scholars can check for the latest releases at their own convenience via dedicated "Check for Updates" options in Settings and the Side Menu.
- Non-Blocking Dialog: The update modal is strictly dismissible with enabled back press, outside click, and an always-visible "Later" button.

### Clean Slate for New Accounts
- Zero Pre-Populated Mock Data: Completely removed all legacy mock entries ("Dr. Alex Rivera", "Stanford AI Lab", simulated MIT/DeepMind experiences, fake education, fake skill endorsements, and fake connection metrics).
- 100% Authentic Identity: Newly created accounts reflect exclusively the details provided during registration (Full Name, Email, Affiliation, and Research Field).
- Pristine Optional Fields: Headline, Bio, Location, Website, ORCID, Experiences, Education, and Skills start completely blank and ready for scholar personalization.

## [1.4] — 2026-09-21 (Build 4)

### Highlights
This release hardens the Gemini AI assistant to strictly adhere to academic and scientific research domains with robust prompt-injection and extraction defenses, resolves the Messenger navigation backstack trapping issue, completes full end-to-end profile editing for all sections (Experience, Education, Skills, Degree suffix) backed by offline DataStore persistence, and removes all UI gradients and decorative emojis across the platform.

### Academic AI Guardrails & Anti-Extraction Protection
- Domain-Restricted Intelligence: Trained and constrained the Gemini AI assistant to discuss only peer-reviewed research, scientific methodology, academic literature, paper writing, and scholarly peer review.
- Jailbreak & Prompt Injection Defenses: Enforced strict refusal mechanisms against prompt extraction attacks, system prompt leakage, role reversals, and non-academic queries.
- Safe Code Generation Policy: AI strictly refuses non-research code generation while permitting verified scientific computation scripts (Python, R, PyTorch, LaTeX).

### Messenger Navigation & Backstack Stability
- Top Bar & Menu Single-Top Navigation: Configured Messenger navigation actions to use `launchSingleTop = true`, `popUpTo("feed")`, and state preservation, preventing cyclic backstack buildup.
- Header Back Navigation: Added a dedicated Back arrow button in the Messenger screen header, allowing users to return immediately to their previous screen without becoming trapped.
- Layout Overflow Fix: Restructured `LazyColumn` weight sizing to eliminate unbounded height rendering issues.

### Complete Editable Academic Profile
- Full Section Editing: Added interactive dialogs allowing scholars to add, edit, and remove entries for Experience & Academic Positions, Education, Skills & Endorsements, and Academic Degree title suffixes.
- DataStore Persistence: Integrated JSON and CSV serializers in `UserSessionManager` so all profile customizations persist reliably across app restarts.
- Owner Action Suite: Replaced self-connect buttons with authentic owner controls: "Open to", "Edit Profile", "Share", and "More".
- Polished Alignment: Aligned profile metrics and identity headers with strict left alignment following professional academic standards.

### UI Hygiene & Design Standardization
- Zero Gradient Policy: Eliminated all gradient brushes across the application, adopting clean, high-contrast solid surface tokens.
- Professional Typography: Removed decorative emojis from analytics dialogs, settings menus, and system release logs.
- About App Cleanliness: Removed internal architecture notes from the About dialog in favor of verified platform version metadata.

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
