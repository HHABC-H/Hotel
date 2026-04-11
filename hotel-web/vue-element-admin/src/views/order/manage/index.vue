<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header" class="header-row">
        <span>订单列表</span>
        <el-button type="primary" size="mini" @click="openCreateDialog">新增订单</el-button>
      </div>

      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="订单号">
          <el-input v-model="query.orderNumber" placeholder="请输入订单号" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="UNPAID" value="UNPAID" />
            <el-option label="PAID" value="PAID" />
            <el-option label="CANCELLED" value="CANCELLED" />
            <el-option label="COMPLETED" value="COMPLETED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNumber" label="订单号" min-width="170" />
        <el-table-column prop="customerId" label="客户ID" width="90" />
        <el-table-column prop="roomId" label="房间ID" width="90" />
        <el-table-column prop="checkInDate" label="入住日期" min-width="120" />
        <el-table-column prop="checkOutDate" label="退房日期" min-width="120" />
        <el-table-column prop="totalAmount" label="总金额" min-width="100" />
        <el-table-column prop="status" label="状态" min-width="110" />
        <el-table-column label="操作" min-width="360" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button type="text" :disabled="!canPay(scope.row)" @click="handlePay(scope.row)">支付</el-button>
            <el-button type="text" :disabled="!canCheckIn(scope.row)" @click="handleCheckIn(scope.row)">入住</el-button>
            <el-button type="text" :disabled="!canCheckOut(scope.row)" @click="handleCheckOut(scope.row)">退房</el-button>
            <el-button type="text" class="danger-btn" :disabled="!canCancel(scope.row)" @click="handleCancel(scope.row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog :title="isEdit ? '编辑订单' : '新增订单'" :visible.sync="dialogVisible" width="560px" @closed="handleDialogClosed">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="客户ID" prop="customerId">
          <el-input-number v-model="form.customerId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="房间ID" prop="roomId">
          <el-input-number v-model="form.roomId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="入住日期" prop="checkInDate">
          <el-date-picker v-model="form.checkInDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择入住日期" />
        </el-form-item>
        <el-form-item label="退房日期" prop="checkOutDate">
          <el-date-picker v-model="form.checkOutDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择退房日期" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  listOrders,
  getOrderDetail,
  createOrder,
  updateOrder,
  payOrder,
  checkInOrder,
  checkOutOrder,
  cancelOrder
} from '@/api/orders'

const createDefaultForm = () => ({
  id: null,
  customerId: 1,
  roomId: 1,
  checkInDate: '',
  checkOutDate: '',
  remark: ''
})

export default {
  name: 'OrderManageIndex',
  data() {
    return {
      loading: false,
      submitting: false,
      total: 0,
      tableData: [],
      query: {
        pageNum: 1,
        pageSize: 10,
        orderNumber: '',
        status: undefined
      },
      dialogVisible: false,
      isEdit: false,
      form: createDefaultForm(),
      rules: {
        customerId: [{ required: true, message: '请输入客户ID', trigger: 'change' }],
        roomId: [{ required: true, message: '请输入房间ID', trigger: 'change' }],
        checkInDate: [{ required: true, message: '请选择入住日期', trigger: 'change' }],
        checkOutDate: [{ required: true, message: '请选择退房日期', trigger: 'change' }]
      }
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    normalizePageData(data) {
      if (Array.isArray(data)) {
        return { records: data, total: data.length }
      }
      return {
        records: data?.records || [],
        total: Number(data?.total || 0)
      }
    },
    fetchData() {
      this.loading = true
      listOrders(this.query)
        .then(res => {
          const { records, total } = this.normalizePageData(res.data)
          this.tableData = records
          this.total = total
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleSearch() {
      this.query.pageNum = 1
      this.fetchData()
    },
    handleReset() {
      this.query = {
        pageNum: 1,
        pageSize: 10,
        orderNumber: '',
        status: undefined
      }
      this.fetchData()
    },
    handleSizeChange(size) {
      this.query.pageSize = size
      this.query.pageNum = 1
      this.fetchData()
    },
    handleCurrentChange(page) {
      this.query.pageNum = page
      this.fetchData()
    },
    openCreateDialog() {
      this.isEdit = false
      this.form = createDefaultForm()
      this.dialogVisible = true
    },
    openEditDialog(row) {
      this.isEdit = true
      getOrderDetail(row.id).then(res => {
        const data = res.data || row
        this.form = {
          id: data.id,
          customerId: Number(data.customerId || 1),
          roomId: Number(data.roomId || 1),
          checkInDate: data.checkInDate || '',
          checkOutDate: data.checkOutDate || '',
          remark: data.remark || ''
        }
        this.dialogVisible = true
      })
    },
    handleDialogClosed() {
      this.$refs.formRef && this.$refs.formRef.clearValidate()
      this.submitting = false
    },
    buildPayload() {
      return {
        customerId: this.form.customerId,
        roomId: this.form.roomId,
        checkInDate: this.form.checkInDate,
        checkOutDate: this.form.checkOutDate,
        remark: this.form.remark
      }
    },
    handleSubmit() {
      this.$refs.formRef.validate(valid => {
        if (!valid) {
          return false
        }
        if (this.form.checkInDate >= this.form.checkOutDate) {
          this.$message.warning('退房日期必须晚于入住日期')
          return
        }
        const payload = this.buildPayload()
        this.submitting = true
        const request = this.isEdit
          ? updateOrder(this.form.id, payload)
          : createOrder(payload)

        request
          .then(() => {
            this.$message.success(this.isEdit ? '更新成功' : '新增成功')
            this.dialogVisible = false
            this.fetchData()
          })
          .finally(() => {
            this.submitting = false
          })
      })
    },
    canPay(row) {
      return row.status === 'UNPAID'
    },
    canCheckIn(row) {
      return row.status === 'PAID'
    },
    canCheckOut(row) {
      return row.status === 'PAID'
    },
    canCancel(row) {
      return row.status !== 'CANCELLED' && row.status !== 'COMPLETED'
    },
    runActionWithConfirm({ row, action, actionName, successText }) {
      this.$confirm(`确认对订单【${row.orderNumber}】执行${actionName}吗？`, '提示', { type: 'warning' })
        .then(() => action(row.id))
        .then(() => {
          this.$message.success(successText)
          this.fetchData()
        })
        .catch(() => {})
    },
    handlePay(row) {
      if (!this.canPay(row)) {
        return
      }
      this.runActionWithConfirm({ row, action: payOrder, actionName: '支付', successText: '支付成功' })
    },
    handleCheckIn(row) {
      if (!this.canCheckIn(row)) {
        return
      }
      this.runActionWithConfirm({ row, action: checkInOrder, actionName: '入住', successText: '入住登记成功' })
    },
    handleCheckOut(row) {
      if (!this.canCheckOut(row)) {
        return
      }
      this.runActionWithConfirm({ row, action: checkOutOrder, actionName: '退房', successText: '退房结算成功' })
    },
    handleCancel(row) {
      if (!this.canCancel(row)) {
        return
      }
      this.runActionWithConfirm({ row, action: cancelOrder, actionName: '取消', successText: '订单已取消' })
    }
  }
}
</script>

<style lang="scss" scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-form {
  margin-bottom: 12px;
}

.pagination-wrapper {
  margin-top: 16px;
  text-align: right;
}

.danger-btn {
  color: #f56c6c;
}
</style>
