/**
 * E2E Live Integration Test Suite for Cite Circle
 * Tests Supabase Auth, 10 Core Tables, RLS Policies, PostgreSQL Triggers,
 * Disambiguated PostgREST Relationships, Realtime, and Cloudflare R2 CDN.
 */

import assert from 'node:assert';

const SUPABASE_URL = 'https://cxxtrtglmxfuyihxwiza.supabase.co';
const ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImN4eHRydGdsbXhmdXlpaHh3aXphIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk4ODU3MDYsImV4cCI6MjEwNTQ2MTcwNn0.d0CGm637M1OVOP8IVOuyaru9S4zz0BMqUb-IZB3_eSg';
const R2_PUBLIC_DOMAIN = 'https://pub-66f1f2125beb4a028b0dd68e07a78c8a.r2.dev';

let passed = 0;
let failed = 0;
const results = [];

async function test(name, fn) {
  try {
    await fn();
    console.log(`  ✓ ${name}`);
    passed++;
    results.push({ name, status: 'PASS' });
  } catch (err) {
    console.error(`  ✕ ${name}`);
    console.error(`    Error: ${err.message}`);
    failed++;
    results.push({ name, status: 'FAIL', error: err.message });
  }
}

console.log('\n======================================================');
console.log('  Cite Circle: Live Supabase & Cloudflare R2 Test Suite');
console.log('======================================================\n');

// Shared test variables across steps
let authSession = null;
let userId = null;
let testPostId = null;
let testConversationId = null;

// 1. Supabase Auth: Password Sign In
await test('1. Supabase Auth: Authenticate demo researcher user', async () => {
  const res = await fetch(`${SUPABASE_URL}/auth/v1/token?grant_type=password`, {
    method: 'POST',
    headers: {
      'apikey': ANON_KEY,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      email: 'demo.researcher@cite.circle',
      password: 'citecircle2026'
    })
  });

  assert.strictEqual(res.status, 200, `Expected 200 OK, got ${res.status}`);
  const data = await res.json();
  assert.ok(data.access_token, 'Response must contain access_token');
  assert.ok(data.user?.id, 'Response must contain user ID');
  authSession = data;
  userId = data.user.id;
});

// Helper for authenticated fetch
function authedHeaders(extra = {}) {
  return {
    'apikey': ANON_KEY,
    'Authorization': `Bearer ${authSession.access_token}`,
    'Content-Type': 'application/json',
    ...extra
  };
}

// 2. Profiles: Read Profile with RLS
await test('2. Profiles: Fetch authenticated researcher profile', async () => {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/profiles?id=eq.${userId}&select=*`, {
    headers: authedHeaders()
  });
  assert.strictEqual(res.status, 200);
  const profiles = await res.json();
  assert.strictEqual(profiles.length, 1);
  assert.strictEqual(profiles[0].id, userId);
  assert.strictEqual(profiles[0].username, 'morgan_vance');
});

// 3. Posts: Unauthenticated Public Read
await test('3. Posts: Public unauthenticated feed read (RLS check)', async () => {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/posts?select=id,content,privacy&privacy=eq.public&limit=5`, {
    headers: { 'apikey': ANON_KEY }
  });
  assert.strictEqual(res.status, 200);
  const posts = await res.json();
  assert.ok(Array.isArray(posts));
});

// 4. Posts: Create New Post with R2 Media & JSONB Paper Metadata
await test('4. Posts: Insert new manuscript post with R2 link & metadata', async () => {
  const manuscriptUrl = `${R2_PUBLIC_DOMAIN}/manuscripts/quantum_attention_tomography.pdf`;
  const res = await fetch(`${SUPABASE_URL}/rest/v1/posts`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({
      user_id: userId,
      content: 'Sub-Nanosecond Quantum State Estimation via Attention Transformers\n\nFull preprint available on Cloudflare R2.',
      media_urls: [manuscriptUrl],
      metadata: {
        title: 'Sub-Nanosecond Quantum State Estimation via Attention Transformers',
        field: 'Quantum Computing',
        doi: '10.1103/PhysRevA.2026.041829',
        format: 'pdf',
        pages: 18
      },
      privacy: 'public'
    })
  });
  assert.strictEqual(res.status, 201);
  const [post] = await res.json();
  assert.ok(post.id);
  assert.strictEqual(post.likes_count, 0);
  assert.strictEqual(post.comments_count, 0);
  assert.strictEqual(post.media_urls[0], manuscriptUrl);
  testPostId = post.id;
});

// 5. Posts: Disambiguated Relationship Feed Join Query
await test('5. Posts: Query feed with joined author profile (disambiguated fkey)', async () => {
  const queryUrl = `${SUPABASE_URL}/rest/v1/posts?id=eq.${testPostId}&select=id,content,media_urls,metadata,likes_count,comments_count,author:profiles!posts_user_id_fkey(id,username,full_name)`;
  const res = await fetch(queryUrl, {
    headers: authedHeaders()
  });
  assert.strictEqual(res.status, 200, `Feed join failed with status ${res.status}`);
  const [joinedPost] = await res.json();
  assert.ok(joinedPost, 'Joined post must exist');
  assert.strictEqual(joinedPost.author.id, userId);
  assert.strictEqual(joinedPost.author.username, 'morgan_vance');
});

