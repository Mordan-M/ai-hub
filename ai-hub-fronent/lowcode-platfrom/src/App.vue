<template>
  <!-- ─── AUTH SCREEN ─── -->
  <div v-if="!authed" class="auth-screen">
    <div class="auth-bg"></div>
    <div class="auth-card">
      <div class="auth-logo">
        <div class="logo-icon">⚡</div>
        <h1>LowForge</h1>
        <p>AI 驱动的低代码开发平台</p>
      </div>
      <div class="tab-switch">
        <button :class="['tab-btn', authTab === 'login' && 'active']" @click="authTab = 'login'">登录</button>
        <button :class="['tab-btn', authTab === 'register' && 'active']" @click="authTab = 'register'">注册</button>
      </div>
      <div v-if="authTab === 'login'">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input v-model="loginForm.username" class="form-input" type="text" placeholder="输入用户名" @keydown.enter="doLogin" autocomplete="username" />
        </div>
        <div class="form-group">
          <label class="form-label">密码</label>
          <input v-model="loginForm.password" class="form-input" type="password" placeholder="输入密码" @keydown.enter="doLogin" autocomplete="current-password" />
        </div>
        <button class="btn-primary" :disabled="loginLoading" @click="doLogin">{{ loginLoading ? '登录中...' : '登 录' }}</button>
        <div class="auth-error">{{ loginError }}</div>
      </div>
      <div v-if="authTab === 'register'">
        <div class="form-group">
          <label class="form-label">用户名 <span class="hint">（3–20 字符）</span></label>
          <input v-model="regForm.username" class="form-input" type="text" placeholder="设置用户名" />
        </div>
        <div class="form-group">
          <label class="form-label">昵称 <span class="hint">（可选）</span></label>
          <input v-model="regForm.nickname" class="form-input" type="text" placeholder="设置显示昵称" />
        </div>
        <div class="form-group">
          <label class="form-label">密码 <span class="hint">（6–30 字符）</span></label>
          <input v-model="regForm.password" class="form-input" type="password" placeholder="设置密码" @keydown.enter="doRegister" />
        </div>
        <button class="btn-primary" :disabled="regLoading" @click="doRegister">{{ regLoading ? '注册中...' : '注 册' }}</button>
        <div class="auth-error">{{ regError }}</div>
      </div>
    </div>
  </div>

  <!-- ─── MAIN APP ─── -->
  <div v-else class="app-layout">
    <nav class="topnav">
      <div class="nav-logo">
        <div class="nav-logo-icon">⚡</div>
        LowForge
      </div>
      <div v-if="currentApp" class="nav-center">
        <button :class="['nav-tab', activeView === 'chat' && 'active']" @click="activeView = 'chat'">💬 AI 对话</button>
        <button :class="['nav-tab', activeView === 'preview' && 'active']" @click="switchToPreview">👁 预览</button>
      </div>
      <div class="nav-right">
        <div class="user-chip">
          <div class="user-avatar">{{ userInitial }}</div>
          <span class="user-name">{{ currentUser?.nickname || currentUser?.username }}</span>
        </div>
        <button class="btn-logout" @click="doLogout">退出</button>
      </div>
    </nav>

    <div class="main-body">
      <aside class="sidebar">
        <div class="sidebar-header">
          <h3>我的应用</h3>
          <button class="btn-new-app" @click="openCreateModal">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="14" height="14"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            新建应用
          </button>
        </div>
        <div class="app-list">
          <div v-if="!apps.length" class="sidebar-empty">暂无应用，创建第一个吧</div>
          <div v-for="app in apps" :key="app.id" :class="['app-item', currentApp?.id === app.id && 'active']">
            <div class="app-item-left" @click="selectApp(app)">
              <div class="app-icon">{{ appIcon(app.name) }}</div>
              <div class="app-info">
                <div class="app-item-name">{{ app.name }}</div>
                <div class="app-item-date">{{ fmtDate(app.updatedAt) }}</div>
              </div>
            </div>
            <button class="app-item-menu" @click.stop="openCtxMenu($event, app)">⋯</button>
          </div>
        </div>
      </aside>

      <div class="content-area">
        <div v-if="!currentApp" class="welcome-view">
          <div style="font-size:64px;margin-bottom:16px">⚡</div>
          <h2>开始构建你的应用</h2>
          <p>从左侧选择应用，或创建一个新应用，<br>然后用 AI 对话生成你的代码</p>
        </div>

        <div v-else class="workspace">
          <div class="workspace-topbar">
            <div class="workspace-topbar-left">
              <div>
                <div class="workspace-title">{{ currentApp.name }}</div>
                <div class="workspace-desc">{{ currentApp.description || '暂无描述' }}</div>
              </div>
              <span class="app-status-badge">ACTIVE</span>
            </div>
            <div class="workspace-actions">
              <button class="btn-action" @click="openEditModal">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                编辑
              </button>
              <button class="btn-action" @click="switchToPreview">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                预览
              </button>
              <button class="btn-action" @click="downloadApp">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                下载
              </button>
            </div>
          </div>

          <!-- CHAT VIEW -->
          <div v-show="activeView === 'chat'" class="chat-area">
            <div class="chat-history-header">
              <span>{{ messages.length }} 条消息</span>
              <button class="btn-clear-history" @click="clearHistory">清空历史</button>
            </div>
            <div class="chat-messages" ref="msgContainer">
              <div v-if="!messages.length && !isGenerating" class="empty-chat">
                <div class="icon">💬</div>
                <h3>开始 AI 对话</h3>
                <p>描述你想要构建的功能，AI 将为你生成完整的前端代码。<br>支持粘贴 API 文档以获得更准确的结果。</p>
              </div>
              <template v-for="msg in messages" :key="msg.id">
                <div v-if="msg.role === 'SYSTEM' || msg.role === 'TOOL'" class="chat-message system">
                  <div class="msg-bubble">🔧 {{ msg.content }}</div>
                </div>
                <div v-else-if="msg.role === 'USER'" class="chat-message user">
                  <div>
                    <div class="msg-bubble user-bubble">{{ msg.content }}</div>
                    <div class="msg-time user-time">{{ fmtTime(msg.createdAt) }}</div>
                  </div>
                  <div class="msg-avatar user-av">{{ userInitial }}</div>
                </div>
                <div v-else class="chat-message assistant">
                  <div class="msg-avatar ai-av">🤖</div>
                  <div>
                    <div class="msg-bubble ai-bubble">
                      <span v-html="renderContent(msg.content)"></span>
                      <div v-if="msg.previewUrl">
                        <button class="preview-link-btn" @click="openPreviewUrl(msg.previewUrl)">👁 查看预览</button>
                      </div>
                    </div>
                    <div class="msg-time">{{ fmtTime(msg.createdAt) }}</div>
                  </div>
                </div>
              </template>
              <div v-if="isGenerating" class="chat-message assistant">
                <div class="msg-avatar ai-av">🤖</div>
                <div class="progress-bubble">
                  <div class="progress-label">{{ progressLabel }}</div>
                  <div class="progress-bar-wrap"><div class="progress-bar" :style="{ width: progressPct + '%' }"></div></div>
                  <div class="progress-pct">{{ progressPct }}%</div>
                </div>
              </div>
            </div>
            <div class="chat-input-area">
              <div class="chat-input-wrap">
                <textarea v-model="chatInput" class="chat-textarea" ref="chatTextarea" rows="1"
                  placeholder="描述你想要生成的页面或功能..." @keydown.enter.prevent.exact="sendMessage" @input="autoResize"></textarea>
                <button class="chat-send-btn" :disabled="isGenerating || !chatInput.trim()" @click="sendMessage">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="15" height="15"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
                </button>
              </div>
              <div class="input-api-doc">
                <button class="api-doc-toggle" @click="apiDocOpen = !apiDocOpen">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12" :style="{ transform: apiDocOpen ? 'rotate(90deg)' : 'rotate(0)', transition: 'transform 0.2s' }"><polyline points="9 18 15 12 9 6"/></svg>
                  {{ apiDocOpen ? '收起 API 文档' : '粘贴 API 文档（可选）' }}
                </button>
                <textarea v-show="apiDocOpen" v-model="apiDocText" class="api-doc-textarea" placeholder="粘贴你的 API 文档文本（最多 20000 字符）..."></textarea>
              </div>
            </div>
          </div>

          <!-- PREVIEW VIEW -->
          <div v-show="activeView === 'preview'" class="preview-panel">
            <div class="preview-topbar">
              <button class="btn-action" @click="activeView = 'chat'">← 返回对话</button>
              <div class="preview-url-bar">{{ previewUrl || '暂无预览地址' }}</div>
              <button class="btn-action" @click="openPreviewNewTab" :disabled="!previewUrl">↗ 新标签页打开</button>
              <button class="btn-action" @click="downloadApp">⬇ 下载</button>
            </div>
            <div class="preview-content">
              <div v-if="!previewUrl" class="preview-placeholder">
                <div class="placeholder-icon">🖼</div>
                <p>暂无预览，请先通过 AI 对话生成代码</p>
              </div>
              <div v-else class="preview-ready">
                <div class="preview-ready-icon">✅</div>
                <h3>代码已生成</h3>
                <p>由于浏览器安全限制，预览页面需在新标签页中打开。<br>点击下方按钮即可查看完整预览效果。</p>
                <a :href="previewUrl" target="_blank" rel="noopener noreferrer" class="btn-open-preview">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
                  在新标签页中打开预览
                </a>
                <div class="preview-url-copy">
                  <span>{{ previewUrl }}</span>
                  <button @click="copyUrl">{{ copied ? '已复制 ✓' : '复制链接' }}</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- CONTEXT MENU -->
  <div v-if="ctxMenu.open" class="context-menu" :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }">
    <div class="ctx-item" @click="openEditModal(); ctxMenu.open = false">✏️ 编辑信息</div>
    <div class="ctx-item" @click="switchToPreview(); ctxMenu.open = false">👁 预览</div>
    <div class="ctx-item" @click="downloadApp(); ctxMenu.open = false">⬇️ 下载代码</div>
    <div class="ctx-sep"></div>
    <div class="ctx-item danger" @click="openDeleteModal(); ctxMenu.open = false">🗑 删除应用</div>
  </div>

  <!-- APP MODAL -->
  <div v-if="appModal.open" class="modal-overlay" @click.self="appModal.open = false">
    <div class="modal">
      <h3>{{ appModal.isEdit ? '编辑应用信息' : '创建新应用' }}</h3>
      <div class="form-group">
        <label class="form-label">应用名称 *</label>
        <input v-model="appModal.name" class="form-input" type="text" placeholder="给应用起个名字..." @keydown.enter="confirmApp" ref="appNameInput" />
      </div>
      <div class="form-group">
        <label class="form-label">应用描述</label>
        <textarea v-model="appModal.desc" class="form-input" rows="3" placeholder="描述这个应用的用途..." style="resize:none"></textarea>
      </div>
      <div class="modal-actions">
        <button class="btn-cancel" @click="appModal.open = false">取消</button>
        <button class="btn-confirm" :disabled="appModal.loading" @click="confirmApp">
          {{ appModal.loading ? '处理中...' : (appModal.isEdit ? '保存' : '创建') }}
        </button>
      </div>
    </div>
  </div>

  <!-- DELETE MODAL -->
  <div v-if="deleteModal.open" class="modal-overlay" @click.self="deleteModal.open = false">
    <div class="modal">
      <h3>删除应用</h3>
      <p style="color:var(--text2);font-size:14px;line-height:1.6">确定要删除应用 <strong style="color:var(--text)">{{ currentApp?.name }}</strong> 吗？此操作不可恢复。</p>
      <div class="modal-actions">
        <button class="btn-cancel" @click="deleteModal.open = false">取消</button>
        <button class="btn-confirm" style="background:var(--danger)" :disabled="deleteModal.loading" @click="confirmDelete">
          {{ deleteModal.loading ? '删除中...' : '确认删除' }}
        </button>
      </div>
    </div>
  </div>

  <!-- TOASTS -->
  <div class="toast-container">
    <div v-for="t in toasts" :key="t.id" :class="['toast', t.type]">
      <span>{{ t.icon }}</span> {{ t.msg }}
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { ENDPOINTS, request } from './api.js'

