import request from '@/utils/request'

export function listSystemLogs(params, endpoint = '/logs') {
  return request({
    url: endpoint,
    method: 'get',
    params
  })
}
