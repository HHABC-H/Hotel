<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header">我的预订</div>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNumber" label="订单号" min-width="170" />
        <el-table-column prop="roomId" label="房间ID" min-width="90" />
        <el-table-column prop="checkInDate" label="入住日期" min-width="120" />
        <el-table-column prop="checkOutDate" label="退房日期" min-width="120" />
        <el-table-column label="状态" min-width="110">
          <template slot-scope="scope">
            {{ orderStatusLabel(scope.row.status) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" min-width="100" />
        <el-table-column prop="remark" label="备注" min-width="180" />
      </el-table>

      <div class="action-wrap">
        <el-button type="primary" @click="fetchData">刷新</el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import { listMyBookings } from '@/api/bookings'
import { getOrderStatusLabel } from '@/constants/dict'

export default {
  name: 'ClientBookingsIndex',
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
    fetchData() {
      this.loading = true
      listMyBookings()
        .then(res => {
          const data = res.data
          this.tableData = Array.isArray(data) ? data : (data?.records || [])
        })
        .finally(() => {
          this.loading = false
        })
    },
    orderStatusLabel(value) {
      return getOrderStatusLabel(value)
    }
  }
}
</script>

<style lang="scss" scoped>
.action-wrap {
  margin-top: 16px;
}
</style>
