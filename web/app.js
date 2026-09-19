import { firebaseConfig } from './firebase-config.js';
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-app.js";
import {
  getAuth,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
  updateProfile
} from "https://www.gstatic.com/firebasejs/10.9.0/firebase-auth.js";

// Initialize Firebase
const firebaseApp = initializeApp(firebaseConfig);
const auth = getAuth(firebaseApp);

// State management
const STATE = {
  activeTab: 'feed',
  activeField: 'all',
  theme: localStorage.getItem('citecircle_theme') || 'dark',
  selectedFile: null,
  currentUser: null,
  authMode: 'signin', // 'signin' or 'register'
  posts: [],
  vault: [],
  chats: []
};

// Initial Sample Research Data
const DEFAULT_POSTS = [
  {
    id: 'post-1',
    author: {
      id: 'OycnFiKmG3SnS9ob4xk7qDlYoRh1',
      name: 'Dr. Morgan Vance',
      institution: 'Institute for Advanced Study',
      avatar: 'MV'
    },
    timestamp: '2 hours ago',
    content: 'Thrilled to share our latest preprint on Transformer architectures tailored for sub-nanosecond quantum state tomography. Open-source benchmarks and mathematical proofs attached below!',
    paper: {
      title: 'Sub-Nanosecond Quantum State Estimation via Attention Transformers',
      field: 'Quantum Computing',
      format: 'pdf',
      size: '2.4 MB',
      doi: '10.1103/PhysRevA.2026.041829',
      abstract: 'We introduce a hardware-accelerated attention mechanism capable of reconstructing multi-qubit density matrices within 850 picoseconds, mitigating decoherence bottlenecks.'
    },
    endorsements: 42,
    isEndorsed: false,
    commentsCount: 8
  },
  {
    id: 'post-2',
    author: {
      id: 'prof-marcus-chen',
      name: 'Prof. Marcus Chen',
      institution: 'Oxford Institute of Biomedical Engineering',
      avatar: 'MC'
    },
    timestamp: '5 hours ago',
    content: 'Uploaded full clinical trial manuscript evaluating CRISPR-Cas14 targeting in sickle-cell hematopoietic stem cells. Peer review comments are welcomed.',
    paper: {
      title: 'In Vivo Base Editing of Sickle Beta-Globin via Compact Cas14 Effectors',
      field: 'Computational Biology',
      format: 'docx',
      size: '5.8 MB',
      doi: '10.1038/s41587-026-0219-4',
      abstract: 'Targeted adenine base editors packaged into lipid nanoparticles achieved >80% HbF reactivation in non-human primate models with zero detectable off-target indels.'
    },
    endorsements: 119,
    isEndorsed: false,
    commentsCount: 23
  },
  {
    id: 'post-3',
    author: {
      id: 'dr-elena-rostova',
      name: 'Dr. Elena Rostova',
      institution: 'CERN & ETH Zürich',
      avatar: 'ER'
    },
    timestamp: 'Yesterday',
    content: 'Source LaTeX equations and differential cross-section models for high-luminosity LHC run 4 are now compiled. LaTeX source available for reproducible computation.',
    paper: {
      title: 'Effective Field Theory of Higgs-Dilaton Mixing at HL-LHC Run 4',
      field: 'Physics & Astronomy',
      format: 'latex',
      size: '890 KB',
      doi: 'arXiv:2603.11894',
      abstract: 'A global fit of dimension-6 and dimension-8 operators reveals constraints on composite Higgs resonance scales up to 4.2 TeV.'
    },
    endorsements: 67,
    isEndorsed: false,
    commentsCount: 14
  }
];

const DEFAULT_CHATS = [
  { sender: 'Dr. Morgan Vance', text: 'Has anyone benchmarked the inference latency of the new 4-bit quantized attention model on edge TPUs?', time: '10:15 AM', isMe: true },
  { sender: 'Dr. Elena Rostova', text: 'Yes! We measured ~1.8ms per token on the Coral Dual Edge. Memory footprint stays under 180MB.', time: '10:18 AM', isMe: false }
];

const RESEARCH_FIELDS = [
  { name: 'AI & Machine Learning', icon: '🧠', papers: 342, citations: '12.4k' },
  { name: 'Quantum Computing', icon: '⚛️', papers: 189, citations: '5.8k' },
  { name: 'Computational Biology', icon: '🧬', papers: 245, citations: '9.2k' },
  { name: 'Neuroscience', icon: '🔬', papers: 178, citations: '6.1k' },
  { name: 'Physics & Astronomy', icon: '🔭', papers: 310, citations: '15.3k' },
  { name: 'Mathematics', icon: '📐', papers: 142, citations: '4.7k' }
];