// ─── STATE ────────────────────────────────────────────────────────────────────
const authed       = ref(false)
const currentUser  = ref(null)
const authTab      = ref('login')
const loginForm    = reactive({ username: '', password: '' })
const loginLoading = ref(false)
const loginError   = ref('')
const regForm      = reactive({ username: '', nickname: '', password: '' })
const regLoading   = ref(false)
const regError     = ref('')

const apps         = ref([])
const currentApp   = ref(null)
const activeView   = ref('chat')

const messages     = ref([])
const msgContainer = ref(null)
const chatTextarea = ref(null)
const chatInput    = ref('')
const apiDocOpen   = ref(false)
const apiDocText   = ref('')
const isGenerating = ref(false)
const progressLabel= ref('准备中...')
const progressPct  = ref(0)

const previewUrl   = ref(null)
const copied       = ref(false)

const appModal     = reactive({ open: false, isEdit: false, name: '', desc: '', loading: false })
const appNameInput = ref(null)
const deleteModal  = reactive({ open: false, loading: false })
const ctxMenu      = reactive({ open: false, x: 0, y: 0 })
const toasts       = ref([])

// ─── COMPUTED ─────────────────────────────────────────────────────────────────
const userInitial = computed(() => {
  const name = currentUser.value?.nickname || currentUser.value?.username || 'U'
  return name.charAt(0).toUpperCase()
})

