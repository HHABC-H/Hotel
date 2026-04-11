<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header">我的订单</div>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNumber" label="订单号" min-width="170" />
        <el-table-column prop="roomId" label="房间ID" min-width="90" />
        <el-table-column prop="checkInDate" label="入住日期" min-width="120" />
        <el-table-column prop="checkOutDate" label="退房日期" min-width="120" />
        <el-table-column prop="totalAmount" label="总金额" min-width="100" />
        <el-table-column label="状态" min-width="110">
          <template slot-scope="scope">
            {{ orderStatusLabel(scope.row.status) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <el-button
              v-if="canRenewOrder(scope.row)"
              type="text"
              :loading="renewingOrderId === scope.row.id"
              @click="openRenewDialog(scope.row)"
            >
              续房
            </el-button>
            <el-button
              v-if="canCancelOrder(scope.row)"
              type="text"
              :loading="cancelingOrderId === scope.row.id"
              @click="handleCancelOrder(scope.row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="action-wrap">
        <el-button type="primary" @click="fetchData">刷新</el-button>
      </div>
    </el-card>

    <el-dialog title="续房" :visible.sync="renewDialogVisible" width="420px" @closed="handleRenewDialogClosed">
      <el-form label-width="100px">
        <el-form-item label="当前退房日">
          <el-input :value="renewTarget?.checkOutDate || '-'" disabled />
        </el-form-item>
        <el-form-item label="新退房日期">
          <el-date-picker
            v-model="renewForm.checkOutDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择新退房日期"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="renewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="renewSubmitting" @click="handleRenewSubmit">确定续房</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listMyOrders, renewOrder } from '@/api/orders'
import { cancelBooking } from '@/api/bookings'
import { getOrderStatusLabel } from '@/constants/dict'

export default {
  name: 'ClientOrdersIndex',
  data() {
    return {
      loading: false,
      cancelingOrderId: null,
      renewingOrderId: null,
      renewDialogVisible: false,
      renewSubmitting: false,
      renewTarget: null,
      renewForm: {
        checkOutDate: ''
      },
      tableData: []
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      this.loading = true
      listMyOrders()
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
    },
    normalizeOrderStatus(value) {
      const raw = String(value || '').trim()
      const upper = raw.toUpperCase()

      if (['UNPAID', 'WAIT_PAY', 'PENDING_PAYMENT', 'TO_PAY'].includes(upper) || raw === '待支付') {
        return 'UNPAID'
      }
      if (['PAID'].includes(upper) || raw === '已支付') {
        return 'PAID'
      }
      if (['CANCELLED', 'CANCELED'].includes(upper) || raw === '已取消') {
        return 'CANCELLED'
      }
      if (['COMPLETED', 'FINISHED', 'DONE'].includes(upper) || raw === '已完成') {
        return 'COMPLETED'
      }
      return upper || raw
    },
    isDateCancelable(checkOutDate) {
      if (!checkOutDate) {
        return false
      }
      const checkout = new Date(`${checkOutDate}T23:59:59`)
      if (Number.isNaN(checkout.getTime())) {
        return false
      }
      return Date.now() <= checkout.getTime()
    },
    canCancelOrder(row) {
      if (!row) {
        return false
      }
      const status = this.normalizeOrderStatus(row.status)
      if (!['UNPAID', 'PAID'].includes(status)) {
        return false
      }
      return this.isDateCancelable(row.checkOutDate)
    },
    canRenewOrder(row) {
      if (!row) {
        return false
      }
      const status = this.normalizeOrderStatus(row.status)
      return ['UNPAID', 'PAID'].includes(status)
    },
    openRenewDialog(row) {
      this.renewTarget = row
      this.renewForm = { checkOutDate: '' }
      this.renewDialogVisible = true
    },
    handleRenewDialogClosed() {
      this.renewSubmitting = false
      this.renewTarget = null
      this.renewForm = { checkOutDate: '' }
    },
    async handleRenewSubmit() {
      if (!this.renewTarget || !this.renewTarget.id) {
        return
      }
      if (!this.renewForm.checkOutDate) {
        this.$message.warning('请选择新退房日期')
        return
      }
      if (this.renewForm.checkOutDate <= this.renewTarget.checkOutDate) {
        this.$message.warning('新退房日期必须晚于当前退房日期')
        return
      }

      this.renewSubmitting = true
      this.renewingOrderId = this.renewTarget.id
      try {
        await renewOrder(this.renewTarget.id, { checkOutDate: this.renewForm.checkOutDate })
        this.$message.success('续房成功')
        this.renewDialogVisible = false
        await this.fetchData()
      } finally {
        this.renewSubmitting = false
        this.renewingOrderId = null
      }
    },
    async handleCancelOrder(row) {
      if (!row || !row.id) {
        return
      }

      try {
        await this.$confirm('确认取消该订单吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch (e) {
        return
      }

      this.cancelingOrderId = row.id
      try {
        await cancelBooking(row.id)
        const rowIndex = this.tableData.findIndex(item => item.id === row.id)
        if (rowIndex !== -1) {
          this.$set(this.tableData[rowIndex], 'status', 'CANCELLED')
        }
        this.$message.success('订单已取消')
        await this.fetchData()
      } finally {
        this.cancelingOrderId = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.action-wrap {
  margin-top: 16px;
}
</style>