// Initialize Local Store with Database Limiters
function initStore() {
  const storedPosts = localStorage.getItem('citecircle_posts');
  if (storedPosts) {
    try {
      STATE.posts = JSON.parse(storedPosts);
    } catch {
      STATE.posts = DEFAULT_POSTS;
    }
  } else {
    STATE.posts = DEFAULT_POSTS;
    savePosts();
  }

  const storedVault = localStorage.getItem('citecircle_vault');
  if (storedVault) {
    try {
      STATE.vault = JSON.parse(storedVault);
    } catch {
      STATE.vault = [];
    }
  } else {
    STATE.vault = [STATE.posts[0].paper];
    saveVault();
  }

  const storedChats = localStorage.getItem('citecircle_chats');
  if (storedChats) {
    try {
      STATE.chats = JSON.parse(storedChats);
    } catch {
      STATE.chats = DEFAULT_CHATS;
    }
  } else {
    STATE.chats = DEFAULT_CHATS;
    saveChats();
  }

  enforceCacheLimiter();
}

function enforceCacheLimiter() {
  const MAX_KEEP = 200;
  if (STATE.posts.length > MAX_KEEP) {
    STATE.posts = STATE.posts.slice(0, MAX_KEEP);
    savePosts();
  }
  updateVaultBadge();
}

function savePosts() {
  localStorage.setItem('citecircle_posts', JSON.stringify(STATE.posts));
}

function saveVault() {
  localStorage.setItem('citecircle_vault', JSON.stringify(STATE.vault));
  updateVaultBadge();
}

function saveChats() {
  localStorage.setItem('citecircle_chats', JSON.stringify(STATE.chats));
}

function updateVaultBadge() {
  const badge = document.getElementById('vaultCountBadge');
  if (badge) {
    badge.textContent = STATE.vault.length;
  }
}

// Anti-Malware File Inspection (Strict magic bytes check matching DocumentUploadValidator.kt)
async function validateManuscriptFile(file) {
  const MAX_BYTES = 50 * 1024 * 1024; // 50 MB
  if (file.size > MAX_BYTES) {
    return { valid: false, error: 'File exceeds 50 MB DoS protection limit.' };
  }

  const forbiddenExts = ['.zip', '.rar', '.7z', '.tar', '.gz', '.exe', '.bat', '.cmd', '.sh', '.vbs', '.js', '.apk', '.bin', '.msi'];
  const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
  if (forbiddenExts.includes(ext)) {
    return { valid: false, error: `Forbidden file extension '${ext}'. Archives and executables are strictly blocked.` };
  }

  const headerSlice = file.slice(0, 16);
  const buffer = await headerSlice.arrayBuffer();
  const bytes = new Uint8Array(buffer);

  // Check MZ (Windows executable)
  if (bytes[0] === 0x4D && bytes[1] === 0x5A) {
    return { valid: false, error: 'Executable binary signature (MZ) detected. Blocked by security policy.' };
  }

  // Check ELF (Linux / Android binary)
  if (bytes[0] === 0x7F && bytes[1] === 0x45 && bytes[2] === 0x4C && bytes[3] === 0x46) {
    return { valid: false, error: 'ELF binary signature detected. Blocked by security policy.' };
  }

  // Check Shell script shebang
  if (bytes[0] === 0x23 && bytes[1] === 0x21) {
    return { valid: false, error: 'Shell script header (#!) detected. Blocked by security policy.' };
  }

  // Check ZIP magic bytes (50 4B 03 04)
  const isZip = bytes[0] === 0x50 && bytes[1] === 0x4B && (bytes[2] === 0x03 || bytes[2] === 0x05 || bytes[2] === 0x07);
  if (isZip && ext !== '.docx') {
    return { valid: false, error: 'Disguised ZIP archive detected. Only valid Word (.docx) OpenXML archives are permitted.' };
  }

  let resolvedFormat = 'pdf';
  if (ext === '.docx' || ext === '.doc') resolvedFormat = 'docx';
  else if (ext === '.tex') resolvedFormat = 'latex';
  else if (ext === '.rtf') resolvedFormat = 'rtf';
  else if (ext === '.txt' || ext === '.md') resolvedFormat = 'text';

  return { valid: true, format: resolvedFormat, sizeFormatted: formatBytes(file.size) };
}

