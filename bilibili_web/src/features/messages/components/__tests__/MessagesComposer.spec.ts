import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MessagesComposer from '../MessagesComposer.vue'

describe('MessagesComposer', () => {
  it('keeps group chat composer controls enabled when composerEnabled is true', () => {
    const wrapper = mount(MessagesComposer, {
      props: {
        activePeerUid: '',
        composerEnabled: true,
        draftImages: [],
        hasUploadingImages: false,
        hasFailedImages: false,
        uploadError: '',
        canSend: true,
        messageDraft: '群里说一句',
      },
    })

    expect(wrapper.get('textarea').attributes('disabled')).toBeUndefined()
    expect(wrapper.get('input[type="file"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.get('button.primary-button').attributes('disabled')).toBeUndefined()
  })
})
