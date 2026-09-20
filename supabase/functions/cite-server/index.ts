import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
};

Deno.serve(async (req: Request) => {
  // 1. Handle CORS Preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  const url = new URL(req.url);

  // 2. GET Requests (Health check or In-App Update check)
  if (req.method === "GET") {
    if (url.searchParams.get("action") === "check_update") {
      const clientCode = Number(url.searchParams.get("code") || 1);
      const latestCode = 2;
      return new Response(
        JSON.stringify({
          success: true,
          update_available: clientCode < latestCode,
          latest_version_name: "1.1",
          latest_version_code: latestCode,
          mandatory: false,
          release_notes: "• Live CrossRef DOI paper resolver\n• Instant citation generator (BibTeX, APA, IEEE, MLA)\n• In-app update notifications & background download\n• Database & security hardening updates",
          download_url: "https://cxxtrtglmxfuyihxwiza.supabase.co/storage/v1/object/public/app-releases/CiteCircle-latest.apk",
          file_size_mb: 28.3
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
        capabilities: ["doi-resolver", "citation-generator", "health-check", "in-app-updater", "platform-sync"]
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
      const latestCode = 2;
      return new Response(
        JSON.stringify({
          success: true,
          update_available: clientCode < latestCode,
          latest_version_name: "1.1",
          latest_version_code: latestCode,
          mandatory: Boolean(body.force_mandatory || false),
          release_notes: "• Live CrossRef DOI paper resolver\n• Instant citation generator (BibTeX, APA, IEEE, MLA)\n• In-app update notifications & background download\n• Database & security hardening updates",
          download_url: "https://cxxtrtglmxfuyihxwiza.supabase.co/storage/v1/object/public/app-releases/CiteCircle-latest.apk",
          file_size_mb: 28.3
        }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 4. Format Citation Action
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

    // 5. Resolve DOI Metadata Action
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
        version: "1.1.0",
        available_actions: ["check_update", "format_citation", "resolve_doi", "health"],
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
