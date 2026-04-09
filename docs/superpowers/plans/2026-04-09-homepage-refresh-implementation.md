# Homepage Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refresh the `bilibili_web` homepage into a youthfully energetic, video-first landing page that only uses existing backend APIs and routes.

**Architecture:** Keep the current Vue Router and API flow intact, then rebuild the homepage around a real featured video from `/videos`, a denser ranking panel from `/videos/rank`, and a stronger shared visual system. Validation should combine focused component tests for real route/API behavior with a production build check so the redesign stays beautiful without inventing unsupported features.

**Tech Stack:** Vue 3, TypeScript, Vite, Vue Router, Vitest, Vue Test Utils, CSS

---

### Task 1: Add Homepage Test Harness And Real-Behavior Spec

**Files:**
- Modify: `bilibili_web/package.json`
- Create: `bilibili_web/vitest.config.ts`
- Create: `bilibili_web/src/views/__tests__/HomeView.spec.ts`

- [ ] **Step 1: Add test tooling and write the failing homepage spec**

Update [package.json](/home/huangnv/biliibli/bilibili_web/package.json) to add a test script and the required dev dependencies:

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview",
    "test": "vitest run"
  },
  "devDependencies": {
    "@types/node": "^24.12.0",
    "@vitejs/plugin-vue": "^6.0.5",
    "@vue/test-utils": "^2.4.6",
    "@vue/tsconfig": "^0.9.0",
    "jsdom": "^26.1.0",
    "typescript": "~5.9.3",
    "vite": "^8.0.1",
    "vitest": "^3.2.4",
    "vue-tsc": "^3.2.5"
  }
}
```

Create [vitest.config.ts](/home/huangnv/biliibli/bilibili_web/vitest.config.ts):

```ts
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
  },
})
```

Create [HomeView.spec.ts](/home/huangnv/biliibli/bilibili_web/src/views/__tests__/HomeView.spec.ts):

```ts
import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import HomeView from '../HomeView.vue'
import { api } from '../../lib/api'

vi.mock('../../lib/api', () => ({
  api: {
    get: vi.fn(),
  },
}))

const mockedGet = vi.mocked(api.get)

