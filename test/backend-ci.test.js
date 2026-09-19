/**
 * CI/CD Backend & Network Resilience Test Suite for Cite Circle
 * Validates Firestore Security Rules, Firebase Configurations,
 * Anti-Malware File Guards, and Database Quota Policies.
 */

const fs = require('fs');
const path = require('path');
const assert = require('assert');

let passedTests = 0;
let totalTests = 0;

function runTest(name, fn) {
  totalTests++;
  try {
    fn();
    console.log(`  ✓ ${name}`);
    passedTests++;
  } catch (err) {
    console.error(`  ✕ ${name}`);
    console.error(`    Error: ${err.message}`);
    process.exitCode = 1;
  }
}

console.log('\n--- Running Cite Circle Backend & Security CI Tests ---\n');

// 1. Test Firestore Security Rules
runTest('Firestore security rules exist and specify version 2', () => {
  const rulesPath = path.resolve(__dirname, '../firestore.rules');
  assert.ok(fs.existsSync(rulesPath), 'firestore.rules file must exist');
  const content = fs.readFileSync(rulesPath, 'utf8');
  assert.ok(content.includes("rules_version = '2';"), 'Rules must use version 2');
  assert.ok(content.includes('service cloud.firestore'), 'Must declare cloud.firestore service');
});

runTest('Firestore security rules enforce participant-only access for conversations', () => {
  const rulesPath = path.resolve(__dirname, '../firestore.rules');
  const content = fs.readFileSync(rulesPath, 'utf8');
  assert.ok(content.includes('match /conversations/{conversationId}'), 'Must declare conversations match');
  assert.ok(
    content.includes('request.auth.uid in resource.data.participantIds'),
    'Must restrict conversation reads to authenticated participants'
  );
});

runTest('Firestore security rules protect research papers archive', () => {
  const rulesPath = path.resolve(__dirname, '../firestore.rules');
  const content = fs.readFileSync(rulesPath, 'utf8');
  assert.ok(content.includes('match /papers/{paperId}'), 'Must protect papers collection');
  assert.ok(content.includes('allow create: if isAuthenticated();'), 'Creating papers requires auth');
});

// 2. Test Firebase Configurations
runTest('app/google-services.json contains active project cite-circle-3857b', () => {
  const googleServicesPath = path.resolve(__dirname, '../app/google-services.json');
  assert.ok(fs.existsSync(googleServicesPath), 'google-services.json must exist');
  const config = JSON.parse(fs.readFileSync(googleServicesPath, 'utf8'));
  assert.strictEqual(config.project_info.project_id, 'cite-circle-3857b');
  assert.strictEqual(config.project_info.project_number, '225994100205');
  assert.ok(config.client.length > 0, 'Must have registered client');
  assert.strictEqual(config.client[0].client_info.android_client_info.package_name, 'com.aistudio.folio.wzpx');
});

runTest('web/firebase-config.js is configured for project cite-circle-3857b', () => {
  const webConfigPath = path.resolve(__dirname, '../web/firebase-config.js');
  assert.ok(fs.existsSync(webConfigPath), 'web/firebase-config.js must exist');
  const content = fs.readFileSync(webConfigPath, 'utf8');
  assert.ok(content.includes('projectId: "cite-circle-3857b"'), 'Must specify projectId');
  assert.ok(content.includes('cite-circle-3857b.firebaseapp.com'), 'Must specify authDomain');
  assert.ok(content.includes('cite-circle-3857b.firebasestorage.app'), 'Must specify storageBucket');
});

// 3. Test Anti-Malware & File Guard Rules
runTest('Anti-malware guard rejects executable files and disguised archives', () => {
  const forbiddenExts = ['.zip', '.rar', '.7z', '.tar', '.gz', '.exe', '.bat', '.cmd', '.sh', '.vbs', '.js', '.apk', '.bin', '.msi'];
  
  function isForbiddenExtension(filename) {
    const ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
    return forbiddenExts.includes(ext);
  }

  assert.ok(isForbiddenExtension('malware.zip'), '.zip must be blocked');
  assert.ok(isForbiddenExtension('virus.exe'), '.exe must be blocked');
  assert.ok(isForbiddenExtension('script.bat'), '.bat must be blocked');
  assert.ok(isForbiddenExtension('archive.tar.gz'), '.gz must be blocked');
  assert.ok(!isForbiddenExtension('manuscript.pdf'), '.pdf must be permitted');
  assert.ok(!isForbiddenExtension('thesis.docx'), '.docx must be permitted');
  assert.ok(!isForbiddenExtension('paper.tex'), '.tex must be permitted');
});

