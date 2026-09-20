import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
};

// Canonical Version & Release Configuration
export const LATEST_VERSION_NAME = "1.2";
export const LATEST_VERSION_CODE = 2;
export const DOWNLOAD_URL = "https://cxxtrtglmxfuyihxwiza.supabase.co/storage/v1/object/public/app-releases/CiteCircle-latest.apk";
export const FILE_SIZE_MB = 28.2;

export const LATEST_RELEASE_NOTES = [
  "🤖 Gemini AI Research Assistant: Powered by Gemini 2.5 Flash (default), 3.5 Flash & 3.1 Flash Lite with real-time Google Search Grounding for live academic citations & arXiv synthesis",
  "🖼️ Multimodal Vision Analysis: Instant equation, chart, and figure transcription from captured or uploaded research images",
  "🔔 Real-Time Activity Alerts: Instant WebSocket notifications & unread badge counters for paper endorsements, comments, quotes, and researcher direct messages",
  "🔄 Smart In-App Updater: Silent background startup checks when up-to-date, manual update checks in Settings, and one-tap background APK downloads",
  "📄 CrossRef DOI Resolver & Citation Engine: Instant metadata resolution and academic citation generation in BibTeX, APA, IEEE, and MLA formats",
  "🛡️ Stability & Resilience: Android 9+ hardware bitmap memory safety, automatic 3-attempt exponential backoff retries, and Cloudflare R2 decentralized vault"
].join("\n");

export const RELEASE_HISTORY = [
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
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
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
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
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
          "citation-generator",
          "in-app-updater",
          "patch-notes",
          "gemini-ai",
          "search-grounding",
          "platform-sync",
          "health-check"
        ]
      }),
      {
        headers: {
          ...corsHeaders,
          "Content-Type": "application/json"
        }
      }
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

    // 6. Resolve DOI Metadata Action
    if (action === "resolve_doi") {
      const doi = (body.doi || "").trim().replace(/^https?:\/\/doi\.org\//, "");
      if (!doi) {
        return new Response(
          JSON.stringify({ success: false, error: "DOI is required" }),
          { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

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

      return new Response(
        JSON.stringify({
          success: true,
          data: {
            title: item.title?.[0] || "Unknown Title",
            authors: authors.length ? authors : ["Unknown Author"],
            journal: item["container-title"]?.[0] || "",
            year: item.published?.["date-parts"]?.[0]?.[0] || item.created?.["date-parts"]?.[0]?.[0] || null,
            doi: item.DOI || doi,
            url: item.URL || `https://doi.org/${doi}`,
            citations_count: item["is-referenced-by-count"] || 0
          }
        }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
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
