<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header">房间浏览</div>

      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="客房类型">
          <el-select v-model="query.roomTypeId" placeholder="全部类型" clearable filterable style="width: 220px;">
            <el-option
              v-for="item in roomTypeOptions"
              :key="item.id"
              :label="item.typeName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button type="success" plain @click="handleSearchAvailable">查询空闲房间</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="roomNumber" label="房间号" min-width="120" />
        <el-table-column label="房型" min-width="140">
          <template slot-scope="scope">
            {{ roomTypeName(scope.row.roomTypeId) }}
          </template>
        </el-table-column>
        <el-table-column prop="floor" label="楼层" min-width="80" />
        <el-table-column label="状态" min-width="110">
          <template slot-scope="scope">
            {{ roomStatusLabel(scope.row.status) }}
          </template>
        </el-table-column>
        <el-table-column label="参考价格" min-width="120">
          <template slot-scope="scope">
            {{ formatPrice(roomReferencePrice(scope.row)) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="openBookingDialog(scope.row)">立即预订</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog title="提交预订" :visible.sync="bookingDialogVisible" width="520px" @closed="handleBookingDialogClosed">
      <el-form ref="bookingFormRef" :model="bookingForm" :rules="bookingRules" label-width="100px">
        <el-form-item label="房间号">
          <el-input :value="selectedRoom ? selectedRoom.roomNumber : ''" disabled />
        </el-form-item>
        <el-form-item label="入住日期" prop="checkInDate">
          <el-date-picker
            v-model="bookingForm.checkInDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择入住日期"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="退房日期" prop="checkOutDate">
          <el-date-picker
            v-model="bookingForm.checkOutDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择退房日期"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="bookingForm.remark" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="bookingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bookingSubmitting" @click="handleConfirmBooking">确认预订</el-button>
      </span>
    </el-dialog>

    <el-dialog title="订单支付" :visible.sync="payDialogVisible" width="520px">
      <el-alert
        type="info"
        :closable="false"
        title="预订已创建，请完成支付。支付完成后会自动刷新房间状态。"
        style="margin-bottom: 16px;"
      />
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单号">{{ payContext.orderNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="房间号">{{ payContext.roomNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="房型">{{ payContext.roomTypeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="入住日期">{{ payContext.checkInDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="退房日期">{{ payContext.checkOutDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="应付金额">{{ formatPrice(payContext.totalAmount) }}</el-descriptions-item>
      </el-descriptions>
      <span slot="footer" class="dialog-footer">
        <el-button :loading="cancelSubmitting" @click="handleCancelNow">取消订单</el-button>
        <el-button @click="payDialogVisible = false">稍后支付</el-button>
        <el-button type="primary" :loading="paySubmitting" @click="handlePayNow">立即支付</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { browseRooms, listAvailableRooms } from '@/api/rooms'
import { listRoomTypes } from '@/api/roomTypes'
import { createBooking, payBooking, cancelBooking } from '@/api/bookings'
import { getRoomStatusLabel } from '@/constants/dict'

const createDefaultBookingForm = () => ({
  roomId: undefined,
  checkInDate: '',
  checkOutDate: '',
  remark: ''
})

export default {
  name: 'ClientRoomsIndex',
  data() {
    return {
      loading: false,
      bookingSubmitting: false,
      paySubmitting: false,
      cancelSubmitting: false,
      tableData: [],
      roomTypeOptions: [],
      query: {
        roomTypeId: undefined
      },
      searchMode: 'browse',
      selectedRoom: null,
      bookingDialogVisible: false,
      payDialogVisible: false,
      bookingForm: createDefaultBookingForm(),
      payContext: {
        orderId: undefined,
        orderNumber: '',
        roomNumber: '',
        roomId: undefined,
        roomTypeName: '',
        checkInDate: '',
        checkOutDate: '',
        totalAmount: '',
        status: ''
      },
      bookingRules: {
        checkInDate: [{ required: true, message: '请选择入住日期', trigger: 'change' }],
        checkOutDate: [{ required: true, message: '请选择退房日期', trigger: 'change' }]
      }
    }
  },
  created() {
    this.fetchRoomTypes()
    this.fetchData()
  },
  methods: {
    normalizeListData(data) {
      return Array.isArray(data) ? data : (data?.records || [])
    },
    fetchRoomTypes() {
      listRoomTypes({ pageNum: 1, pageSize: 1000, status: 1 })
        .then(res => {
          this.roomTypeOptions = this.normalizeListData(res.data)
        })
        .catch(() => {
          this.roomTypeOptions = []
        })
    },
    roomTypeName(roomTypeId) {
      const found = this.roomTypeOptions.find(item => String(item.id) === String(roomTypeId))
      return found?.typeName || `房型#${roomTypeId}`
    },
    roomReferencePrice(row) {
      if (row?.price !== undefined && row?.price !== null && row?.price !== '') {
        return row.price
      }
      const found = this.roomTypeOptions.find(item => String(item.id) === String(row?.roomTypeId))
      return found?.price
    },
    formatPrice(value) {
      if (value === undefined || value === null || value === '') {
        return '-'
      }
      const num = Number(value)
      return Number.isNaN(num) ? value : `¥${num.toFixed(2)}`
    },
    fetchData() {
      const roomTypeId = this.query.roomTypeId
      const params = {}

      this.loading = true

      const request = this.searchMode === 'available'
        ? listAvailableRooms(params)
        : browseRooms(roomTypeId ? { roomTypeId } : {})

      request
        .then(res => {
          let records = this.normalizeListData(res.data)
          if (this.searchMode === 'available' && roomTypeId) {
            records = records.filter(item => String(item.roomTypeId) === String(roomTypeId))
          }
          this.tableData = records
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleSearch() {
      this.searchMode = 'browse'
      this.fetchData()
    },
    handleSearchAvailable() {
      this.searchMode = 'available'
      this.fetchData()
    },
    handleReset() {
      this.query = {
        roomTypeId: undefined
      }
      this.searchMode = 'browse'
      this.fetchData()
    },
    openBookingDialog(row) {
      this.selectedRoom = row
      this.bookingForm = {
        roomId: row.id,
        checkInDate: '',
        checkOutDate: '',
        remark: ''
      }
      this.bookingDialogVisible = true
    },
    handleBookingDialogClosed() {
      this.bookingSubmitting = false
      this.$refs.bookingFormRef && this.$refs.bookingFormRef.clearValidate()
    },
    isValidDateRange() {
      if (!this.bookingForm.checkInDate || !this.bookingForm.checkOutDate) {
        return false
      }
      return this.bookingForm.checkInDate < this.bookingForm.checkOutDate
    },
    extractOrderFromBookingResponse(data) {
      const payload = data?.order || data || {}
      return {
        orderId: payload.id || payload.orderId || undefined,
        orderNumber: payload.orderNumber || '',
        roomNumber: payload.roomNumber || this.selectedRoom?.roomNumber || '',
        roomId: payload.roomId || this.bookingForm.roomId,
        roomTypeName: payload.roomTypeName || this.roomTypeName(payload.roomTypeId || this.selectedRoom?.roomTypeId),
        checkInDate: payload.checkInDate || this.bookingForm.checkInDate,
        checkOutDate: payload.checkOutDate || this.bookingForm.checkOutDate,
        totalAmount: payload.totalAmount || this.roomReferencePrice(this.selectedRoom),
        status: payload.status || ''
      }
    },
    handleConfirmBooking() {
      this.$refs.bookingFormRef.validate(async(valid) => {
        if (!valid) {
          return false
        }
        if (!this.isValidDateRange()) {
          this.$message.warning('退房日期必须晚于入住日期')
          return
        }

        this.bookingSubmitting = true
        try {
          const res = await createBooking(this.bookingForm)

          const orderInfo = this.extractOrderFromBookingResponse(res.data)
          this.bookingDialogVisible = false
          this.payContext = orderInfo
          this.$message.success('预订成功')
          this.payDialogVisible = true
        } finally {
          this.bookingSubmitting = false
        }
      })
    },
    handlePayNow() {
      if (!this.payContext.orderId) {
        this.$message.warning('未定位到订单ID，请到“我的订单”页面完成支付')
        return
      }
      this.paySubmitting = true
      payBooking(this.payContext.orderId)
        .then(() => {
          this.$message.success('支付成功，已刷新房间状态')
          this.payDialogVisible = false
          this.searchMode = 'available'
          this.fetchData()
        })
        .finally(() => {
          this.paySubmitting = false
        })
    },
    handleCancelNow() {
      if (!this.payContext.orderId) {
        this.$message.warning('未定位到订单ID，无法取消')
        return
      }
      this.cancelSubmitting = true
      cancelBooking(this.payContext.orderId)
        .then(() => {
          this.$message.success('订单已取消')
          this.payDialogVisible = false
          this.searchMode = 'available'
          this.fetchData()
        })
        .finally(() => {
          this.cancelSubmitting = false
        })
    },
    roomStatusLabel(value) {
      return getRoomStatusLabel(value)
    }
  }
}
</script>

<style lang="scss" scoped>
.filter-form {
  margin-bottom: 12px;
}
</style>
