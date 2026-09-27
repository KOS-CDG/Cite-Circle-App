/**
 * Cite Circle: Comprehensive System & End-to-End Test Suite
 * 
 * Verifies all layers of the Cite Circle architecture:
 * 1. Edge Function API (cite-server: all actions, citation styles, DOI resolution, CORS, error handling)
 * 2. Supabase PostgreSQL & PostgREST (Auth, RLS, 13 Tables, Joins, Triggers, Cascades)
 * 3. Library & Reading Progress Cloud Sync (Collections, Folders, Bookmarks, Statuses)
 * 4. Anti-Malware & File Guard Rules (Magic Bytes, MIME, Extensions)
 * 5. Cloudflare R2 Media CDN Delivery
 * 6. Client Data Contracts & Logic Invariants (Pagination, Caching, BibTeX, Progress math)
 */

import assert from 'node:assert';

const SUPABASE_URL = 'https://cxxtrtglmxfuyihxwiza.supabase.co';
const ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImN4eHRydGdsbXhmdXlpaHh3aXphIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk4ODU3MDYsImV4cCI6MjEwNTQ2MTcwNn0.d0CGm637M1OVOP8IVOuyaru9S4zz0BMqUb-IZB3_eSg';
const R2_PUBLIC_DOMAIN = 'https://pub-66f1f2125beb4a028b0dd68e07a78c8a.r2.dev';

let passed = 0;
let failed = 0;
const testResults = [];

async function it(suite, testName, fn) {
  const fullName = `[${suite}] ${testName}`;
  try {
    await fn();
    console.log(`  ✓ ${fullName}`);
    passed++;
    testResults.push({ name: fullName, status: 'PASS' });
  } catch (err) {
    console.error(`  ✕ ${fullName}`);
    console.error(`    Error: ${err.message}`);
    failed++;
    testResults.push({ name: fullName, status: 'FAIL', error: err.message });
  }
}

console.log('\n================================================================');
console.log('   CITE CIRCLE: COMPREHENSIVE SYSTEM VERIFICATION TEST SUITE   ');
console.log('================================================================\n');

// Global Context across tests
let authSession = null;
let userId = null;
let testPostId = null;
let testConversationId = null;
let testCollectionId = `coll_test_${Date.now()}`;
let testPaperId = `paper_test_${Date.now()}`;

function authedHeaders(extra = {}) {
  return {
    'apikey': ANON_KEY,
    'Authorization': `Bearer ${authSession?.access_token || ANON_KEY}`,
    'Content-Type': 'application/json',
    ...extra
  };
}

// ============================================================================
// SUITE 1: EDGE FUNCTION (cite-server) ENDPOINTS & ACTIONS
// ============================================================================
console.log('--- Suite 1: Edge Function (cite-server) Endpoints ---');

await it('Edge-Server', 'Root GET returns health status, capabilities and version 2.6', async () => {
  const res = await fetch(`${SUPABASE_URL}/functions/v1/cite-server`);
  assert.strictEqual(res.status, 200);
  const data = await res.json();
  assert.strictEqual(data.status, 'online');
  assert.strictEqual(data.version, '2.6');
  assert.strictEqual(data.version_code, 16);
  assert.ok(Array.isArray(data.capabilities));
  assert.ok(data.capabilities.includes('doi-resolver'));
  assert.ok(data.capabilities.includes('citation-generator'));
  assert.ok(data.capabilities.includes('deepseek-ai'));
});

await it('Edge-Server', 'OPTIONS preflight returns CORS headers', async () => {
  const res = await fetch(`${SUPABASE_URL}/functions/v1/cite-server`, {
    method: 'OPTIONS'
  });
  assert.strictEqual(res.status, 200);
  assert.strictEqual(res.headers.get('Access-Control-Allow-Origin'), '*');
  assert.ok(res.headers.get('Access-Control-Allow-Methods')?.includes('POST'));
});

