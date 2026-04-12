import axios from 'axios'
import { MessageBox, Message } from 'element-ui'
import store from '@/store'
import { getToken } from '@/utils/auth'

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 10000
})

const ERROR_MESSAGE_MAP = {
  'Room already booked for this date range': '该时间段房间已被预订，请更换日期或房间',
  'Insufficient balance': '余额不足，请先充值后支付',
  'Room already booked for the renewed period': '续住失败：该时间段房间已被预订',
  'Insufficient balance for renewal': '余额不足，无法完成续住支付'
}

function localizeErrorMessage(message) {
  const text = String(message || '').trim()
  if (!text) {
    return '请求失败'
  }
  return ERROR_MESSAGE_MAP[text] || text
}

service.interceptors.request.use(
  config => {
    if (store.getters.token) {
      config.headers.Authorization = `Bearer ${getToken()}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      const localizedMessage = localizeErrorMessage(res.message)
      Message({
        message: localizedMessage,
        type: 'error',
        duration: 5 * 1000
      })

      if (res.code === 401 || res.code === 403) {
        MessageBox.confirm('登录状态已失效，请重新登录', '提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          store.dispatch('user/resetToken').then(() => {
            location.reload()
          })
        })
      }
      return Promise.reject(new Error(localizedMessage))
    }
    return res
  },
  error => {
    const message = localizeErrorMessage(error.response?.data?.message || error.message || '网络错误')
    Message({
      message,
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(error)
  }
)

export default service
