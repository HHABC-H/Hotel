import request from '@/utils/request'

export function listRoomTypes(params) {
  return request({
    url: '/room-types',
    method: 'get',
    params
  })
}

export function getRoomTypeDetail(id) {
  return request({
    url: `/room-types/${id}`,
    method: 'get'
  })
}

export function createRoomType(data) {
  return request({
    url: '/room-types',
    method: 'post',
    data
  })
}

export function updateRoomType(id, data) {
  return request({
    url: `/room-types/${id}`,
    method: 'put',
    data
  })
}

export function deleteRoomType(id) {
  return request({
    url: `/room-types/${id}`,
    method: 'delete'
  })
}
