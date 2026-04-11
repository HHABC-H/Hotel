export const ROLE_OPTIONS = [
  { label: '管理员', value: 'ADMIN' },
  { label: '前台', value: 'RECEPTIONIST' },
  { label: '客户', value: 'CLIENT' }
]

export const ROOM_STATUS_OPTIONS = [
  { label: '空闲', value: 'AVAILABLE' },
  { label: '入住中', value: 'OCCUPIED' },
  { label: '维护中', value: 'MAINTENANCE' }
]

export const ORDER_STATUS_OPTIONS = [
  { label: '待支付', value: 'UNPAID' },
  { label: '已支付', value: 'PAID' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '已完成', value: 'COMPLETED' }
]

export const ENABLE_STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

export const GENDER_OPTIONS = [
  { label: '男', value: 'M' },
  { label: '女', value: 'F' },
  { label: '未知', value: 'UNKNOWN' }
]

function labelFromOptions(options, value, fallback = '-') {
  const found = options.find(item => item.value === value)
  return found ? found.label : (value || fallback)
}

export function getRoleLabel(value) {
  return labelFromOptions(ROLE_OPTIONS, value)
}

export function getRoomStatusLabel(value) {
  return labelFromOptions(ROOM_STATUS_OPTIONS, value)
}

export function getOrderStatusLabel(value) {
  return labelFromOptions(ORDER_STATUS_OPTIONS, value)
}

export function getEnableStatusLabel(value) {
  return labelFromOptions(ENABLE_STATUS_OPTIONS, value, '')
}

export function getGenderLabel(value) {
  return labelFromOptions(GENDER_OPTIONS, value)
}
