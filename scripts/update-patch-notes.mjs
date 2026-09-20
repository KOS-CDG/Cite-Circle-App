#!/usr/bin/env node

/**
 * Cite Circle Automated Patch Notes & Version Synchronizer
 * 
 * Usage:
 *   node scripts/update-patch-notes.mjs --check
 *   node scripts/update-patch-notes.mjs --dry-run
 *   node scripts/update-patch-notes.mjs --bump minor
 *   node scripts/update-patch-notes.mjs --version 1.3 --build 3
 */

import fs from 'node:fs';
import path from 'node:path';
import { execSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const ROOT_DIR = path.resolve(__dirname, '..');

const PATCH_NOTES_FILE = path.join(ROOT_DIR, 'PATCH_NOTES.md');
const GRADLE_FILE = path.join(ROOT_DIR, 'app', 'build.gradle.kts');
const EDGE_FN_FILE = path.join(ROOT_DIR, 'supabase', 'functions', 'cite-server', 'index.ts');

const args = process.argv.slice(2);
const isCheck = args.includes('--check');
const isDryRun = args.includes('--dry-run');

// Parse flags
function getArg(flag) {
  const idx = args.indexOf(flag);
  return idx !== -1 && args[idx + 1] ? args[idx + 1] : null;
}

const customVersion = getArg('--version');
const customBuild = getArg('--build');
const bumpType = getArg('--bump'); // patch, minor, major

// Helper to read file safely
function readFile(filePath) {
  return fs.readFileSync(filePath, 'utf8');
}

// 1. Read Current Versions
function getCurrentVersions() {
  const gradleContent = readFile(GRADLE_FILE);
  const vCodeMatch = gradleContent.match(/versionCode\s*=\s*(\d+)/);
  const vNameMatch = gradleContent.match(/versionName\s*=\s*["']([^"']+)["']/);

  const edgeContent = readFile(EDGE_FN_FILE);
  const edgeNameMatch = edgeContent.match(/LATEST_VERSION_NAME\s*=\s*["']([^"']+)["']/);
  const edgeCodeMatch = edgeContent.match(/LATEST_VERSION_CODE\s*=\s*(\d+)/);

  return {
    gradle: {
      versionCode: vCodeMatch ? Number(vCodeMatch[1]) : null,
      versionName: vNameMatch ? vNameMatch[1] : null
    },
    edge: {
      versionCode: edgeCodeMatch ? Number(edgeCodeMatch[1]) : null,
      versionName: edgeNameMatch ? edgeNameMatch[1] : null
    }
  };
}

// 2. Perform Consistency Check
if (isCheck) {
  console.log('\n--- Checking Cite Circle Version & Patch Notes Consistency ---\n');
  const current = getCurrentVersions();
  console.log(`Android App:     v${current.gradle.versionName} (Build ${current.gradle.versionCode})`);
  console.log(`cite-server API: v${current.edge.versionName} (Build ${current.edge.versionCode})`);

  let consistent = true;
  if (current.gradle.versionName !== current.edge.versionName) {
    console.error(`  ✕ Version name mismatch: Gradle (${current.gradle.versionName}) vs Edge (${current.edge.versionName})`);
    consistent = false;
  }
  if (current.gradle.versionCode !== current.edge.versionCode) {
    console.error(`  ✕ Version code mismatch: Gradle (${current.gradle.versionCode}) vs Edge (${current.edge.versionCode})`);
    consistent = false;
  }

  const patchNotes = readFile(PATCH_NOTES_FILE);
  const versionRegex = new RegExp(`\\[${current.edge.versionName}(\\.\\d+)?\\]`);
  if (!versionRegex.test(patchNotes)) {
    console.error(`  ✕ PATCH_NOTES.md missing entry for [${current.edge.versionName}]`);
    consistent = false;
  }

  if (consistent) {
    console.log('  ✓ All versions, build codes, and patch notes are perfectly synchronized!\n');
    process.exit(0);
  } else {
    console.error('\nConsistency check failed. Run `node scripts/update-patch-notes.mjs` to synchronize.\n');
    process.exit(1);
  }
}

// 3. Extract Recent Git Commits
function getRecentGitCommits(limit = 10) {
  try {
    const raw = execSync(`git log -n ${limit} --pretty=format:"%h|%s"`, { cwd: ROOT_DIR, encoding: 'utf8' });
    return raw.split('\n').filter(Boolean).map(line => {
      const [hash, ...subject] = line.split('|');
      return { hash, subject: subject.join('|') };
    });
  } catch (e) {
    return [];
  }
}

// 4. Determine Next Version
const current = getCurrentVersions();
let nextVersion = customVersion;
let nextBuild = customBuild ? Number(customBuild) : (current.gradle.versionCode || 1) + 1;

if (!nextVersion) {
  if (bumpType) {
    const parts = (current.gradle.versionName || '1.0').split('.').map(Number);
    while (parts.length < 2) parts.push(0);
    if (bumpType === 'major') {
      parts[0] += 1;
      parts[1] = 0;
    } else if (bumpType === 'minor') {
      parts[1] += 1;
    } else {
      if (parts.length === 2) parts.push(1);
      else parts[2] += 1;
    }
    nextVersion = parts.join('.');
  } else {
    nextVersion = current.gradle.versionName || '1.2';
    nextBuild = current.gradle.versionCode || 2;
  }
}

console.log(`\n======================================================`);
console.log(`  Cite Circle Patch Notes Synchronizer`);
console.log(`  Target Version: v${nextVersion} (Build ${nextBuild})`);
console.log(`======================================================\n`);

// 5. Categorize commits
const commits = getRecentGitCommits(15);
const features = [];
const fixes = [];
const other = [];

for (const c of commits) {
  const s = c.subject;
  if (/^feat/i.test(s)) {
    features.push(s.replace(/^feat(\([^)]+\))?:\s*/i, ''));
  } else if (/^fix/i.test(s)) {
    fixes.push(s.replace(/^fix(\([^)]+\))?:\s*/i, ''));
  } else {
    other.push(s);
  }
}

