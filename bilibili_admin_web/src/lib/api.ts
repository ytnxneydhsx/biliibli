import axios, { AxiosError } from 'axios'
import JSONBigFactory from 'json-bigint'

type ApiResult<T> = {
  code: number
  message: string
  data: T
}

const JSONBig = JSONBigFactory({ storeAsString: true })

export const adminStorageKeys = {
  token: 'bilibili_admin_token',
  uid: 'bilibili_admin_uid',
  username: 'bilibili_admin_username',
  roleCode: 'bilibili_admin_role_code',
}

function parseResponseBody(data: unknown) {
  if (typeof data !== 'string') {
    return data
  }

  const trimmed = data.trim()
  if (!trimmed) {
    return data
  }

  try {
    return JSONBig.parse(trimmed)
  } catch {
    return data
  }
}

function clearAdminSession() {
  localStorage.removeItem(adminStorageKeys.token)
  localStorage.removeItem(adminStorageKeys.uid)
  localStorage.removeItem(adminStorageKeys.username)
  localStorage.removeItem(adminStorageKeys.roleCode)
}

const http = axios.create({
  timeout: 30000,
  transformResponse: [(data) => parseResponseBody(data)],
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(adminStorageKeys.token)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResult<unknown>
    if (typeof payload?.code === 'number') {
      if (payload.code === 0) {
        return payload.data
      }
      return Promise.reject(new Error(payload.message || '请求失败'))
    }
    return response.data
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      clearAdminSession()
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }

    const message = error.response?.data?.message || error.message || '网络请求失败'
    return Promise.reject(new Error(message))
  },
)

export const api = {
  get<T>(url: string, params?: Record<string, unknown>) {
    return http.get(url, { params }) as Promise<T>
  },
  post<T>(url: string, data?: unknown, config?: Record<string, unknown>) {
    return http.post(url, data, config) as Promise<T>
  },
}