// ─── AUTH ─────────────────────────────────────────────────────────────────────
async function checkAuth() {
  const token = localStorage.getItem('lf_token')
  if (!token) return
  try {
    const data = await request(ENDPOINTS.me())
    if (data.code === 0) {
      currentUser.value = data.data
      authed.value = true
      await loadApps()
    } else {
      localStorage.removeItem('lf_token')
    }
  } catch (e) {
    localStorage.removeItem('lf_token')
  }
}

async function doLogin() {
  const { username, password } = loginForm
  if (!username || !password) { loginError.value = '请填写用户名和密码'; return }
  loginLoading.value = true; loginError.value = ''
  try {
    const data = await request(ENDPOINTS.login(), { method: 'POST', body: JSON.stringify({ username, password }) })
    if (data.code === 0) {
      localStorage.setItem('lf_token', data.data.token)
      currentUser.value = data.data
      authed.value = true
      await loadApps()
    } else {
      loginError.value = data.message || '登录失败'
    }
  } catch (e) { loginError.value = '网络错误，请检查连接' }
  loginLoading.value = false
}

async function doRegister() {
  const { username, password, nickname } = regForm
  if (!username || !password) { regError.value = '请填写用户名和密码'; return }
  if (username.length < 3 || username.length > 20) { regError.value = '用户名长度 3~20 字符'; return }
  if (password.length < 6) { regError.value = '密码至少 6 位'; return }
  regLoading.value = true; regError.value = ''
  try {
    const body = { username, password }
    if (nickname) body.nickname = nickname
    const data = await request(ENDPOINTS.register(), { method: 'POST', body: JSON.stringify(body) })
    if (data.code === 0) {
      toast('注册成功，请登录', 'success')
      authTab.value = 'login'
      loginForm.username = username
    } else { regError.value = data.message || '注册失败' }
  } catch (e) { regError.value = '网络错误' }
  regLoading.value = false
}

async function doLogout() {
  try { await request(ENDPOINTS.logout(), { method: 'POST' }) } catch (e) {}
  localStorage.removeItem('lf_token')
  authed.value = false; currentUser.value = null
  apps.value = []; currentApp.value = null
  messages.value = []; previewUrl.value = null
  loginForm.username = ''; loginForm.password = ''
}

// ─── APPS ─────────────────────────────────────────────────────────────────────
async function loadApps() {
  try {
    const data = await request(ENDPOINTS.apps())
    if (data.code === 0) apps.value = data.data || []
  } catch (e) { toast('加载应用列表失败', 'error') }
}

async function selectApp(app) {
  currentApp.value = app
  activeView.value = 'chat'
  previewUrl.value = null
  messages.value = []
  await loadConversations()
}

function openCreateModal() {
  appModal.isEdit = false; appModal.name = ''; appModal.desc = ''; appModal.open = true
  nextTick(() => appNameInput.value?.focus())
}
function openEditModal() {
  if (!currentApp.value) return
  appModal.isEdit = true; appModal.name = currentApp.value.name
  appModal.desc = currentApp.value.description || ''; appModal.open = true
  nextTick(() => appNameInput.value?.focus())
}
async function confirmApp() {
  if (!appModal.name.trim()) { toast('请输入应用名称', 'error'); return }
  appModal.loading = true
  try {
    const body = { name: appModal.name.trim(), description: appModal.desc.trim() }
    let data
    if (appModal.isEdit) {
      data = await request(ENDPOINTS.app(currentApp.value.id), { method: 'PUT', body: JSON.stringify(body) })
    } else {
      data = await request(ENDPOINTS.apps(), { method: 'POST', body: JSON.stringify(body) })
    }
    if (data.code === 0) {
      toast(appModal.isEdit ? '应用已更新' : '应用已创建', 'success')
      appModal.open = false; await loadApps()
      if (!appModal.isEdit) await selectApp(data.data)
      else currentApp.value = data.data
    } else { toast(data.message || '操作失败', 'error') }
  } catch (e) { toast('网络错误', 'error') }
  appModal.loading = false
}

