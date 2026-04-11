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

export function payBooking(id) {
  return request({
    url: `/bookings/${id}/pay`,
    method: 'put'
  })
}

export function cancelBooking(id) {
  return request({
    url: `/bookings/${id}/cancel`,
    method: 'delete'
  })
}

