import request from '@/utils/request'

export function getProfile() {
  return request({
    url: '/profile',
    method: 'get'
  })
}

export function updateProfile(data) {
  return request({
    url: '/profile',
    method: 'put',
    data
  })
}

export function rechargeProfile(data) {
  return request({
    url: '/profile/recharge',
    method: 'put',
    data
  })
}
