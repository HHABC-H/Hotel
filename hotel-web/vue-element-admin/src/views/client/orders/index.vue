<template>
  <div class="app-container oasis-page">
    <div class="page-toolbar">
      <div class="toolbar-title">
        <h2>我的订单</h2>
        <p>查看预订记录，并在规则允许时取消订单</p>
      </div>
      <el-button type="primary" plain @click="fetchData">刷新列表</el-button>
    </div>

    <el-card shadow="never" class="content-card">
      <el-table v-loading="loading" :data="tableData" border class="data-table">
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
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="scope">
            <el-button
              type="text"
              class="danger-btn"
              :disabled="!canCancel(scope.row)"
              @click="handleCancel(scope.row)"
            >
              取消订单
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { listMyBookings, cancelBooking } from '@/api/bookings'
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
        const res = await listMyBookings()
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
    canCancel(row) {
      const statusAllowed = row.status === 'UNPAID' || row.status === 'PAID'
      if (!statusAllowed) {
        return false
      }
      const today = this.toDateStart(new Date())
      const checkOut = this.toDateStart(row.checkOutDate)
      if (!checkOut) {
        return false
      }
      return today <= checkOut
    },
    toDateStart(value) {
      if (!value) {
        return null
      }
      const source = value instanceof Date ? value : new Date(`${value}T00:00:00`)
      const ts = new Date(source).setHours(0, 0, 0, 0)
      return Number.isNaN(ts) ? null : ts
    },
    handleCancel(row) {
      if (!this.canCancel(row)) {
        this.$message.warning('当前订单不可取消')
        return
      }
      this.$confirm(`确认取消订单【${row.orderNumber}】吗？`, '提示', { type: 'warning' })
        .then(() => cancelBooking(row.id))
        .then(() => {
          this.$message.success('订单已取消')
          this.fetchData()
        })
        .catch(() => {})
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
.oasis-page {
  .content-card {
    border: 1px solid #dce9e5;
    border-radius: 16px;
    box-shadow: 0 12px 26px rgba(11, 63, 54, 0.08);
  }
}

.page-toolbar {
  margin-bottom: 16px;
  padding: 16px 18px;
  border-radius: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(120deg, rgba(9, 38, 50, 0.93), rgba(17, 93, 89, 0.86));
  box-shadow: 0 10px 26px rgba(9, 38, 50, 0.22);
}

.toolbar-title h2 {
  margin: 0;
  font-size: 20px;
  color: #f8fcff;
  letter-spacing: 0.4px;
}

.toolbar-title p {
  margin: 6px 0 0;
  color: rgba(226, 242, 246, 0.86);
  font-size: 13px;
}

.data-table {
  border-radius: 8px;
  overflow: hidden;
}

.danger-btn {
  color: #f56c6c;
}
</style>