describe('HomeView', () => {
  beforeEach(() => {
    mockedGet.mockReset()
  })

  it('renders a featured hero from the first real video and a real ranking list', async () => {
    mockedGet
      .mockResolvedValueOnce({
        records: [
          {
            id: '101',
            authorUid: '7',
            title: '春日第一条视频',
            coverUrl: 'https://example.com/cover-1.jpg',
            viewCount: 1200,
            duration: 120,
            createTime: '2026-04-09 09:00:00',
            nickname: '小明',
          },
          {
            id: '102',
            authorUid: '8',
            title: '第二条公开视频',
            coverUrl: 'https://example.com/cover-2.jpg',
            viewCount: 800,
            duration: 95,
            createTime: '2026-04-09 10:00:00',
            nickname: '小红',
          },
        ],
      })
      .mockResolvedValueOnce({
        records: [
          {
            rank: 1,
            score: 99,
            id: '301',
            authorUid: '15',
            title: '热榜第一',
            coverUrl: 'https://example.com/rank-1.jpg',
            viewCount: 9900,
            duration: 66,
            createTime: '2026-04-08 12:00:00',
            nickname: '榜单作者',
          },
        ],
      })

    const wrapper = mount(HomeView, {
      global: {
        stubs: {
          RouterLink: RouterLinkStub,
          VideoCard: true,
        },
      },
    })

    await flushPromises()

    expect(wrapper.find('.featured-hero').exists()).toBe(true)
    expect(wrapper.text()).toContain('春日第一条视频')
    expect(wrapper.find('[data-testid="featured-watch-link"]').attributes('to')).toBe('/video/101')
    expect(wrapper.findAll('.hero-rank-item')).toHaveLength(1)
    expect(wrapper.text()).toContain('热榜第一')
    expect(mockedGet).toHaveBeenNthCalledWith(1, '/videos', { pageNo: 1, pageSize: 12 })
    expect(mockedGet).toHaveBeenNthCalledWith(2, '/videos/rank', { pageNo: 1, pageSize: 8 })
  })

  it('shows an honest empty state when videos are empty', async () => {
    mockedGet
      .mockResolvedValueOnce({ records: [] })
      .mockResolvedValueOnce({ records: [] })

    const wrapper = mount(HomeView, {
      global: {
        stubs: {
          RouterLink: RouterLinkStub,
          VideoCard: true,
        },
      },
    })

    await flushPromises()

    expect(wrapper.find('.featured-hero').exists()).toBe(false)
    expect(wrapper.text()).toContain('当前还没有可展示的视频')
  })
})
```

- [ ] **Step 2: Install the new test dependencies and refresh the lockfile**

Run: `npm install`

Expected: `package-lock.json` updates with `vitest`, `jsdom`, and `@vue/test-utils`.

- [ ] **Step 3: Run the homepage spec to verify it fails before implementation**

Run: `npm run test -- src/views/__tests__/HomeView.spec.ts`

Expected: FAIL because the current [HomeView.vue](/home/huangnv/biliibli/bilibili_web/src/views/HomeView.vue) does not yet render `.featured-hero`, `[data-testid="featured-watch-link"]`, or `.hero-rank-item`.

- [ ] **Step 4: Commit the testing scaffold**

```bash
git add package.json package-lock.json vitest.config.ts src/views/__tests__/HomeView.spec.ts
git commit -m "test: add homepage view coverage"
```

### Task 2: Rebuild HomeView Around Real Featured Content And Ranking Data

**Files:**
- Modify: `bilibili_web/src/views/HomeView.vue`
- Test: `bilibili_web/src/views/__tests__/HomeView.spec.ts`

- [ ] **Step 1: Implement the real-data homepage hero and ranking layout**

Update [HomeView.vue](/home/huangnv/biliibli/bilibili_web/src/views/HomeView.vue) so the first `/videos` record becomes the featured hero, remaining records fill the lower grid, and `/videos/rank` renders as a custom ranking list instead of a generic card stack.

Use this script block shape:

```ts
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import VideoCard from '../components/VideoCard.vue'
import { api } from '../lib/api'
import { formatCount, formatDate } from '../lib/format'
import type { PageVO, VideoRankVO, VideoVO } from '../types'

const videos = ref<VideoVO[]>([])
const ranks = ref<VideoRankVO[]>([])
const loading = ref(true)
const error = ref('')

const featuredVideo = computed(() => videos.value[0] ?? null)
const feedVideos = computed(() => videos.value.slice(1))
const featuredSummary = computed(() => {
  if (!featuredVideo.value) return ''
  return `${featuredVideo.value.nickname} 刚刚发布的公开视频，已经可以直接进入详情页观看。`
})

