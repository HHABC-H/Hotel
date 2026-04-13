<template>
  <div class="app-container oasis-page">
    <div class="page-toolbar">
      <div class="toolbar-title">
        <h2>创建订单</h2>
        <p>先选择日期，再从可用房间中为顾客创建订单</p>
      </div>
      <el-button type="primary" plain @click="refreshOptions">刷新选项</el-button>
    </div>

    <div class="order-layout">
      <el-card shadow="never" class="content-card form-card">
        <el-alert
          type="info"
          :closable="false"
          title="请先选择入住/退房日期，再选择可用房间。"
          class="tip-alert"
        />

        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="order-form">
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
              placeholder="选择入住日期"
              style="width: 100%;"
              :picker-options="checkInDatePickerOptions"
              @change="handleCheckInDateChange"
            />
          </el-form-item>

          <el-form-item label="退房日期" prop="checkOutDate">
            <el-date-picker
              v-model="form.checkOutDate"
              type="date"
              value-format="yyyy-MM-dd"
              placeholder="选择退房日期"
              style="width: 100%;"
              :picker-options="checkOutDatePickerOptions"
              @change="handleCheckOutDateChange"
            />
          </el-form-item>

          <el-form-item label="可用房间" prop="roomId">
            <el-select
              v-model="form.roomId"
              placeholder="请选择房间"
              filterable
              clearable
              :loading="roomLoading"
              style="width: 100%;"
              :disabled="!canLoadRooms"
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
            <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="选填" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="content-card selected-room-panel">
        <div class="selected-room-title">房间预览</div>
        <template v-if="selectedRoom">
          <div class="selected-room-item"><span class="selected-room-label">房间号</span><span class="selected-room-value">{{ selectedRoom.roomNumber || '-' }}</span></div>
          <div class="selected-room-item"><span class="selected-room-label">房态</span><span class="selected-room-value">{{ roomStatusLabel(selectedRoom.status) }}</span></div>
          <div class="selected-room-item"><span class="selected-room-label">楼层</span><span class="selected-room-value">{{ selectedRoom.floor || '-' }}</span></div>
          <div class="selected-room-item"><span class="selected-room-label">房型ID</span><span class="selected-room-value">{{ selectedRoom.roomTypeId || '-' }}</span></div>
        </template>
        <div v-else class="room-placeholder">请选择可用房间后查看详情</div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { createOrder } from '@/api/orders'
import { listCustomers } from '@/api/customers'
import { listAvailableRooms } from '@/api/rooms'
import { getRoomStatusLabel } from '@/constants/dict'

const createDefaultForm = () => ({
  customerId: undefined,
  roomId: undefined,
  checkInDate: '',
  checkOutDate: '',
  remark: ''
})

export default {
  name: 'OrderCreateIndex',
  data() {
    return {
      submitting: false,
      customerLoading: false,
      roomLoading: false,
      customerOptions: [],
      roomOptions: [],
      form: createDefaultForm(),
      checkInDatePickerOptions: {
        disabledDate(time) {
          const todayStart = new Date().setHours(0, 0, 0, 0)
          return time.getTime() < todayStart
        }
      },
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
    },
    selectedRoom() {
      return this.roomOptions.find(item => item.id === this.form.roomId)
    },
    checkOutDatePickerOptions() {
      return {
        disabledDate: (time) => {
          const current = new Date(time).setHours(0, 0, 0, 0)
          const checkIn = this.parseDateStart(this.form.checkInDate)
          if (!checkIn) {
            const todayStart = new Date().setHours(0, 0, 0, 0)
            return current < todayStart
          }
          return current <= checkIn
        }
      }
    }
  },
  created() {
    this.fetchCustomers()
  },
  methods: {
    normalizePageData(data) {
      if (Array.isArray(data)) {
        return data
      }
      return data?.records || []
    },
    fetchCustomers() {
      this.customerLoading = true
      listCustomers({ pageNum: 1, pageSize: 500, keyword: '' })
        .then(res => {
          this.customerOptions = this.normalizePageData(res.data)
        })
        .finally(() => {
          this.customerLoading = false
        })
    },
    fetchAvailableRooms() {
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
      listAvailableRooms(params)
        .then(res => {
          this.roomOptions = this.normalizePageData(res.data)
          if (!this.roomOptions.some(item => item.id === this.form.roomId)) {
            this.form.roomId = undefined
          }
        })
        .finally(() => {
          this.roomLoading = false
        })
    },
    parseDateStart(dateStr) {
      if (!dateStr) {
        return null
      }
      const timestamp = new Date(`${dateStr}T00:00:00`).getTime()
      return Number.isNaN(timestamp) ? null : timestamp
    },
    plusOneDay(dateStr) {
      const start = this.parseDateStart(dateStr)
      if (!start) {
        return ''
      }
      const dayMs = 24 * 60 * 60 * 1000
      const next = new Date(start + dayMs)
      const y = next.getFullYear()
      const m = String(next.getMonth() + 1).padStart(2, '0')
      const d = String(next.getDate()).padStart(2, '0')
      return `${y}-${m}-${d}`
    },
    ensureValidDateRange() {
      if (!this.form.checkInDate) {
        return
      }
      const inTs = this.parseDateStart(this.form.checkInDate)
      const outTs = this.parseDateStart(this.form.checkOutDate)
      if (!outTs || outTs <= inTs) {
        this.form.checkOutDate = this.plusOneDay(this.form.checkInDate)
      }
    },
    handleCheckInDateChange() {
      this.ensureValidDateRange()
      this.fetchAvailableRooms()
    },
    handleCheckOutDateChange() {
      this.fetchAvailableRooms()
    },
    refreshOptions() {
      this.fetchCustomers()
      this.fetchAvailableRooms()
    },
    roomStatusLabel(value) {
      return getRoomStatusLabel(value)
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

        this.submitting = true
        createOrder(this.form)
          .then(() => {
            this.$message.success('创建成功')
            this.$router.push('/order/list')
          })
          .finally(() => {
            this.submitting = false
          })
      })
    },
    handleReset() {
      this.form = createDefaultForm()
      this.roomOptions = []
      this.$nextTick(() => {
        this.$refs.formRef.clearValidate()
      })
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

.order-layout {
  display: grid;
  grid-template-columns: minmax(540px, 1.1fr) minmax(300px, 0.8fr);
  gap: 16px;
}

.form-card {
  min-width: 0;
}

.tip-alert {
  margin-bottom: 16px;
}

.order-form {
  max-width: 620px;
}

.selected-room-panel {
  min-height: 220px;
  padding: 18px 16px;
}

.selected-room-title {
  margin-bottom: 14px;
  font-size: 16px;
  font-weight: 600;
  color: #173d44;
}

.selected-room-item {
  display: flex;
  margin-bottom: 12px;
  line-height: 20px;
}

.selected-room-label {
  width: 72px;
  flex-shrink: 0;
  color: #5f7f87;
}

.selected-room-value {
  color: #1f2f35;
  word-break: break-all;
}

.room-placeholder {
  color: #6b8a90;
  padding-top: 10px;
}

@media (max-width: 1200px) {
  .order-layout {
    grid-template-columns: 1fr;
  }

  .order-form {
    max-width: 100%;
  }
}
</style>
