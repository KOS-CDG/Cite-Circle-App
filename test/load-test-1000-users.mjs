/**
 * Cite Circle: 1,000 Users Concurrency & Server Stress Test Suite
 * 
 * Simulates 1,000 active users accessing the server infrastructure:
 * - Scenario A: 1,000 users checking for in-app updates (Edge Function GET check_update)
 * - Scenario B: 1,000 users generating paper citations (Edge Function POST format_citation)
 * - Scenario C: 1,000 users querying the public research feed (Supabase PostgREST GET posts)
 * - Scenario D: High-Concurrency Burst (100-250 concurrent VUs)
 * - Scenario E: Upstream External Rate Limit Audit (CrossRef DOI resolver under load)
 * 
 * Tracks Latency Percentiles (Min, P50, P90, P95, P99, Max, Mean), RPS Throughput,
 * HTTP Status codes, and Failure Bottlenecks.
 */

const SUPABASE_URL = 'https://cxxtrtglmxfuyihxwiza.supabase.co';
const ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImN4eHRydGdsbXhmdXlpaHh3aXphIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk4ODU3MDYsImV4cCI6MjEwNTQ2MTcwNn0.d0CGm637M1OVOP8IVOuyaru9S4zz0BMqUb-IZB3_eSg';

// Helper to calculate percentiles
function calculateStats(latencies) {
  if (latencies.length === 0) return {};
  latencies.sort((a, b) => a - b);
  const sum = latencies.reduce((acc, v) => acc + v, 0);
  const getP = (p) => {
    const idx = Math.min(latencies.length - 1, Math.floor(latencies.length * (p / 100)));
    return latencies[idx];
  };

  return {
    count: latencies.length,
    min: Math.round(latencies[0]),
    mean: Math.round(sum / latencies.length),
    p50: Math.round(getP(50)),
    p90: Math.round(getP(90)),
    p95: Math.round(getP(95)),
    p99: Math.round(getP(99)),
    max: Math.round(latencies[latencies.length - 1])
  };
}

async function runScenario({ name, totalRequests, concurrency, requestFactory, timeoutMs = 15000 }) {
  console.log(`\n======================================================`);
  console.log(`  STARTING SCENARIO: ${name}`);
  console.log(`  Target: ${totalRequests} Requests | Concurrency Pool: ${concurrency} VUs`);
  console.log(`======================================================`);

  const latencies = [];
  const statusCodes = {};
  let errors = 0;
  let errorReasons = {};
  let completed = 0;
  let requestIndex = 0;

  const startTime = performance.now();

  async function worker(workerId) {
    while (true) {
      const currentReq = requestIndex++;
      if (currentReq >= totalRequests) break;

      const reqInfo = requestFactory(currentReq, workerId);
      const reqStart = performance.now();
      try {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), timeoutMs);

        const res = await fetch(reqInfo.url, {
          method: reqInfo.method || 'GET',
          headers: reqInfo.headers || {},
          body: reqInfo.body ? JSON.stringify(reqInfo.body) : undefined,
          signal: controller.signal
        });
        clearTimeout(timer);

        const reqEnd = performance.now();
        const duration = reqEnd - reqStart;
        latencies.push(duration);

        const code = res.status;
        statusCodes[code] = (statusCodes[code] || 0) + 1;

        // Drain body to free connection
        await res.text().catch(() => {});
      } catch (err) {
        const reqEnd = performance.now();
        latencies.push(reqEnd - reqStart);
        errors++;
        const reason = err.name === 'AbortError' ? 'TIMEOUT' : (err.code || err.message);
        errorReasons[reason] = (errorReasons[reason] || 0) + 1;
      } finally {
        completed++;
        if (completed % 200 === 0 || completed === totalRequests) {
          process.stdout.write(`  Progress: ${completed}/${totalRequests} requests completed...\r`);
        }
      }
    }
  }

  // Launch concurrency pool
  const workers = [];
  for (let i = 0; i < concurrency; i++) {
    workers.push(worker(i));
  }
  await Promise.all(workers);

  const totalTimeSec = (performance.now() - startTime) / 1000;
  const stats = calculateStats(latencies);
  const rps = Math.round((completed / totalTimeSec) * 10) / 10;
  const successRate = Math.round(((statusCodes[200] || 0) + (statusCodes[201] || 0)) / totalRequests * 1000) / 10;

  console.log(`\n\n--- RESULTS FOR: ${name} ---`);
  console.log(`  Total Requests:     ${completed} in ${totalTimeSec.toFixed(2)}s`);
  console.log(`  Throughput:         ${rps} req/sec`);
  console.log(`  Success Rate:       ${successRate}%`);
  console.log(`  Status Codes:       ${JSON.stringify(statusCodes)}`);
  if (errors > 0) {
    console.log(`  Errors Count:       ${errors}`);
    console.log(`  Error Reasons:      ${JSON.stringify(errorReasons)}`);
  }
  console.log(`  Latency Breakdown:`);
  console.log(`    • Min:            ${stats.min} ms`);
  console.log(`    • Mean:           ${stats.mean} ms`);
  console.log(`    • Median (P50):   ${stats.p50} ms`);
  console.log(`    • P90:            ${stats.p90} ms`);
  console.log(`    • P95:            ${stats.p95} ms`);
  console.log(`    • P99:            ${stats.p99} ms`);
  console.log(`    • Max:            ${stats.max} ms`);

  return {
    name,
    totalRequests,
    concurrency,
    totalTimeSec,
    rps,
    successRate,
    statusCodes,
    errors,
    errorReasons,
    stats
  };
}