async function loadHome() {
  loading.value = true
  error.value = ''
  try {
    const [videoPage, rankPage] = await Promise.all([
      api.get<PageVO<VideoVO>>('/videos', { pageNo: 1, pageSize: 12 }),
      api.get<PageVO<VideoRankVO>>('/videos/rank', { pageNo: 1, pageSize: 8 }),
    ])
    videos.value = videoPage.records
    ranks.value = rankPage.records
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载首页失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadHome)
</script>
```

Use this template structure for the top half:

```vue
<section class="home-page">
  <section class="hero-grid">
    <article v-if="featuredVideo" class="featured-hero panel">
      <div class="featured-copy">
        <span class="tag-chip">青春活泼 · 看视频优先</span>
        <h1>{{ featuredVideo.title }}</h1>
        <p>{{ featuredSummary }}</p>
        <div class="status-line featured-stats">
          <span>{{ formatCount(featuredVideo.viewCount) }} 播放</span>
          <span>{{ formatDate(featuredVideo.createTime) }}</span>
          <span>UP 主 {{ featuredVideo.nickname }}</span>
        </div>
        <div class="hero-actions">
          <RouterLink
            class="primary-button"
            :to="`/video/${featuredVideo.id}`"
            data-testid="featured-watch-link"
          >
            立即观看
          </RouterLink>
          <RouterLink class="secondary-button" :to="{ name: 'search' }">去搜索</RouterLink>
          <RouterLink class="secondary-button" to="/studio">创作中心</RouterLink>
        </div>
      </div>

      <RouterLink class="featured-cover" :to="`/video/${featuredVideo.id}`">
        <img :src="featuredVideo.coverUrl" :alt="featuredVideo.title" />
      </RouterLink>
    </article>

    <article v-else-if="!loading && !error" class="panel empty-state">
      当前还没有可展示的视频。
    </article>

    <aside class="hero-rank panel">
      <div class="section-title">
        <h2>今日热榜</h2>
        <RouterLink class="text-button" :to="{ name: 'search' }">更多内容</RouterLink>
      </div>
      <div v-if="loading" class="loading-state">正在加载排行榜…</div>
      <div v-else-if="error" class="error-state">{{ error }}</div>
      <div v-else-if="ranks.length" class="rank-list">
        <RouterLink
          v-for="item in ranks"
          :key="item.id"
          class="hero-rank-item"
          :to="`/video/${item.id}`"
        >
          <span class="hero-rank-number">0{{ item.rank }}</span>
          <img :src="item.coverUrl" :alt="item.title" />
          <div class="hero-rank-copy">
            <strong>{{ item.title }}</strong>
            <span>{{ item.nickname }}</span>
            <span>{{ formatCount(item.viewCount) }} 播放</span>
          </div>
        </RouterLink>
      </div>
      <div v-else class="empty-state">排行榜暂时还没有内容。</div>
    </aside>
  </section>
</section>
```

Use the lower content section shape:

```vue
<section class="content-section">
  <div class="section-title">
    <div>
      <h2>最新公开视频</h2>
      <p class="muted">全部来自后端真实返回的数据，不添加假分类和假推荐位。</p>
    </div>
    <RouterLink class="text-button" :to="{ name: 'search' }">去搜索</RouterLink>
  </div>

  <div v-if="loading" class="loading-state">正在加载视频…</div>
  <div v-else-if="error" class="error-state">{{ error }}</div>
  <div v-else-if="feedVideos.length" class="video-grid">
    <VideoCard v-for="video in feedVideos" :key="video.id" :video="video" />
  </div>
  <div v-else class="empty-state">首页主推荐位之外还没有更多公开视频。</div>
</section>
```

- [ ] **Step 2: Add scoped styles that match the new layout without introducing new behavior**

Replace the current [HomeView.vue](/home/huangnv/biliibli/bilibili_web/src/views/HomeView.vue) scoped CSS with a layout that supports the hero, ranking list, and editorial grid:

```css
.home-page {
  display: grid;
  gap: 30px;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(300px, 0.88fr);
  gap: 24px;
}

.featured-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.92fr);
  gap: 22px;
  padding: 26px;
  overflow: hidden;
}

.featured-copy h1 {
  margin: 18px 0 14px;
  font-family: var(--font-heading);
  font-size: clamp(34px, 5vw, 60px);
  line-height: 1;
}

.featured-cover {
  min-height: 320px;
  border-radius: 24px;
  overflow: hidden;
}

.featured-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-rank {
  padding: 24px;
}

.rank-list {
  display: grid;
  gap: 12px;
}

.hero-rank-item {
  display: grid;
  grid-template-columns: 48px 92px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 10px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
}

.hero-rank-item img {
  width: 92px;
  height: 64px;
  border-radius: 14px;
  object-fit: cover;
}

