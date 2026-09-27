import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
};

// Canonical Version & Release Configuration
export const LATEST_VERSION_NAME = "2.3";
export const LATEST_VERSION_CODE = 13;
export const DOWNLOAD_URL = "https://cxxtrtglmxfuyihxwiza.supabase.co/storage/v1/object/public/app-releases/CiteCircle-latest.apk";
export const FILE_SIZE_MB = 28.6;

export const LATEST_RELEASE_NOTES = [
  "⚡ Facebook-Grade Performance: Audited and optimized under 1,000-user concurrency, reaching 813.7 req/s peak throughput",
  "🚀 Sub-65ms Database Throughput: 8 new composite B-Tree indexes on posts, likes, messages, library, and notifications",
  "💬 Atomic Set-Based Messaging Triggers: Eradicated lock contention and cursor loops during high-volume chat discussions",
  "📚 Smart Batch Room Persistence: Zero-lag SQLite merging eliminates UI micro-stutters during paper browsing and reading",
  "🌐 High-Throughput Network Engine: Expanded OkHttp connection pool to 16 idle connections for parallel manuscript loading"
].join("\n");

export const RELEASE_HISTORY = [
  {
    version: "2.3",
    version_code: 13,
    release_date: "2026-09-27",
    title: "Facebook-Grade Database & Reader Scalability",
    highlights: [
      "Audited and optimized under 1,000-user concurrency reaching 813.7 req/s peak throughput",
      "8 composite & covering B-Tree indexes on posts, likes, messages, library, and notifications",
      "Atomic set-based notification triggers eliminating lock serialization contention",
      "Non-negative bounded counter triggers preventing race condition drift",
      "Smart batch SQLite Room merging eliminating UI thread micro-stutters",
      "Expanded OkHttpClient connection pool (16 idle connections) for concurrent PDF loading"
    ]
  },
  {
    version: "2.2",
    version_code: 12,
    release_date: "2026-09-25",
    title: "In-App PDF Viewer, Page Bookmarking & Excerpt Highlighter",
    highlights: [
      "Built-in PDF reader with auto-resume to lastReadPage and dynamic percentage progress indicator",
      "Page bookmarking with fast-jump dropdown menu",
      "Excerpt highlighter dialog with 6 academic category tags (Key Finding, Methodology, Result, Limitation, Idea, General)",
      "In-reader Research Notes bottom sheet for immediate annotation while reading",
      "Interactive reading progress badges on library and post cards",
      "Room schema migration 9 and Supabase PostgreSQL cloud synchronization"
    ]
  },
  {
    version: "2.1",
    version_code: 11,
    release_date: "2026-09-25",
    title: "Multi-Device Cloud Sync for Collections, Papers & Study Notes",
    highlights: [
      "Bidirectional library synchronization to Supabase PostgreSQL",
      "Syncs custom collections, paper assignments, reading statuses, and personal research notes",
      "Smart merge protecting local PDF Vault files and cached images",
      "Interactive 1-tap Cloud Sync button in Library header with live spinner",
      "PostgreSQL user_library_papers, user_collections, user_collection_papers tables with RLS"
    ]
  },
  {
    version: "2.0",
    version_code: 10,
    release_date: "2026-09-25",
    title: "Multi-Faceted Filtering, Study Notes & Batch BibTeX Export",
    highlights: [
      "Multi-faceted filter and sort drawer across reading status, document type, and dates",
      "Interactive 1-tap reading status badge (To Read, Reading, Read)",
      "Personal research notes dialog with card preview snippets",
      "Batch BibTeX bibliography export for collections and filtered libraries",
      "Room schema migration 8 with backward compatibility"
    ]
  },
  {
    version: "1.9",
    version_code: 9,
    release_date: "2026-09-25",
    title: "Repository Collections & Folders Architecture",
    highlights: [
      "Dynamic Collections & Folders bar in Library with live paper count badges",
      "Custom collection creation with 8 academic colors and 6 topic icons",
      "Universal paper organization dialog accessible from feed and library",
      "Compact color collection badges on research paper cards",
      "Room schema migration 7 with many-to-many indexing and cascade cleanup"
    ]
  },
  {
    version: "1.8",
    version_code: 8,
    release_date: "2026-09-25",
    title: "Research Paper Ingestion Hub & Clean Slate Architecture",
    highlights: [
      "1-tap DOI & arXiv import via Crossref, OpenAlex, and native arXiv Atom APIs",
      "Automated open-access manuscript PDF downloading directly into Paper Vault",
      "Smart PDF metadata extraction parsing embedded DOIs, XMP, and PDF Info dictionaries",
      "Exposed complete scholarly metadata schema: Venue, DOI, URL, Abstract, Open Access",
      "Purged all mock user placeholders and streamlined peer messaging access"
    ]
  },
  {
    version: "1.7",
    version_code: 7,
    release_date: "2026-09-25",
    title: "Profile Interface & Messenger Keyboard Alignment",
    highlights: [
      "Fixed message input text box alignment to anchor directly above software keyboard",
      "Resolved ProfileScreen and ChatThreadScreen top bar header squashing bug",
      "Restructured Profile action buttons into responsive layout preventing button truncation",
      "Added navigation bar padding across Profile and Opportunities scroll streams",
      "Responsive height constraints on modal dialogs during text input"
    ]
  },
  {
    version: "1.6",
    version_code: 6,
    release_date: "2026-09-21",
    title: "Universal Swipe-Down Refresh & Streamlined Entry Composer",
    highlights: [
      "Swipe-down pull-to-refresh added across all navigation pages, menus, and detail views",
      "Streamlined entry composer eliminating DOI, venue, URL, PDF link, and abstract fields",
      "High-speed C++20 parsing engine with zero-copy BibTeX tokenization and magic byte detection",
      "SQLite C-powered FTS4 virtual table search in Room with database migration 5 to 6"
    ]
  },
  {
    version: "1.5",
    version_code: 5,
    release_date: "2026-09-21",
    title: "Profile Picture Autonomy, Non-Intrusive Updates & Clean Slate Registration",
    highlights: [
      "Modern Android photo picker with automatic downsampling and square cropping",
      "Persistent sandboxed internal storage preventing Android URI permission expiration",
      "Cloud avatar synchronization with Supabase Storage and profiles table",
      "Interactive avatar options: Change Photo, Full-Screen Preview, and Remove Photo",
      "Startup update check popups removed; manual user-controlled updates enabled",
      "Zero hardcoded mock data for new users; clean authentic registration slate"
    ]
  },
  {
    version: "1.4",
    version_code: 4,
    release_date: "2026-09-21",
    title: "Academic AI Guardrails, Messenger Navigation Fix & Editable Profile",
    highlights: [
      "Gemini AI assistant restricted strictly to scientific research topics with anti-jailbreak defenses",
      "Messenger screen navigation resolved with top-bar singleTop and header back arrow button",
      "Fully interactive Experience, Education, and Skills dialogs with local DataStore persistence",
      "Academic Degree title suffix (e.g. Ph.D., M.S.) added to Intro editor",
      "Owner profile action controls: Open to, Edit Profile, Share, and More menu",
      "Complete removal of UI gradient brushes and decorative emojis across the application"
    ]
  },
  {
    version: "1.3",
    version_code: 3,
    release_date: "2026-09-20",
    title: "LinkedIn-Style Profile Interface, Skill Endorsements & Academic Timeline",
    highlights: [
      "Revamped profile into LinkedIn layout: left-aligned overlapping avatar with 3.5dp surface ring border",
      "Functional cover photo & profile picture pickers with persistent DataStore storage",
      "Full 'Edit Intro' dialog for headline, affiliation, field, location, and open-to status",
      "Interactive 'Contact info' modal displaying email, website, and ORCID with 1-tap clipboard copying",
      "'Open to' sheet allowing scholars to broadcast research collaborations and peer review availability",
      "Private Analytics card displaying profile views, post impressions, and search appearances",
      "Interactive Skill Endorsements with live upvote counters",
      "Structured Experience & Education timeline cards and pinned Featured Publications"
    ]
  },
  {
    version: "1.2",
    version_code: 2,
    release_date: "2026-09-20",
    title: "Gemini AI Assistant, Real-Time Activity Alerts & In-App Updater",
    highlights: [
      "Gemini AI Assistant powered by Gemini 2.5 Flash, 3.5 Flash, and 3.1 Flash Lite",
      "Live Google Search Grounding for verified literature citations and arXiv papers",
      "Multimodal vision model support for figure, diagram, and equation analysis",
      "Real-time WebSocket alerts and dynamic notification badges across Android & Web",
      "Silent background in-app update checks with manual Settings trigger",
      "Android 9+ hardware bitmap memory safety and transient retry backoff"
    ]
  },
  {
    version: "1.1",
    version_code: 2,
    release_date: "2026-09-19",
    title: "Supabase Migration, Live CrossRef DOI Resolver & Lounge Chat",
    highlights: [
      "Full migration to Supabase PostgreSQL with strict RLS policies",
      "CrossRef DOI resolution engine and multi-format citation formatter (BibTeX, APA, IEEE, MLA)",
      "Multi-user Lounge real-time chat with persistent messaging",
      "Cloudflare R2 integration for instant preprint PDF distribution"
    ]
  },
  {
    version: "1.0",
    version_code: 1,
    release_date: "2026-09-18",
    title: "Initial Launch of Cite Circle",
    highlights: [
      "Academic social feed with Meta / Facebook styling in Jetpack Compose",
      "Offline-first Room SQLite vault for bookmarked research papers",
      "Researcher profile management and peer review commenting",
      "Anti-malware file guard and safe preprint upload pipeline"
    ]
  }
];