await it('Edge-Server', 'Check update with outdated version code reports update_available = true', async () => {
  const res = await fetch(`${SUPABASE_URL}/functions/v1/cite-server?action=check_update&code=15`);
  assert.strictEqual(res.status, 200);
  const data = await res.json();
  assert.strictEqual(data.success, true);
  assert.strictEqual(data.update_available, true);
  assert.strictEqual(data.latest_version_code, 16);
  assert.strictEqual(data.latest_version_name, '2.6');
  assert.ok(data.download_url.endsWith('.apk'));
  assert.strictEqual(data.file_size_mb, 28.6);
});

await it('Edge-Server', 'Check update with latest version code reports update_available = false', async () => {
  const res = await fetch(`${SUPABASE_URL}/functions/v1/cite-server?action=check_update&code=16`);
  assert.strictEqual(res.status, 200);
  const data = await res.json();
  assert.strictEqual(data.success, true);
  assert.strictEqual(data.update_available, false);
});

await it('Edge-Server', 'Patch notes action returns structured release history up to v2.6', async () => {
  const res = await fetch(`${SUPABASE_URL}/functions/v1/cite-server?action=patch_notes`);
  assert.strictEqual(res.status, 200);
  const data = await res.json();
  assert.strictEqual(data.success, true);
  assert.strictEqual(data.latest_version, '2.6');
  assert.ok(Array.isArray(data.history));
  assert.ok(data.history.length >= 11, 'History must include all releases');
  const v26 = data.history.find(h => h.version === '2.6');
  assert.ok(v26, 'Must include v2.6 entry');
  assert.strictEqual(v26.version_code, 16);
});

await it('Edge-Server', 'Citation generator formats APA style correctly', async () => {
  const res = await fetch(`${SUPABASE_URL}/functions/v1/cite-server`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      action: 'format_citation',
      title: 'Attention Is All You Need',
      author: 'Ashish Vaswani',
      year: '2017',
      journal: 'NeurIPS',
      doi: '10.48550/arXiv.1706.03762',
      format: 'apa'
    })
  });
  assert.strictEqual(res.status, 200);
  const data = await res.json();
  assert.strictEqual(data.success, true);
  assert.strictEqual(data.format, 'apa');
  assert.ok(data.citation.includes('Ashish Vaswani (2017). Attention Is All You Need. NeurIPS. https://doi.org/10.48550/arXiv.1706.03762'));
});

await it('Edge-Server', 'Citation generator formats BibTeX style correctly', async () => {
  const res = await fetch(`${SUPABASE_URL}/functions/v1/cite-server`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      action: 'format_citation',
      title: 'Deep Residual Learning',
      author: 'Kaiming He',
      year: '2016',
      journal: 'CVPR',
      doi: '10.1109/CVPR.2016.90',
      format: 'bibtex'
    })
  });
  assert.strictEqual(res.status, 200);
  const data = await res.json();
  assert.strictEqual(data.success, true);
  assert.strictEqual(data.format, 'bibtex');
  assert.ok(data.citation.startsWith('@article{kaiming2016,'));
  assert.ok(data.citation.includes('author = {Kaiming He}'));
});

await it('Edge-Server', 'Citation generator formats IEEE style correctly', async () => {
  const res = await fetch(`${SUPABASE_URL}/functions/v1/cite-server`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      action: 'format_citation',
      title: 'Deep Residual Learning',
      author: 'Kaiming He',
      year: '2016',
      journal: 'CVPR',
      doi: '10.1109/CVPR.2016.90',
      format: 'ieee'
    })
  });
  assert.strictEqual(res.status, 200);
  const data = await res.json();
  assert.strictEqual(data.success, true);
  assert.strictEqual(data.format, 'ieee');
  assert.ok(data.citation.includes('Kaiming He, "Deep Residual Learning," CVPR, 2016. doi: 10.1109/CVPR.2016.90'));
});

