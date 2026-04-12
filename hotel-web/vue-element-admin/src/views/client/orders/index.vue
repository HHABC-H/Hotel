<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header">我的订单</div>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="orderNumber" label="订单号" min-width="180" />
        <el-table-column label="下单时间" min-width="170">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="roomNumber" label="房间号" min-width="100" />
        <el-table-column prop="roomTypeName" label="房型" min-width="140" />
        <el-table-column prop="checkInDate" label="入住日期" min-width="120" />
        <el-table-column prop="checkOutDate" label="退房日期" min-width="120" />
        <el-table-column label="总金额" min-width="110">
          <template slot-scope="scope">
            {{ formatAmount(scope.row.totalAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="110">
          <template slot-scope="scope">
            {{ orderStatusLabel(scope.row.status) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="action-wrap">
        <el-button type="primary" @click="fetchData">刷新</el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import { listMyOrders } from '@/api/orders'
import { getRoomDetailForClient } from '@/api/rooms'
import { getOrderStatusLabel } from '@/constants/dict'

export default {
  name: 'ClientOrdersIndex',
  data() {
    return {
      loading: false,
      tableData: []
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const res = await listMyOrders()
        const data = res.data
        const records = Array.isArray(data) ? data : (data?.records || [])
        this.tableData = await this.enrichRoomInfo(records)
      } finally {
        this.loading = false
      }
    },
    async enrichRoomInfo(records) {
      if (!records.length) {
        return records
      }
      const roomIds = Array.from(new Set(records.map(item => item.roomId).filter(Boolean)))
      const roomMap = {}

      await Promise.all(roomIds.map(async(roomId) => {
        try {
          const res = await getRoomDetailForClient(roomId)
          roomMap[roomId] = res.data || {}
        } catch (e) {
          roomMap[roomId] = {}
        }
      }))

      return records.map(item => {
        const room = roomMap[item.roomId] || {}
        return {
          ...item,
          roomNumber: room.roomNumber || '-',
          roomTypeName: room.roomTypeName || (item.roomTypeId ? `房型#${item.roomTypeId}` : '-')
        }
      })
    },
    orderStatusLabel(value) {
      return getOrderStatusLabel(value)
    },
    formatDateTime(value) {
      if (!value) {
        return '-'
      }
      return String(value).replace('T', ' ')
    },
    formatAmount(value) {
      if (value === undefined || value === null || value === '') {
        return '-'
      }
      const num = Number(value)
      return Number.isNaN(num) ? value : `¥${num.toFixed(2)}`
    }
  }
}
</script>

<style lang="scss" scoped>
.action-wrap {
  margin-top: 16px;
}
</style>
