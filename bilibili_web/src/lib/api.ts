import axios, { AxiosError } from 'axios'

type ApiResult<T> = {
  code: number
  message: string
  data: T
}

const http = axios.create({
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('bilibili_token')
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
  put<T>(url: string, data?: unknown) {
    return http.put(url, data) as Promise<T>
  },
  delete<T>(url: string) {
    return http.delete(url) as Promise<T>
  },
}