// MAIN RUNNER
async function main() {
  console.log(`\n################################################################`);
  console.log(`   CITE CIRCLE: 1,000 USERS LIVE SERVER LOAD & BOTTLENECK TEST   `);
  console.log(`################################################################\n`);

  const report = [];

  // TEST 1: 1,000 Users checking for app updates
  // Simulates thundering herd when new app version drops or users open app
  const test1 = await runScenario({
    name: '1,000 Users: Edge Function Update Check (GET cite-server?action=check_update)',
    totalRequests: 1000,
    concurrency: 50,
    requestFactory: (reqIndex) => ({
      url: `${SUPABASE_URL}/functions/v1/cite-server?action=check_update&code=${(reqIndex % 12) + 1}`,
      method: 'GET',
      headers: {
        'apikey': ANON_KEY
      }
    })
  });
  report.push(test1);

  // TEST 2: 1,000 Users generating citations concurrently (CPU & Edge processing)
  const formats = ['apa', 'bibtex', 'ieee', 'mla'];
  const test2 = await runScenario({
    name: '1,000 Users: Citation Formatting Engine (POST cite-server action=format_citation)',
    totalRequests: 1000,
    concurrency: 50,
    requestFactory: (reqIndex) => ({
      url: `${SUPABASE_URL}/functions/v1/cite-server`,
      method: 'POST',
      headers: {
        'apikey': ANON_KEY,
        'Content-Type': 'application/json'
      },
      body: {
        action: 'format_citation',
        title: `Research Study #${reqIndex}: Quantum Attention Networks`,
        author: `Author ${reqIndex % 50}, Initial J.`,
        year: 2026,
        journal: 'Cite Circle Transactions on Computation',
        doi: `10.1145/3372278.${reqIndex + 1000}`,
        format: formats[reqIndex % formats.length]
      }
    })
  });
  report.push(test2);

  // TEST 3: 1,000 Users fetching feed from Supabase PostgREST
  // Simulates 1,000 users reading public feed at the same time (Database pooler test)
  const test3 = await runScenario({
    name: '1,000 Users: Public Feed PostgREST Query (GET /rest/v1/posts)',
    totalRequests: 1000,
    concurrency: 50,
    requestFactory: () => ({
      url: `${SUPABASE_URL}/rest/v1/posts?select=id,content,likes_count,comments_count,created_at,metadata,author:profiles!posts_user_id_fkey(username,full_name)&limit=10&order=created_at.desc`,
      method: 'GET',
      headers: {
        'apikey': ANON_KEY
      }
    })
  });
  report.push(test3);

  // TEST 4: High-Concurrency Burst (100 Concurrent Virtual Users) on Edge Function
  const test4 = await runScenario({
    name: 'High-Concurrency Stress Burst: 1,000 Requests @ 100 Concurrent VUs',
    totalRequests: 1000,
    concurrency: 100,
    requestFactory: () => ({
      url: `${SUPABASE_URL}/functions/v1/cite-server?action=patch_notes`,
      method: 'GET',
      headers: {
        'apikey': ANON_KEY
      }
    })
  });
  report.push(test4);

  // TEST 5: External Dependency Bottleneck Test (DOI Resolution via CrossRef)
  // We limit to 30 requests to observe the latency impact and avoid CrossRef IP ban
  const test5 = await runScenario({
    name: 'External Bottleneck Audit: DOI Resolution (CrossRef Upstream)',
    totalRequests: 30,
    concurrency: 5,
    requestFactory: (reqIndex) => ({
      url: `${SUPABASE_URL}/functions/v1/cite-server`,
      method: 'POST',
      headers: {
        'apikey': ANON_KEY,
        'Content-Type': 'application/json'
      },
      body: {
        action: 'resolve_doi',
        doi: '10.1038/nature12373'
      }
    })
  });
  report.push(test5);

  // PRINT COMPREHENSIVE EXECUTIVE SUMMARY
  console.log(`\n\n================================================================`);
  console.log(`            EXECUTIVE LOAD TESTING & BOTTLENECK REPORT           `);
  console.log(`================================================================\n`);

  console.table(report.map(r => ({
    Scenario: r.name.substring(0, 45) + '...',
    Total: r.totalRequests,
    Concurrency: r.concurrency,
    SuccessRate: `${r.successRate}%`,
    RPS: r.rps,
    P50_ms: r.stats.p50,
    P95_ms: r.stats.p95,
    P99_ms: r.stats.p99,
    Max_ms: r.stats.max
  })));

  console.log('\n>>> LOAD & STRESS TESTING COMPLETED SUCCESSFULLY <<<\n');
}

main().catch(err => {
  console.error('Fatal load test error:', err);
  process.exit(1);
});
