<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header" class="header-row">
        <span>创建订单</span>
        <el-button size="mini" @click="refreshOptions">刷新选项</el-button>
      </div>

      <el-alert
        type="info"
        :closable="false"
        title="请先选择入住/退房日期，再选择可用房间。"
        style="margin-bottom: 16px;"
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
            @change="handleDateChange"
          />
        </el-form-item>

        <el-form-item label="退房日期" prop="checkOutDate">
          <el-date-picker
            v-model="form.checkOutDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="选择退房日期"
            style="width: 100%;"
            @change="handleDateChange"
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

      <el-descriptions v-if="selectedRoom" title="已选房间信息" :column="2" border size="small" style="margin-top: 16px;">
        <el-descriptions-item label="房间号">{{ selectedRoom.roomNumber }}</el-descriptions-item>
        <el-descriptions-item label="房态">{{ roomStatusLabel(selectedRoom.status) }}</el-descriptions-item>
        <el-descriptions-item label="楼层">{{ selectedRoom.floor || '-' }}</el-descriptions-item>
        <el-descriptions-item label="房型ID">{{ selectedRoom.roomTypeId || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
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
    handleDateChange() {
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
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-form {
  max-width: 560px;
}
</style>