.hero-rank-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.hero-rank-copy strong {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

@media (max-width: 1200px) {
  .featured-hero,
  .hero-grid,
  .video-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .featured-hero {
    padding: 20px;
  }

  .video-grid {
    grid-template-columns: 1fr;
  }
}
```

- [ ] **Step 3: Run the homepage spec again and make sure it passes**

Run: `npm run test -- src/views/__tests__/HomeView.spec.ts`

Expected: PASS with both homepage tests green.

- [ ] **Step 4: Commit the homepage structure change**

```bash
git add src/views/HomeView.vue src/views/__tests__/HomeView.spec.ts
git commit -m "feat: redesign homepage hero and ranking layout"
```

### Task 3: Refresh Shared Visuals, Header Presentation, And Video Cards

**Files:**
- Modify: `bilibili_web/src/components/SiteHeader.vue`
- Modify: `bilibili_web/src/components/VideoCard.vue`
- Modify: `bilibili_web/src/style.css`

- [ ] **Step 1: Write a small failing header behavior spec**

Create [SiteHeader.spec.ts](/home/huangnv/biliibli/bilibili_web/src/components/__tests__/SiteHeader.spec.ts):

```ts
import { mount, RouterLinkStub } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { describe, expect, it, beforeEach } from 'vitest'
import SiteHeader from '../SiteHeader.vue'
import { authState } from '../../lib/auth'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/', name: 'home', component: { template: '<div />' } },
    { path: '/search', name: 'search', component: { template: '<div />' } },
    { path: '/auth', name: 'auth', component: { template: '<div />' } },
    { path: '/studio', name: 'studio', component: { template: '<div />' } },
  ],
})

describe('SiteHeader', () => {
  beforeEach(async () => {
    authState.token = ''
    authState.uid = ''
    authState.username = ''
    authState.profile = null
    await router.push('/')
    await router.isReady()
  })

  it('keeps the real search route and guest login entry', async () => {
    const wrapper = mount(SiteHeader, {
      global: {
        plugins: [router],
        stubs: {
          RouterLink: RouterLinkStub,
        },
      },
    })

    await wrapper.get('input[type="search"]').setValue('动画')
    await wrapper.get('form').trigger('submit.prevent')

    expect(router.currentRoute.value.name).toBe('search')
    expect(router.currentRoute.value.query.q).toBe('动画')
    expect(wrapper.text()).toContain('登录 / 注册')
  })
})
```

- [ ] **Step 2: Run the new header spec to capture the current failure**

Run: `npm run test -- src/components/__tests__/SiteHeader.spec.ts`

Expected: FAIL until the new header structure and updated test file paths are in place.

- [ ] **Step 3: Restyle the shared shell without changing business behavior**

Update [style.css](/home/huangnv/biliibli/bilibili_web/src/style.css) to strengthen the visual system:

```css
:root {
  --bg: #f4f7ff;
  --bg-panel: rgba(255, 255, 255, 0.84);
  --bg-panel-strong: rgba(255, 255, 255, 0.94);
  --line: rgba(123, 139, 169, 0.16);
  --line-strong: rgba(73, 93, 131, 0.18);
  --shadow: 0 22px 60px rgba(42, 56, 92, 0.12);
  --shadow-soft: 0 16px 34px rgba(53, 77, 122, 0.12);
  --pink-soft: #ffe3ef;
  --blue-soft: #dff4ff;
  --hero-gradient: linear-gradient(135deg, rgba(255, 226, 239, 0.95), rgba(225, 244, 255, 0.95));
}