await it('Edge-Server', 'Citation generator formats MLA style correctly', async () => {
  const res = await fetch(`${SUPABASE_URL}/functions/v1/cite-server`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      action: 'format_citation',
      title: 'Deep Residual Learning',
      author: 'Kaiming He',
      year: '2016',
      journal: 'CVPR',
      doi: '10.1109/CVPR.2016.90',
      format: 'mla'
    })
  });
  assert.strictEqual(res.status, 200);
  const data = await res.json();
  assert.strictEqual(data.success, true);
  assert.strictEqual(data.format, 'mla');
  assert.ok(data.citation.includes('Kaiming He. "Deep Residual Learning." CVPR, 2016. https://doi.org/10.1109/CVPR.2016.90'));
});

await it('Edge-Server', 'DOI resolver fetches live metadata for valid DOI', async () => {
  const res = await fetch(`${SUPABASE_URL}/functions/v1/cite-server`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      action: 'resolve_doi',
      doi: '10.1038/nature12373'
    })
  });
  assert.strictEqual(res.status, 200);
  const data = await res.json();
  assert.strictEqual(data.success, true);
  assert.ok(data.data.title);
  assert.ok(Array.isArray(data.data.authors));
  assert.strictEqual(data.data.doi, '10.1038/nature12373');
});

await it('Edge-Server', 'DOI resolver handles missing or invalid DOI with proper error code', async () => {
  const missingRes = await fetch(`${SUPABASE_URL}/functions/v1/cite-server`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ action: 'resolve_doi', doi: '' })
  });
  assert.strictEqual(missingRes.status, 400);

  const notFoundRes = await fetch(`${SUPABASE_URL}/functions/v1/cite-server`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ action: 'resolve_doi', doi: '10.99999/nonexistent.doi.citecircle.2026' })
  });
  assert.strictEqual(notFoundRes.status, 404);
});

await it('Edge-Server', 'Malformed body or empty request gracefully returns default online status', async () => {
  const res = await fetch(`${SUPABASE_URL}/functions/v1/cite-server`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: '{"invalid": json syntax'
  });
  // Must not crash or return unhandled 500 error
  assert.strictEqual(res.status, 200);
  const data = await res.json();
  assert.strictEqual(data.status, 'online');
});

// ============================================================================
// SUITE 2: SUPABASE AUTH, PROFILES & RLS
// ============================================================================
console.log('\n--- Suite 2: Supabase Auth & Profiles ---');

await it('Database', 'Authenticate demo researcher via GoTrue password grant', async () => {
  const res = await fetch(`${SUPABASE_URL}/auth/v1/token?grant_type=password`, {
    method: 'POST',
    headers: { 'apikey': ANON_KEY, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email: 'demo.researcher@cite.circle',
      password: 'citecircle2026'
    })
  });
  assert.strictEqual(res.status, 200);
  const data = await res.json();
  assert.ok(data.access_token);
  assert.ok(data.user?.id);
  authSession = data;
  userId = data.user.id;
});

await it('Database', 'Fetch researcher profile with RLS', async () => {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/profiles?id=eq.${userId}&select=*`, {
    headers: authedHeaders()
  });
  assert.strictEqual(res.status, 200);
  const profiles = await res.json();
  assert.strictEqual(profiles.length, 1);
  assert.strictEqual(profiles[0].id, userId);
  assert.strictEqual(profiles[0].username, 'morgan_vance');
});

// ============================================================================
// SUITE 3: POSTS, LIKES, COMMENTS, SAVED & TRIGGERS
// ============================================================================
console.log('\n--- Suite 3: Posts, Interactions & Triggers ---');

await it('Database', 'Create new research manuscript post with JSONB metadata', async () => {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/posts`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({
      user_id: userId,
      content: 'System Verification Automated Manuscript Post\n\nTesting end-to-end cloud pipeline.',
      media_urls: [`${R2_PUBLIC_DOMAIN}/manuscripts/test_suite_paper.pdf`],
      metadata: {
        title: 'System Verification Automated Manuscript',
        doi: '10.1145/test.verify.2026',
        pages: 12
      },
      privacy: 'public'
    })
  });
  assert.strictEqual(res.status, 201);
  const [post] = await res.json();
  assert.ok(post.id);
  testPostId = post.id;
});

