<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户提示词
const userPrompt = ref('')
const genMode = ref('normal')
const creating = ref(false)

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 精选应用数据
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 设置提示词
const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

// 优化提示词功能已移除

// 创建应用
const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
      genMode: genMode.value,
    })

    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      // 跳转到对话页面，确保ID是字符串类型
      const appId = String(res.data.data)
      await router.push(`/app/chat/${appId}`)
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
  }
}

// 加载精选应用
const loadFeaturedApps = async () => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载精选应用失败：', error)
  }
}

// 查看对话
const viewChat = (appId: string | number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}?view=1`)
  }
}

// 查看作品
const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

// 格式化时间函数已移除，不再需要显示创建时间

// 页面加载时获取数据
onMounted(() => {
  loadMyApps()
  loadFeaturedApps()

  // 鼠标跟随光效
  const handleMouseMove = (e: MouseEvent) => {
    const { clientX, clientY } = e
    const { innerWidth, innerHeight } = window
    const x = (clientX / innerWidth) * 100
    const y = (clientY / innerHeight) * 100

    document.documentElement.style.setProperty('--mouse-x', `${x}%`)
    document.documentElement.style.setProperty('--mouse-y', `${y}%`)
  }

  document.addEventListener('mousemove', handleMouseMove)

  // 清理事件监听器
  return () => {
    document.removeEventListener('mousemove', handleMouseMove)
  }
})
</script>

<template>
  <div id="homePage">
    <div class="container">
      <!-- 网站标题和描述 -->
      <div class="hero-section">
        <h1 class="hero-title">AI 应用生成平台</h1>
        <p class="hero-description">一句话轻松创建网站应用</p>
      </div>

      <!-- 用户提示词输入框 -->
      <div class="input-section">
        <a-textarea
          v-model:value="userPrompt"
          placeholder="帮我创建个人博客网站"
          :rows="4"
          :maxlength="1000"
          class="prompt-input"
        />
        <a-button type="primary" size="large" @click="createApp" :loading="creating" class="submit-btn">
          <template #icon>
            <span>↑</span>
          </template>
        </a-button>
      </div>

      <!-- 模式选择 -->
      <div class="mode-section">
        <div class="mode-cards">
          <div
            class="mode-card"
            :class="{ active: genMode === 'normal' }"
            @click="genMode = 'normal'"
          >
            <div class="mode-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
              </svg>
            </div>
            <div class="mode-info">
              <div class="mode-name">快速模式</div>
              <div class="mode-desc">逐字流式输出，所见即所得</div>
            </div>
            <div class="mode-check" v-if="genMode === 'normal'">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
          </div>
          <div
            class="mode-card"
            :class="{ active: genMode === 'workflow' }"
            @click="genMode = 'workflow'"
          >
            <div class="mode-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                <path d="M2 17l10 5 10-5"/>
                <path d="M2 12l10 5 10-5"/>
              </svg>
            </div>
            <div class="mode-info">
              <div class="mode-name">工作流模式</div>
              <div class="mode-desc">AI 素材收集 + 质量审查，更完整</div>
            </div>
            <div class="mode-check" v-if="genMode === 'workflow'">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <!-- 快捷按钮 -->
      <div class="quick-actions">
        <a-button
          type="default"
          @click="
            setPrompt(
              '创建一个现代化的个人博客网站，包含文章列表、详情页、分类标签、搜索功能、评论系统和个人简介页面。采用简洁的设计风格，支持响应式布局，文章支持Markdown格式，首页展示最新文章和热门推荐。',
            )
          "
          >个人博客网站</a-button
        >
        <a-button
          type="default"
          @click="
            setPrompt(
              '设计一个专业的企业官网，包含公司介绍、产品服务展示、新闻资讯、联系我们等页面。采用商务风格的设计，包含轮播图、产品展示卡片、团队介绍、客户案例展示，支持多语言切换和在线客服功能。',
            )
          "
          >企业官网</a-button
        >
        <a-button
          type="default"
          @click="
            setPrompt(
              '构建一个功能完整的在线商城，包含商品展示、购物车、用户注册登录、订单管理、支付结算等功能。设计现代化的商品卡片布局，支持商品搜索筛选、用户评价、优惠券系统和会员积分功能。',
            )
          "
          >在线商城</a-button
        >
        <a-button
          type="default"
          @click="
            setPrompt(
              '制作一个精美的作品展示网站，适合设计师、摄影师、艺术家等创作者。包含作品画廊、项目详情页、个人简历、联系方式等模块。采用瀑布流或网格布局展示作品，支持图片放大预览和作品分类筛选。',
            )
          "
          >作品展示网站</a-button
        >
      </div>

      <!-- 我的作品 -->
      <div class="section">
        <h2 class="section-title">我的作品</h2>
        <div class="app-grid">
          <AppCard
            v-for="app in myApps"
            :key="app.id"
            :app="app"
            @view-chat="viewChat"
            @view-work="viewWork"
          />
        </div>
        <div class="pagination-wrapper">
          <a-pagination
            v-model:current="myAppsPage.current"
            v-model:page-size="myAppsPage.pageSize"
            :total="myAppsPage.total"
            :show-size-changer="false"
            :show-total="(total: number) => `共 ${total} 个应用`"
            @change="loadMyApps"
          />
        </div>
      </div>

      <!-- 精选案例 -->
      <div class="section">
        <h2 class="section-title">精选案例</h2>
        <div class="featured-grid">
          <AppCard
            v-for="app in featuredApps"
            :key="app.id"
            :app="app"
            :featured="true"
            @view-chat="viewChat"
            @view-work="viewWork"
          />
        </div>
        <div class="pagination-wrapper">
          <a-pagination
            v-model:current="featuredAppsPage.current"
            v-model:page-size="featuredAppsPage.pageSize"
            :total="featuredAppsPage.total"
            :show-size-changer="false"
            :show-total="(total: number) => `共 ${total} 个案例`"
            @change="loadFeaturedApps"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
#homePage {
  width: 100%;
  margin: 0;
  padding: 0;
  min-height: 100vh;
  background:
    radial-gradient(circle at 0% 0%, rgba(99, 102, 241, 0.12) 0%, transparent 50%),
    radial-gradient(circle at 100% 100%, rgba(13, 148, 136, 0.10) 0%, transparent 50%),
    radial-gradient(circle at 50% 50%, rgba(168, 85, 247, 0.06) 0%, transparent 70%),
    linear-gradient(180deg, #eef0fd 0%, #e4e7f7 20%, #dde1f2 45%, #d5daea 70%, #ced2e5 100%);
  position: relative;
  overflow: hidden;
}

#homePage::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    linear-gradient(rgba(99, 102, 241, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.06) 1px, transparent 1px),
    linear-gradient(rgba(13, 148, 136, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(13, 148, 136, 0.04) 1px, transparent 1px);
  background-size: 64px 64px, 64px 64px, 12px 12px, 12px 12px;
  mask-image: radial-gradient(ellipse 80% 60% at 50% 40%, black 40%, transparent 80%);
  -webkit-mask-image: radial-gradient(ellipse 80% 60% at 50% 40%, black 40%, transparent 80%);
  pointer-events: none;
  animation: gridFloat 20s ease-in-out infinite;
}

#homePage::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(
      700px circle at var(--mouse-x, 50%) var(--mouse-y, 50%),
      rgba(99, 102, 241, 0.15) 0%,
      rgba(13, 148, 136, 0.10) 35%,
      transparent 70%
    ),
    radial-gradient(ellipse 500px 350px at 20% 85%, rgba(168, 85, 247, 0.08) 0%, transparent 70%),
    radial-gradient(ellipse 400px 300px at 80% 15%, rgba(13, 148, 136, 0.08) 0%, transparent 70%);
  pointer-events: none;
  animation: lightPulse 8s ease-in-out infinite alternate;
}

@keyframes gridFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(3px, -2px) scale(1.005); }
  66% { transform: translate(-2px, 3px) scale(1.003); }
}

@keyframes lightPulse {
  0% { opacity: 0.5; }
  50% { opacity: 0.9; }
  100% { opacity: 0.6; }
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  position: relative;
  z-index: 2;
  width: 100%;
  box-sizing: border-box;
}

/* ===== Hero ===== */
.hero-section {
  text-align: center;
  padding: 80px 0 48px;
  margin-bottom: 28px;
  position: relative;
}

.hero-title {
  font-size: 56px;
  font-weight: 700;
  margin: 0 0 20px;
  line-height: 1.15;
  background: linear-gradient(135deg, var(--color-primary) 0%, #7c3aed 40%, var(--color-accent) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.03em;
  animation: titleShimmer 4s ease-in-out infinite;
  background-size: 200% 200%;
}

@keyframes titleShimmer {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.hero-description {
  font-size: 19px;
  margin: 0;
  color: var(--color-text-secondary);
  font-weight: 400;
  letter-spacing: -0.01em;
}

/* ===== Input section ===== */
.input-section {
  position: relative;
  margin: 0 auto 24px;
  max-width: 760px;
}

.prompt-input {
  border-radius: var(--radius-xl) !important;
  border: 1px solid var(--color-border) !important;
  font-size: 15px;
  padding: 18px 56px 18px 20px !important;
  background: var(--color-surface) !important;
  box-shadow: var(--shadow-lg);
  transition: all var(--transition-base);
  resize: none;
}

.prompt-input:focus {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 4px var(--color-primary-soft), var(--shadow-xl) !important;
  transform: translateY(-1px);
}

.submit-btn {
  position: absolute;
  bottom: 10px;
  right: 10px;
  border-radius: 50% !important;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ===== Mode selector cards ===== */
.mode-section {
  margin: 0 auto 24px;
  max-width: 760px;
}

.mode-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.mode-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 14px;
  border: 2px solid var(--color-border);
  background: var(--color-surface);
  cursor: pointer;
  transition: all 0.25s ease;
  user-select: none;
}

.mode-card:hover {
  border-color: var(--color-primary);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.15);
  transform: translateY(-1px);
}

.mode-card.active {
  border-color: var(--color-primary);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.06) 0%, rgba(139, 92, 246, 0.04) 100%);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
}

.mode-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  transition: all 0.25s ease;
}

.mode-icon svg {
  width: 22px;
  height: 22px;
}

.mode-card.active .mode-icon {
  color: var(--color-primary);
  background: rgba(99, 102, 241, 0.1);
}

.mode-info {
  flex: 1;
  min-width: 0;
}

.mode-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 2px;
}

.mode-desc {
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.4;
}

.mode-check {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.mode-check svg {
  width: 14px;
  height: 14px;
}

/* ===== Quick actions ===== */
.quick-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 56px;
  flex-wrap: wrap;
}

.quick-actions .ant-btn {
  border-radius: 9999px;
  padding: 8px 22px;
  height: auto;
  font-size: 14px;
  font-weight: 500;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-base);
}

.quick-actions .ant-btn:hover {
  background: var(--color-surface);
  border-color: var(--color-primary);
  color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg), 0 0 0 4px var(--color-primary-soft);
}

/* ===== Section ===== */
.section {
  margin-bottom: 56px;
}

.section-title {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 28px;
  color: var(--color-text);
  letter-spacing: -0.02em;
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.section-title::after {
  content: '';
  display: inline-block;
  width: 40px;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
  border-radius: 3px;
  flex-shrink: 0;
}

/* ===== Grids ===== */
.app-grid,
.featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

/* ===== Pagination ===== */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .hero-title { font-size: 32px; }
  .hero-description { font-size: 16px; }
  .app-grid, .featured-grid { grid-template-columns: 1fr; }
  .hero-section { padding: 48px 0 32px; }
  .quick-actions { gap: 8px; }
  .quick-actions .ant-btn { padding: 6px 16px; font-size: 13px; }
}
</style>
