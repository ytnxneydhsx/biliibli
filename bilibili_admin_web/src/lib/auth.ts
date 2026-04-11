import { reactive } from 'vue'
import { api, adminStorageKeys } from './api'
import { ADMIN_ROLE_CODE, type UserLoginVO } from '../types'

type AuthState = {
  token: string
  uid: string | null
  username: string
  roleCode: number | null
  ready: boolean
}

function parseUid(raw: string | null): string | null {
  if (!raw) {
    return null
  }

  const value = raw.trim()
  return /^\d+$/.test(value) ? value : null
}

function parseRoleCode(raw: string | null): number | null {
  if (!raw) {
    return null
  }

  const value = raw.trim()
  return /^\d+$/.test(value) ? Number(value) : null
}

export const authState = reactive<AuthState>({
  token: localStorage.getItem(adminStorageKeys.token) || '',
  uid: parseUid(localStorage.getItem(adminStorageKeys.uid)),
  username: localStorage.getItem(adminStorageKeys.username) || '',
  roleCode: parseRoleCode(localStorage.getItem(adminStorageKeys.roleCode)),
  ready: false,
})

export function isAdmin() {
  return authState.roleCode === ADMIN_ROLE_CODE
}

export function restoreAuth() {
  authState.ready = true
}

export async function loginAsAdmin(payload: { username: string; password: string }) {
  const result = await api.post<UserLoginVO>('/users/login', payload)

  if (result.roleCode !== ADMIN_ROLE_CODE) {
    throw new Error('你没有管理员权限')
  }

  authState.token = result.token
  authState.uid = result.uid
  authState.username = result.username
  authState.roleCode = result.roleCode
  authState.ready = true

  localStorage.setItem(adminStorageKeys.token, result.token)
  if (result.uid) {
    localStorage.setItem(adminStorageKeys.uid, String(result.uid))
  } else {
    localStorage.removeItem(adminStorageKeys.uid)
  }
  localStorage.setItem(adminStorageKeys.username, result.username)
  localStorage.setItem(adminStorageKeys.roleCode, String(result.roleCode))
}

export function logout() {
  authState.token = ''
  authState.uid = null
  authState.username = ''
  authState.roleCode = null
  authState.ready = true

  localStorage.removeItem(adminStorageKeys.token)
  localStorage.removeItem(adminStorageKeys.uid)
  localStorage.removeItem(adminStorageKeys.username)
  localStorage.removeItem(adminStorageKeys.roleCode)
}
