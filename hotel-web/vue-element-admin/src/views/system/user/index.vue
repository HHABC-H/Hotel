<template>
  <div class="app-container oasis-page">
    <div class="page-toolbar">
      <div class="toolbar-title">
        <h2>员工管理</h2>
        <p>维护管理员与前台账号状态与权限角色</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">新增员工</el-button>
    </div>

    <el-card shadow="never" class="content-card">
      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="请输入用户名" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部" clearable>
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border class="data-table">
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="余额" min-width="110">
          <template slot-scope="scope">
            {{ formatCurrency(scope.row.balance) }}
          </template>
        </el-table-column>
        <el-table-column prop="idCard" label="身份证号" min-width="170" />
        <el-table-column label="角色" width="130">
          <template slot-scope="scope">
            {{ roleLabel(scope.row.role) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" min-width="280" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" class="op-btn" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button type="text" class="op-btn" @click="toggleStatus(scope.row)">{{ scope.row.status === 1 ? '禁用' : '启用' }}</el-button>
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

    <el-dialog :title="isEdit ? '编辑员工' : '新增员工'" :visible.sync="dialogVisible" width="560px" @closed="handleDialogClosed">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEdit ? '' : 'password'">
          <el-input v-model="form.password" type="password" autocomplete="new-password" :placeholder="isEdit ? '留空表示不修改密码' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="账户余额" prop="balance">
          <el-input-number v-model="form.balance" :min="0" :precision="2" :step="100" controls-position="right" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="form.gender" placeholder="请选择">
            <el-option label="男" value="M" />
            <el-option label="女" value="F" />
            <el-option label="未知" value="UNKNOWN" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
import { listUsers, getUserDetail, createUser, updateUser, deleteUser, updateUserStatus } from '@/api/users'
import { validPhoneCN, validIdCardCN } from '@/utils/validate'
import { ROLE_OPTIONS, getRoleLabel } from '@/constants/dict'

const createDefaultForm = () => ({
  id: null,
  username: '',
  password: '',
  realName: '',
  phone: '',
  balance: 0,
  idCard: '',
  gender: 'UNKNOWN',
  role: 'RECEPTIONIST',
  status: 1
})

export default {
  name: 'SystemStaffIndex',
  data() {
    const validatePhone = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入手机号'))
        return
      }
      if (!validPhoneCN(value)) {
        callback(new Error('手机号格式不正确'))
        return
      }
      callback()
    }
    const validateIdCard = (rule, value, callback) => {
      if (!value) {
        callback()
        return
      }
      if (!validIdCardCN(value)) {
        callback(new Error('身份证号格式不正确'))
        return
      }
      callback()
    }
    const validatePassword = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入密码'))
        return
      }
      if (String(value).length < 6) {
        callback(new Error('密码长度至少 6 位'))
        return
      }
      callback()
    }
    return {
      loading: false,
      submitting: false,
      total: 0,
      tableData: [],
      query: {
        pageNum: 1,
        pageSize: 10,
        username: '',
        role: undefined,
        status: undefined
      },
      dialogVisible: false,
      isEdit: false,
      form: createDefaultForm(),
      roleOptions: ROLE_OPTIONS.filter(item => ['ADMIN', 'RECEPTIONIST'].includes(item.value)),
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, trigger: 'blur', validator: validatePassword }],
        realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
        phone: [{ required: true, trigger: 'blur', validator: validatePhone }],
        idCard: [{ trigger: 'blur', validator: validateIdCard }],
        role: [{ required: true, message: '请选择角色', trigger: 'change' }],
        gender: [{ required: true, message: '请选择性别', trigger: 'change' }]
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
      listUsers(this.query)
        .then(res => {
          const { records, total } = this.normalizePageData(res.data)
          const filtered = records.filter(item => ['ADMIN', 'RECEPTIONIST'].includes(item.role))
          this.tableData = filtered
          this.total = this.query.role ? total : filtered.length
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
        username: '',
        role: undefined,
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
      getUserDetail(row.id).then(res => {
        const data = res.data || row
        this.form = {
          id: data.id,
          username: data.username || '',
          password: '',
          realName: data.realName || '',
          phone: data.phone || '',
          balance: Number(data.balance || 0),
          idCard: data.idCard || '',
          gender: data.gender || 'UNKNOWN',
          role: data.role || 'RECEPTIONIST',
          status: data.status === 0 ? 0 : 1
        }
        this.dialogVisible = true
      })
    },
    handleDialogClosed() {
      this.$refs.formRef && this.$refs.formRef.clearValidate()
      this.submitting = false
    },
    buildPayload() {
      const payload = {
        username: this.form.username,
        realName: this.form.realName,
        phone: this.form.phone,
        balance: Number(this.form.balance || 0),
        idCard: this.form.idCard || null,
        gender: this.form.gender,
        role: this.form.role,
        status: this.form.status
      }
      if (this.form.password) {
        payload.password = this.form.password
      }
      return payload
    },
    handleSubmit() {
      this.$refs.formRef.validate(valid => {
        if (!valid) {
          return false
        }

        if (!this.isEdit && !this.form.password) {
          this.$message.warning('新增员工必须填写密码')
          return
        }

        const payload = this.buildPayload()
        this.submitting = true
        const request = this.isEdit
          ? updateUser(this.form.id, payload)
          : createUser(payload)

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
    toggleStatus(row) {
      const nextStatus = row.status === 1 ? 0 : 1
      const text = nextStatus === 1 ? '启用' : '禁用'
      this.$confirm(`确认${text}该用户吗？`, '提示', { type: 'warning' })
        .then(() => updateUserStatus(row.id, { status: nextStatus }))
        .then(() => {
          this.$message.success('状态更新成功')
          this.fetchData()
        })
        .catch(() => {})
    },
    handleDelete(row) {
      this.$confirm(`确认删除员工【${row.username}】吗？`, '提示', { type: 'warning' })
        .then(() => deleteUser(row.id))
        .then(() => {
          this.$message.success('删除成功')
          this.fetchData()
        })
        .catch(() => {})
    },
    roleLabel(value) {
      return getRoleLabel(value)
    },
    formatCurrency(value) {
      return `¥${Number(value || 0).toFixed(2)}`
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
  margin-left: 8px;
}
</style>