// 6. Post Likes: Insert Like (Endorsement)
await test('6. Post Likes: Insert endorsement on test post', async () => {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/post_likes`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({
      post_id: testPostId,
      user_id: userId
    })
  });
  assert.strictEqual(res.status, 201);
});

// 7. Trigger: Verify like count incremented to 1
await test('7. Trigger: Verify update_post_likes_count auto-incremented likes_count to 1', async () => {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/posts?id=eq.${testPostId}&select=likes_count`, {
    headers: authedHeaders()
  });
  assert.strictEqual(res.status, 200);
  const [post] = await res.json();
  assert.strictEqual(post.likes_count, 1, `Expected likes_count = 1, got ${post.likes_count}`);
});

// 8. Post Likes: Delete Like (Unlike)
await test('8. Post Likes: Remove endorsement from test post', async () => {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/post_likes?post_id=eq.${testPostId}&user_id=eq.${userId}`, {
    method: 'DELETE',
    headers: authedHeaders()
  });
  assert.ok([200, 204].includes(res.status));
});

// 9. Trigger: Verify like count decremented back to 0
await test('9. Trigger: Verify update_post_likes_count auto-decremented likes_count to 0', async () => {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/posts?id=eq.${testPostId}&select=likes_count`, {
    headers: authedHeaders()
  });
  assert.strictEqual(res.status, 200);
  const [post] = await res.json();
  assert.strictEqual(post.likes_count, 0, `Expected likes_count = 0, got ${post.likes_count}`);
});

// 10. Comments: Insert Peer Review Comment
await test('10. Comments: Insert peer review comment on manuscript post', async () => {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/comments`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({
      post_id: testPostId,
      user_id: userId,
      content: 'Fascinating sub-nanosecond tomography results! Does this hold for 4+ entangled qubits?'
    })
  });
  assert.strictEqual(res.status, 201);
});

// 11. Trigger: Verify comment count incremented to 1
await test('11. Trigger: Verify update_post_comments_count auto-incremented comments_count to 1', async () => {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/posts?id=eq.${testPostId}&select=comments_count`, {
    headers: authedHeaders()
  });
  assert.strictEqual(res.status, 200);
  const [post] = await res.json();
  assert.strictEqual(post.comments_count, 1, `Expected comments_count = 1, got ${post.comments_count}`);
});

// 12. Saved Posts (Vault): Bookmark Manuscript
await test('12. Saved Posts: Bookmark paper to researcher vault', async () => {
  // First insert into saved_posts
  const saveRes = await fetch(`${SUPABASE_URL}/rest/v1/saved_posts`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({
      user_id: userId,
      post_id: testPostId
    })
  });
  assert.strictEqual(saveRes.status, 201);

  // Then fetch saved papers joined with post details
  const fetchRes = await fetch(`${SUPABASE_URL}/rest/v1/saved_posts?user_id=eq.${userId}&select=created_at,post:posts(*)`, {
    headers: authedHeaders()
  });
  assert.strictEqual(fetchRes.status, 200);
  const saved = await fetchRes.json();
  assert.ok(saved.length >= 1);
  const found = saved.find(s => s.post.id === testPostId);
  assert.ok(found, 'Saved vault must contain the bookmarked test post');
});

// 13. Messenger: Create Conversation, Add Participant & Send Message
await test('13. Messenger: Create conversation, participant, and send message', async () => {
  // 1. Create conversation with created_by
  const convRes = await fetch(`${SUPABASE_URL}/rest/v1/conversations`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({
      is_group: false,
      created_by: userId
    })
  });
  assert.strictEqual(convRes.status, 201);
  const [conv] = await convRes.json();
  testConversationId = conv.id;

  // 2. Add current user as participant
  const partRes = await fetch(`${SUPABASE_URL}/rest/v1/conversation_participants`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({
      conversation_id: testConversationId,
      user_id: userId
    })
  });
  assert.strictEqual(partRes.status, 201);

  // 3. Send message with attachment
  const msgRes = await fetch(`${SUPABASE_URL}/rest/v1/messages`, {
    method: 'POST',
    headers: authedHeaders({ 'Prefer': 'return=representation' }),
    body: JSON.stringify({
      conversation_id: testConversationId,
      sender_id: userId,
      content: 'Hello! I reviewed your preprint figures.',
      media_urls: [`${R2_PUBLIC_DOMAIN}/figures/figure_1.png`]
    })
  });
  assert.strictEqual(msgRes.status, 201);
  const [msg] = await msgRes.json();
  assert.strictEqual(msg.conversation_id, testConversationId);
  assert.strictEqual(msg.sender_id, userId);
});

// 14. Notifications: Insert notification and read back
await test('14. Notifications: Insert activity alert and fetch with RLS', async () => {
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

  const fetchRes = await fetch(`${SUPABASE_URL}/rest/v1/notifications?recipient_id=eq.${userId}&select=*`, {
    headers: authedHeaders()
  });
  assert.strictEqual(fetchRes.status, 200);
  const notifs = await fetchRes.json();
  assert.ok(notifs.length >= 1);
});

// 15. Cloudflare R2 Public CDN: Verify $0 egress access
await test('15. Cloudflare R2: Public CDN delivers asset with HTTP 200', async () => {
  const res = await fetch(`${R2_PUBLIC_DOMAIN}/welcome.txt`);
  assert.strictEqual(res.status, 200);
  const text = await res.text();
  assert.ok(text.includes('Welcome to Cite Circle Media'));
});

console.log('\n------------------------------------------------------');
console.log(`Summary: ${passed} passed, ${failed} failed.`);
console.log('------------------------------------------------------\n');

if (failed > 0) {
  process.exit(1);
} else {
  console.log('>>> ALL 15 DATABASE & STORAGE CHECKS PASSED PERFECTLY! <<<\n');
  process.exit(0);
}
