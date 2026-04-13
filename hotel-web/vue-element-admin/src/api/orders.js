import request from '@/utils/request'

export function listOrders(params) {
  return request({
    url: '/orders',
    method: 'get',
    params
  })
}

export function getOrderDetail(id) {
  return request({
    url: `/orders/${id}`,
    method: 'get'
  })
}

export function createOrder(data) {
  return request({
    url: '/orders',
    method: 'post',
    data
  })
}

export function updateOrder(id, data) {
  return request({
    url: `/orders/${id}`,
    method: 'put',
    data
  })
}

export function cancelOrder(id) {
  return request({
    url: `/bookings/${id}/cancel`,
    method: 'delete'
  })
}

export function payOrder(id) {
  return request({
    url: `/orders/${id}/pay`,
    method: 'put'
  })
}

export function checkInOrder(id) {
  return request({
    url: `/orders/${id}/check-in`,
    method: 'put'
  })
}

export function checkOutOrder(id) {
  return request({
    url: `/orders/${id}/check-out`,
    method: 'put'
  })
}

export function renewOrder(id, data) {
  return request({
    url: `/orders/${id}/renew`,
    method: 'put',
    data
  })
}

export function listMyOrders(params) {
  return request({
    url: '/orders/my',
    method: 'get',
    params
  })
}