function openDeleteModal() { deleteModal.open = true }
async function confirmDelete() {
  deleteModal.loading = true
  try {
    const data = await request(ENDPOINTS.app(currentApp.value.id), { method: 'DELETE' })
    if (data.code === 0) {
      toast('应用已删除', 'success'); deleteModal.open = false
      apps.value = apps.value.filter(a => a.id !== currentApp.value.id)
      currentApp.value = null; messages.value = []; previewUrl.value = null
    } else { toast(data.message || '删除失败', 'error') }
  } catch (e) { toast('网络错误', 'error') }
  deleteModal.loading = false
}

// ─── CONVERSATIONS ────────────────────────────────────────────────────────────
// Bug fix #1: 历史对话记录加载
// 问题：API返回records按时间倒序，需要翻转；且可能有多页需全部加载
async function loadConversations() {
  if (!currentApp.value) return
  try {
    const first = await request(ENDPOINTS.conversations(currentApp.value.id, 1, 50))
    if (first.code !== 0) return
    const { records: firstRecords, pages } = first.data
    let all = [...(firstRecords || [])]

    // 加载剩余页
    if (pages > 1) {
      const rest = await Promise.all(
        Array.from({ length: pages - 1 }, (_, i) =>
          request(ENDPOINTS.conversations(currentApp.value.id, i + 2, 50))
        )
      )
      for (const r of rest) {
        if (r.code === 0) all = all.concat(r.data.records || [])
      }
    }

    // API 返回倒序（最新在前），翻转为正序展示
    all.reverse()
    messages.value = all

    // 提取最新的 previewUrl
    for (let i = all.length - 1; i >= 0; i--) {
      if (all[i].previewUrl) { previewUrl.value = all[i].previewUrl; break }
    }

    await scrollToBottom()
  } catch (e) { toast('加载历史记录失败', 'error') }
}

async function clearHistory() {
  if (!currentApp.value || !confirm('确定清空所有对话历史？')) return
  try {
    const data = await request(ENDPOINTS.conversations(currentApp.value.id), { method: 'DELETE' })
    if (data.code === 0) { messages.value = []; previewUrl.value = null; toast('历史已清空', 'success') }
  } catch (e) { toast('清空失败', 'error') }
}

// ─── SEND / GENERATE ──────────────────────────────────────────────────────────
async function sendMessage() {
  if (!currentApp.value || isGenerating.value || !chatInput.value.trim()) return
  const prompt = chatInput.value.trim()
  const apiDoc = apiDocText.value.trim()
  chatInput.value = ''
  await nextTick(); autoResize()

  // 乐观插入用户消息
  messages.value.push({ id: Date.now(), role: 'USER', content: prompt, createdAt: Date.now() })
  await scrollToBottom()

  isGenerating.value = true; progressLabel.value = '正在提交任务...'; progressPct.value = 5
  try {
    const body = { prompt }
    if (apiDoc) body.apiDocText = apiDoc
    const genData = await request(ENDPOINTS.generate(currentApp.value.id), { method: 'POST', body: JSON.stringify(body) })
    if (genData.code !== 0) {
      messages.value.push({ id: Date.now(), role: 'ASSISTANT', content: `❌ 提交失败：${genData.message || '未知错误'}`, createdAt: Date.now() })
      isGenerating.value = false; await scrollToBottom(); return
    }
    progressLabel.value = '任务已提交，建立连接...'; progressPct.value = 10
    await listenToStream(genData.data.id)
  } catch (e) {
    messages.value.push({ id: Date.now(), role: 'ASSISTANT', content: `❌ 网络错误：${e.message}`, createdAt: Date.now() })
    isGenerating.value = false; await scrollToBottom()
  }
}

function listenToStream(taskId) {
  return new Promise((resolve) => {
    const appId = currentApp.value.id
    const token = localStorage.getItem('lf_token') || ''
    const url = ENDPOINTS.taskStream(appId, taskId) + (token ? `?token=${encodeURIComponent(token)}` : '')
    const sse = new EventSource(url)
    let fallbackTimer = null
    let resolved = false

    function done() {
      if (resolved) return; resolved = true
      sse.close(); clearInterval(fallbackTimer); isGenerating.value = false; resolve()
    }

    sse.addEventListener('connected', () => { progressLabel.value = '已连接，等待生成...'; progressPct.value = 15 })
    sse.addEventListener('progress', e => {
      try {
        const d = JSON.parse(e.data)
        progressLabel.value = d.message || '生成中...'; progressPct.value = d.percent || 50
        scrollToBottom()
      } catch (_) {}
    })
    sse.addEventListener('error', async e => {
      try {
        const d = JSON.parse(e.data)
        messages.value.push({ id: Date.now(), role: 'ASSISTANT', content: `❌ 生成失败：${d.message || '未知错误'}`, createdAt: Date.now() })
      } catch (_) {
        messages.value.push({ id: Date.now(), role: 'ASSISTANT', content: '❌ 生成过程中发生错误', createdAt: Date.now() })
      }
      await scrollToBottom(); done()
    })
    sse.addEventListener('complete', async () => {
      progressLabel.value = '生成完成！'; progressPct.value = 100
      await new Promise(r => setTimeout(r, 400))
      await loadConversations()
      toast('代码生成成功！', 'success'); done()
    })
    // SSE 断线降级轮询
    sse.onerror = () => {
      if (resolved) return; sse.close()
      fallbackTimer = setInterval(async () => {
        if (resolved) { clearInterval(fallbackTimer); return }
        try {
          const data = await request(ENDPOINTS.taskStatus(appId, taskId))
          if (data.code === 0) {
            const s = data.data.status
            if (s === 'SUCCESS') {
              progressLabel.value = '生成完成！'; progressPct.value = 100
              await new Promise(r => setTimeout(r, 400))
              await loadConversations(); toast('代码生成成功！', 'success'); done()
            } else if (s === 'FAILED') {
              messages.value.push({ id: Date.now(), role: 'ASSISTANT', content: `❌ 生成失败：${data.data.errorMessage || '未知错误'}`, createdAt: Date.now() })
              await scrollToBottom(); done()
            } else { progressLabel.value = '处理中...'; progressPct.value = 50 }
          }
        } catch (_) {}
      }, 3000)
    }
  })
}