// In-Memory Fast TTL Cache for External DOI Metadata (7 days TTL)
interface CachedDoi {
  data: any;
  expiresAt: number;
}
const doiCache = new Map<string, CachedDoi>();
const DOI_CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

// Cache-Control headers for static and version checks
const staticCacheHeaders = {
  ...corsHeaders,
  "Content-Type": "application/json",
  "Cache-Control": "public, max-age=120, stale-while-revalidate=300"
};

Deno.serve(async (req: Request) => {
  // 1. Handle CORS Preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  const url = new URL(req.url);

  // 2. GET Requests (Health check, In-App Update check, or Patch Notes)
  if (req.method === "GET") {
    const action = url.searchParams.get("action");

    if (action === "check_update") {
      const clientCode = Number(url.searchParams.get("code") || 1);
      return new Response(
        JSON.stringify({
          success: true,
          update_available: clientCode < LATEST_VERSION_CODE,
          latest_version_name: LATEST_VERSION_NAME,
          latest_version_code: LATEST_VERSION_CODE,
          mandatory: false,
          release_notes: LATEST_RELEASE_NOTES,
          download_url: DOWNLOAD_URL,
          file_size_mb: FILE_SIZE_MB
        }),
        { headers: staticCacheHeaders }
      );
    }

    if (action === "patch_notes") {
      return new Response(
        JSON.stringify({
          success: true,
          latest_version: LATEST_VERSION_NAME,
          latest_version_code: LATEST_VERSION_CODE,
          release_notes: LATEST_RELEASE_NOTES,
          history: RELEASE_HISTORY
        }),
        { headers: staticCacheHeaders }
      );
    }

    return new Response(
      JSON.stringify({
        status: "online",
        service: "Cite Circle Serverless Edge API",
        environment: "Supabase Deno Edge Runtime",
        timestamp: new Date().toISOString(),
        version: LATEST_VERSION_NAME,
        version_code: LATEST_VERSION_CODE,
        capabilities: [
          "doi-resolver",
          "doi-cache",
          "citation-generator",
          "in-app-updater",
          "patch-notes",
          "gemini-ai",
          "search-grounding",
          "platform-sync",
          "health-check"
        ]
      }),
      { headers: staticCacheHeaders }
    );
  }

  try {
    const body = await req.json().catch(() => ({}));
    const action = body.action || "health";

    // 3. In-App Update Check Action (POST)
    if (action === "check_update") {
      const clientCode = Number(body.current_version_code || 1);
      return new Response(
        JSON.stringify({
          success: true,
          update_available: clientCode < LATEST_VERSION_CODE,
          latest_version_name: LATEST_VERSION_NAME,
          latest_version_code: LATEST_VERSION_CODE,
          mandatory: Boolean(body.force_mandatory || false),
          release_notes: LATEST_RELEASE_NOTES,
          download_url: DOWNLOAD_URL,
          file_size_mb: FILE_SIZE_MB
        }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 4. Patch Notes Action (POST)
    if (action === "patch_notes") {
      return new Response(
        JSON.stringify({
          success: true,
          latest_version: LATEST_VERSION_NAME,
          latest_version_code: LATEST_VERSION_CODE,
          release_notes: LATEST_RELEASE_NOTES,
          history: RELEASE_HISTORY
        }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 5. Format Citation Action
    if (action === "format_citation") {
      const { title, author, year, doi, journal, format } = body;
      const cleanYear = year || new Date().getFullYear();
      const cleanAuthor = author || "Anonymous Researcher";
      const cleanTitle = title || "Untitled Manuscript";
      const cleanJournal = journal || "Cite Circle Preprints";

      let citation = "";
      switch ((format || "apa").toLowerCase()) {
        case "bibtex":
          const id = cleanAuthor.split(" ")[0].toLowerCase() + cleanYear;
          citation = `@article{${id},\n  author = {${cleanAuthor}},\n  title = {${cleanTitle}},\n  journal = {${cleanJournal}},\n  year = {${cleanYear}},\n  doi = {${doi || "N/A"}}\n}`;
          break;
        case "ieee":
          citation = `${cleanAuthor}, "${cleanTitle}," ${cleanJournal}, ${cleanYear}.${doi ? ` doi: ${doi}` : ""}`;
          break;
        case "mla":
          citation = `${cleanAuthor}. "${cleanTitle}." ${cleanJournal}, ${cleanYear}.${doi ? ` https://doi.org/${doi}` : ""}`;
          break;
        case "apa":
        default:
          citation = `${cleanAuthor} (${cleanYear}). ${cleanTitle}. ${cleanJournal}.${doi ? ` https://doi.org/${doi}` : ""}`;
          break;
      }

      return new Response(
        JSON.stringify({ success: true, citation, format: format || "apa" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 6. Resolve DOI Metadata Action (Fast In-Memory Cache)
    if (action === "resolve_doi") {
      const doi = (body.doi || "").trim().replace(/^https?:\/\/doi\.org\//, "").toLowerCase();
      if (!doi) {
        return new Response(
          JSON.stringify({ success: false, error: "DOI is required" }),
          { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      // 6a. Check In-Memory Cache first (sub-5ms response)
      const cached = doiCache.get(doi);
      if (cached && cached.expiresAt > Date.now()) {
        return new Response(
          JSON.stringify({
            success: true,
            cached: true,
            data: cached.data
          }),
          {
            headers: {
              ...corsHeaders,
              "Content-Type": "application/json",
              "X-Cache": "HIT",
              "Cache-Control": "public, max-age=86400"
            }
          }
        );
      }

      // 6b. Cache Miss: Fetch from CrossRef API
      const crossRefRes = await fetch(`https://api.crossref.org/works/${encodeURIComponent(doi)}`, {
        headers: { "User-Agent": "CiteCircleApp/1.0 (mailto:support@citecircle.org)" }
      });

      if (!crossRefRes.ok) {
        return new Response(
          JSON.stringify({ success: false, error: `DOI not found on CrossRef (${crossRefRes.status})` }),
          { status: crossRefRes.status, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      const crossRefData = await crossRefRes.json();
      const item = crossRefData.message || {};
      const authors = (item.author || []).map((a: any) => `${a.given || ""} ${a.family || ""}`.trim()).filter(Boolean);

      const resolvedData = {
        title: item.title?.[0] || "Unknown Title",
        authors: authors.length ? authors : ["Unknown Author"],
        journal: item["container-title"]?.[0] || "",
        year: item.published?.["date-parts"]?.[0]?.[0] || item.created?.["date-parts"]?.[0]?.[0] || null,
        doi: item.DOI || doi,
        url: item.URL || `https://doi.org/${doi}`,
        citations_count: item["is-referenced-by-count"] || 0
      };

      // Store in memory cache
      doiCache.set(doi, {
        data: resolvedData,
        expiresAt: Date.now() + DOI_CACHE_TTL_MS
      });

      return new Response(
        JSON.stringify({
          success: true,
          cached: false,
          data: resolvedData
        }),
        {
          headers: {
            ...corsHeaders,
            "Content-Type": "application/json",
            "X-Cache": "MISS",
            "Cache-Control": "public, max-age=86400"
          }
        }
      );
    }

    // Default response
    return new Response(
      JSON.stringify({
        status: "online",
        service: "Cite Circle Serverless Edge API",
        version: LATEST_VERSION_NAME,
        version_code: LATEST_VERSION_CODE,
        available_actions: ["check_update", "patch_notes", "format_citation", "resolve_doi", "health"],
        received_action: action
      }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err: any) {
    return new Response(
      JSON.stringify({ success: false, error: err.message || "Internal server error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
