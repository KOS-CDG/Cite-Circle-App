import { supabaseConfig } from './supabase-config.js';
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { uploadFileToR2, r2Config } from './r2-config.js';

// Initialize Supabase Client
export const supabase = createClient(supabaseConfig.url, supabaseConfig.anonKey);


// Clean initial profiles for multi-account switching
const INITIAL_SAVED_ACCOUNTS = [];

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
  chats: [],
  userProfile: JSON.parse(localStorage.getItem('citecircle_user_profile') || JSON.stringify({
    name: 'Guest Researcher',
    email: '',
    affiliation: 'Academic Community',
    field: 'All Fields',
    avatar: '👤'
  })),
  savedAccounts: JSON.parse(localStorage.getItem('citecircle_saved_accounts') || '[]'),
  rememberLogin: localStorage.getItem('citecircle_remember_login') !== 'false',
  dataSaver: localStorage.getItem('citecircle_data_saver') === 'true',
  alertsEnabled: localStorage.getItem('citecircle_alerts_enabled') !== 'false'
};

const DEFAULT_POSTS = [];
const DEFAULT_CHATS = [];

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
      STATE.posts = [];
    }
  } else {
    STATE.posts = [];
  }

  const storedVault = localStorage.getItem('citecircle_vault');
  if (storedVault) {
    try {
      STATE.vault = JSON.parse(storedVault);
    } catch {
      STATE.vault = [];
    }
  } else {
    STATE.vault = [];
  }

  const storedChats = localStorage.getItem('citecircle_chats');
  if (storedChats) {
    try {
      STATE.chats = JSON.parse(storedChats);
    } catch {
      STATE.chats = [];
    }
  } else {
    STATE.chats = [];
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
  if (!name || name === 'Guest Researcher') return '👤';
  const parts = name.trim().split(' ').filter(p => p.length > 0);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function formatTimeAgo(date) {
  if (!date || isNaN(date.getTime())) return 'Recently';
  const seconds = Math.floor((Date.now() - date.getTime()) / 1000);
  if (seconds < 60) return 'Just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

// Live Supabase Post & Vault Hydration
export async function syncPostsFromSupabase() {
  try {
    const { data: dbPosts, error } = await supabase
      .from('posts')
      .select(`
        id,
        user_id,
        content,
        media_urls,
        metadata,
        likes_count,
        comments_count,
        created_at,
        author:profiles!posts_user_id_fkey (
          id,
          username,
          full_name,
          avatar_url
        )
      `)
      .order('created_at', { ascending: false })
      .limit(50);

    if (error) {
      console.warn('Could not sync posts from Supabase:', error.message);
      return;
    }

    if (dbPosts && dbPosts.length > 0) {
      let userLikedPostIds = new Set();
      if (STATE.currentUser) {
        try {
          const { data: likes } = await supabase
            .from('post_likes')
            .select('post_id')
            .eq('user_id', STATE.currentUser.id);
          if (likes) {
            userLikedPostIds = new Set(likes.map(l => l.post_id));
          }
        } catch (e) {
          console.warn('Could not fetch user likes:', e);
        }
      }

      const livePosts = dbPosts.map(p => {
        const authorName = p.author?.full_name || p.author?.username || 'Verified Researcher';
        const meta = p.metadata || {};
        const firstMedia = (p.media_urls && p.media_urls.length > 0) ? p.media_urls[0] : null;

        const lines = p.content ? p.content.split('\n\n') : [''];
        const title = meta.title || lines[0] || 'Research Manuscript';
        const abstract = meta.abstract || (lines.length > 1 ? lines.slice(1).join('\n\n') : p.content);

        return {
          id: p.id,
          author: {
            id: p.user_id,
            name: authorName,
            institution: 'Cite Circle Academic Network',
            avatar: getInitials(authorName)
          },
          timestamp: formatTimeAgo(new Date(p.created_at)),
          content: p.content,
          paper: {
            title: title,
            field: meta.field || 'AI & Machine Learning',
            format: meta.format || (firstMedia?.endsWith('.docx') ? 'docx' : (firstMedia?.endsWith('.tex') ? 'latex' : 'pdf')),
            size: meta.size || '1.4 MB',
            doi: meta.doi || `10.48550/arXiv.2609.${p.id.slice(0, 5)}`,
            abstract: abstract,
            url: firstMedia
          },
          endorsements: p.likes_count || 0,
          isEndorsed: userLikedPostIds.has(p.id),
          commentsCount: p.comments_count || 0
        };
      });

      STATE.posts = livePosts;
      savePosts();
      enforceCacheLimiter();
      renderPosts();
    } else {
      STATE.posts = [];
      savePosts();
      renderPosts();
    }
  } catch (err) {
    console.warn('Network error syncing posts:', err);
  }
}

export async function syncVaultFromSupabase() {
  if (!STATE.currentUser) return;
  try {
    const { data: savedData, error } = await supabase
      .from('saved_posts')
      .select('post_id, created_at, post:posts(*)')
      .eq('user_id', STATE.currentUser.id);

    if (!error && savedData && savedData.length > 0) {
      const serverVault = savedData.map(item => {
        const p = item.post;
        if (!p) return null;
        const meta = p.metadata || {};
        const firstMedia = (p.media_urls && p.media_urls.length > 0) ? p.media_urls[0] : null;
        return {
          title: meta.title || p.content.split('\n')[0],
          field: meta.field || 'AI & Machine Learning',
          format: meta.format || (firstMedia?.endsWith('.docx') ? 'docx' : (firstMedia?.endsWith('.tex') ? 'latex' : 'pdf')),
          size: meta.size || '1.4 MB',
          doi: meta.doi || `10.48550/arXiv.2609.${p.id.slice(0, 5)}`,
          abstract: meta.abstract || p.content,
          url: firstMedia
        };
      }).filter(Boolean);

      if (serverVault.length > 0) {
        const existingTitles = new Set(serverVault.map(v => v.title));
        const localOnly = STATE.vault.filter(v => !existingTitles.has(v.title));
        STATE.vault = [...serverVault, ...localOnly];
        saveVault();
        renderVault();
      }
    }
  } catch (err) {
    console.warn('Could not sync vault from Supabase:', err);
  }
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
            ${post.paper.url ? `
              <div style="margin-top: 0.65rem; display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap;">
                <a href="${post.paper.url}" target="_blank" rel="noopener noreferrer" style="display: inline-flex; align-items: center; gap: 0.4rem; font-size: 0.8rem; font-weight: 600; color: #f38020; text-decoration: none; padding: 0.35rem 0.75rem; background: rgba(243, 128, 32, 0.12); border: 1px solid rgba(243, 128, 32, 0.35); border-radius: var(--radius-sm);">
                  <span>☁️</span> Download via Cloudflare R2 (Zero Egress)
                </a>
                <span style="font-size: 0.7rem; color: var(--text-muted); font-family: var(--font-mono);">Hosted on R2 CDN</span>
              </div>
            ` : ''}
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

          <button class="post-action-btn" onclick="window.citeCircleApp.openCommentsModal('${post.id}')">
            <span>💬</span>
            <span>${post.commentsCount || 0} Reviews</span>
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

export const LOUNGE_CONVERSATION_ID = '00000000-0000-0000-0000-000000000001';

export async function syncChatsFromSupabase() {
  try {
    const { data: messages, error } = await supabase
      .from('messages')
      .select('id, sender_id, content, created_at, sender:profiles!messages_sender_id_fkey(full_name, username)')
      .eq('conversation_id', LOUNGE_CONVERSATION_ID)
      .order('created_at', { ascending: true })
      .limit(50);

    if (!error && messages && messages.length > 0) {
      STATE.chats = messages.map(m => {
        const isMe = STATE.currentUser ? m.sender_id === STATE.currentUser.id : false;
        const senderName = m.sender?.full_name || m.sender?.username || 'Researcher';
        return {
          id: m.id,
          sender: isMe ? 'You' : senderName,
          text: m.content,
          time: new Date(m.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          isMe
        };
      });
      saveChats();
      renderChats();
    }
  } catch (err) {
    console.warn('Could not sync chats from Supabase:', err);
  }
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

// Sync Supabase Auth state with UI
async function updateAuthStateUI(user) {
  STATE.currentUser = user;
  const statusBadge = document.getElementById('cloudStatusText');
  const headerAvatar = document.getElementById('userHeaderAvatar');
  const openAuthBtn = document.getElementById('openAuthModalBtn');
  const profileAvatar = document.getElementById('profileAvatar');
  const profileName = document.getElementById('profileName');
  const profileAffiliation = document.getElementById('profileAffiliation');
  const profileEmail = document.getElementById('profileEmail');
  const profileUid = document.getElementById('profileUid');
  const profileDetailsDesc = document.getElementById('profileDetailsDesc');
  const logoutBtnLabel = document.getElementById('logoutBtnLabel');
  const fbLogoutAvatar = document.getElementById('fbLogoutAvatar');
  const fbLogoutHeading = document.getElementById('fbLogoutHeading');
  const fbLogoutSubtext = document.getElementById('fbLogoutSubtext');

  let profile = STATE.userProfile;
  let displayName = '';
  let displayEmail = '';
  let displayAffil = '';
  let displayField = '';

  if (user) {
    displayEmail = user.email;
    displayName = user.user_metadata?.full_name || user.user_metadata?.username || user.email.split('@')[0];
    displayAffil = 'Cite Circle Verified Researcher';
    displayField = 'Academic Research';

    // Attempt to load live profile row from public.profiles
    try {
      const { data: dbProfile, error } = await supabase
        .from('profiles')
        .select('*')
        .eq('id', user.id)
        .single();
      if (dbProfile && !error) {
        if (dbProfile.full_name) displayName = dbProfile.full_name;
        if (dbProfile.bio) displayAffil = dbProfile.bio;
        STATE.userProfile = {
          name: displayName,
          email: user.email,
          affiliation: displayAffil,
          field: displayField,
          avatar: getInitials(displayName)
        };
      }
    } catch (err) {
      console.warn('Could not load profile from Supabase:', err);
    }

    if (statusBadge) statusBadge.textContent = `Supabase: ${user.email}`;
    if (openAuthBtn) openAuthBtn.style.display = 'none';
    if (headerAvatar) headerAvatar.style.display = 'flex';
    if (profileUid) profileUid.textContent = `UID: ${user.id.slice(0, 8)}...`;
  } else {
    displayName = 'Guest Researcher';
    displayEmail = 'Not signed in';
    displayAffil = 'Academic Community';
    displayField = 'Public Access';
    if (statusBadge) statusBadge.textContent = 'Guest Mode';
    if (openAuthBtn) openAuthBtn.style.display = 'inline-block';
    if (headerAvatar) headerAvatar.style.display = 'none';
    if (profileUid) profileUid.textContent = 'Guest Session';
  }

  const initials = getInitials(displayName);

  if (headerAvatar) {
    headerAvatar.textContent = initials;
    headerAvatar.title = user ? `${displayName} (${user.email})` : 'Guest Mode';
  }

  if (profileAvatar) profileAvatar.textContent = initials;
  if (profileName) profileName.textContent = displayName;
  if (profileAffiliation) profileAffiliation.textContent = displayField ? `${displayAffil} • ${displayField}` : displayAffil;
  if (profileEmail) profileEmail.textContent = displayEmail;
  if (profileDetailsDesc) profileDetailsDesc.textContent = `${displayName} • ${displayAffil}`;
  if (logoutBtnLabel) logoutBtnLabel.textContent = user ? `Log Out (${displayName})` : 'Sign In';
  if (fbLogoutAvatar) fbLogoutAvatar.textContent = initials;
  if (fbLogoutHeading) fbLogoutHeading.textContent = user ? `Log out of Cite Circle?` : 'Sign In to Cite Circle';
  if (fbLogoutSubtext) fbLogoutSubtext.textContent = user ? `${displayName} (${displayEmail})` : 'Access your academic profile, publications and peer reviews';
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
  async toggleEndorse(postId) {
    const post = STATE.posts.find(p => p.id === postId);
    if (!post) return;

    post.isEndorsed = !post.isEndorsed;
    post.endorsements += post.isEndorsed ? 1 : -1;
    savePosts();
    renderPosts();

    // Persist to Supabase post_likes if authenticated
    if (STATE.currentUser && String(postId).includes('-') && String(postId).length > 20) {
      try {
        if (post.isEndorsed) {
          await supabase.from('post_likes').insert({
            post_id: postId,
            user_id: STATE.currentUser.id
          });
        } else {
          await supabase.from('post_likes').delete().match({
            post_id: postId,
            user_id: STATE.currentUser.id
          });
        }
      } catch (err) {
        console.warn('Could not sync endorsement to Supabase:', err);
      }
    }
  },

  async toggleSaveVault(postId) {
    const post = STATE.posts.find(p => p.id === postId);
    if (!post || !post.paper) return;

    const existingIdx = STATE.vault.findIndex(p => p.title === post.paper.title);
    const willSave = existingIdx < 0;

    if (existingIdx >= 0) {
      STATE.vault.splice(existingIdx, 1);
    } else {
      STATE.vault.push(post.paper);
    }
    saveVault();
    renderPosts();

    // Persist to Supabase saved_posts if authenticated
    if (STATE.currentUser && String(postId).includes('-') && String(postId).length > 20) {
      try {
        if (willSave) {
          await supabase.from('saved_posts').insert({
            post_id: postId,
            user_id: STATE.currentUser.id
          });
        } else {
          await supabase.from('saved_posts').delete().match({
            post_id: postId,
            user_id: STATE.currentUser.id
          });
        }
      } catch (err) {
        console.warn('Could not sync vault bookmark to Supabase:', err);
      }
    }
  },

  async openCommentsModal(postId) {
    const post = STATE.posts.find(p => p.id === postId);
    if (!post) return;

    if (!STATE.currentUser) {
      alert('Please sign in to view and deposit peer review comments.');
      return;
    }

    let existingComments = [];
    if (String(postId).includes('-') && String(postId).length > 20) {
      try {
        const { data, error } = await supabase
          .from('comments')
          .select('id, content, created_at, user:profiles!comments_user_id_fkey(full_name, username)')
          .eq('post_id', postId)
          .order('created_at', { ascending: true });
        if (!error && data) existingComments = data;
      } catch (err) {
        console.warn('Error fetching peer reviews:', err);
      }
    }

    const reviewsHeader = existingComments.length > 0
      ? `Peer Reviews for "${post.paper?.title || 'Manuscript'}" (${existingComments.length}):\n\n` +
        existingComments.map(c => `• ${c.user?.full_name || c.user?.username || 'Peer'}: "${c.content}"`).join('\n') +
        `\n\nSubmit your peer review comment below:`
      : `No reviews yet for "${post.paper?.title || 'this manuscript'}".\n\nBe the first to submit a peer review comment:`;

    const userComment = prompt(reviewsHeader);
    if (!userComment || !userComment.trim()) return;

    if (String(postId).includes('-') && String(postId).length > 20) {
      try {
        await supabase.from('comments').insert({
          post_id: postId,
          user_id: STATE.currentUser.id,
          content: userComment.trim()
        });
      } catch (insertErr) {
        console.warn('Could not persist comment to Supabase:', insertErr);
      }
    }

    post.commentsCount = (post.commentsCount || 0) + 1;
    savePosts();
    renderPosts();
    alert('Peer review commentary recorded!');
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

  // Hydrate feed with live manuscripts from Supabase
  syncPostsFromSupabase();

  // Listen to live Supabase Auth state changes
  supabase.auth.onAuthStateChange(async (event, session) => {
    await updateAuthStateUI(session?.user || null);
    if (session?.user) {
      await syncPostsFromSupabase();
      await syncVaultFromSupabase();
    }
  });

  // Subscribe to Realtime post updates for live feed broadcasts
  try {
    supabase
      .channel('public:posts:feed')
      .on('postgres_changes', { event: '*', schema: 'public', table: 'posts' }, () => {
        syncPostsFromSupabase();
      })
      .subscribe();
  } catch (rtErr) {
    console.warn('Realtime channel subscription error:', rtErr);
  }

  // Hydrate chats from Academic Lounge and subscribe to live messages
  syncChatsFromSupabase();
  try {
    supabase
      .channel('public:messages:lounge')
      .on('postgres_changes', {
        event: 'INSERT',
        schema: 'public',
        table: 'messages',
        filter: `conversation_id=eq.${LOUNGE_CONVERSATION_ID}`
      }, () => {
        syncChatsFromSupabase();
      })
      .subscribe();
  } catch (rtChatErr) {
    console.warn('Realtime chat subscription error:', rtChatErr);
  }


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
        const { data, error } = await supabase.auth.signInWithPassword({
          email: 'demo.researcher@cite.circle',
          password: 'citecircle2026'
        });
        if (error) throw error;
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

  function validatePasswordSecurity(password) {
    if (!password || password.length < 8) {
      return { valid: false, message: 'Password must be at least 8 characters long.' };
    }
    const commonWeak = [
      'password', '12345678', '123456789', 'qwerty123', 'admin123', 'welcome123',
      'password1', 'pass1234', '11111111', '00000000', 'testing123', 'letmein123',
      'citecircle', 'researcher'
    ];
    if (commonWeak.includes(password.toLowerCase())) {
      return { valid: false, message: 'This password is too common and easily guessed. Please choose a stronger password.' };
    }
    const hasLetter = /[a-zA-Z]/.test(password);
    const hasDigitOrSpecial = /[\d!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?~`]/.test(password);
    if (!hasLetter || !hasDigitOrSpecial) {
      return { valid: false, message: 'Password must include both letters and numbers/symbols.' };
    }
    return { valid: true };
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

      if (STATE.authMode === 'signup') {
        const check = validatePasswordSecurity(password);
        if (!check.valid) {
          if (authErrorMsg) {
            authErrorMsg.textContent = check.message;
            authErrorMsg.style.display = 'block';
          }
          return;
        }
      }

      try {
        authSubmitBtn.disabled = true;
        authSubmitBtn.textContent = 'Processing...';

        if (STATE.authMode === 'signin') {
          const { data, error } = await supabase.auth.signInWithPassword({ email, password });
          if (error) throw error;
        } else {
          const { data, error } = await supabase.auth.signUp({
            email,
            password,
            options: {
              data: {
                full_name: name || email.split('@')[0],
                username: name ? name.toLowerCase().replace(/[^a-z0-9]/g, '_') : email.split('@')[0]
              }
            }
          });
          if (error) throw error;
          // Auto sign-in in case email confirmation is auto-completed
          await supabase.auth.signInWithPassword({ email, password }).catch(() => {});
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

  // --- Facebook / Meta Settings & Privacy Interactive Handlers ---

  // 1. Edit Personal Details Modal
  const editProfileModal = document.getElementById('editProfileModal');
  const openEditProfileBtn = document.getElementById('openEditProfileBtn');
  const closeEditProfileModalBtn = document.getElementById('closeEditProfileModalBtn');
  const cancelEditProfileBtn = document.getElementById('cancelEditProfileBtn');
  const saveProfileBtn = document.getElementById('saveProfileBtn');
  const editProfileNameInput = document.getElementById('editProfileNameInput');
  const editProfileAffiliationInput = document.getElementById('editProfileAffiliationInput');
  const editProfileFieldInput = document.getElementById('editProfileFieldInput');

  if (openEditProfileBtn && editProfileModal) {
    openEditProfileBtn.addEventListener('click', () => {
      const p = STATE.userProfile;
      if (editProfileNameInput) editProfileNameInput.value = STATE.currentUser?.displayName || p.name || '';
      if (editProfileAffiliationInput) editProfileAffiliationInput.value = p.affiliation || '';
      if (editProfileFieldInput) editProfileFieldInput.value = p.field || '';
      editProfileModal.style.display = 'flex';
    });
  }

  const closeEditProfile = () => { if (editProfileModal) editProfileModal.style.display = 'none'; };
  if (closeEditProfileModalBtn) closeEditProfileModalBtn.addEventListener('click', closeEditProfile);
  if (cancelEditProfileBtn) cancelEditProfileBtn.addEventListener('click', closeEditProfile);

  if (saveProfileBtn) {
    saveProfileBtn.addEventListener('click', async () => {
      const newName = editProfileNameInput?.value.trim();
      const newAffil = editProfileAffiliationInput?.value.trim();
      const newField = editProfileFieldInput?.value.trim();

      if (!newName) {
        alert('Please enter your full name.');
        return;
      }

      saveProfileBtn.disabled = true;
      saveProfileBtn.textContent = 'Saving...';

      try {
        if (STATE.currentUser) {
          await supabase.from('profiles').update({
            full_name: newName,
            bio: `${newAffil || 'Academic Researcher'} • ${newField || 'General Science'}`
          }).eq('id', STATE.currentUser.id);
        }
      } catch (err) {
        console.warn('Supabase profile update fallback to local store:', err);
      }

      STATE.userProfile = {
        ...STATE.userProfile,
        name: newName,
        affiliation: newAffil || 'Academic Researcher',
        field: newField || 'General Science',
        avatar: getInitials(newName)
      };
      localStorage.setItem('citecircle_user_profile', JSON.stringify(STATE.userProfile));

      // Update in saved accounts list if present
      const currentEmail = STATE.currentUser?.email || STATE.userProfile.email;
      const acctIdx = STATE.savedAccounts.findIndex(a => a.email === currentEmail);
      if (acctIdx >= 0) {
        STATE.savedAccounts[acctIdx] = { ...STATE.savedAccounts[acctIdx], ...STATE.userProfile };
        localStorage.setItem('citecircle_saved_accounts', JSON.stringify(STATE.savedAccounts));
      }

      updateAuthStateUI(STATE.currentUser);
      saveProfileBtn.disabled = false;
      saveProfileBtn.textContent = 'Save Changes';
      closeEditProfile();
      alert('Personal details updated successfully!');
    });
  }

  // 2. Change Password Modal
  const changePasswordModal = document.getElementById('changePasswordModal');
  const openChangePasswordBtn = document.getElementById('openChangePasswordBtn');
  const closeChangePasswordModalBtn = document.getElementById('closeChangePasswordModalBtn');
  const cancelChangePasswordBtn = document.getElementById('cancelChangePasswordBtn');
  const savePasswordBtn = document.getElementById('savePasswordBtn');
  const currentPasswordInput = document.getElementById('currentPasswordInput');
  const newPasswordInput = document.getElementById('newPasswordInput');
  const confirmPasswordInput = document.getElementById('confirmPasswordInput');
  const passwordErrorMsg = document.getElementById('passwordErrorMsg');

  if (openChangePasswordBtn && changePasswordModal) {
    openChangePasswordBtn.addEventListener('click', () => {
      if (passwordErrorMsg) passwordErrorMsg.style.display = 'none';
      if (currentPasswordInput) currentPasswordInput.value = '';
      if (newPasswordInput) newPasswordInput.value = '';
      if (confirmPasswordInput) confirmPasswordInput.value = '';
      changePasswordModal.style.display = 'flex';
    });
  }

  const closeChangePassword = () => { if (changePasswordModal) changePasswordModal.style.display = 'none'; };
  if (closeChangePasswordModalBtn) closeChangePasswordModalBtn.addEventListener('click', closeChangePassword);
  if (cancelChangePasswordBtn) cancelChangePasswordBtn.addEventListener('click', closeChangePassword);

  if (savePasswordBtn) {
    savePasswordBtn.addEventListener('click', async () => {
      const curPass = currentPasswordInput?.value.trim();
      const newPass = newPasswordInput?.value.trim();
      const confPass = confirmPasswordInput?.value.trim();

      const check = validatePasswordSecurity(newPass);
      if (!check.valid) {
        if (passwordErrorMsg) {
          passwordErrorMsg.textContent = check.message;
          passwordErrorMsg.style.display = 'block';
        }
        return;
      }
      if (newPass !== confPass) {
        if (passwordErrorMsg) {
          passwordErrorMsg.textContent = 'New passwords do not match. Please re-enter.';
          passwordErrorMsg.style.display = 'block';
        }
        return;
      }

      savePasswordBtn.disabled = true;
      savePasswordBtn.textContent = 'Updating...';

      try {
        if (STATE.currentUser) {
          const { error } = await supabase.auth.updateUser({ password: newPass });
          if (error) throw error;
        }
        closeChangePassword();
        alert('Password updated securely! Next time you sign in, use your new credentials.');
      } catch (err) {
        if (passwordErrorMsg) {
          passwordErrorMsg.textContent = err.message || 'Error updating password. You may need to sign in again first.';
          passwordErrorMsg.style.display = 'block';
        }
      } finally {
        savePasswordBtn.disabled = false;
        savePasswordBtn.textContent = 'Change Password';
      }
    });
  }

  // 3. Dedicated Facebook Logout Flow
  const facebookLogoutModal = document.getElementById('facebookLogoutModal');
  const openFacebookLogoutBtn = document.getElementById('openFacebookLogoutBtn');
  const cancelFacebookLogoutBtn = document.getElementById('cancelFacebookLogoutBtn');
  const confirmFacebookLogoutBtn = document.getElementById('confirmFacebookLogoutBtn');
  const rememberLoginCheckbox = document.getElementById('rememberLoginCheckbox');

  if (openFacebookLogoutBtn && facebookLogoutModal) {
    openFacebookLogoutBtn.addEventListener('click', () => {
      if (rememberLoginCheckbox) rememberLoginCheckbox.checked = STATE.rememberLogin;
      facebookLogoutModal.style.display = 'flex';
    });
  }

  if (cancelFacebookLogoutBtn && facebookLogoutModal) {
    cancelFacebookLogoutBtn.addEventListener('click', () => {
      facebookLogoutModal.style.display = 'none';
    });
  }

  if (confirmFacebookLogoutBtn) {
    confirmFacebookLogoutBtn.addEventListener('click', async () => {
      const keepSaved = rememberLoginCheckbox ? rememberLoginCheckbox.checked : true;
      STATE.rememberLogin = keepSaved;
      localStorage.setItem('citecircle_remember_login', String(keepSaved));

      const activeEmail = STATE.currentUser?.email || STATE.userProfile.email;

      if (!keepSaved) {
        // Facebook behavior: remove remembered profile from device
        STATE.savedAccounts = STATE.savedAccounts.filter(a => a.email !== activeEmail);
        localStorage.setItem('citecircle_saved_accounts', JSON.stringify(STATE.savedAccounts));
      }

      try {
        await supabase.auth.signOut();
      } catch (err) {
        console.warn('Sign out:', err);
      }

      facebookLogoutModal.style.display = 'none';
      STATE.currentUser = null;
      await updateAuthStateUI(null);
      alert(keepSaved
        ? 'Logged out. Your login credentials remain saved on this device for quick access.'
        : 'Logged out. Your login info has been removed from this device.');
    });
  }

  // 4. Switch Accounts Modal
  const switchAccountModal = document.getElementById('switchAccountModal');
  const openSwitchAccountBtn = document.getElementById('openSwitchAccountBtn');
  const closeSwitchAccountModalBtn = document.getElementById('closeSwitchAccountModalBtn');
  const accountsListContainer = document.getElementById('accountsListContainer');
  const addNewAccountFromSwitchBtn = document.getElementById('addNewAccountFromSwitchBtn');

  function renderSwitchAccountsList() {
    if (!accountsListContainer) return;
    const currentEmail = STATE.currentUser?.email || STATE.userProfile.email;

    if (!STATE.savedAccounts || STATE.savedAccounts.length === 0) {
      accountsListContainer.innerHTML = `
        <div style="text-align: center; padding: 2.5rem 1rem; color: var(--text-secondary);">
          <div style="font-size: 2rem; margin-bottom: 0.5rem;">👥</div>
          <div style="font-weight: 600; color: var(--text-primary);">No other accounts on this device</div>
          <div style="font-size: 0.8rem; margin-top: 0.25rem;">Sign in or create another account to switch between research identities.</div>
        </div>
      `;
      return;
    }

    accountsListContainer.innerHTML = STATE.savedAccounts.map(acct => {
      const isCurrent = acct.email === currentEmail;
      return `
        <div style="display: flex; align-items: center; justify-content: space-between; padding: 0.75rem; border-radius: var(--radius-md); background: ${isCurrent ? 'var(--bg-card-hover)' : 'var(--bg-primary)'}; border: 1px solid ${isCurrent ? 'var(--accent-primary)' : 'var(--border-subtle)'};">
          <div style="display: flex; align-items: center; gap: 0.75rem;">
            <div class="author-avatar" style="width: 40px; height: 40px; font-size: 0.95rem;">${acct.avatar || getInitials(acct.name)}</div>
            <div>
              <div style="font-weight: 700; font-size: 0.9rem; color: var(--text-primary);">${escapeHtml(acct.name)}</div>
              <div style="font-size: 0.75rem; color: var(--text-secondary);">${escapeHtml(acct.affiliation)}</div>
              <div style="font-size: 0.7rem; color: var(--text-muted); font-family: var(--font-mono);">${escapeHtml(acct.email)}</div>
            </div>
          </div>
          <div>
            ${isCurrent ? `
              <span style="font-size: 0.75rem; font-weight: 700; color: var(--accent-emerald); padding: 0.25rem 0.5rem; background: rgba(16, 185, 129, 0.15); border-radius: var(--radius-full);">Active</span>
            ` : `
              <button class="btn-icon switch-to-acct-btn" data-email="${acct.email}" style="padding: 0.35rem 0.75rem; font-size: 0.8rem; font-weight: 600; color: var(--accent-primary);">Switch</button>
            `}
          </div>
        </div>
      `;
    }).join('');

    // Attach switch clicks
    accountsListContainer.querySelectorAll('.switch-to-acct-btn').forEach(btn => {
      btn.addEventListener('click', async () => {
        const targetEmail = btn.getAttribute('data-email');
        const target = STATE.savedAccounts.find(a => a.email === targetEmail);
        if (target) {
          STATE.userProfile = target;
          localStorage.setItem('citecircle_user_profile', JSON.stringify(target));
          if (target.email === 'demo.researcher@cite.circle') {
            try {
              await supabase.auth.signInWithPassword({
                email: 'demo.researcher@cite.circle',
                password: 'citecircle2026'
              });
            } catch {
              await updateAuthStateUI(null);
            }
          } else {
            await updateAuthStateUI(null);
          }
          if (switchAccountModal) switchAccountModal.style.display = 'none';
          alert(`Switched active profile to ${target.name}!`);
        }
      });
    });
  }

  if (openSwitchAccountBtn && switchAccountModal) {
    openSwitchAccountBtn.addEventListener('click', () => {
      renderSwitchAccountsList();
      switchAccountModal.style.display = 'flex';
    });
  }

  if (closeSwitchAccountModalBtn && switchAccountModal) {
    closeSwitchAccountModalBtn.addEventListener('click', () => {
      switchAccountModal.style.display = 'none';
    });
  }

  if (addNewAccountFromSwitchBtn) {
    addNewAccountFromSwitchBtn.addEventListener('click', () => {
      if (switchAccountModal) switchAccountModal.style.display = 'none';
      showAuthModal('register');
    });
  }

  // 5. Information and Permissions Modal
  const permissionsModal = document.getElementById('permissionsModal');
  const openPermissionsBtn = document.getElementById('openPermissionsBtn');
  const closePermissionsModalBtn = document.getElementById('closePermissionsModalBtn');
  const modalExportBibBtn = document.getElementById('modalExportBibBtn');
  const modalExportJsonBtn = document.getElementById('modalExportJsonBtn');

  if (openPermissionsBtn && permissionsModal) {
    openPermissionsBtn.addEventListener('click', () => {
      permissionsModal.style.display = 'flex';
    });
  }
  if (closePermissionsModalBtn && permissionsModal) {
    closePermissionsModalBtn.addEventListener('click', () => {
      permissionsModal.style.display = 'none';
    });
  }

  if (modalExportBibBtn) {
    modalExportBibBtn.addEventListener('click', () => {
      if (STATE.vault.length === 0) {
        alert('Your research vault is currently empty.');
        return;
      }
      const allBib = STATE.vault.map((p, i) => `@article{vault_${i + 1},\n  title = {${p.title}},\n  doi = {${p.doi}},\n  year = {2026}\n}`).join('\n\n');
      navigator.clipboard?.writeText(allBib).then(() => {
        alert(`Exported ${STATE.vault.length} BibTeX citations to clipboard!`);
      });
    });
  }

  if (modalExportJsonBtn) {
    modalExportJsonBtn.addEventListener('click', () => {
      const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify({
        profile: STATE.userProfile,
        vault: STATE.vault,
        timestamp: new Date().toISOString()
      }, null, 2));
      const downloadAnchor = document.createElement('a');
      downloadAnchor.setAttribute("href", dataStr);
      downloadAnchor.setAttribute("download", `citecircle_vault_export_${Date.now()}.json`);
      document.body.appendChild(downloadAnchor);
      downloadAnchor.click();
      downloadAnchor.remove();
    });
  }

  // 6. Privacy Policy & Terms Modals
  const privacyPolicyModal = document.getElementById('privacyPolicyModal');
  const openPrivacyPolicyBtn = document.getElementById('openPrivacyPolicyBtn');
  const closePrivacyPolicyModalBtn = document.getElementById('closePrivacyPolicyModalBtn');
  if (openPrivacyPolicyBtn && privacyPolicyModal) {
    openPrivacyPolicyBtn.addEventListener('click', () => { privacyPolicyModal.style.display = 'flex'; });
  }
  if (closePrivacyPolicyModalBtn && privacyPolicyModal) {
    closePrivacyPolicyModalBtn.addEventListener('click', () => { privacyPolicyModal.style.display = 'none'; });
  }

  const termsModal = document.getElementById('termsModal');
  const openTermsBtn = document.getElementById('openTermsBtn');
  const closeTermsModalBtn = document.getElementById('closeTermsModalBtn');
  if (openTermsBtn && termsModal) {
    openTermsBtn.addEventListener('click', () => { termsModal.style.display = 'flex'; });
  }
  if (closeTermsModalBtn && termsModal) {
    closeTermsModalBtn.addEventListener('click', () => { termsModal.style.display = 'none'; });
  }

  // 7. Account Ownership & Control Modal
  const accountControlModal = document.getElementById('accountControlModal');
  const openAccountControlBtn = document.getElementById('openAccountControlBtn');
  const closeAccountControlModalBtn = document.getElementById('closeAccountControlModalBtn');
  const modalClearCacheBtn = document.getElementById('modalClearCacheBtn');
  const modalWipeDataBtn = document.getElementById('modalWipeDataBtn');

  if (openAccountControlBtn && accountControlModal) {
    openAccountControlBtn.addEventListener('click', () => { accountControlModal.style.display = 'flex'; });
  }
  if (closeAccountControlModalBtn && accountControlModal) {
    closeAccountControlModalBtn.addEventListener('click', () => { accountControlModal.style.display = 'none'; });
  }

  if (modalClearCacheBtn) {
    modalClearCacheBtn.addEventListener('click', () => {
      STATE.posts = DEFAULT_POSTS;
      savePosts();
      renderPosts();
      if (accountControlModal) accountControlModal.style.display = 'none';
      alert('Local feed cache purged. Your saved reading vault remains protected.');
    });
  }

  if (modalWipeDataBtn) {
    modalWipeDataBtn.addEventListener('click', () => {
      if (confirm('GDPR Erasure: Are you sure you want to permanently delete all local cache, accounts, reading lists, and session records?')) {
        localStorage.clear();
        STATE.posts = [];
        STATE.vault = [];
        STATE.chats = [];
        STATE.savedAccounts = INITIAL_SAVED_ACCOUNTS.slice(0, 1);
        STATE.userProfile = INITIAL_SAVED_ACCOUNTS[0];
        renderPosts();
        renderVault();
        renderChats();
        updateVaultBadge();
        updateAuthStateUI(null);
        if (accountControlModal) accountControlModal.style.display = 'none';
        alert('All local data, sessions, and cache wiped permanently.');
      }
    });
  }

  // 8. Settings Preferences
  const settingsThemeToggleBtn = document.getElementById('settingsThemeToggleBtn');
  const themeStatusText = document.getElementById('themeStatusText');
  const dataSaverCheckbox = document.getElementById('dataSaverCheckbox');
  const alertsCheckbox = document.getElementById('alertsCheckbox');

  if (themeStatusText) {
    themeStatusText.textContent = `${STATE.theme.charAt(0).toUpperCase() + STATE.theme.slice(1)} theme is currently active`;
  }

  if (settingsThemeToggleBtn) {
    settingsThemeToggleBtn.addEventListener('click', () => {
      STATE.theme = STATE.theme === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', STATE.theme);
      localStorage.setItem('citecircle_theme', STATE.theme);
      if (themeStatusText) {
        themeStatusText.textContent = `${STATE.theme.charAt(0).toUpperCase() + STATE.theme.slice(1)} theme is currently active`;
      }
    });
  }

  if (dataSaverCheckbox) {
    dataSaverCheckbox.checked = STATE.dataSaver;
    dataSaverCheckbox.addEventListener('change', () => {
      STATE.dataSaver = dataSaverCheckbox.checked;
      localStorage.setItem('citecircle_data_saver', String(STATE.dataSaver));
    });
  }

  if (alertsCheckbox) {
    alertsCheckbox.checked = STATE.alertsEnabled;
    alertsCheckbox.addEventListener('change', () => {
      STATE.alertsEnabled = alertsCheckbox.checked;
      localStorage.setItem('citecircle_alerts_enabled', String(STATE.alertsEnabled));
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

  const triggerQuickPublish = document.getElementById('triggerQuickPublish');
  if (triggerQuickPublish && publishModal) {
    triggerQuickPublish.addEventListener('click', () => {
      publishModal.style.display = 'flex';
    });
  }

  if (submitPublishBtn) {
    submitPublishBtn.addEventListener('click', async () => {
      const title = document.getElementById('paperTitleInput')?.value.trim();
      const field = document.getElementById('paperFieldSelect')?.value || 'AI & ML';
      const abstract = document.getElementById('paperAbstractInput')?.value.trim();

      if (!title) {
        alert('Please specify a title for your paper.');
        return;
      }

      submitPublishBtn.disabled = true;
      let r2UploadedFile = null;

      if (STATE.selectedFile?.file) {
        submitPublishBtn.textContent = 'Uploading Manuscript to CDN...';
        try {
          r2UploadedFile = await uploadFileToR2(STATE.selectedFile.file, 'manuscripts', supabase);
          console.log('Manuscript uploaded:', r2UploadedFile);
        } catch (uploadErr) {
          console.error('Manuscript upload error:', uploadErr);
          alert('Upload note: ' + uploadErr.message);
        }
      }

      submitPublishBtn.textContent = 'Publishing...';

      const activeAuthorName = STATE.userProfile?.name || (STATE.currentUser ? STATE.currentUser.email.split('@')[0] : 'Dr. Morgan Vance');
      const activeAuthorId = STATE.currentUser?.id || 'c6bcdff0-d285-4ac4-a345-fe1bf934a11e';
      const mediaUrls = r2UploadedFile ? [r2UploadedFile.publicUrl] : [];

      // If signed in, persist to Supabase posts table
      let dbPostId = null;
      const paperFormat = STATE.selectedFile ? STATE.selectedFile.format : 'pdf';
      const paperSize = STATE.selectedFile ? (STATE.selectedFile.sizeFormatted || STATE.selectedFile.size) : (r2UploadedFile ? `${Math.round(r2UploadedFile.size / 1024)} KB` : '1.2 MB');
      const paperDoi = `10.48550/arXiv.${Math.floor(2600 + Math.random() * 99)}.${Math.floor(10000 + Math.random() * 90000)}`;

      if (STATE.currentUser) {
        try {
          const { data: dbPost, error: dbErr } = await supabase.from('posts').insert({
            user_id: STATE.currentUser.id,
            content: `${title}\n\n${abstract || ''}`,
            media_urls: mediaUrls,
            metadata: {
              title: title,
              field: field,
              format: paperFormat,
              size: paperSize,
              doi: paperDoi,
              abstract: abstract || ''
            },
            privacy: 'public'
          }).select().single();

          if (!dbErr && dbPost) {
            dbPostId = dbPost.id;
          }
        } catch (dbErr) {
          console.warn('Supabase post insert fallback to local store:', dbErr);
        }
      }

      const newPost = {
        id: dbPostId || ('post-' + Date.now()),
        author: {
          id: activeAuthorId,
          name: activeAuthorName,
          institution: STATE.userProfile?.affiliation || 'Institute for Advanced Study',
          avatar: getInitials(activeAuthorName)
        },
        timestamp: 'Just now',
        content: abstract || 'New manuscript deposited to Cite Circle archives.',
        paper: {
          title: title,
          field: field,
          format: paperFormat,
          size: paperSize,
          doi: paperDoi,
          abstract: abstract || 'Full manuscript archived and verified with anti-malware safeguards.',
          url: r2UploadedFile?.publicUrl || null
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
      submitPublishBtn.disabled = false;
      submitPublishBtn.textContent = 'Publish to Academic Feed';

      alert(r2UploadedFile
        ? `Manuscript published successfully!\n\nFile hosted on Cloudflare R2:\n${r2UploadedFile.publicUrl}`
        : 'Manuscript published to academic feed!');
    });
  }

  // Chat send
  const chatInput = document.getElementById('chatInput');
  const sendChatBtn = document.getElementById('sendChatBtn');
  if (sendChatBtn && chatInput) {
    const send = async () => {
      const text = chatInput.value.trim();
      if (!text) return;
      const sender = STATE.currentUser?.displayName || (STATE.currentUser ? STATE.currentUser.email.split('@')[0] : 'Dr. Morgan Vance');
      STATE.chats.push({
        sender: 'You',
        text: text,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        isMe: true
      });
      chatInput.value = '';
      saveChats();
      renderChats();

      // Persist to Supabase messages table if authenticated
      if (STATE.currentUser) {
        try {
          await supabase.from('messages').insert({
            conversation_id: LOUNGE_CONVERSATION_ID,
            sender_id: STATE.currentUser.id,
            content: text
          });
        } catch (dbErr) {
          console.warn('Could not persist chat message to Supabase:', dbErr);
        }
      }
    };

    sendChatBtn.addEventListener('click', send);
    chatInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') send();
    });
  }

  // Search input
  // Debounced search for maximum typing responsiveness
  const globalSearchInput = document.getElementById('globalSearchInput');
  if (globalSearchInput) {
    let searchTimer = null;
    globalSearchInput.addEventListener('input', (e) => {
      clearTimeout(searchTimer);
      searchTimer = setTimeout(() => {
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
      }, 120);
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

  // Check for In-App Updates via Supabase Edge Function
  async function checkForInAppUpdates() {
    try {
      const res = await fetch('https://cxxtrtglmxfuyihxwiza.supabase.co/functions/v1/cite-server?action=check_update&code=1');
      if (res.ok) {
        const data = await res.json();
        if (data.update_available) {
          const banner = document.getElementById('inAppUpdateBanner');
          const versionTag = document.getElementById('updateVersionTag');
          const notesText = document.getElementById('updateNotesText');
          if (banner) {
            if (versionTag) versionTag.textContent = `v${data.latest_version_name}`;
            if (notesText && data.release_notes) notesText.textContent = data.release_notes;
            banner.style.display = 'block';
          }
        }
      }
    } catch (e) {
      // Offline or Edge function silent fallback
    }
  }

  const dismissUpdateBannerBtn = document.getElementById('dismissUpdateBannerBtn');
  if (dismissUpdateBannerBtn) {
    dismissUpdateBannerBtn.addEventListener('click', () => {
      const banner = document.getElementById('inAppUpdateBanner');
      if (banner) banner.style.display = 'none';
    });
  }

  const applyUpdateBtn = document.getElementById('applyUpdateBtn');
  if (applyUpdateBtn) {
    applyUpdateBtn.addEventListener('click', () => {
      window.location.reload();
    });
  }

  checkForInAppUpdates();
});