// ─── PREVIEW ──────────────────────────────────────────────────────────────────
// Bug fix #2: 预览无法在 iframe 内访问
// 根本原因：服务端静态文件响应头含 X-Frame-Options: SAMEORIGIN 或 CSP frame-ancestors 'self'，
// 导致跨域嵌入被浏览器阻止；而直接打开链接正常是因为不走 frame 加载。
// 解决方案：弃用 iframe，改为提供"新标签页打开"+ 复制链接 的交互方式。
function openPreviewUrl(url) { previewUrl.value = url; activeView.value = 'preview' }
function switchToPreview() {
  if (!currentApp.value) return
  if (!previewUrl.value) previewUrl.value = ENDPOINTS.preview(currentApp.value.id)
  activeView.value = 'preview'
}
function openPreviewNewTab() {
  if (previewUrl.value) window.open(previewUrl.value, '_blank', 'noopener,noreferrer')
}
async function copyUrl() {
  if (!previewUrl.value) return
  try { await navigator.clipboard.writeText(previewUrl.value); copied.value = true; setTimeout(() => { copied.value = false }, 2000) }
  catch (e) { toast('复制失败', 'error') }
}

async function downloadApp() {
  if (!currentApp.value) return
  toast('准备下载...', 'info')
  try {
    const token = localStorage.getItem('lf_token') || ''
    const res = await fetch(ENDPOINTS.download(currentApp.value.id), { method: 'POST', headers: { Authorization: `Bearer ${token}` } })
    if (!res.ok) { const err = await res.json().catch(() => ({})); toast(err.message || '下载失败', 'error'); return }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = `${currentApp.value.name || 'app'}-source.zip`
    document.body.appendChild(a); a.click(); document.body.removeChild(a); URL.revokeObjectURL(url)
    toast('下载成功！', 'success')
  } catch (e) { toast('下载失败：' + e.message, 'error') }
}

// ─── HELPERS ──────────────────────────────────────────────────────────────────
function appIcon(name) {
  const icons = ['🚀','💡','⚡','🎯','🛠','📊','🎨','📦','🔧','🌟']
  return icons[(name || '').charCodeAt(0) % icons.length]
}
function fmtDate(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) + ' ' +
         d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
function fmtTime(ts) {
  if (!ts) return ''
  return new Date(ts).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
function renderContent(content) {
  if (!content) return ''
  return content.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\n/g,'<br>')
}
async function scrollToBottom() {
  await nextTick()
  if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight
}
function autoResize() {
  const el = chatTextarea.value; if (!el) return
  el.style.height = 'auto'; el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}
function openCtxMenu(e, app) {
  currentApp.value = app
  ctxMenu.x = Math.min(e.clientX, window.innerWidth - 180)
  ctxMenu.y = Math.min(e.clientY, window.innerHeight - 180)
  ctxMenu.open = true
}
function toast(msg, type = 'info') {
  const icons = { success: '✅', error: '❌', info: 'ℹ️' }
  const id = Date.now() + Math.random()
  toasts.value.push({ id, msg, type, icon: icons[type] || 'ℹ️' })
  setTimeout(() => { toasts.value = toasts.value.filter(t => t.id !== id) }, 3000)
}
function handleDocClick(e) {
  const menu = document.querySelector('.context-menu')
  if (menu && !menu.contains(e.target)) ctxMenu.open = false
}
function handleEsc(e) {
  if (e.key === 'Escape') { ctxMenu.open = false; appModal.open = false; deleteModal.open = false }
}
onMounted(() => { checkAuth(); document.addEventListener('click', handleDocClick); document.addEventListener('keydown', handleEsc) })
onBeforeUnmount(() => { document.removeEventListener('click', handleDocClick); document.removeEventListener('keydown', handleEsc) })
</script>

<style>
*, *::before, *::after { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { width: 100%; height: 100%; overflow: hidden; }
:root {
  --bg: #f4f3ff; --surface: #ffffff; --surface2: #f0effe; --surface3: #e5e2fc;
  --border: rgba(100,80,220,0.18); --border2: rgba(100,80,220,0.1);
  --accent: #6341f0; --accent2: #7c5cfc; --accent3: #0ea5e9; --accent4: #059669;
  --danger: #dc2626; --text: #1a1535; --text2: #4b4480; --text3: #9490c0;
  --r: 12px; --r2: 8px;
  --shadow: 0 0 0 1px var(--border), 0 8px 32px rgba(80,60,200,0.08);
  --glow: 0 0 24px rgba(99,65,240,0.12);
}
body { font-family: 'Noto Sans SC', sans-serif; background: var(--bg); color: var(--text); }
::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--surface3); border-radius: 3px; }

