# Studio Settings Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the current combined authenticated creator page into `/studio` for publishing and `/settings` for profile editing, while keeping all existing backend APIs and auth behavior unchanged.

**Architecture:** Extract the profile-editing responsibilities from `StudioView.vue` into a new `SettingsView.vue`, add a dedicated authenticated `/settings` route, and update top-level navigation so each entry points to the correct page. Validation should focus on route protection, navigation accuracy, and preserving existing profile update and video publish flows.

**Tech Stack:** Vue 3, TypeScript, Vue Router, Vite, Vitest, Vue Test Utils, CSS

---

### Task 1: Add Route And Navigation Coverage For The Split

**Files:**
- Create: `bilibili_web/src/router.spec.ts`
- Modify: `bilibili_web/src/components/__tests__/SiteHeader.spec.ts`

- [ ] **Step 1: Write the failing route split test**

Create [router.spec.ts](/home/huangnv/.config/superpowers/worktrees/biliibli/homepage-refresh/bilibili_web/src/router.spec.ts):

```ts
import { beforeEach, describe, expect, it } from 'vitest'
import router from './router'
import { authState } from './lib/auth'

describe('router studio/settings split', () => {
  beforeEach(async () => {
    authState.token = ''
    authState.uid = null
    authState.username = ''
    authState.profile = null
    await router.push('/')
    await router.isReady()
  })

  it('redirects guests from both studio and settings to auth', async () => {
    await router.push('/studio')
    expect(router.currentRoute.value.name).toBe('auth')
    expect(router.currentRoute.value.query.redirect).toBe('/studio')

    await router.push('/settings')
    expect(router.currentRoute.value.name).toBe('auth')
    expect(router.currentRoute.value.query.redirect).toBe('/settings')
  })
})
```

Update [SiteHeader.spec.ts](/home/huangnv/.config/superpowers/worktrees/biliibli/homepage-refresh/bilibili_web/src/components/__tests__/SiteHeader.spec.ts) so it checks distinct header links:

```ts
it('uses separate navigation targets for studio and settings', async () => {
  authState.token = 'token'
  authState.uid = '7'
  authState.username = 'alice'

  const wrapper = mount(SiteHeader, {
    global: {
      plugins: [router],
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  })

  const links = wrapper.findAllComponents(RouterLinkStub)
  const studioLink = links.find((component) => component.text() === '创作中心')
  const settingsLink = links.find((component) => component.text() === '资料设置')

  expect(studioLink?.props('to')).toBe('/studio')
  expect(settingsLink?.props('to')).toEqual({ name: 'settings' })
})
```

- [ ] **Step 2: Run the new split tests to verify they fail**

Run:

```bash
npm run test -- src/router.spec.ts src/components/__tests__/SiteHeader.spec.ts
```

Expected:

- `router.spec.ts` fails because `/settings` does not exist yet
- `SiteHeader.spec.ts` fails because `资料设置` still points to `/studio`

- [ ] **Step 3: Commit the failing split tests**

```bash
git add bilibili_web/src/router.spec.ts bilibili_web/src/components/__tests__/SiteHeader.spec.ts
git commit -m "test: cover studio settings split"
```

### Task 2: Add The New `/settings` Route And Extract The Profile Editor

**Files:**
- Modify: `bilibili_web/src/router.ts`
- Create: `bilibili_web/src/views/SettingsView.vue`
- Create: `bilibili_web/src/views/__tests__/SettingsView.spec.ts`

- [ ] **Step 1: Write the failing settings view test**

Create [SettingsView.spec.ts](/home/huangnv/.config/superpowers/worktrees/biliibli/homepage-refresh/bilibili_web/src/views/__tests__/SettingsView.spec.ts):

```ts
import { mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SettingsView from '../SettingsView.vue'
import { authState, refreshCurrentUser } from '../../lib/auth'
import { api } from '../../lib/api'

vi.mock('../../lib/api', () => ({
  api: {
    post: vi.fn(),
    put: vi.fn(),
  },
}))

vi.mock('../../lib/auth', async () => {
  const actual = await vi.importActual<typeof import('../../lib/auth')>('../../lib/auth')
  return {
    ...actual,
    refreshCurrentUser: vi.fn(),
  }
})

describe('SettingsView', () => {
  beforeEach(() => {
    authState.token = 'token'
    authState.uid = '7'
    authState.username = 'alice'
    authState.profile = {
      uid: '7',
      nickname: '小桃',
      avatar: '',
      sign: '热爱分享日常',
      followerCount: 12,
      followingCount: 8,
    }
  })

  it('renders profile editing UI without upload studio controls', () => {
    const wrapper = mount(SettingsView, {
      global: {
        stubs: {
          RouterLink: RouterLinkStub,
        },
      },
    })

    expect(wrapper.text()).toContain('资料设置')
    expect(wrapper.text()).toContain('昵称')
    expect(wrapper.text()).toContain('签名')
    expect(wrapper.text()).toContain('修改头像')
    expect(wrapper.text()).not.toContain('上传视频')
    expect(wrapper.text()).not.toContain('上传并发布')
  })
})
```

- [ ] **Step 2: Add the authenticated settings route**

