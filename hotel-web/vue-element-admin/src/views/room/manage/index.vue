<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header" class="header-row">
        <span>客房信息</span>
        <el-button v-if="canManage" type="primary" size="mini" @click="openCreateDialog">新增客房</el-button>
      </div>

      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="房间号">
          <el-input v-model="query.roomNumber" placeholder="请输入房间号" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="客房类型ID">
          <el-input v-model="query.roomTypeId" placeholder="请输入类型ID" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="AVAILABLE" value="AVAILABLE" />
            <el-option label="OCCUPIED" value="OCCUPIED" />
            <el-option label="MAINTENANCE" value="MAINTENANCE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="roomNumber" label="房间号" min-width="110" />
        <el-table-column prop="roomTypeId" label="类型ID" min-width="90" />
        <el-table-column prop="floor" label="楼层" min-width="80" />
        <el-table-column prop="status" label="状态" min-width="110" />
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column v-if="canManage" label="操作" min-width="260" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-dropdown @command="(status) => handleStatusChange(scope.row, status)">
              <el-button type="text">改状态<i class="el-icon-arrow-down el-icon--right" /></el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="AVAILABLE">AVAILABLE</el-dropdown-item>
                <el-dropdown-item command="OCCUPIED">OCCUPIED</el-dropdown-item>
                <el-dropdown-item command="MAINTENANCE">MAINTENANCE</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
            <el-button type="text" class="danger-btn" @click="handleDelete(scope.row)">删除</el-button>
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

    <el-dialog v-if="canManage" :title="isEdit ? '编辑客房' : '新增客房'" :visible.sync="dialogVisible" width="520px" @closed="handleDialogClosed">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="房间号" prop="roomNumber">
          <el-input v-model="form.roomNumber" />
        </el-form-item>
        <el-form-item label="客房类型ID" prop="roomTypeId">
          <el-input-number v-model="form.roomTypeId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="楼层" prop="floor">
          <el-input-number v-model="form.floor" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status">
            <el-option label="AVAILABLE" value="AVAILABLE" />
            <el-option label="OCCUPIED" value="OCCUPIED" />
            <el-option label="MAINTENANCE" value="MAINTENANCE" />
          </el-select>
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
import { mapGetters } from 'vuex'
import { listRooms, getRoomDetail, createRoom, updateRoom, deleteRoom, updateRoomStatus } from '@/api/rooms'

const createDefaultForm = () => ({
  id: null,
  roomNumber: '',
  roomTypeId: 1,
  floor: 1,
  status: 'AVAILABLE'
})

export default {
  name: 'RoomManageIndex',
  data() {
    return {
      loading: false,
      submitting: false,
      total: 0,
      tableData: [],
      query: {
        pageNum: 1,
        pageSize: 10,
        roomTypeId: '',
        status: undefined,
        roomNumber: ''
      },
      dialogVisible: false,
      isEdit: false,
      form: createDefaultForm(),
      rules: {
        roomNumber: [{ required: true, message: '请输入房间号', trigger: 'blur' }],
        roomTypeId: [{ required: true, message: '请输入客房类型ID', trigger: 'change' }],
        floor: [{ required: true, message: '请输入楼层', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    ...mapGetters(['roles']),
    canManage() {
      return this.roles.includes('ADMIN')
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
      const params = { ...this.query }
      if (!params.roomTypeId) {
        delete params.roomTypeId
      }
      this.loading = true
      listRooms(params)
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
        roomTypeId: '',
        status: undefined,
        roomNumber: ''
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
      getRoomDetail(row.id).then(res => {
        const data = res.data || row
        this.form = {
          id: data.id,
          roomNumber: data.roomNumber || '',
          roomTypeId: Number(data.roomTypeId || 1),
          floor: Number(data.floor || 1),
          status: data.status || 'AVAILABLE'
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
        roomNumber: this.form.roomNumber,
        roomTypeId: this.form.roomTypeId,
        floor: this.form.floor,
        status: this.form.status
      }
    },
    handleSubmit() {
      this.$refs.formRef.validate(valid => {
        if (!valid) {
          return false
        }
        const payload = this.buildPayload()
        this.submitting = true
        const request = this.isEdit
          ? updateRoom(this.form.id, payload)
          : createRoom(payload)
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
    handleStatusChange(row, status) {
      if (row.status === status) {
        return
      }
      this.$confirm(`确认将房间【${row.roomNumber}】状态改为 ${status} 吗？`, '提示', { type: 'warning' })
        .then(() => updateRoomStatus(row.id, { status }))
        .then(() => {
          this.$message.success('状态更新成功')
          this.fetchData()
        })
        .catch(() => {})
    },
    handleDelete(row) {
      this.$confirm(`确认删除房间【${row.roomNumber}】吗？`, '提示', { type: 'warning' })
        .then(() => deleteRoom(row.id))
        .then(() => {
          this.$message.success('删除成功')
          this.fetchData()
        })
        .catch(() => {})
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
