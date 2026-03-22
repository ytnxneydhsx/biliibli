import { reactive } from 'vue'
import type { UserLoginVO, UserProfileVO } from '../types'
import { api } from './api'

type AuthState = {
  token: string
  uid: string | null
  username: string
  profile: UserProfileVO | null
  ready: boolean
}

function parseUid(raw: string | null): string | null {
  if (!raw) {
    return null
  }
  const value = raw.trim()
  return /^\d+$/.test(value) ? value : null
}

function parseJwtUid(token: string | null): string | null {
  if (!token) {
    return null
  }
  try {
    const segments = token.split('.')
    if (segments.length < 2) {
      return null
    }
    const base64 = segments[1].replace(/-/g, '+').replace(/_/g, '/')
    const normalized = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
    const payload = JSON.parse(atob(normalized)) as { sub?: string }
    return parseUid(payload.sub ?? null)
  } catch {
    return null
  }
}

export const authState = reactive<AuthState>({
  token: localStorage.getItem('bilibili_token') || '',
  uid: parseUid(localStorage.getItem('bilibili_uid')),
  username: localStorage.getItem('bilibili_username') || '',
  profile: null,
  ready: false,
})

export async function refreshCurrentUser() {
  if (!authState.uid) {
    authState.profile = null
    authState.ready = true
    return
  }

  try {
    authState.profile = await api.get<UserProfileVO>(`/users/${authState.uid}`)
  } catch {
    authState.profile = null
  } finally {
    authState.ready = true
  }
}

export async function applyLogin(payload: UserLoginVO) {
  authState.token = payload.token
  authState.uid = parseJwtUid(payload.token) ?? payload.uid
  authState.username = payload.username

  localStorage.setItem('bilibili_token', payload.token)
  if (authState.uid) {
    localStorage.setItem('bilibili_uid', String(authState.uid))
  } else {
    localStorage.removeItem('bilibili_uid')
  }
  localStorage.setItem('bilibili_username', payload.username)

  await refreshCurrentUser()
}

export function logout() {
  authState.token = ''
  authState.uid = null
  authState.username = ''
  authState.profile = null
  authState.ready = true
  localStorage.removeItem('bilibili_token')
  localStorage.removeItem('bilibili_uid')
  localStorage.removeItem('bilibili_username')
}
