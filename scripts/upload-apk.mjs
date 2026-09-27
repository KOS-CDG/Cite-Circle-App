import fs from 'node:fs';
import path from 'node:path';

const SUPABASE_URL = 'https://cxxtrtglmxfuyihxwiza.supabase.co';
const ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImN4eHRydGdsbXhmdXlpaHh3aXphIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk4ODU3MDYsImV4cCI6MjEwNTQ2MTcwNn0.d0CGm637M1OVOP8IVOuyaru9S4zz0BMqUb-IZB3_eSg';

async function main() {
  console.log('1. Authenticating with Supabase Auth...');
  const authRes = await fetch(`${SUPABASE_URL}/auth/v1/token?grant_type=password`, {
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

  if (!authRes.ok) {
    const errText = await authRes.text();
    throw new Error(`Authentication failed (${authRes.status}): ${errText}`);
  }

  const authData = await authRes.json();
  const accessToken = authData.access_token;
  console.log('✓ Successfully authenticated user:', authData.user?.email);

  const apkPath = path.resolve('app/build/outputs/apk/debug/app-debug.apk');
  if (!fs.existsSync(apkPath)) {
    throw new Error(`APK file not found at ${apkPath}`);
  }

  const fileStats = fs.statSync(apkPath);
  const fileSizeMb = (fileStats.size / (1024 * 1024)).toFixed(2);
  console.log(`2. Reading APK from ${apkPath} (${fileStats.size} bytes, ${fileSizeMb} MB)...`);
  const apkBuffer = fs.readFileSync(apkPath);

  console.log('3. Uploading CiteCircle-latest.apk to app-releases bucket...');
  const uploadRes = await fetch(`${SUPABASE_URL}/storage/v1/object/app-releases/CiteCircle-latest.apk`, {
    method: 'POST',
    headers: {
      'apikey': ANON_KEY,
      'Authorization': `Bearer ${accessToken}`,
      'x-upsert': 'true',
      'Content-Type': 'application/vnd.android.package-archive'
    },
    body: apkBuffer
  });

  if (!uploadRes.ok) {
    const errText = await uploadRes.text();
    throw new Error(`Upload failed (${uploadRes.status}): ${errText}`);
  }

  const uploadResult = await uploadRes.json();
  console.log('✓ Successfully uploaded CiteCircle-latest.apk:', uploadResult);

  console.log('4. Verifying public download URL...');
  const publicUrl = `${SUPABASE_URL}/storage/v1/object/public/app-releases/CiteCircle-latest.apk`;
  const headRes = await fetch(publicUrl, { method: 'HEAD' });
  console.log(`✓ HEAD status: ${headRes.status} ${headRes.statusText}`);
  console.log(`✓ Content-Length: ${headRes.headers.get('content-length')} bytes`);
  console.log(`✓ Content-Type: ${headRes.headers.get('content-type')}`);
  console.log(`\n🎉 Public download URL ready: ${publicUrl}`);
}

main().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
