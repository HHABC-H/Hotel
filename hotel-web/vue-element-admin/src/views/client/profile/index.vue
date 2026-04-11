<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header">个人资料</div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="profile-form">
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-input :value="roleLabel(form.roleCode)" disabled />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
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
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
          <el-button @click="fetchProfileData">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getProfile, updateProfile } from '@/api/profile'
import { validPhoneCN, validIdCardCN } from '@/utils/validate'
import { getRoleLabel } from '@/constants/dict'

export default {
  name: 'ClientProfileIndex',
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
    return {
      saving: false,
      form: {
        username: '',
        roleCode: '',
        realName: '',
        phone: '',
        idCard: '',
        gender: 'UNKNOWN'
      },
      rules: {
        realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
        phone: [{ required: true, trigger: 'blur', validator: validatePhone }],
        idCard: [{ trigger: 'blur', validator: validateIdCard }],
        gender: [{ required: true, message: '请选择性别', trigger: 'change' }]
      }
    }
  },
  created() {
    this.fetchProfileData()
  },
  methods: {
    fetchProfileData() {
      getProfile().then(res => {
        const data = res.data || {}
        this.form = {
          username: data.username || '',
          roleCode: data.role || '',
          realName: data.realName || '',
          phone: data.phone || '',
          idCard: data.idCard || '',
          gender: data.gender || 'UNKNOWN'
        }
      })
    },
    handleSave() {
      this.$refs.formRef.validate(valid => {
        if (!valid) {
          return false
        }

        const payload = {
          realName: this.form.realName,
          phone: this.form.phone,
          idCard: this.form.idCard,
          gender: this.form.gender
        }

        this.saving = true
        updateProfile(payload)
          .then(() => {
            this.$message.success('更新成功')
            this.fetchProfileData()
          })
          .finally(() => {
            this.saving = false
          })
      })
    },
    roleLabel(value) {
      return getRoleLabel(value)
    }
  }
}
</script>

<style lang="scss" scoped>
.profile-form {
  max-width: 560px;
}
</style>