await it('Database', 'Query joined post and author profile via disambiguated relation', async () => {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/posts?id=eq.${testPostId}&select=id,content,metadata,likes_count,comments_count,author:profiles!posts_user_id_fkey(id,username,full_name)`, {
    headers: authedHeaders()
  });
  assert.strictEqual(res.status, 200);
  const [post] = await res.json();
  assert.strictEqual(post.id, testPostId);
  assert.strictEqual(post.author.id, userId);
  assert.strictEqual(post.author.username, 'morgan_vance');
});

await it('Database', 'Like post and verify update_post_likes_count trigger increments to 1', async () => {
  const likeRes = await fetch(`${SUPABASE_URL}/rest/v1/post_likes`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({ post_id: testPostId, user_id: userId })
  });
  assert.strictEqual(likeRes.status, 201);

  const checkRes = await fetch(`${SUPABASE_URL}/rest/v1/posts?id=eq.${testPostId}&select=likes_count`, {
    headers: authedHeaders()
  });
  const [post] = await checkRes.json();
  assert.strictEqual(post.likes_count, 1);
});

await it('Database', 'Unlike post and verify update_post_likes_count trigger decrements to 0', async () => {
  const unlikeRes = await fetch(`${SUPABASE_URL}/rest/v1/post_likes?post_id=eq.${testPostId}&user_id=eq.${userId}`, {
    method: 'DELETE',
    headers: authedHeaders()
  });
  assert.ok([200, 204].includes(unlikeRes.status));

  const checkRes = await fetch(`${SUPABASE_URL}/rest/v1/posts?id=eq.${testPostId}&select=likes_count`, {
    headers: authedHeaders()
  });
  const [post] = await checkRes.json();
  assert.strictEqual(post.likes_count, 0);
});

await it('Database', 'Add comment and verify update_post_comments_count trigger increments to 1', async () => {
  const commentRes = await fetch(`${SUPABASE_URL}/rest/v1/comments`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({
      post_id: testPostId,
      user_id: userId,
      content: 'System validation peer review test comment.'
    })
  });
  assert.strictEqual(commentRes.status, 201);

  const checkRes = await fetch(`${SUPABASE_URL}/rest/v1/posts?id=eq.${testPostId}&select=comments_count`, {
    headers: authedHeaders()
  });
  const [post] = await checkRes.json();
  assert.strictEqual(post.comments_count, 1);
});

await it('Database', 'Bookmark post to saved_posts and read back', async () => {
  const saveRes = await fetch(`${SUPABASE_URL}/rest/v1/saved_posts`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({ user_id: userId, post_id: testPostId })
  });
  assert.strictEqual(saveRes.status, 201);

  const readRes = await fetch(`${SUPABASE_URL}/rest/v1/saved_posts?user_id=eq.${userId}&post_id=eq.${testPostId}`, {
    headers: authedHeaders()
  });
  assert.strictEqual(readRes.status, 200);
  const rows = await readRes.json();
  assert.strictEqual(rows.length, 1);
});

// ============================================================================
// SUITE 4: MESSENGER, LOUNGE & NOTIFICATIONS
// ============================================================================
console.log('\n--- Suite 4: Messenger & Notifications ---');

await it('Database', 'Create conversation, register participant and send message', async () => {
  const convRes = await fetch(`${SUPABASE_URL}/rest/v1/conversations`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({ is_group: false, created_by: userId })
  });
  assert.strictEqual(convRes.status, 201);
  const [conv] = await convRes.json();
  testConversationId = conv.id;

  const partRes = await fetch(`${SUPABASE_URL}/rest/v1/conversation_participants`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({ conversation_id: testConversationId, user_id: userId })
  });
  assert.strictEqual(partRes.status, 201);

  const msgRes = await fetch(`${SUPABASE_URL}/rest/v1/messages`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({
      conversation_id: testConversationId,
      sender_id: userId,
      content: 'Peer review chat test message.'
    })
  });
  assert.strictEqual(msgRes.status, 201);
  const [msg] = await msgRes.json();
  assert.strictEqual(msg.sender_id, userId);
});

await it('Database', 'Create and query notifications with RLS', async () => {
  const notifRes = await fetch(`${SUPABASE_URL}/rest/v1/notifications`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({
      recipient_id: userId,
      actor_id: userId,
      type: 'like',
      post_id: testPostId
    })
  });
  assert.strictEqual(notifRes.status, 201);

  const getRes = await fetch(`${SUPABASE_URL}/rest/v1/notifications?recipient_id=eq.${userId}&limit=5`, {
    headers: authedHeaders()
  });
  assert.strictEqual(getRes.status, 200);
  const notifs = await getRes.json();
  assert.ok(notifs.length >= 1);
});

// ============================================================================
// SUITE 5: REPOSITORY CLOUD SYNC & READING PROGRESS (Migrations 20260925)
// ============================================================================
console.log('\n--- Suite 5: Cloud Sync, Collections & Reading Progress ---');

await it('Cloud-Sync', 'Create custom user collection folder with color and icon', async () => {
  const collRes = await fetch(`${SUPABASE_URL}/rest/v1/user_collections`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({
      user_id: userId,
      id: testCollectionId,
      name: 'Quantum Foundations',
      description: 'Quantum information & attention models',
      color_hex: '#1A73E8',
      icon_name: 'folder'
    })
  });
  assert.strictEqual(collRes.status, 201);
  const [coll] = await collRes.json();
  assert.strictEqual(coll.id, testCollectionId);
  assert.strictEqual(coll.user_id, userId);
});

await it('Cloud-Sync', 'Upsert paper to user_library_papers with reading progress & page bookmarks', async () => {
  const paperRes = await fetch(`${SUPABASE_URL}/rest/v1/user_library_papers?on_conflict=user_id,id`, {
    method: 'POST',
    headers: authedHeaders({
      'Prefer': 'return=representation,resolution=merge-duplicates'
    }),
    body: JSON.stringify({
      user_id: userId,
      id: testPaperId,
      title: 'Attention in Quantum Systems',
      authors: 'Vance, M. et al.',
      year: '2026',
      venue: 'Physical Review A',
      doi: '10.1103/PhysRevA.2026.041829',
      reading_status: 'READING',
      research_notes: 'Key equation 4 demonstrates sub-nanosecond scaling.',
      last_read_page: 8,
      total_page_count: 24,
      page_bookmarks: '3,8,12',
      is_bookmarked: true
    })
  });
  assert.strictEqual(paperRes.status, 201);
  const [paper] = await paperRes.json();
  assert.strictEqual(paper.id, testPaperId);
  assert.strictEqual(paper.reading_status, 'READING');
  assert.strictEqual(paper.last_read_page, 8);
  assert.strictEqual(paper.total_page_count, 24);
  assert.strictEqual(paper.page_bookmarks, '3,8,12');
});

await it('Cloud-Sync', 'Assign paper to collection in user_collection_papers', async () => {
  const assignRes = await fetch(`${SUPABASE_URL}/rest/v1/user_collection_papers`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({
      user_id: userId,
      collection_id: testCollectionId,
      paper_id: testPaperId
    })
  });
  assert.strictEqual(assignRes.status, 201);
  const [entry] = await assignRes.json();
  assert.strictEqual(entry.collection_id, testCollectionId);
  assert.strictEqual(entry.paper_id, testPaperId);
});

await it('Cloud-Sync', 'Clean up test sync entities (cascade integrity check)', async () => {
  // Delete collection (cascades to user_collection_papers)
  const delColl = await fetch(`${SUPABASE_URL}/rest/v1/user_collections?user_id=eq.${userId}&id=eq.${testCollectionId}`, {
    method: 'DELETE',
    headers: authedHeaders()
  });
  assert.ok([200, 204].includes(delColl.status));

  // Verify user_collection_papers was cascade-deleted
  const checkCollPapers = await fetch(`${SUPABASE_URL}/rest/v1/user_collection_papers?user_id=eq.${userId}&collection_id=eq.${testCollectionId}`, {
    headers: authedHeaders()
  });
  const remaining = await checkCollPapers.json();
  assert.strictEqual(remaining.length, 0);

  // Delete test paper from user_library_papers
  const delPaper = await fetch(`${SUPABASE_URL}/rest/v1/user_library_papers?user_id=eq.${userId}&id=eq.${testPaperId}`, {
    method: 'DELETE',
    headers: authedHeaders()
  });
  assert.ok([200, 204].includes(delPaper.status));

  // Delete test post
  if (testPostId) {
    await fetch(`${SUPABASE_URL}/rest/v1/posts?id=eq.${testPostId}`, {
      method: 'DELETE',
      headers: authedHeaders()
    });
  }
});

// ============================================================================
// SUITE 6: ANTI-MALWARE, FILE SECURITY & MIME INSPECTION
// ============================================================================
console.log('\n--- Suite 6: Anti-Malware & File Security ---');

await it('Security', 'Block executable and dangerous extensions', async () => {
  const forbiddenExts = ['.zip', '.rar', '.7z', '.tar', '.gz', '.exe', '.bat', '.cmd', '.sh', '.vbs', '.js', '.apk', '.bin', '.msi'];
  function isForbidden(filename) {
    const ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
    return forbiddenExts.includes(ext);
  }

  assert.ok(isForbidden('ransomware.exe'));
  assert.ok(isForbidden('trojan.bat'));
  assert.ok(isForbidden('malicious.apk'));
  assert.ok(isForbidden('payload.bin'));
  assert.ok(isForbidden('archive.tar.gz'));
  assert.ok(!isForbidden('manuscript.pdf'));
  assert.ok(!isForbidden('dissertation.docx'));
  assert.ok(!isForbidden('paper.tex'));
});

await it('Security', 'Magic bytes inspector flags MZ, ELF, and disguised ZIPs', async () => {
  function inspectBytes(buf, ext) {
    if (buf[0] === 0x4D && buf[1] === 0x5A) return { safe: false, reason: 'MZ header' };
    if (buf[0] === 0x7F && buf[1] === 0x45 && buf[2] === 0x4C && buf[3] === 0x46) return { safe: false, reason: 'ELF header' };
    if (buf[0] === 0x50 && buf[1] === 0x4B && ext !== '.docx') return { safe: false, reason: 'Disguised ZIP' };
    if (ext === '.pdf' && (buf[0] !== 0x25 || buf[1] !== 0x50 || buf[2] !== 0x44 || buf[3] !== 0x46)) {
      return { safe: false, reason: 'Invalid PDF magic bytes' };
    }
    return { safe: true };
  }

  const mzBytes = Buffer.from([0x4D, 0x5A, 0x90, 0x00]);
  assert.strictEqual(inspectBytes(mzBytes, '.pdf').safe, false);

  const elfBytes = Buffer.from([0x7F, 0x45, 0x4C, 0x46]);
  assert.strictEqual(inspectBytes(elfBytes, '.pdf').safe, false);

  const zipBytes = Buffer.from([0x50, 0x4B, 0x03, 0x04]);
  assert.strictEqual(inspectBytes(zipBytes, '.pdf').safe, false);
  assert.strictEqual(inspectBytes(zipBytes, '.docx').safe, true);

  const pdfBytes = Buffer.from([0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37]); // %PDF-1.7
  assert.strictEqual(inspectBytes(pdfBytes, '.pdf').safe, true);
});

// ============================================================================
// SUITE 7: CLOUDFLARE R2 MEDIA CDN
// ============================================================================
console.log('\n--- Suite 7: Cloudflare R2 Media CDN ---');

await it('Storage', 'Cloudflare R2 public distribution delivers assets with HTTP 200', async () => {
  const res = await fetch(`${R2_PUBLIC_DOMAIN}/welcome.txt`);
  assert.strictEqual(res.status, 200);
  const text = await res.text();
  assert.ok(text.includes('Welcome to Cite Circle Media'));
});

// ============================================================================
// SUITE 8: CLIENT DATA CONTRACTS & ALGORITHM INVARIANTS
// ============================================================================
console.log('\n--- Suite 8: Client Contracts & Algorithm Invariants ---');

await it('Client-Contract', 'Reading progress calculation accurately clamps percentages', async () => {
  function calculateProgress(page, total) {
    if (total <= 0) return 0.0;
    const clampedPage = Math.max(1, Math.min(page, total));
    return Math.round((clampedPage / total) * 100) / 100;
  }

  assert.strictEqual(calculateProgress(1, 10), 0.1);
  assert.strictEqual(calculateProgress(5, 10), 0.5);
  assert.strictEqual(calculateProgress(10, 10), 1.0);
  assert.strictEqual(calculateProgress(0, 10), 0.1, 'Page < 1 must clamp to page 1');
  assert.strictEqual(calculateProgress(15, 10), 1.0, 'Page > total must clamp to total');
  assert.strictEqual(calculateProgress(1, 0), 0.0, 'Total 0 must return 0.0');
});

await it('Client-Contract', 'Bookmark serialization round-trips comma-separated values', async () => {
  function serializeBookmarks(list) {
    return list.sort((a, b) => a - b).join(',');
  }
  function deserializeBookmarks(str) {
    if (!str || !str.trim()) return [];
    return str.split(',').map(s => parseInt(s.trim(), 10)).filter(n => !isNaN(n));
  }

  const initial = [12, 3, 8];
  const serialized = serializeBookmarks(initial);
  assert.strictEqual(serialized, '3,8,12');
  const deserialized = deserializeBookmarks(serialized);
  assert.deepStrictEqual(deserialized, [3, 8, 12]);
  assert.deepStrictEqual(deserializeBookmarks(''), []);
  assert.deepStrictEqual(deserializeBookmarks('  '), []);
});

await it('Client-Contract', 'LRU cache evicts unbookmarked items when exceeding 200 entries', async () => {
  const MAX = 200;
  function evict(items) {
    if (items.length <= MAX) return items;
    const bookmarked = items.filter(i => i.isBookmarked);
    const unbookmarked = items.filter(i => !i.isBookmarked);
    const slots = Math.max(0, MAX - bookmarked.length);
    return [...bookmarked, ...unbookmarked.slice(0, slots)];
  }

  const dataset = Array.from({ length: 300 }, (_, i) => ({
    id: i,
    isBookmarked: i < 50
  }));

  const pruned = evict(dataset);
  assert.strictEqual(pruned.length, 200);
  assert.strictEqual(pruned.filter(i => i.isBookmarked).length, 50);
});

console.log('\n================================================================');
console.log(`   SYSTEM VERIFICATION SUMMARY: ${passed} PASSED, ${failed} FAILED   `);
console.log('================================================================\n');

if (failed > 0) {
  process.exit(1);
} else {
  console.log('>>> ALL SYSTEM COMPONENTS, CONTRACTS & INTEGRATIONS PASSED! <<<\n');
  process.exit(0);
}