Update [router.ts](/home/huangnv/.config/superpowers/worktrees/biliibli/homepage-refresh/bilibili_web/src/router.ts) by inserting a new route after `/studio`:

```ts
  {
    path: '/settings',
    name: 'settings',
    component: () => import('./views/SettingsView.vue'),
    meta: { requiresAuth: true },
  },
```

- [ ] **Step 3: Implement `SettingsView.vue` with the extracted profile behavior**

Create [SettingsView.vue](/home/huangnv/.config/superpowers/worktrees/biliibli/homepage-refresh/bilibili_web/src/views/SettingsView.vue) using the profile-related logic currently living in `StudioView.vue`.

Use this script shape:

```ts
<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { authState, refreshCurrentUser } from '../lib/auth'
import { api } from '../lib/api'

const profileForm = reactive({
  nickname: '',
  sign: '',
})

const profileMessage = ref('')
const avatarMessage = ref('')

const currentAvatar = computed(() => authState.profile?.avatar || '')
const currentNickname = computed(() => authState.profile?.nickname || authState.username || '我')

function syncProfileForm() {
  profileForm.nickname = authState.profile?.nickname || ''
  profileForm.sign = authState.profile?.sign || ''
}

syncProfileForm()

async function saveProfile() {
  profileMessage.value = ''
  try {
    await api.put<void>('/me/profile', profileForm)
    await refreshCurrentUser()
    syncProfileForm()
    profileMessage.value = '资料已更新'
  } catch (err) {
    profileMessage.value = err instanceof Error ? err.message : '资料更新失败'
  }
}

async function uploadAvatar(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const formData = new FormData()
  formData.append('file', file)

  try {
    await api.post<string>('/me/uploads/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    await refreshCurrentUser()
    syncProfileForm()
    avatarMessage.value = '头像上传成功'
  } catch (err) {
    avatarMessage.value = err instanceof Error ? err.message : '头像上传失败'
  } finally {
    input.value = ''
  }
}
</script>
```

Use this template shape:

```vue
<template>
  <section class="settings-layout">
    <article class="panel settings-hero">
      <span class="tag-chip">资料设置</span>
      <h1>把你的主页整理成你喜欢的样子。</h1>
      <p class="muted">头像、昵称和签名都会在社区里陪你一起出现。</p>
    </article>

    <article class="panel settings-card">
      <div class="settings-profile">
        <img v-if="currentAvatar" class="profile-avatar" :src="currentAvatar" alt="avatar" />
        <div v-else class="profile-avatar fallback-avatar">{{ currentNickname.slice(0, 1) }}</div>
        <div class="settings-profile-actions">
          <label class="primary-button upload-picker">
            修改头像
            <input type="file" accept="image/*" @change="uploadAvatar" />
          </label>
          <span class="muted">{{ avatarMessage || '换一个更像你的头像吧。' }}</span>
        </div>
      </div>

      <div class="field-grid">
        <div class="field-group">
          <label for="settings-nickname">昵称</label>
          <input id="settings-nickname" v-model.trim="profileForm.nickname" />
        </div>
        <div class="field-group">
          <label for="settings-sign">签名</label>
          <textarea id="settings-sign" v-model.trim="profileForm.sign" />
        </div>
        <div class="status-line">
          <button class="primary-button" type="button" @click="saveProfile">保存资料</button>
          <span class="muted">{{ profileMessage }}</span>
        </div>
      </div>
    </article>
  </section>
</template>
```

- [ ] **Step 4: Run the settings-specific tests and make sure they pass**

Run:

```bash
npm run test -- src/router.spec.ts src/views/__tests__/SettingsView.spec.ts
```

Expected:

- both tests pass
- `/settings` is now route-protected
- `SettingsView` contains only profile editing UI

- [ ] **Step 5: Commit the route and settings extraction**

```bash
git add bilibili_web/src/router.ts bilibili_web/src/views/SettingsView.vue bilibili_web/src/views/__tests__/SettingsView.spec.ts
git commit -m "feat: add dedicated settings page"
```

### Task 3: Shrink `StudioView` To Upload-Only And Update Navigation

**Files:**
- Modify: `bilibili_web/src/views/StudioView.vue`
- Modify: `bilibili_web/src/components/SiteHeader.vue`
- Modify: `bilibili_web/src/views/AuthView.vue`
- Modify: `bilibili_web/src/views/__tests__/AuthView.spec.ts`
- Modify: `bilibili_web/src/components/__tests__/SiteHeader.spec.ts`

- [ ] **Step 1: Write the failing studio-only test**

Append this test to [AuthView.spec.ts](/home/huangnv/.config/superpowers/worktrees/biliibli/homepage-refresh/bilibili_web/src/views/__tests__/AuthView.spec.ts):