body {
  background:
    radial-gradient(circle at 0% 0%, rgba(255, 153, 188, 0.2), transparent 28%),
    radial-gradient(circle at 100% 0%, rgba(74, 192, 255, 0.18), transparent 24%),
    linear-gradient(180deg, #fcfdff 0%, #f4f7ff 42%, #eef4ff 100%);
}

.panel {
  background: var(--bg-panel);
  border: 1px solid var(--line);
  box-shadow: var(--shadow);
  backdrop-filter: blur(24px);
}

.primary-button {
  box-shadow: 0 14px 30px rgba(251, 114, 153, 0.3);
}

.secondary-button {
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--line-strong);
}
```

Update [SiteHeader.vue](/home/huangnv/biliibli/bilibili_web/src/components/SiteHeader.vue) to keep all the same links and auth logic, but use a more expressive presentation:

```vue
<header class="site-header panel">
  <RouterLink class="brand" to="/">
    <div class="brand-mark">bili</div>
    <div class="brand-copy">
      <strong>BiliBili Clone</strong>
      <span>青春内容站 · Spring Boot + Vue</span>
    </div>
  </RouterLink>

  <form class="header-search" @submit.prevent="submitSearch">
    <input v-model="searchKeyword" type="search" placeholder="搜索视频、作者、关键字" />
    <button class="primary-button" type="submit">开搜</button>
  </form>

  <nav class="header-nav">
    <RouterLink to="/">首页</RouterLink>
    <RouterLink :to="{ name: 'search' }">搜索</RouterLink>
    <RouterLink v-if="authState.token" :to="{ name: 'messages' }">私信</RouterLink>
    <RouterLink v-if="authState.token" to="/studio">创作中心</RouterLink>
    <RouterLink v-if="authState.token" to="/studio">资料设置</RouterLink>
  </nav>

  <div class="header-user">
    <template v-if="authState.token && authState.uid">
      <RouterLink class="user-chip" :to="`/user/${authState.uid}`">
        <img v-if="currentAvatar" :src="currentAvatar" alt="avatar" />
        <span v-else class="fallback-avatar">{{ currentNickname.slice(0, 1) }}</span>
        <span>{{ currentNickname }}</span>
      </RouterLink>
      <button class="secondary-button" type="button" @click="handleLogout">退出</button>
    </template>
    <template v-else>
      <RouterLink class="primary-button" to="/auth">登录 / 注册</RouterLink>
    </template>
  </div>
</header>
```

Use supporting CSS blocks like:

```css
.site-header {
  position: sticky;
  top: 16px;
  z-index: 20;
  grid-template-columns: 240px minmax(280px, 1fr) auto auto;
  padding: 18px 22px;
  background: var(--bg-panel-strong);
}

.brand-mark {
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7), var(--shadow-soft);
}

.header-search input {
  background: rgba(255, 255, 255, 0.92);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.header-nav {
  flex-wrap: wrap;
}
```

Update [VideoCard.vue](/home/huangnv/biliibli/bilibili_web/src/components/VideoCard.vue) so the homepage feed feels more premium while still using only `VideoVO` and `VideoRankVO` fields:

```css
.video-card {
  position: relative;
  overflow: hidden;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.video-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 36px rgba(52, 80, 130, 0.16);
}

.video-cover::after {
  content: "";
  position: absolute;
  inset: auto 0 0;
  height: 48%;
  background: linear-gradient(180deg, transparent, rgba(18, 28, 45, 0.46));
}

.video-body {
  display: grid;
  gap: 8px;
  padding: 16px;
}

.video-author {
  font-size: 14px;
}
```

- [ ] **Step 4: Run tests and the production build**

Run:

```bash
npm run test -- src/views/__tests__/HomeView.spec.ts src/components/__tests__/SiteHeader.spec.ts
npm run build
```

Expected:

- Vitest reports PASS for both spec files
- Vite build completes successfully without TypeScript or SFC compile errors

- [ ] **Step 5: Commit the shared visual refresh**

```bash
git add src/style.css src/components/SiteHeader.vue src/components/VideoCard.vue src/components/__tests__/SiteHeader.spec.ts
git commit -m "feat: polish homepage visual system"
```

## Self-Review

### Spec Coverage

- Featured hero from real `/videos` data: covered in Task 2
- Ranking panel from real `/videos/rank` data: covered in Task 2
- No fake homepage features or new endpoints: enforced in Task 2 implementation notes and test expectations
- Youthfully energetic shared styling: covered in Task 3
- Refined header and stronger cards: covered in Task 3
- Build validation: covered in Task 3

### Placeholder Scan

- No `TODO` or `TBD` markers remain
- Every task includes exact file paths
- Every validation step includes an exact command

### Type Consistency

- Homepage hero and grid stay on existing `VideoVO`
- Ranking module stays on existing `VideoRankVO`
- No new backend fields are introduced

