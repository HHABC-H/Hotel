<template>
  <div class="app-container oasis-page">
    <div class="page-toolbar">
      <div class="toolbar-title">
        <h2>个人中心</h2>
        <p>维护个人资料、账户余额与充值记录</p>
      </div>
      <el-button type="primary" plain @click="fetchProfileData">刷新资料</el-button>
    </div>

    <el-card shadow="never" class="content-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="profile-form">
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-input :value="roleLabel(form.roleCode)" disabled />
        </el-form-item>
        <el-form-item label="账户余额">
          <div class="balance-row">
            <el-input :value="formatCurrency(form.balance)" disabled />
            <el-button type="primary" plain @click="openRechargeDialog">充值</el-button>
          </div>
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
          <el-button @click="fetchProfileData">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog title="账户充值" :visible.sync="rechargeDialogVisible" width="460px">
      <el-form label-width="100px">
        <el-form-item label="选择金额">
          <el-radio-group v-model="rechargeAmount">
            <el-radio-button v-for="item in rechargeOptions" :key="item" :label="item">
              {{ formatCurrency(item) }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRechargeConfirm">确定</el-button>
      </span>
    </el-dialog>

    <el-dialog title="扫码充值" :visible.sync="qrDialogVisible" width="420px" :close-on-click-modal="false" :show-close="false">
      <div class="qr-box">
        <el-image class="qr-image" :src="qrCodeUrl" fit="contain">
          <div slot="error" class="qr-fallback">二维码加载失败，请稍后重试</div>
        </el-image>
        <p v-if="!rechargeSuccess" class="qr-tip">请扫码支付，{{ rechargeSecondsLeft }} 秒后自动到账</p>
        <p v-else class="qr-tip success">充值成功</p>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="handleRechargeCancel">取消</el-button>
        <el-button type="primary" :disabled="!rechargeSuccess" @click="handleRechargeBack">返回</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getProfile, rechargeProfile, updateProfile } from '@/api/profile'
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
        balance: 0,
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
      },
      rechargeDialogVisible: false,
      qrDialogVisible: false,
      rechargeOptions: [50, 100, 200, 500, 1000],
      rechargeAmount: 100,
      rechargeSecondsLeft: 3,
      rechargeSuccess: false,
      rechargeTimer: null
    }
  },
  computed: {
    qrCodeUrl() {
      const content = `hotel-recharge:${this.form.username || 'user'}:${this.rechargeAmount}`
      return `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(content)}`
    }
  },
  created() {
    this.fetchProfileData()
  },
  beforeDestroy() {
    this.clearRechargeTimer()
  },
  methods: {
    fetchProfileData() {
      getProfile().then(res => {
        const data = res.data || {}
        this.form = {
          username: data.username || '',
          roleCode: data.role || '',
          balance: Number(data.balance || 0),
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
    openRechargeDialog() {
      this.rechargeAmount = this.rechargeOptions[0]
      this.rechargeDialogVisible = true
    },
    handleRechargeConfirm() {
      this.rechargeDialogVisible = false
      this.qrDialogVisible = true
      this.rechargeSuccess = false
      this.rechargeSecondsLeft = 3
      this.clearRechargeTimer()
      this.rechargeTimer = setInterval(() => {
        if (this.rechargeSecondsLeft > 1) {
          this.rechargeSecondsLeft -= 1
          return
        }
        this.clearRechargeTimer()
        this.finishRecharge()
      }, 1000)
    },
    finishRecharge() {
      rechargeProfile({ amount: this.rechargeAmount }).then(res => {
        const data = res.data || {}
        this.form.balance = Number(data.balance || this.form.balance)
        this.rechargeSuccess = true
        this.$message.success('充值成功')
      })
    },
    handleRechargeCancel() {
      this.clearRechargeTimer()
      this.qrDialogVisible = false
      this.rechargeSuccess = false
      this.rechargeSecondsLeft = 3
    },
    handleRechargeBack() {
      this.qrDialogVisible = false
      this.rechargeSuccess = false
      this.rechargeSecondsLeft = 3
    },
    clearRechargeTimer() {
      if (this.rechargeTimer) {
        clearInterval(this.rechargeTimer)
        this.rechargeTimer = null
      }
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

.profile-form {
  max-width: 620px;
}

.balance-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.qr-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0 4px;
}

.qr-image {
  width: 220px;
  height: 220px;
  border: 1px solid #dcdfe6;
}

.qr-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 13px;
}

.qr-tip {
  margin-top: 12px;
  color: #606266;
}

.qr-tip.success {
  color: #67c23a;
  font-weight: 600;
}
</style>
