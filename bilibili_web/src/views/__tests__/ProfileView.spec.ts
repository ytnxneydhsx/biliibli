import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import ProfileView from '../ProfileView.vue'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    {
      path: '/profile',
      component: ProfileView,
      children: [
        { path: '', name: 'profile', component: { template: '<div>资料设置内容</div>' } },
        { path: 'privacy', name: 'profile-privacy', component: { template: '<div>隐私设置内容</div>' } },
      ],
    },
  ],
})

describe('ProfileView', () => {
  beforeEach(async () => {
    await router.push('/profile')
    await router.isReady()
  })

  it('shows a left menu for profile and privacy sections', () => {
    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router],
      },
    })

    expect(wrapper.text()).toContain('个人')
    expect(wrapper.text()).toContain('资料设置')
    expect(wrapper.text()).toContain('隐私设置')
    expect(wrapper.text()).toContain('资料设置内容')
  })
})
