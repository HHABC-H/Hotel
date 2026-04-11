import request from '@/utils/request'

export function createBooking(data) {
  return request({
    url: '/bookings',
    method: 'post',
    data
  })
}

export function listMyBookings(params) {
  return request({
    url: '/bookings/my',
    method: 'get',
    params
  })
}