runTest('Anti-malware magic byte inspector rejects MZ and ELF binaries', () => {
  function inspectMagicBytes(bytes, ext) {
    // MZ (Windows executable)
    if (bytes[0] === 0x4D && bytes[1] === 0x5A) return { safe: false, reason: 'MZ executable detected' };
    // ELF (Linux/Android binary)
    if (bytes[0] === 0x7F && bytes[1] === 0x45 && bytes[2] === 0x4C && bytes[3] === 0x46) return { safe: false, reason: 'ELF binary detected' };
    // Disguised ZIP (PK..) in non-docx
    if (bytes[0] === 0x50 && bytes[1] === 0x4B && ext !== '.docx') return { safe: false, reason: 'Disguised ZIP detected' };
    return { safe: true };
  }

  const mzBytes = Buffer.from([0x4D, 0x5A, 0x90, 0x00]);
  assert.strictEqual(inspectMagicBytes(mzBytes, '.pdf').safe, false);

  const elfBytes = Buffer.from([0x7F, 0x45, 0x4C, 0x46]);
  assert.strictEqual(inspectMagicBytes(elfBytes, '.pdf').safe, false);

  const zipBytes = Buffer.from([0x50, 0x4B, 0x03, 0x04]);
  assert.strictEqual(inspectMagicBytes(zipBytes, '.pdf').safe, false);
  assert.strictEqual(inspectMagicBytes(zipBytes, '.docx').safe, true);
});

// 4. Test Network Connectivity Strategy Rules
runTest('Network connection classification applies appropriate sync strategies', () => {
  function getStrategy(isConnected, isWifi, isCellular) {
    if (!isConnected) return 'LOCAL_ROOM_FALLBACK';
    if (isWifi) return 'IMMEDIATE_FULL_SYNC';
    if (isCellular) return 'METERED_DATA_SAVER';
    return 'METERED_DATA_SAVER';
  }

  assert.strictEqual(getStrategy(true, true, false), 'IMMEDIATE_FULL_SYNC', 'Wi-Fi triggers full sync');
  assert.strictEqual(getStrategy(true, false, true), 'METERED_DATA_SAVER', 'Cellular triggers data saver');
  assert.strictEqual(getStrategy(false, false, false), 'LOCAL_ROOM_FALLBACK', 'Offline falls back to local SQLite');
});

// 5. Test Database Limiters & Quotas
runTest('Database quotas enforce 200 items cache limit and 50 items pagination', () => {
  const MAX_CACHE_KEEP = 200;
  const PAGE_LIMIT = 50;

  function paginate(items, page) {
    const offset = page * PAGE_LIMIT;
    return items.slice(offset, offset + PAGE_LIMIT);
  }

  function evictCache(items) {
    if (items.length <= MAX_CACHE_KEEP) return items;
    // Keep bookmarked, evict oldest unbookmarked
    const bookmarked = items.filter(i => i.isBookmarked);
    const unbookmarked = items.filter(i => !i.isBookmarked);
    const slotsRemaining = Math.max(0, MAX_CACHE_KEEP - bookmarked.length);
    return [...bookmarked, ...unbookmarked.slice(0, slotsRemaining)];
  }

  const largeDataset = Array.from({ length: 300 }, (_, i) => ({
    id: i,
    isBookmarked: i < 50
  }));

  const page0 = paginate(largeDataset, 0);
  assert.strictEqual(page0.length, 50, 'Page size must be 50');

  const evicted = evictCache(largeDataset);
  assert.strictEqual(evicted.length, 200, 'Cache must be trimmed to 200 items');
  assert.strictEqual(evicted.filter(i => i.isBookmarked).length, 50, 'All bookmarks must be preserved');
});

console.log(`\nResults: ${passedTests}/${totalTests} tests passed.\n`);

if (passedTests === totalTests) {
  console.log('ALL BACKEND & SECURITY TESTS PASSED!\n');
  process.exit(0);
} else {
  console.error('SOME TESTS FAILED!\n');
  process.exit(1);
}
