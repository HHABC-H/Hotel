<template>
  <div class="app-container oasis-page">
    <div class="page-toolbar">
      <div class="toolbar-title">
        <h2>房间管理</h2>
        <p>查看房态并维护客房基础信息</p>
      </div>
      <el-button v-if="canManage" type="primary" @click="openCreateDialog">新增客房</el-button>
    </div>

    <el-card shadow="never" class="content-card">
      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="房间号">
          <el-input v-model="query.roomNumber" placeholder="请输入房间号" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="房型ID">
          <el-input v-model="query.roomTypeId" placeholder="请输入房型ID" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option v-for="item in roomStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border class="data-table">
        <el-table-column prop="roomNumber" label="房间号" min-width="120" />
        <el-table-column prop="roomTypeId" label="房型ID" min-width="100" />
        <el-table-column prop="floor" label="楼层" min-width="90" />
        <el-table-column label="状态" min-width="110">
          <template slot-scope="scope">
            {{ roomStatusLabel(scope.row.status) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column v-if="canManage" label="操作" min-width="260" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" class="op-btn" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-dropdown class="op-btn" @command="(status) => handleStatusChange(scope.row, status)">
              <el-button type="text">改状态<i class="el-icon-arrow-down el-icon--right" /></el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item
                  v-for="item in roomStatusOptions"
                  :key="item.value"
                  :command="item.value"
                >
                  {{ item.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
            <el-button type="text" class="danger-btn op-btn" @click="handleDelete(scope.row)">删除</el-button>
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
            <el-option v-for="item in roomStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
import { ROOM_STATUS_OPTIONS, getRoomStatusLabel } from '@/constants/dict'

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
      roomStatusOptions: ROOM_STATUS_OPTIONS,
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
      this.$confirm(`确认将房间【${row.roomNumber}】状态改为 ${this.roomStatusLabel(status)} 吗？`, '提示', { type: 'warning' })
        .then(() => updateRoomStatus(row.id, { status }))
        .then(() => {
          this.$message.success('状态更新成功')
          this.fetchData()
        })
        .catch(() => {})
    },
    roomStatusLabel(value) {
      return getRoomStatusLabel(value)
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

.filter-form {
  margin-bottom: 14px;
  padding: 14px 12px 2px;
  background: #f7fbfa;
  border: 1px solid #dcebe7;
  border-radius: 10px;
}

.data-table {
  border-radius: 8px;
  overflow: hidden;
}

.pagination-wrapper {
  margin-top: 18px;
  text-align: right;
}

.danger-btn {
  color: #f56c6c;
}

.op-btn + .op-btn {
  margin-left: 10px;
}
</style>