/* AUTH */
.auth-screen { position: fixed; inset: 0; display: flex; align-items: center; justify-content: center; background: var(--bg); }
.auth-bg { position: absolute; inset: 0; overflow: hidden; pointer-events: none; }
.auth-bg::before { content: ''; position: absolute; top: -30%; left: 50%; transform: translateX(-50%); width: 800px; height: 800px; background: radial-gradient(ellipse, rgba(99,65,240,0.08) 0%, transparent 70%); }
.auth-bg::after { content: ''; position: absolute; bottom: -20%; right: -10%; width: 500px; height: 500px; background: radial-gradient(ellipse, rgba(14,165,233,0.06) 0%, transparent 70%); }
.auth-card { position: relative; z-index: 1; width: 420px; background: var(--surface); border: 1px solid var(--border); border-radius: 20px; padding: 40px; box-shadow: var(--shadow), var(--glow); animation: slideUp 0.5s cubic-bezier(0.34,1.56,0.64,1); }
@keyframes slideUp { from { opacity: 0; transform: translateY(30px); } to { opacity: 1; transform: translateY(0); } }
.auth-logo { text-align: center; margin-bottom: 32px; }
.logo-icon { display: inline-flex; align-items: center; justify-content: center; width: 52px; height: 52px; border-radius: 14px; background: linear-gradient(135deg, var(--accent), var(--accent3)); font-size: 24px; margin-bottom: 12px; box-shadow: 0 0 20px rgba(99,65,240,0.3); }
.auth-logo h1 { font-family: 'Syne', sans-serif; font-size: 22px; font-weight: 800; letter-spacing: -0.5px; background: linear-gradient(135deg, var(--text), var(--accent2)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.auth-logo p { font-size: 13px; color: var(--text3); margin-top: 4px; }
.hint { color: var(--text3); font-weight: 400; }
.tab-switch { display: flex; background: var(--surface2); border-radius: var(--r2); padding: 3px; margin-bottom: 24px; border: 1px solid var(--border2); }
.tab-btn { flex: 1; padding: 8px; border: none; background: transparent; color: var(--text3); border-radius: 6px; cursor: pointer; font-size: 14px; font-family: 'Noto Sans SC', sans-serif; transition: all 0.2s; }
.tab-btn.active { background: white; color: var(--text); box-shadow: 0 1px 4px rgba(80,60,200,0.12); }
.form-group { margin-bottom: 16px; }
.form-label { display: block; font-size: 12px; color: var(--text3); margin-bottom: 6px; font-weight: 500; }
.form-input { width: 100%; padding: 11px 14px; background: var(--surface2); border: 1px solid var(--border2); border-radius: var(--r2); color: var(--text); font-size: 14px; font-family: 'Noto Sans SC', sans-serif; transition: border-color 0.2s, box-shadow 0.2s; outline: none; }
.form-input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px rgba(99,65,240,0.12); }
.form-input::placeholder { color: var(--text3); }
.btn-primary { width: 100%; padding: 12px; background: linear-gradient(135deg, var(--accent), #5a3fd4); border: none; border-radius: var(--r2); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; font-family: 'Noto Sans SC', sans-serif; transition: all 0.2s; margin-top: 4px; }
.btn-primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 8px 20px rgba(99,65,240,0.35); }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.auth-error { color: var(--danger); font-size: 13px; margin-top: 8px; text-align: center; min-height: 18px; }

/* LAYOUT */
.app-layout { display: flex; flex-direction: column; height: 100vh; }
.topnav { height: 52px; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; border-bottom: 1px solid var(--border2); background: rgba(255,255,255,0.9); backdrop-filter: blur(12px); flex-shrink: 0; position: relative; z-index: 100; }
.nav-logo { display: flex; align-items: center; gap: 10px; font-family: 'Syne', sans-serif; font-size: 17px; font-weight: 800; letter-spacing: -0.5px; background: linear-gradient(135deg, var(--text), var(--accent2)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.nav-logo-icon { width: 28px; height: 28px; border-radius: 7px; background: linear-gradient(135deg, var(--accent), var(--accent3)); display: flex; align-items: center; justify-content: center; font-size: 14px; }
.nav-center { display: flex; gap: 4px; }
.nav-tab { padding: 6px 14px; border-radius: 6px; border: none; background: transparent; color: var(--text3); font-size: 13px; cursor: pointer; font-family: 'Noto Sans SC', sans-serif; transition: all 0.15s; }
.nav-tab:hover { color: var(--text2); background: var(--surface2); }
.nav-tab.active { color: var(--text); background: var(--surface2); font-weight: 500; }
.nav-right { display: flex; align-items: center; gap: 10px; }
.user-chip { display: flex; align-items: center; gap: 8px; padding: 5px 12px 5px 5px; background: var(--surface2); border: 1px solid var(--border2); border-radius: 20px; }
.user-avatar { width: 26px; height: 26px; border-radius: 50%; background: linear-gradient(135deg, var(--accent), var(--accent3)); display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; color: #fff; }
.user-name { font-size: 13px; color: var(--text2); }
.btn-logout { padding: 6px 12px; border-radius: 6px; border: 1px solid var(--border2); background: transparent; color: var(--text3); font-size: 12px; cursor: pointer; font-family: 'Noto Sans SC', sans-serif; transition: all 0.15s; }
.btn-logout:hover { color: var(--danger); border-color: var(--danger); }
.main-body { display: flex; flex: 1; overflow: hidden; }

/* SIDEBAR */
.sidebar { width: 240px; border-right: 1px solid var(--border2); background: var(--surface); display: flex; flex-direction: column; flex-shrink: 0; }
.sidebar-header { padding: 16px 16px 12px; border-bottom: 1px solid var(--border2); }
.sidebar-header h3 { font-size: 11px; font-weight: 600; color: var(--text3); letter-spacing: 0.8px; text-transform: uppercase; margin-bottom: 10px; }
.btn-new-app { width: 100%; padding: 9px 14px; border-radius: var(--r2); background: var(--accent); border: none; color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; font-family: 'Noto Sans SC', sans-serif; transition: all 0.2s; display: flex; align-items: center; justify-content: center; gap: 6px; }
.btn-new-app:hover { background: #5530d4; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,65,240,0.3); }
.app-list { flex: 1; overflow-y: auto; padding: 8px; }
.sidebar-empty { padding: 24px 8px; text-align: center; color: var(--text3); font-size: 13px; }
.app-item { display: flex; align-items: center; justify-content: space-between; border-radius: var(--r2); cursor: pointer; transition: all 0.15s; margin-bottom: 2px; border: 1px solid transparent; }
.app-item:hover { background: var(--surface2); border-color: var(--border2); }
.app-item.active { background: rgba(99,65,240,0.07); border-color: rgba(99,65,240,0.2); }
.app-item-left { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; padding: 10px 6px 10px 10px; }
.app-icon { width: 32px; height: 32px; border-radius: 8px; flex-shrink: 0; background: linear-gradient(135deg, rgba(99,65,240,0.1), rgba(14,165,233,0.08)); display: flex; align-items: center; justify-content: center; font-size: 16px; }
.app-info { flex: 1; min-width: 0; }
.app-item-name { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.app-item-date { font-size: 11px; color: var(--text3); margin-top: 1px; }
.app-item-menu { width: 28px; height: 28px; border-radius: 4px; border: none; background: transparent; color: var(--text3); cursor: pointer; opacity: 0; transition: opacity 0.15s; font-size: 18px; margin-right: 4px; display: flex; align-items: center; justify-content: center; }
.app-item:hover .app-item-menu { opacity: 1; }

/* CONTENT */
.content-area { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.welcome-view { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px; text-align: center; color: var(--text3); }
.welcome-view h2 { font-family: 'Syne', sans-serif; font-size: 26px; font-weight: 800; color: var(--text); margin-bottom: 10px; }
.welcome-view p { font-size: 15px; line-height: 1.7; }

/* WORKSPACE */
.workspace { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.workspace-topbar { padding: 12px 20px; border-bottom: 1px solid var(--border2); display: flex; align-items: center; justify-content: space-between; background: var(--surface); flex-shrink: 0; }
.workspace-topbar-left { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.workspace-title { font-size: 15px; font-weight: 600; }
.workspace-desc { font-size: 12px; color: var(--text3); margin-top: 2px; }
.app-status-badge { padding: 2px 8px; border-radius: 20px; font-size: 11px; font-weight: 600; background: rgba(5,150,105,0.08); color: var(--accent4); border: 1px solid rgba(5,150,105,0.2); flex-shrink: 0; }
.workspace-actions { display: flex; gap: 8px; flex-shrink: 0; }
.btn-action { padding: 7px 12px; border-radius: var(--r2); border: 1px solid var(--border2); background: var(--surface2); color: var(--text2); font-size: 13px; cursor: pointer; font-family: 'Noto Sans SC', sans-serif; transition: all 0.15s; display: flex; align-items: center; gap: 5px; }
.btn-action:hover:not(:disabled) { border-color: var(--border); color: var(--text); background: var(--surface3); }
.btn-action:disabled { opacity: 0.5; cursor: not-allowed; }

/* CHAT */
.chat-area { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.chat-history-header { padding: 10px 20px; border-bottom: 1px solid var(--border2); display: flex; align-items: center; justify-content: space-between; flex-shrink: 0; background: var(--surface); }
.chat-history-header span { font-size: 13px; color: var(--text3); }
.btn-clear-history { padding: 4px 10px; border-radius: 4px; border: 1px solid rgba(220,38,38,0.2); background: transparent; color: var(--danger); font-size: 12px; cursor: pointer; font-family: 'Noto Sans SC', sans-serif; transition: all 0.15s; }
.btn-clear-history:hover { background: rgba(220,38,38,0.06); }
.chat-messages { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; }
.empty-chat { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; color: var(--text3); padding: 40px; text-align: center; }
.empty-chat .icon { font-size: 48px; margin-bottom: 4px; }
.empty-chat h3 { font-family: 'Syne', sans-serif; font-size: 18px; font-weight: 700; color: var(--text2); }
.empty-chat p { font-size: 14px; line-height: 1.7; }
.chat-message { display: flex; gap: 12px; }
.chat-message.user { flex-direction: row-reverse; }
.chat-message.system { justify-content: center; }
.msg-avatar { width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.user-av { background: linear-gradient(135deg, var(--accent), var(--accent3)); font-size: 13px; color: #fff; font-weight: 700; }
.ai-av { background: var(--surface2); border: 1px solid var(--border2); font-size: 16px; }
.msg-bubble { padding: 11px 15px; border-radius: 14px; font-size: 14px; line-height: 1.65; max-width: 72%; }
.user-bubble { background: var(--accent); color: #fff; border-bottom-right-radius: 4px; }
.ai-bubble { background: var(--surface2); border: 1px solid var(--border2); color: var(--text); border-bottom-left-radius: 4px; }
.chat-message.system .msg-bubble { background: rgba(14,165,233,0.06); border: 1px solid rgba(14,165,233,0.15); color: var(--accent3); font-size: 13px; border-radius: 8px; max-width: 100%; }
.msg-time { font-size: 11px; color: var(--text3); margin-top: 4px; }
.user-time { text-align: right; }
.preview-link-btn { display: inline-flex; align-items: center; gap: 6px; margin-top: 10px; padding: 7px 14px; border-radius: 6px; background: rgba(5,150,105,0.08); border: 1px solid rgba(5,150,105,0.2); color: var(--accent4); font-size: 13px; cursor: pointer; font-weight: 500; transition: all 0.15s; font-family: 'Noto Sans SC', sans-serif; }
.preview-link-btn:hover { background: rgba(5,150,105,0.14); }
.progress-bubble { background: var(--surface2); border: 1px solid var(--border2); border-radius: 12px; padding: 14px 16px; min-width: 260px; }
.progress-label { font-size: 13px; color: var(--text2); margin-bottom: 8px; }
.progress-bar-wrap { background: var(--surface3); border-radius: 4px; height: 6px; }
.progress-bar { height: 100%; border-radius: 4px; background: linear-gradient(90deg, var(--accent), var(--accent3)); transition: width 0.4s; }
.progress-pct { font-size: 11px; color: var(--text3); margin-top: 4px; }
.chat-input-area { padding: 14px 20px; border-top: 1px solid var(--border2); background: var(--surface); flex-shrink: 0; }
.chat-input-wrap { display: flex; gap: 10px; align-items: flex-end; background: var(--surface2); border: 1px solid var(--border2); border-radius: 14px; padding: 10px 14px; transition: border-color 0.2s; }
.chat-input-wrap:focus-within { border-color: var(--accent); box-shadow: 0 0 0 3px rgba(99,65,240,0.08); }
.chat-textarea { flex: 1; background: transparent; border: none; outline: none; color: var(--text); font-size: 14px; font-family: 'Noto Sans SC', sans-serif; resize: none; max-height: 120px; line-height: 1.5; min-height: 22px; }
.chat-textarea::placeholder { color: var(--text3); }
.chat-send-btn { width: 34px; height: 34px; border-radius: 8px; border: none; background: var(--accent); color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; flex-shrink: 0; }
.chat-send-btn:hover:not(:disabled) { background: #5530d4; transform: scale(1.05); }
.chat-send-btn:disabled { background: var(--surface3); color: var(--text3); cursor: not-allowed; transform: none; }
.input-api-doc { margin-top: 8px; }
.api-doc-toggle { font-size: 12px; color: var(--text3); cursor: pointer; display: flex; align-items: center; gap: 4px; background: none; border: none; font-family: 'Noto Sans SC', sans-serif; padding: 2px 0; }
.api-doc-toggle:hover { color: var(--text2); }
.api-doc-textarea { width: 100%; margin-top: 6px; padding: 8px 12px; background: var(--surface2); border: 1px solid var(--border2); border-radius: 8px; color: var(--text2); font-size: 12px; font-family: 'JetBrains Mono', monospace; resize: vertical; min-height: 60px; max-height: 120px; outline: none; }
.api-doc-textarea:focus { border-color: var(--accent); }

/* PREVIEW */
.preview-panel { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.preview-topbar { padding: 10px 16px; border-bottom: 1px solid var(--border2); display: flex; align-items: center; gap: 10px; background: var(--surface); flex-shrink: 0; }
.preview-url-bar { flex: 1; padding: 6px 12px; background: var(--surface2); border: 1px solid var(--border2); border-radius: 6px; font-size: 12px; color: var(--text2); font-family: 'JetBrains Mono', monospace; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.preview-content { flex: 1; display: flex; align-items: center; justify-content: center; background: var(--bg); padding: 40px; overflow: auto; }
.preview-placeholder { text-align: center; color: var(--text3); }
.placeholder-icon { font-size: 56px; margin-bottom: 12px; }
.preview-placeholder p { font-size: 14px; }
.preview-ready { text-align: center; max-width: 480px; }
.preview-ready-icon { font-size: 56px; margin-bottom: 16px; }
.preview-ready h3 { font-family: 'Syne', sans-serif; font-size: 22px; font-weight: 700; color: var(--text); margin-bottom: 10px; }
.preview-ready p { font-size: 14px; color: var(--text3); line-height: 1.7; margin-bottom: 24px; }
.btn-open-preview { display: inline-flex; align-items: center; gap: 8px; padding: 12px 24px; border-radius: var(--r2); background: linear-gradient(135deg, var(--accent), #5530d4); color: #fff; font-size: 15px; font-weight: 600; text-decoration: none; font-family: 'Noto Sans SC', sans-serif; transition: all 0.2s; box-shadow: 0 4px 16px rgba(99,65,240,0.3); }
.btn-open-preview:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(99,65,240,0.4); }
.preview-url-copy { margin-top: 20px; display: flex; align-items: center; gap: 8px; background: var(--surface); border: 1px solid var(--border2); border-radius: var(--r2); padding: 8px 12px; }
.preview-url-copy span { flex: 1; font-size: 12px; font-family: 'JetBrains Mono', monospace; color: var(--text2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.preview-url-copy button { padding: 4px 10px; border-radius: 4px; border: 1px solid var(--border2); background: var(--surface2); color: var(--accent); font-size: 12px; cursor: pointer; font-family: 'Noto Sans SC', sans-serif; white-space: nowrap; transition: all 0.15s; font-weight: 500; }
.preview-url-copy button:hover { background: var(--surface3); }

/* MODALS & OVERLAYS */
.context-menu { position: fixed; z-index: 300; background: #fff; border: 1px solid var(--border); border-radius: 10px; padding: 4px; min-width: 160px; box-shadow: 0 8px 24px rgba(80,60,200,0.15); }
.ctx-item { display: flex; align-items: center; gap: 8px; padding: 9px 12px; border-radius: 6px; cursor: pointer; font-size: 13px; color: var(--text2); transition: all 0.1s; }
.ctx-item:hover { background: var(--surface2); color: var(--text); }
.ctx-item.danger { color: var(--danger); }
.ctx-item.danger:hover { background: rgba(220,38,38,0.06); }
.ctx-sep { height: 1px; background: var(--border2); margin: 4px 0; }
.modal-overlay { position: fixed; inset: 0; background: rgba(80,60,200,0.1); backdrop-filter: blur(4px); z-index: 500; display: flex; align-items: center; justify-content: center; }
.modal { background: #fff; border: 1px solid var(--border); border-radius: 18px; padding: 28px; width: 480px; max-width: 90vw; box-shadow: 0 20px 60px rgba(80,60,200,0.15), var(--glow); animation: slideUp 0.2s cubic-bezier(0.34,1.56,0.64,1); }
.modal h3 { font-family: 'Syne', sans-serif; font-size: 18px; font-weight: 700; margin-bottom: 20px; }
.modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px; }
.btn-cancel { padding: 9px 18px; border-radius: var(--r2); border: 1px solid var(--border2); background: transparent; color: var(--text3); font-size: 14px; cursor: pointer; font-family: 'Noto Sans SC', sans-serif; transition: all 0.15s; }
.btn-cancel:hover { color: var(--text); border-color: var(--border); }
.btn-confirm { padding: 9px 20px; border-radius: var(--r2); border: none; background: var(--accent); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; font-family: 'Noto Sans SC', sans-serif; transition: all 0.2s; }
.btn-confirm:hover:not(:disabled) { background: #5530d4; }
.btn-confirm:disabled { opacity: 0.6; cursor: not-allowed; }
.toast-container { position: fixed; top: 20px; right: 20px; z-index: 2000; display: flex; flex-direction: column; gap: 8px; }
.toast { padding: 12px 16px; border-radius: 10px; font-size: 13px; background: #fff; border: 1px solid var(--border); box-shadow: 0 4px 16px rgba(80,60,200,0.12); display: flex; align-items: center; gap: 8px; max-width: 300px; color: var(--text); animation: toastIn 0.3s cubic-bezier(0.34,1.56,0.64,1); }
.toast.success { border-color: rgba(5,150,105,0.3); }
.toast.error { border-color: rgba(220,38,38,0.3); }
.toast.info { border-color: rgba(14,165,233,0.3); }
@keyframes toastIn { from { opacity: 0; transform: translateX(30px); } to { opacity: 1; transform: translateX(0); } }
</style>
