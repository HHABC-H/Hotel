<template>
  <div class="app-container oasis-page">
    <div class="page-toolbar">
      <div class="toolbar-title">
        <h2>房型管理</h2>
        <p>维护房型价格、人数、图片和状态</p>
      </div>
      <el-button v-if="canManage" type="primary" @click="openCreateDialog">新增房型</el-button>
    </div>

    <el-card shadow="never" class="content-card">
      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="类型名称">
          <el-input v-model="query.typeName" placeholder="请输入类型名称" clearable @keyup.enter.native="handleSearch" />
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
        <el-table-column prop="typeName" label="类型名称" min-width="140" />
        <el-table-column label="图片" min-width="140">
          <template slot-scope="scope">
            <el-image
              v-if="scope.row.img"
              :src="scope.row.img"
              fit="cover"
              class="type-image"
              :preview-src-list="[scope.row.img]"
            />
            <span v-else class="image-empty">无图片</span>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格/晚" min-width="100" />
        <el-table-column prop="capacity" label="可住人数" min-width="100" />
        <el-table-column prop="bedType" label="床型" min-width="120" />
        <el-table-column prop="area" label="面积" min-width="90" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canManage" label="操作" width="170" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" class="op-btn" @click="openEditDialog(scope.row)">编辑</el-button>
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

    <el-dialog v-if="canManage" :title="isEdit ? '编辑客房类型' : '新增客房类型'" :visible.sync="dialogVisible" width="560px" @closed="handleDialogClosed">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="form.typeName" />
        </el-form-item>
        <el-form-item label="价格/晚" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" controls-position="right" />
        </el-form-item>
        <el-form-item label="可住人数" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="20" controls-position="right" />
        </el-form-item>
        <el-form-item label="床型" prop="bedType">
          <el-input v-model="form.bedType" />
        </el-form-item>
        <el-form-item label="面积" prop="area">
          <el-input-number v-model="form.area" :min="0" :precision="2" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="房型图片">
          <div class="image-uploader-wrap">
            <el-image
              v-if="form.img"
              :src="form.img"
              fit="cover"
              class="type-image form-image"
              :preview-src-list="[form.img]"
            />
            <span v-else class="image-empty">未上传图片</span>

            <el-upload
              action="#"
              :show-file-list="false"
              :http-request="handleUploadRequest"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <el-button size="mini" type="primary" plain :loading="uploadingImage">上传图片</el-button>
            </el-upload>
            <el-button size="mini" type="danger" plain :disabled="!form.img" :loading="removingImage" @click="handleRemoveImage">删除图片</el-button>
          </div>
        </el-form-item>
        <el-form-item label="状态">
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
import { mapGetters } from 'vuex'
import { listRoomTypes, getRoomTypeDetail, createRoomType, updateRoomType, deleteRoomType, uploadRoomTypeImage, deleteRoomTypeImage } from '@/api/roomTypes'

const createDefaultForm = () => ({
  id: null,
  typeName: '',
  price: 0,
  capacity: 1,
  bedType: '',
  area: 0,
  description: '',
  img: '',
  status: 1
})

export default {
  name: 'RoomTypeIndex',
  data() {
    return {
      loading: false,
      submitting: false,
      uploadingImage: false,
      removingImage: false,
      total: 0,
      tableData: [],
      query: {
        pageNum: 1,
        pageSize: 10,
        typeName: '',
        status: undefined
      },
      dialogVisible: false,
      isEdit: false,
      form: createDefaultForm(),
      rules: {
        typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
        price: [{ required: true, message: '请输入价格', trigger: 'change' }],
        capacity: [{ required: true, message: '请输入可住人数', trigger: 'change' }],
        bedType: [{ required: true, message: '请输入床型', trigger: 'blur' }]
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
      this.loading = true
      listRoomTypes(this.query)
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
        typeName: '',
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
      getRoomTypeDetail(row.id).then(res => {
        const data = res.data || row
        this.form = {
          id: data.id,
          typeName: data.typeName || '',
          price: Number(data.price || 0),
          capacity: Number(data.capacity || 1),
          bedType: data.bedType || '',
          area: Number(data.area || 0),
          description: data.description || '',
          img: data.img || '',
          status: data.status === 0 ? 0 : 1
        }
        this.dialogVisible = true
      })
    },
    handleDialogClosed() {
      this.$refs.formRef && this.$refs.formRef.clearValidate()
      this.submitting = false
      this.uploadingImage = false
      this.removingImage = false
    },
    buildPayload() {
      return {
        typeName: this.form.typeName,
        price: this.form.price,
        capacity: this.form.capacity,
        bedType: this.form.bedType,
        area: this.form.area,
        description: this.form.description,
        img: this.form.img || null,
        status: this.form.status
      }
    },
    beforeUpload(file) {
      const isImage = String(file.type || '').startsWith('image/')
      if (!isImage) {
        this.$message.error('仅支持上传图片文件')
        return false
      }
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) {
        this.$message.error('图片大小不能超过 10MB')
        return false
      }
      return true
    },
    handleUploadRequest(option) {
      this.uploadingImage = true
      uploadRoomTypeImage(option.file)
        .then(res => {
          this.form.img = (res.data && res.data.url) || ''
          this.$message.success('图片上传成功')
          option.onSuccess && option.onSuccess(res)
        })
        .catch(err => {
          option.onError && option.onError(err)
        })
        .finally(() => {
          this.uploadingImage = false
        })
    },
    handleRemoveImage() {
      if (!this.form.img) {
        return
      }
      this.$confirm('确认删除当前房型图片吗？', '提示', { type: 'warning' })
        .then(() => {
          if (this.isEdit && this.form.id) {
            this.removingImage = true
            return deleteRoomTypeImage(this.form.id)
              .then(() => {
                this.form.img = ''
                this.$message.success('图片已删除')
              })
              .finally(() => {
                this.removingImage = false
              })
          }
          this.form.img = ''
          this.$message.success('图片已删除')
        })
        .catch(() => {})
    },
    handleSubmit() {
      this.$refs.formRef.validate(valid => {
        if (!valid) {
          return false
        }
        const payload = this.buildPayload()
        this.submitting = true
        const request = this.isEdit
          ? updateRoomType(this.form.id, payload)
          : createRoomType(payload)
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
    handleDelete(row) {
      this.$confirm(`确认删除客房类型【${row.typeName}】吗？`, '提示', { type: 'warning' })
        .then(() => deleteRoomType(row.id))
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
  margin-left: 8px;
}

.type-image {
  width: 90px;
  height: 60px;
  border-radius: 6px;
  border: 1px solid #d9e6e2;
}

.form-image {
  margin-right: 10px;
}

.image-empty {
  color: #909399;
  font-size: 12px;
}

.image-uploader-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
