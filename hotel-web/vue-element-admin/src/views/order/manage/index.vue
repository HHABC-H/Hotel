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
            <el-option v-for="item in orderStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-table-column label="状态" min-width="110">
          <template slot-scope="scope">
            {{ orderStatusLabel(scope.row.status) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="420" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="openDetailDialog(scope.row)">详情</el-button>
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
        <el-form-item label="客户" prop="customerId">
          <el-select
            v-model="form.customerId"
            placeholder="请选择客户"
            filterable
            clearable
            :loading="customerLoading"
            style="width: 100%;"
          >
            <el-option
              v-for="item in customerOptions"
              :key="item.id"
              :label="`${item.realName || item.username}（${item.phone || '无手机号'}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="入住日期" prop="checkInDate">
          <el-date-picker
            v-model="form.checkInDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择入住日期"
            style="width: 100%;"
            @change="handleDialogDateChange"
          />
        </el-form-item>
        <el-form-item label="退房日期" prop="checkOutDate">
          <el-date-picker
            v-model="form.checkOutDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择退房日期"
            style="width: 100%;"
            @change="handleDialogDateChange"
          />
        </el-form-item>
        <el-form-item label="房间" prop="roomId">
          <el-select
            v-model="form.roomId"
            placeholder="请选择房间"
            filterable
            clearable
            :loading="roomLoading"
            :disabled="!canLoadRooms"
            style="width: 100%;"
          >
            <el-option
              v-for="item in roomOptions"
              :key="item.id"
              :label="`#${item.roomNumber}（${roomStatusLabel(item.status || 'AVAILABLE')}）`"
              :value="item.id"
            />
          </el-select>
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

    <el-dialog title="订单详情" :visible.sync="detailDialogVisible" width="860px">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <div v-else>
        <el-divider content-position="left">订单信息</el-divider>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="订单ID">{{ detailData.id || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ detailData.orderNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ orderStatusLabel(detailData.status) }}</el-descriptions-item>
          <el-descriptions-item label="总金额">{{ formatAmount(detailData.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="入住日期">{{ detailData.checkInDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="退房日期">{{ detailData.checkOutDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="入住天数">{{ stayNights(detailData) }}</el-descriptions-item>
          <el-descriptions-item label="客户ID">{{ detailData.customerId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="房间ID">{{ detailData.roomId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="3">{{ detailData.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">房间信息</el-divider>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="房间ID">{{ detailRoom.id || detailData.roomId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="房间号">{{ detailRoom.roomNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="楼层">{{ detailRoom.floor || '-' }}</el-descriptions-item>
          <el-descriptions-item label="房型ID">{{ detailRoom.roomTypeId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="房态">{{ roomStatusLabel(detailRoom.status || '-') }}</el-descriptions-item>
          <el-descriptions-item label="参考价格">{{ formatAmount(detailRoom.price) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">顾客信息</el-divider>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="客户ID">{{ detailCustomer.id || detailData.customerId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ detailCustomer.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ detailCustomer.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detailCustomer.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="身份证">{{ detailCustomer.idCard || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ genderLabel(detailCustomer.gender) }}</el-descriptions-item>
          <el-descriptions-item label="账户状态">{{ enableStatusLabel(detailCustomer.status) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="账户余额">{{ formatAmount(detailCustomer.balance) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">操作信息</el-divider>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="下单人ID">{{ detailData.createUserId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ formatDateTime(detailData.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(detailData.updateTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
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
import { listCustomers, getCustomerDetail } from '@/api/customers'
import { listAvailableRooms, getRoomDetail } from '@/api/rooms'
import { ORDER_STATUS_OPTIONS, getEnableStatusLabel, getGenderLabel, getOrderStatusLabel, getRoomStatusLabel } from '@/constants/dict'

const createDefaultForm = () => ({
  id: null,
  customerId: undefined,
  roomId: undefined,
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
      orderStatusOptions: ORDER_STATUS_OPTIONS,
      query: {
        pageNum: 1,
        pageSize: 10,
        orderNumber: '',
        status: undefined
      },
      dialogVisible: false,
      isEdit: false,
      customerLoading: false,
      roomLoading: false,
      customerOptions: [],
      roomOptions: [],
      form: createDefaultForm(),
      detailDialogVisible: false,
      detailLoading: false,
      detailData: {},
      detailCustomer: {},
      detailRoom: {},
      rules: {
        customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
        roomId: [{ required: true, message: '请选择房间', trigger: 'change' }],
        checkInDate: [{ required: true, message: '请选择入住日期', trigger: 'change' }],
        checkOutDate: [{ required: true, message: '请选择退房日期', trigger: 'change' }]
      }
    }
  },
  computed: {
    canLoadRooms() {
      return !!this.form.checkInDate && !!this.form.checkOutDate
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
    normalizeListData(data) {
      if (Array.isArray(data)) {
        return data
      }
      return data?.records || []
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
    fetchCustomerOptions() {
      this.customerLoading = true
      return listCustomers({ pageNum: 1, pageSize: 500, keyword: '' })
        .then(res => {
          this.customerOptions = this.normalizeListData(res.data)
        })
        .finally(() => {
          this.customerLoading = false
        })
    },
    async fetchRoomOptions(keepRoomId) {
      if (!this.canLoadRooms) {
        this.roomOptions = []
        return
      }
      if (this.form.checkInDate >= this.form.checkOutDate) {
        this.roomOptions = []
        this.form.roomId = undefined
        return
      }

      const params = {
        checkInDate: this.form.checkInDate,
        checkOutDate: this.form.checkOutDate
      }

      this.roomLoading = true
      try {
        const res = await listAvailableRooms(params)
        const records = this.normalizeListData(res.data)
        this.roomOptions = records

        if (keepRoomId && !this.roomOptions.some(item => item.id === keepRoomId)) {
          const detailRes = await getRoomDetail(keepRoomId)
          if (detailRes?.data?.id) {
            this.roomOptions.unshift(detailRes.data)
          }
        }

        if (!this.roomOptions.some(item => item.id === this.form.roomId)) {
          this.form.roomId = undefined
        }
      } finally {
        this.roomLoading = false
      }
    },
    async openCreateDialog() {
      this.isEdit = false
      this.form = createDefaultForm()
      this.roomOptions = []
      this.dialogVisible = true
      await this.fetchCustomerOptions()
    },
    async openEditDialog(row) {
      this.isEdit = true
      const res = await getOrderDetail(row.id)
      const data = res.data || row
      this.form = {
        id: data.id,
        customerId: Number(data.customerId || 0) || undefined,
        roomId: Number(data.roomId || 0) || undefined,
        checkInDate: data.checkInDate || '',
        checkOutDate: data.checkOutDate || '',
        remark: data.remark || ''
      }
      this.dialogVisible = true
      await this.fetchCustomerOptions()
      await this.fetchRoomOptions(this.form.roomId)
    },
    async openDetailDialog(row) {
      this.detailDialogVisible = true
      this.detailLoading = true
      try {
        const res = await getOrderDetail(row.id)
        const order = res.data || row || {}
        this.detailData = order
        this.detailCustomer = {}
        this.detailRoom = {}

        const requests = []
        if (order.customerId) {
          requests.push(
            getCustomerDetail(order.customerId)
              .then(customerRes => {
                this.detailCustomer = customerRes.data || {}
              })
              .catch(() => {
                this.detailCustomer = {}
              })
          )
        }
        if (order.roomId) {
          requests.push(
            getRoomDetail(order.roomId)
              .then(roomRes => {
                this.detailRoom = roomRes.data || {}
              })
              .catch(() => {
                this.detailRoom = {}
              })
          )
        }
        if (requests.length) {
          await Promise.all(requests)
        }
      } finally {
        this.detailLoading = false
      }
    },
    handleDialogDateChange() {
      this.fetchRoomOptions(this.isEdit ? this.form.roomId : undefined)
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
    roomStatusLabel(value) {
      return getRoomStatusLabel(value)
    },
    orderStatusLabel(value) {
      return getOrderStatusLabel(value)
    },
    genderLabel(value) {
      return getGenderLabel(value)
    },
    enableStatusLabel(value) {
      return getEnableStatusLabel(value)
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
      if (Number.isNaN(num)) {
        return value
      }
      return num.toFixed(2)
    },
    stayNights(order) {
      const inDate = order?.checkInDate
      const outDate = order?.checkOutDate
      if (!inDate || !outDate) {
        return '-'
      }
      const start = new Date(`${inDate}T00:00:00`).getTime()
      const end = new Date(`${outDate}T00:00:00`).getTime()
      if (!start || !end || end <= start) {
        return '-'
      }
      const dayMs = 24 * 60 * 60 * 1000
      return Math.round((end - start) / dayMs)
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
