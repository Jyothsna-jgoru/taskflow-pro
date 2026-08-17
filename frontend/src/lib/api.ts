import axios from 'axios'

export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '/api', timeout: 15000 })
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('taskflow_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !String(error.config?.url).includes('/auth/login')) {
      localStorage.removeItem('taskflow_token')
      localStorage.removeItem('taskflow_user')
      localStorage.removeItem('taskflow_workspace')
      window.dispatchEvent(new Event('taskflow:unauthorized'))
    }
    return Promise.reject(error)
  },
)
export function errorMessage(error: unknown) {
  return axios.isAxiosError(error) ? (error.response?.data?.message ?? error.message) : 'Something went wrong'
}