console.log(`Found ${commits.length} recent commits:`);
console.log(`  • ${features.length} features`);
console.log(`  • ${fixes.length} fixes`);

// Format markdown section
const today = new Date().toISOString().split('T')[0];
let newPatchSection = `## [${nextVersion}] — ${today} (Build ${nextBuild})\n\n`;

if (features.length > 0) {
  newPatchSection += `### Added\n` + features.map(f => `- ${f}`).join('\n') + '\n\n';
}
if (fixes.length > 0) {
  newPatchSection += `### Fixed\n` + fixes.map(f => `- ${f}`).join('\n') + '\n\n';
}

if (isDryRun) {
  console.log('\n[DRY RUN] Generated Patch Note Entry:');
  console.log('--------------------------------------------------');
  console.log(newPatchSection);
  console.log('--------------------------------------------------');
  console.log('[DRY RUN] No files modified.');
  process.exit(0);
}

// 6. Update Files if needed
console.log('Synchronizing configuration across repository...');

// A. Update Gradle
let gradleText = readFile(GRADLE_FILE);
gradleText = gradleText.replace(/versionCode\s*=\s*\d+/, `versionCode = ${nextBuild}`);
gradleText = gradleText.replace(/versionName\s*=\s*["'][^"']+["']/, `versionName = "${nextVersion}"`);
fs.writeFileSync(GRADLE_FILE, gradleText, 'utf8');
console.log(`  ✓ Updated app/build.gradle.kts (versionCode = ${nextBuild}, versionName = "${nextVersion}")`);

// B. Update Edge Function
let edgeText = readFile(EDGE_FN_FILE);
edgeText = edgeText.replace(/export const LATEST_VERSION_NAME\s*=\s*["'][^"']+["'];/, `export const LATEST_VERSION_NAME = "${nextVersion}";`);
edgeText = edgeText.replace(/export const LATEST_VERSION_CODE\s*=\s*\d+;/, `export const LATEST_VERSION_CODE = ${nextBuild};`);
fs.writeFileSync(EDGE_FN_FILE, edgeText, 'utf8');
console.log(`  ✓ Updated cite-server/index.ts (LATEST_VERSION_NAME = "${nextVersion}", LATEST_VERSION_CODE = ${nextBuild})`);

// C. Verify PATCH_NOTES.md
let patchText = readFile(PATCH_NOTES_FILE);
if (!patchText.includes(`## [${nextVersion}]`)) {
  const insertMarker = '---';
  const markerIdx = patchText.indexOf(insertMarker);
  if (markerIdx !== -1) {
    const before = patchText.substring(0, markerIdx + insertMarker.length);
    const after = patchText.substring(markerIdx + insertMarker.length);
    patchText = `${before}\n\n${newPatchSection.trim()}\n${after}`;
    fs.writeFileSync(PATCH_NOTES_FILE, patchText, 'utf8');
    console.log(`  ✓ Appended entry to PATCH_NOTES.md for v${nextVersion}`);
  }
} else {
  console.log(`  ✓ PATCH_NOTES.md already has an entry for v${nextVersion}`);
}

console.log('\n>>> All files synchronized successfully! <<<\n');
