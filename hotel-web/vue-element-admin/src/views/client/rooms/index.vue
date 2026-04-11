<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header">房间浏览</div>

      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="入住日期">
          <el-date-picker v-model="query.checkInDate" type="date" value-format="yyyy-MM-dd" placeholder="入住日期" />
        </el-form-item>
        <el-form-item label="退房日期">
          <el-date-picker v-model="query.checkOutDate" type="date" value-format="yyyy-MM-dd" placeholder="退房日期" />
        </el-form-item>
        <el-form-item label="客房类型ID">
          <el-input v-model="query.roomTypeId" placeholder="选填" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="roomNumber" label="房间号" min-width="120" />
        <el-table-column prop="roomTypeId" label="类型ID" min-width="100" />
        <el-table-column prop="floor" label="楼层" min-width="80" />
        <el-table-column prop="status" label="状态" min-width="110" />
        <el-table-column prop="price" label="参考价格" min-width="100" />
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="handleBooking(scope.row)">立即预订</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { browseRooms } from '@/api/rooms'
import { createBooking } from '@/api/bookings'

export default {
  name: 'ClientRoomsIndex',
  data() {
    return {
      loading: false,
      tableData: [],
      query: {
        checkInDate: '',
        checkOutDate: '',
        roomTypeId: ''
      }
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      const params = { ...this.query }
      if (!params.roomTypeId) {
        delete params.roomTypeId
      }
      if (!params.checkInDate) {
        delete params.checkInDate
      }
      if (!params.checkOutDate) {
        delete params.checkOutDate
      }

      this.loading = true
      browseRooms(params)
        .then(res => {
          const data = res.data
          this.tableData = Array.isArray(data) ? data : (data?.records || [])
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleSearch() {
      this.fetchData()
    },
    handleReset() {
      this.query = {
        checkInDate: '',
        checkOutDate: '',
        roomTypeId: ''
      }
      this.fetchData()
    },
    handleBooking(row) {
      if (!this.query.checkInDate || !this.query.checkOutDate) {
        this.$message.warning('请先选择入住和退房日期再预订')
        return
      }

      const payload = {
        roomId: row.id,
        checkInDate: this.query.checkInDate,
        checkOutDate: this.query.checkOutDate,
        remark: '前端骨架版预订'
      }

      createBooking(payload).then(() => {
        this.$message.success('预订成功')
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.filter-form {
  margin-bottom: 12px;
}
</style>
