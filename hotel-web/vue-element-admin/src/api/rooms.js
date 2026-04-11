import request from '@/utils/request'

export function listRooms(params) {
  return request({
    url: '/rooms',
    method: 'get',
    params
  })
}

export function getRoomDetail(id) {
  return request({
    url: `/rooms/${id}`,
    method: 'get'
  })
}

export function createRoom(data) {
  return request({
    url: '/rooms',
    method: 'post',
    data
  })
}

export function updateRoom(id, data) {
  return request({
    url: `/rooms/${id}`,
    method: 'put',
    data
  })
}

export function deleteRoom(id) {
  return request({
    url: `/rooms/${id}`,
    method: 'delete'
  })
}

export function updateRoomStatus(id, data) {
  return request({
    url: `/rooms/${id}/status`,
    method: 'put',
    data
  })
}

export function listAvailableRooms(params) {
  return request({
    url: '/rooms/available',
    method: 'get',
    params
  })
}

export function browseRooms(params) {
  return request({
    url: '/rooms/browse',
    method: 'get',
    params
  })
}

export function getRoomDetailForClient(id) {
  return request({
    url: `/rooms/${id}/detail`,
    method: 'get'
  })
}