```ts
it('keeps creative and profile destinations distinct', () => {
  authState.token = 'token'
  authState.uid = '7'
  authState.username = 'alice'
  authState.profile = {
    uid: '7',
    nickname: '小桃',
    avatar: '',
    sign: '热爱分享日常',
    followerCount: 12,
    followingCount: 8,
  }

  const wrapper = mount(AuthView, {
    global: {
      plugins: [router],
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  })

  const links = wrapper.findAllComponents(RouterLinkStub)
  const homeLink = links.find((component) => component.text() === '去首页看看')
  const studioLink = links.find((component) => component.text() === '进入创作中心')

  expect(homeLink?.props('to')).toBe('/')
  expect(studioLink?.props('to')).toBe('/studio')
})
```

Add a studio-specific view test in [SettingsView.spec.ts](/home/huangnv/.config/superpowers/worktrees/biliibli/homepage-refresh/bilibili_web/src/views/__tests__/SettingsView.spec.ts) or a new [StudioView.spec.ts](/home/huangnv/.config/superpowers/worktrees/biliibli/homepage-refresh/bilibili_web/src/views/__tests__/StudioView.spec.ts):

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StudioView from '../StudioView.vue'

describe('StudioView', () => {
  it('renders upload controls without profile editing fields', () => {
    const wrapper = mount(StudioView)

    expect(wrapper.text()).toContain('上传视频')
    expect(wrapper.text()).toContain('上传并发布')
    expect(wrapper.text()).not.toContain('资料设置')
    expect(wrapper.text()).not.toContain('修改头像')
    expect(wrapper.find('#nickname').exists()).toBe(false)
  })
})
```

- [ ] **Step 2: Remove profile editing from `StudioView.vue`**

Update [StudioView.vue](/home/huangnv/.config/superpowers/worktrees/biliibli/homepage-refresh/bilibili_web/src/views/StudioView.vue):

- remove `profileForm`, `profileMessage`, `avatarMessage`, `currentAvatar`, `syncProfileForm`, `saveProfile`, and avatar upload handling
- keep `coverMessage`, `uploadMessage`, `coverUrl`, `selectedVideo`, `selectedVideoDuration`, `uploadProgress`, `uploadBusy`, `currentUploadId`, and publish flow

The remaining top section should become an upload-only intro block like:

```vue
<section class="studio-layout">
  <article class="panel studio-hero">
    <span class="tag-chip">创作中心</span>
    <h1>把这一次想分享的内容认真发出去。</h1>
    <p class="muted">在这里上传视频、补上封面和简介，然后把它发布给大家看。</p>
  </article>

  <article class="panel studio-block">
    <div class="section-title">
      <h2>上传视频</h2>
      <RouterLink class="text-button" :to="{ name: 'settings' }">去资料设置</RouterLink>
    </div>
```

Keep the rest of the upload UI intact, including cover upload and multipart publish.

- [ ] **Step 3: Update header and auth entry points**

Update [SiteHeader.vue](/home/huangnv/.config/superpowers/worktrees/biliibli/homepage-refresh/bilibili_web/src/components/SiteHeader.vue):

```vue
<RouterLink v-if="authState.token" to="/studio">创作中心</RouterLink>
<RouterLink v-if="authState.token" :to="{ name: 'settings' }">资料设置</RouterLink>
```

Keep `handleLogout()` safe for both protected pages:

```ts
function handleLogout() {
  logout()
  if (route.name === 'studio' || route.name === 'settings') {
    router.push('/')
  }
}
```

Update [AuthView.vue](/home/huangnv/.config/superpowers/worktrees/biliibli/homepage-refresh/bilibili_web/src/views/AuthView.vue) only if any profile-editing CTA still incorrectly points to `/studio`. Keep creative CTA on `/studio` and do not add profile CTA unless it already fits the page.

- [ ] **Step 4: Run the full split verification**

Run:

```bash
npm run test -- src/router.spec.ts src/views/__tests__/SettingsView.spec.ts src/views/__tests__/AuthView.spec.ts src/components/__tests__/SiteHeader.spec.ts
npm run build
```

Expected:

- all tests pass
- Vite build succeeds
- route guard covers both `/studio` and `/settings`
- studio and settings responsibilities are visibly separated

- [ ] **Step 5: Commit the page split**

```bash
git add bilibili_web/src/views/StudioView.vue bilibili_web/src/components/SiteHeader.vue bilibili_web/src/views/AuthView.vue bilibili_web/src/views/__tests__/AuthView.spec.ts bilibili_web/src/components/__tests__/SiteHeader.spec.ts bilibili_web/src/router.spec.ts bilibili_web/src/views/__tests__/StudioView.spec.ts
git commit -m "feat: split studio and settings pages"
```

## Self-Review

### Spec Coverage

- separate `/studio` and `/settings`: covered in Tasks 2 and 3
- keep `/studio` upload-only: covered in Task 3
- move profile editor to `/settings`: covered in Task 2
- fix header entry points: covered in Tasks 1 and 3
- preserve auth redirect behavior: covered in Tasks 1 and 2
- preserve existing backend APIs: reflected in Task 2 extraction and Task 3 upload retention

### Placeholder Scan

- No `TODO` or `TBD` markers remain
- Every task includes exact file paths
- Every verification step includes exact commands

### Type Consistency

- `SettingsView` uses existing `authState`, `refreshCurrentUser`, and `api`
- `StudioView` keeps the existing upload types and multipart flow
- route name `settings` is used consistently across router and navigation