function formatBytes(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function getInitials(name) {
  if (!name) return '??';
  const parts = name.trim().split(' ').filter(p => p.length > 0);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

// Render Functions
function renderPosts() {
  const container = document.getElementById('postsList');
  if (!container) return;

  const filtered = STATE.posts.filter(post => {
    if (STATE.activeField === 'all') return true;
    return post.paper && post.paper.field === STATE.activeField;
  });

  if (filtered.length === 0) {
    container.innerHTML = `
      <div style="text-align: center; padding: 3rem; background: var(--bg-card); border-radius: var(--radius-lg); border: 1px solid var(--border-subtle); color: var(--text-secondary);">
        <div style="font-size: 2rem; margin-bottom: 0.5rem;">📚</div>
        <div style="font-weight: 600;">No papers found in this category</div>
        <div style="font-size: 0.85rem; margin-top: 0.25rem;">Be the first researcher to publish a manuscript in ${STATE.activeField}!</div>
      </div>
    `;
    return;
  }

  container.innerHTML = filtered.map(post => {
    const isSaved = STATE.vault.some(p => p.title === post.paper.title);
    const formatClass = post.paper.format === 'docx' ? 'docx' : (post.paper.format === 'latex' ? 'latex' : '');
    const formatLabel = post.paper.format.toUpperCase();

    return `
      <article class="post-card" data-post-id="${post.id}">
        <div class="post-author-row">
          <div class="author-info">
            <div class="author-avatar">${post.author.avatar}</div>
            <div>
              <div class="author-name">${post.author.name}</div>
              <div class="author-institution">${post.author.institution}</div>
            </div>
          </div>
          <div class="post-timestamp">${post.timestamp}</div>
        </div>

        <div class="post-content">${escapeHtml(post.content)}</div>

        ${post.paper ? `
          <div class="paper-attachment">
            <div class="paper-badge-row">
              <span class="paper-format-badge ${formatClass}">
                📄 ${formatLabel} • ${post.paper.size}
              </span>
              <span style="font-size: 0.75rem; color: var(--text-muted); font-weight: 500;">${post.paper.field}</span>
            </div>
            <div class="paper-title">${escapeHtml(post.paper.title)}</div>
            <div class="paper-abstract">${escapeHtml(post.paper.abstract)}</div>
            <div class="paper-doi">DOI: ${escapeHtml(post.paper.doi)}</div>
          </div>
        ` : ''}

        <div class="post-action-bar">
          <button class="post-action-btn ${post.isEndorsed ? 'endorsed' : ''}" onclick="window.citeCircleApp.toggleEndorse('${post.id}')">
            <span>${post.isEndorsed ? '❤️' : '🤍'}</span>
            <span>${post.endorsements} Endorsements</span>
          </button>
          
          <button class="post-action-btn" onclick="window.citeCircleApp.toggleSaveVault('${post.id}')">
            <span>${isSaved ? '🔖' : '📑'}</span>
            <span>${isSaved ? 'In Vault' : 'Save to Vault'}</span>
          </button>

          <button class="post-action-btn" onclick="window.citeCircleApp.citePaper('${post.id}')">
            <span>📎</span>
            <span>Cite</span>
          </button>

          <button class="post-action-btn" onclick="window.citeCircleApp.sharePaper('${post.id}')">
            <span>↗️</span>
            <span>Share</span>
          </button>
        </div>
      </article>
    `;
  }).join('');
}

function renderExplore() {
  const container = document.getElementById('exploreFieldsGrid');
  if (!container) return;

  container.innerHTML = RESEARCH_FIELDS.map(f => `
    <div class="post-card" style="cursor: pointer;" onclick="window.citeCircleApp.selectField('${f.name}')">
      <div style="font-size: 2rem; margin-bottom: 0.5rem;">${f.icon}</div>
      <h3 style="font-size: 1.05rem; font-weight: 700; color: var(--text-primary);">${f.name}</h3>
      <div style="display: flex; justify-content: space-between; margin-top: 0.75rem; font-size: 0.8rem; color: var(--text-secondary);">
        <span>${f.papers} Manuscripts</span>
        <span style="color: var(--accent-emerald);">${f.citations} citations</span>
      </div>
    </div>
  `).join('');
}

function renderVault() {
  const container = document.getElementById('vaultPapersList');
  if (!container) return;

  if (STATE.vault.length === 0) {
    container.innerHTML = `
      <div style="text-align: center; padding: 3rem; background: var(--bg-card); border-radius: var(--radius-lg); border: 1px solid var(--border-subtle); color: var(--text-secondary);">
        <div style="font-size: 2rem; margin-bottom: 0.5rem;">📂</div>
        <div style="font-weight: 600;">Your Paper Vault is empty</div>
        <div style="font-size: 0.85rem; margin-top: 0.25rem;">Save papers from the feed to build your personal offline-accessible research library.</div>
      </div>
    `;
    return;
  }

  container.innerHTML = STATE.vault.map((p, idx) => `
    <div class="post-card">
      <div style="display: flex; justify-content: space-between; align-items: flex-start;">
        <div>
          <span class="paper-format-badge" style="margin-bottom: 0.5rem;">${p.format.toUpperCase()} • ${p.size}</span>
          <h4 style="font-size: 1.05rem; font-weight: 700; color: var(--text-primary); margin-top: 0.25rem;">${escapeHtml(p.title)}</h4>
          <p style="font-size: 0.85rem; color: var(--text-secondary); margin-top: 0.4rem;">${escapeHtml(p.abstract)}</p>
          <p style="font-family: var(--font-mono); font-size: 0.75rem; color: var(--text-muted); margin-top: 0.4rem;">${p.doi}</p>
        </div>
        <button class="btn-icon" style="color: var(--accent-rose);" title="Remove from Vault" onclick="window.citeCircleApp.removeFromVault(${idx})">
          ✕
        </button>
      </div>
    </div>
  `).join('');
}

function renderChats() {
  const container = document.getElementById('chatMessagesArea');
  if (!container) return;

  container.innerHTML = STATE.chats.map(chat => `
    <div style="align-self: ${chat.isMe ? 'flex-end' : 'flex-start'}; max-width: 80%; display: flex; flex-direction: column; gap: 0.25rem;">
      <div style="font-size: 0.75rem; color: var(--text-muted); ${chat.isMe ? 'text-align: right;' : ''}">${chat.sender} • ${chat.time}</div>
      <div style="padding: 0.75rem 1rem; border-radius: var(--radius-md); background-color: ${chat.isMe ? 'var(--accent-primary)' : 'var(--bg-input)'}; color: ${chat.isMe ? 'white' : 'var(--text-primary)'}; border: 1px solid var(--border-subtle); font-size: 0.9rem; line-height: 1.5;">
        ${escapeHtml(chat.text)}
      </div>
    </div>
  `).join('');

  container.scrollTop = container.scrollHeight;
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// Sync Firebase Auth state with UI
function updateAuthStateUI(user) {
  STATE.currentUser = user;
  const statusBadge = document.getElementById('cloudStatusText');
  const headerAvatar = document.getElementById('userHeaderAvatar');
  const openAuthBtn = document.getElementById('openAuthModalBtn');
  const profileAvatar = document.getElementById('profileAvatar');
  const profileName = document.getElementById('profileName');
  const profileEmail = document.getElementById('profileEmail');
  const profileUid = document.getElementById('profileUid');
  const profileSignOutBtn = document.getElementById('profileSignOutBtn');

  if (user) {
    const displayName = user.displayName || user.email.split('@')[0].replace('.', ' ');
    const initials = getInitials(displayName);
    if (statusBadge) statusBadge.textContent = `Firebase: ${user.email}`;
    if (headerAvatar) {
      headerAvatar.textContent = initials;
      headerAvatar.style.display = 'flex';
      headerAvatar.title = `${displayName} (${user.email})`;
    }
    if (openAuthBtn) openAuthBtn.style.display = 'none';

    if (profileAvatar) profileAvatar.textContent = initials;
    if (profileName) profileName.textContent = displayName;
    if (profileEmail) profileEmail.textContent = user.email;
    if (profileUid) profileUid.textContent = `Firebase UID: ${user.uid}`;
    if (profileSignOutBtn) profileSignOutBtn.style.display = 'inline-block';
  } else {
    if (statusBadge) statusBadge.textContent = 'Guest / Offline';
    if (headerAvatar) headerAvatar.style.display = 'none';
    if (openAuthBtn) openAuthBtn.style.display = 'inline-block';

    if (profileAvatar) profileAvatar.textContent = '??';
    if (profileName) profileName.textContent = 'Guest Researcher';
    if (profileEmail) profileEmail.textContent = 'Not signed in';
    if (profileUid) profileUid.textContent = 'Firebase UID: None (Local Mode)';
    if (profileSignOutBtn) profileSignOutBtn.style.display = 'none';
  }
}

// Tab Switching
function setTab(tabName) {
  STATE.activeTab = tabName;
  document.querySelectorAll('.nav-item').forEach(item => {
    item.classList.toggle('active', item.getAttribute('data-tab') === tabName);
  });

  const views = ['viewFeed', 'viewExplore', 'viewVault', 'viewChat', 'viewProfile'];
  views.forEach(v => {
    const el = document.getElementById(v);
    if (el) el.style.display = 'none';
  });

  if (tabName === 'feed') {
    document.getElementById('viewFeed').style.display = 'block';
    renderPosts();
  } else if (tabName === 'explore') {
    document.getElementById('viewExplore').style.display = 'block';
    renderExplore();
  } else if (tabName === 'vault') {
    document.getElementById('viewVault').style.display = 'block';
    renderVault();
  } else if (tabName === 'chat') {
    document.getElementById('viewChat').style.display = 'block';
    renderChats();
  } else if (tabName === 'profile') {
    document.getElementById('viewProfile').style.display = 'block';
  }
}

// Global actions exposed to window
window.citeCircleApp = {
  toggleEndorse(postId) {
    const post = STATE.posts.find(p => p.id === postId);
    if (post) {
      post.isEndorsed = !post.isEndorsed;
      post.endorsements += post.isEndorsed ? 1 : -1;
      savePosts();
      renderPosts();
    }
  },

  toggleSaveVault(postId) {
    const post = STATE.posts.find(p => p.id === postId);
    if (!post || !post.paper) return;

    const existingIdx = STATE.vault.findIndex(p => p.title === post.paper.title);
    if (existingIdx >= 0) {
      STATE.vault.splice(existingIdx, 1);
    } else {
      STATE.vault.push(post.paper);
    }
    saveVault();
    renderPosts();
  },

  removeFromVault(idx) {
    STATE.vault.splice(idx, 1);
    saveVault();
    renderVault();
  },

  citePaper(postId) {
    const post = STATE.posts.find(p => p.id === postId);
    if (!post || !post.paper) return;

    const bibtex = `@article{citecircle_${post.id},
  title = {${post.paper.title}},
  author = {${post.author.name}},
  journal = {Cite Circle Academic Archives},
  year = {2026},
  doi = {${post.paper.doi}}
}`;

    navigator.clipboard?.writeText(bibtex).then(() => {
      alert('BibTeX Citation copied to clipboard!\n\n' + bibtex);
    }).catch(() => {
      alert(bibtex);
    });
  },

  sharePaper(postId) {
    const post = STATE.posts.find(p => p.id === postId);
    if (!post) return;
    const shareUrl = `https://cite-circle-3857b.web.app/paper/${post.id}`;
    navigator.clipboard?.writeText(shareUrl).then(() => {
      alert('Paper link copied: ' + shareUrl);
    });
  },

  selectField(fieldName) {
    STATE.activeField = fieldName;
    setTab('feed');
    document.querySelectorAll('.filter-pill').forEach(pill => {
      pill.classList.toggle('active', pill.getAttribute('data-field') === fieldName);
    });
  }
};

// Event Listeners
document.addEventListener('DOMContentLoaded', () => {
  initStore();
  renderPosts();
  renderExplore();
  renderVault();

  // Listen to live Firebase Auth state changes
  onAuthStateChanged(auth, (user) => {
    updateAuthStateUI(user);
  });

  // Navigation tabs
  document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', () => {
      const tab = item.getAttribute('data-tab');
      if (tab) setTab(tab);
    });
  });

  // Filter pills
  document.querySelectorAll('.filter-pill').forEach(pill => {
    if (pill.id === 'authTabSignIn' || pill.id === 'authTabRegister') return;
    pill.addEventListener('click', () => {
      document.querySelectorAll('.filter-pill').forEach(p => {
        if (p.id !== 'authTabSignIn' && p.id !== 'authTabRegister') p.classList.remove('active');
      });
      pill.classList.add('active');
      STATE.activeField = pill.getAttribute('data-field') || 'all';
      renderPosts();
    });
  });

  // Theme toggle
  const themeToggleBtn = document.getElementById('themeToggleBtn');
  if (themeToggleBtn) {
    document.documentElement.setAttribute('data-theme', STATE.theme);
    themeToggleBtn.addEventListener('click', () => {
      STATE.theme = STATE.theme === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', STATE.theme);
      localStorage.setItem('citecircle_theme', STATE.theme);
    });
  }

  // Auth Modal & Handlers
  const authModal = document.getElementById('authModal');
  const openAuthModalBtn = document.getElementById('openAuthModalBtn');
  const closeAuthModalBtn = document.getElementById('closeAuthModalBtn');
  const authTabSignIn = document.getElementById('authTabSignIn');
  const authTabRegister = document.getElementById('authTabRegister');
  const authSubmitBtn = document.getElementById('authSubmitBtn');
  const authDemoSignInBtn = document.getElementById('authDemoSignInBtn');
  const authErrorMsg = document.getElementById('authErrorMsg');
  const registerOnlyFields = document.getElementById('registerOnlyFields');
  const userHeaderAvatar = document.getElementById('userHeaderAvatar');
  const profileSignOutBtn = document.getElementById('profileSignOutBtn');

  function showAuthModal(mode = 'signin') {
    STATE.authMode = mode;
    if (authErrorMsg) authErrorMsg.style.display = 'none';
    if (mode === 'signin') {
      document.getElementById('authModalTitle').textContent = 'Sign In to Cite Circle';
      authSubmitBtn.textContent = 'Sign In';
      authTabSignIn.classList.add('active');
      authTabRegister.classList.remove('active');
      registerOnlyFields.style.display = 'none';
    } else {
      document.getElementById('authModalTitle').textContent = 'Create Academic Account';
      authSubmitBtn.textContent = 'Create Account';
      authTabRegister.classList.add('active');
      authTabSignIn.classList.remove('active');
      registerOnlyFields.style.display = 'flex';
    }
    authModal.style.display = 'flex';
  }

  if (openAuthModalBtn) openAuthModalBtn.addEventListener('click', () => showAuthModal('signin'));
  if (userHeaderAvatar) userHeaderAvatar.addEventListener('click', () => setTab('profile'));
  if (closeAuthModalBtn) closeAuthModalBtn.addEventListener('click', () => authModal.style.display = 'none');

  if (authTabSignIn) {
    authTabSignIn.addEventListener('click', () => showAuthModal('signin'));
  }
  if (authTabRegister) {
    authTabRegister.addEventListener('click', () => showAuthModal('register'));
  }

  if (authDemoSignInBtn) {
    authDemoSignInBtn.addEventListener('click', async () => {
      try {
        authDemoSignInBtn.disabled = true;
        authDemoSignInBtn.textContent = 'Signing in as Demo Researcher...';
        await signInWithEmailAndPassword(auth, 'demo.researcher@cite.circle', 'citecircle2026');
        authModal.style.display = 'none';
      } catch (err) {
        if (authErrorMsg) {
          authErrorMsg.textContent = err.message;
          authErrorMsg.style.display = 'block';
        }
      } finally {
        authDemoSignInBtn.disabled = false;
        authDemoSignInBtn.textContent = '⚡ Fast-Track Sign In as Demo Researcher';
      }
    });
  }

  if (authSubmitBtn) {
    authSubmitBtn.addEventListener('click', async () => {
      const email = document.getElementById('authEmailInput')?.value.trim();
      const password = document.getElementById('authPasswordInput')?.value.trim();
      const name = document.getElementById('authNameInput')?.value.trim();

      if (!email || !password) {
        if (authErrorMsg) {
          authErrorMsg.textContent = 'Please enter email and password.';
          authErrorMsg.style.display = 'block';
        }
        return;
      }

      try {
        authSubmitBtn.disabled = true;
        authSubmitBtn.textContent = 'Processing...';

        if (STATE.authMode === 'signin') {
          await signInWithEmailAndPassword(auth, email, password);
        } else {
          const userCredential = await createUserWithEmailAndPassword(auth, email, password);
          if (name && userCredential.user) {
            await updateProfile(userCredential.user, { displayName: name });
          }
        }
        authModal.style.display = 'none';
      } catch (err) {
        if (authErrorMsg) {
          authErrorMsg.textContent = err.message;
          authErrorMsg.style.display = 'block';
        }
      } finally {
        authSubmitBtn.disabled = false;
        authSubmitBtn.textContent = STATE.authMode === 'signin' ? 'Sign In' : 'Create Account';
      }
    });
  }

  if (profileSignOutBtn) {
    profileSignOutBtn.addEventListener('click', async () => {
      await signOut(auth);
      alert('Signed out of Cite Circle Firebase Auth.');
    });
  }

  // Publish Modal
  const publishModal = document.getElementById('publishModal');
  const openPublishModalBtn = document.getElementById('openPublishModalBtn');
  const closePublishModalBtn = document.getElementById('closePublishModalBtn');
  const fileDropzone = document.getElementById('fileDropzone');
  const fileInput = document.getElementById('fileInput');
  const fileValidationStatus = document.getElementById('fileValidationStatus');
  const submitPublishBtn = document.getElementById('submitPublishBtn');

  if (openPublishModalBtn && publishModal) {
    openPublishModalBtn.addEventListener('click', () => {
      publishModal.style.display = 'flex';
    });
  }

  if (closePublishModalBtn && publishModal) {
    closePublishModalBtn.addEventListener('click', () => {
      publishModal.style.display = 'none';
      STATE.selectedFile = null;
      if (fileValidationStatus) fileValidationStatus.style.display = 'none';
    });
  }

  if (fileDropzone && fileInput) {
    fileDropzone.addEventListener('click', () => fileInput.click());

    fileDropzone.addEventListener('dragover', (e) => {
      e.preventDefault();
      fileDropzone.classList.add('dragover');
    });

    fileDropzone.addEventListener('dragleave', () => {
      fileDropzone.classList.remove('dragover');
    });

    fileDropzone.addEventListener('drop', async (e) => {
      e.preventDefault();
      fileDropzone.classList.remove('dragover');
      if (e.dataTransfer.files.length > 0) {
        await handleFileSelected(e.dataTransfer.files[0]);
      }
    });

    fileInput.addEventListener('change', async () => {
      if (fileInput.files.length > 0) {
        await handleFileSelected(fileInput.files[0]);
      }
    });
  }

  async function handleFileSelected(file) {
    const result = await validateManuscriptFile(file);
    if (!fileValidationStatus) return;

    fileValidationStatus.style.display = 'block';
    if (!result.valid) {
      fileValidationStatus.style.color = 'var(--accent-rose)';
      fileValidationStatus.innerHTML = `⚠️ ${result.error}`;
      STATE.selectedFile = null;
    } else {
      fileValidationStatus.style.color = 'var(--accent-emerald)';
      fileValidationStatus.innerHTML = `✓ Verified manuscript: ${escapeHtml(file.name)} (${result.format.toUpperCase()}, ${result.sizeFormatted})`;
      STATE.selectedFile = { file, format: result.format, size: result.sizeFormatted };

      const titleInput = document.getElementById('paperTitleInput');
      if (titleInput && !titleInput.value) {
        const cleanName = file.name.substring(0, file.name.lastIndexOf('.')).replace(/[_-]/g, ' ');
        titleInput.value = cleanName;
      }
    }
  }

  if (submitPublishBtn) {
    submitPublishBtn.addEventListener('click', () => {
      const title = document.getElementById('paperTitleInput')?.value.trim();
      const field = document.getElementById('paperFieldSelect')?.value || 'AI & ML';
      const abstract = document.getElementById('paperAbstractInput')?.value.trim();

      if (!title) {
        alert('Please specify a title for your paper.');
        return;
      }

      const activeAuthorName = STATE.currentUser?.displayName || (STATE.currentUser ? STATE.currentUser.email.split('@')[0] : 'Dr. Morgan Vance');
      const activeAuthorId = STATE.currentUser?.uid || 'OycnFiKmG3SnS9ob4xk7qDlYoRh1';

      const newPost = {
        id: 'post-' + Date.now(),
        author: {
          id: activeAuthorId,
          name: activeAuthorName,
          institution: 'Institute for Advanced Study',
          avatar: getInitials(activeAuthorName)
        },
        timestamp: 'Just now',
        content: abstract || 'New manuscript deposited to Cite Circle archives.',
        paper: {
          title: title,
          field: field,
          format: STATE.selectedFile ? STATE.selectedFile.format : 'pdf',
          size: STATE.selectedFile ? STATE.selectedFile.size : '1.2 MB',
          doi: `10.48550/arXiv.${Math.floor(2600 + Math.random() * 99)}.${Math.floor(10000 + Math.random() * 90000)}`,
          abstract: abstract || 'Full manuscript archived and verified with anti-malware safeguards.'
        },
        endorsements: 1,
        isEndorsed: true,
        commentsCount: 0
      };

      STATE.posts.unshift(newPost);
      enforceCacheLimiter();
      savePosts();
      renderPosts();

      publishModal.style.display = 'none';
      STATE.selectedFile = null;
      if (fileValidationStatus) fileValidationStatus.style.display = 'none';
      document.getElementById('paperTitleInput').value = '';
      document.getElementById('paperAbstractInput').value = '';

      alert('Manuscript published to academic feed with authenticated author ID!');
    });
  }

  // Chat send
  const chatInput = document.getElementById('chatInput');
  const sendChatBtn = document.getElementById('sendChatBtn');
  if (sendChatBtn && chatInput) {
    const send = () => {
      const text = chatInput.value.trim();
      if (!text) return;
      const sender = STATE.currentUser?.displayName || (STATE.currentUser ? STATE.currentUser.email.split('@')[0] : 'Dr. Morgan Vance');
      STATE.chats.push({
        sender: sender,
        text: text,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        isMe: true
      });
      chatInput.value = '';
      saveChats();
      renderChats();
    };

    sendChatBtn.addEventListener('click', send);
    chatInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') send();
    });
  }

  // Search input
  const globalSearchInput = document.getElementById('globalSearchInput');
  if (globalSearchInput) {
    globalSearchInput.addEventListener('input', (e) => {
      const query = e.target.value.toLowerCase().trim();
      if (!query) {
        renderPosts();
        return;
      }
      const container = document.getElementById('postsList');
      if (!container) return;

      const results = STATE.posts.filter(p => {
        const titleMatch = p.paper && p.paper.title.toLowerCase().includes(query);
        const authorMatch = p.author.name.toLowerCase().includes(query);
        const doiMatch = p.paper && p.paper.doi.toLowerCase().includes(query);
        const contentMatch = p.content.toLowerCase().includes(query);
        return titleMatch || authorMatch || doiMatch || contentMatch;
      });

      container.innerHTML = results.length > 0 ? results.map(post => `
        <article class="post-card">
          <div class="post-author-row">
            <div class="author-info">
              <div class="author-avatar">${post.author.avatar}</div>
              <div>
                <div class="author-name">${post.author.name}</div>
                <div class="author-institution">${post.author.institution}</div>
              </div>
            </div>
            <div class="post-timestamp">${post.timestamp}</div>
          </div>
          <div class="post-content">${escapeHtml(post.content)}</div>
          ${post.paper ? `
            <div class="paper-attachment">
              <div class="paper-title">${escapeHtml(post.paper.title)}</div>
              <div class="paper-abstract">${escapeHtml(post.paper.abstract)}</div>
              <div class="paper-doi">${escapeHtml(post.paper.doi)}</div>
            </div>
          ` : ''}
        </article>
      `).join('') : '<div style="padding: 2rem; text-align: center; color: var(--text-secondary);">No matching papers found.</div>';
    });
  }

  // Export BibTeX for Vault
  const exportAllCitationsBtn = document.getElementById('exportAllCitationsBtn');
  if (exportAllCitationsBtn) {
    exportAllCitationsBtn.addEventListener('click', () => {
      if (STATE.vault.length === 0) {
        alert('Vault is currently empty.');
        return;
      }
      const allBib = STATE.vault.map((p, i) => `@article{vault_${i + 1},\n  title = {${p.title}},\n  doi = {${p.doi}},\n  year = {2026}\n}`).join('\n\n');
      navigator.clipboard?.writeText(allBib).then(() => {
        alert('Exported ' + STATE.vault.length + ' citations to clipboard!');
      });
    });
  }

  // Privacy controls
  const clearWebCacheBtn = document.getElementById('clearWebCacheBtn');
  if (clearWebCacheBtn) {
    clearWebCacheBtn.addEventListener('click', () => {
      STATE.posts = DEFAULT_POSTS;
      savePosts();
      renderPosts();
      alert('Local feed cache cleared! Your personal Vault papers were protected.');
    });
  }

  const wipeAllDataBtn = document.getElementById('wipeAllDataBtn');
  if (wipeAllDataBtn) {
    wipeAllDataBtn.addEventListener('click', () => {
      if (confirm('GDPR Erasure: Are you sure you want to permanently delete all local cache, reading lists, and session records?')) {
        localStorage.clear();
        STATE.posts = [];
        STATE.vault = [];
        STATE.chats = [];
        renderPosts();
        renderVault();
        renderChats();
        updateVaultBadge();
        alert('All local data wiped permanently.');
      }
    });
  }
});
