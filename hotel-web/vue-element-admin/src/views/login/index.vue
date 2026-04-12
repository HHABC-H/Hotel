<template>
  <div class="mall-page">
    <header class="topbar">
      <div class="brand-wrap">
        <div class="brand-title">秋暮酒店</div>
        <div class="brand-subtitle">品质客房 · 在线预订</div>
      </div>
      <div class="topbar-right">
        <el-input
          v-model="keyword"
          class="search-input"
          clearable
          placeholder="搜索房间号 / 房型"
          prefix-icon="el-icon-search"
        />
        <el-button type="primary" icon="el-icon-user" @click="openLoginDialog">登录</el-button>
      </div>
    </header>

    <section class="banner">
      <div class="banner-content">
        <h2>精选房型，在线选房</h2>
        <p>浏览房间详情、价格和房态，登录后即可下单预订。</p>
      </div>
    </section>

    <section class="room-section">
      <div class="section-title">客房推荐</div>
      <div v-if="roomLoading" class="loading-wrap">
        <i class="el-icon-loading" />
        <span>正在加载房间信息...</span>
      </div>
      <div v-else class="room-grid">
        <div
          v-for="room in filteredRooms"
          :key="room.id"
          class="room-card"
          @click="openDetailDialog(room)"
        >
          <img :src="roomImage(room)" class="room-cover" alt="room">
          <div class="room-body">
            <div class="room-title">{{ room.roomTypeName || '未命名房型' }}</div>
            <div class="room-subtitle">房间 {{ room.roomNumber || '-' }}</div>
            <div class="room-meta">
              <span>{{ room.roomTypeBedType || '标准床型' }}</span>
              <span>{{ room.roomTypeCapacity || '-' }}人可住</span>
              <span>{{ room.roomTypeArea || '-' }}</span>
            </div>
            <div class="room-footer">
              <div class="room-price">{{ formatPrice(room.referencePrice || room.price) }}<span>/晚</span></div>
              <el-tag size="mini" :type="statusTagType(room.status)">{{ roomStatusLabel(room.status) }}</el-tag>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!roomLoading && !filteredRooms.length" class="empty-wrap">
        暂无符合条件的房间
      </div>
    </section>

    <el-dialog title="登录" :visible.sync="loginDialogVisible" width="420px" @closed="resetLoginForm">
      <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="72px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            autocomplete="current-password"
            show-password
          />
        </el-form-item>
      </el-form>
      <div class="switch-tip">
        若没有账号，可点击
        <span class="link-btn" @click="openRegisterFromLogin">注册</span>
      </div>
      <span slot="footer">
        <el-button @click="loginDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loginLoading" @click="handleLogin">登录</el-button>
      </span>
    </el-dialog>

    <el-dialog title="注册账号" :visible.sync="registerDialogVisible" width="520px" @closed="resetRegisterForm">
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="92px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="registerForm.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="registerForm.idCard" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="registerForm.gender" placeholder="请选择">
            <el-option label="男" value="M" />
            <el-option label="女" value="F" />
            <el-option label="未知" value="UNKNOWN" />
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegister">注册</el-button>
      </span>
    </el-dialog>

    <el-dialog title="房间详情" :visible.sync="detailDialogVisible" width="760px">
      <el-skeleton v-if="detailLoading" :rows="6" animated />
      <div v-else class="detail-wrap">
        <img :src="roomImage(detailRoom)" class="detail-cover" alt="room-detail">
        <div class="detail-info">
          <h3>{{ detailRoom.roomTypeName || '未命名房型' }}（房间 {{ detailRoom.roomNumber || '-' }}）</h3>
          <p><strong>参考价格：</strong>{{ formatPrice(detailRoom.referencePrice || detailRoom.price) }}/晚</p>
          <p><strong>楼层：</strong>{{ detailRoom.floor || '-' }}</p>
          <p><strong>床型：</strong>{{ detailRoom.roomTypeBedType || '-' }}</p>
          <p><strong>可住人数：</strong>{{ detailRoom.roomTypeCapacity || '-' }} 人</p>
          <p><strong>面积：</strong>{{ detailRoom.roomTypeArea || '-' }}</p>
          <p><strong>状态：</strong>{{ roomStatusLabel(detailRoom.status) }}</p>
          <p class="desc"><strong>简介：</strong>{{ detailRoom.roomTypeDescription || '暂无简介' }}</p>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="openLoginDialog">登录后预订</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { browseRooms, getRoomDetailForClient } from '@/api/rooms'
import { register } from '@/api/auth'
import { getRoomStatusLabel } from '@/constants/dict'
import { validIdCardCN, validPhoneCN } from '@/utils/validate'

const createLoginForm = () => ({
  username: '',
  password: ''
})

const createRegisterForm = () => ({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  idCard: '',
  gender: 'UNKNOWN'
})

const fallbackRoomImages = [
  'https://images.unsplash.com/photo-1631049552057-403cdb8f0658?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1616594039964-3d5d6b43f6b9?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=1200&q=80'
]

export default {
  name: 'Login',
  data() {
    const validatePhone = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请填写手机号'))
        return
      }
      if (!validPhoneCN(value)) {
        callback(new Error('手机号格式不正确'))
        return
      }
      callback()
    }

    const validatePasswordConfirm = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请确认密码'))
        return
      }
      if (value !== this.registerForm.password) {
        callback(new Error('两次密码不一致'))
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
      roomLoading: false,
      roomList: [],
      keyword: '',

      detailDialogVisible: false,
      detailLoading: false,
      detailRoom: {},

      loginDialogVisible: false,
      loginLoading: false,
      registerDialogVisible: false,
      registerLoading: false,

      loginForm: createLoginForm(),
      registerForm: createRegisterForm(),

      loginRules: {
        username: [{ required: true, message: '请填写用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请填写密码', trigger: 'blur' }]
      },
      registerRules: {
        username: [{ required: true, message: '请填写用户名', trigger: 'blur' }],
        password: [
          { required: true, message: '请填写密码', trigger: 'blur' },
          { min: 6, message: '密码至少6位', trigger: 'blur' }
        ],
        confirmPassword: [{ trigger: 'blur', validator: validatePasswordConfirm }],
        realName: [{ required: true, message: '请填写真实姓名', trigger: 'blur' }],
        phone: [{ trigger: 'blur', validator: validatePhone }],
        idCard: [{ trigger: 'blur', validator: validateIdCard }],
        gender: [{ required: true, message: '请选择性别', trigger: 'change' }]
      },

      redirect: undefined,
      otherQuery: {}
    }
  },
  computed: {
    filteredRooms() {
      const keyword = this.keyword.trim().toLowerCase()
      if (!keyword) {
        return this.roomList
      }
      return this.roomList.filter(item => {
        const roomNumber = String(item.roomNumber || '').toLowerCase()
        const roomTypeName = String(item.roomTypeName || '').toLowerCase()
        return roomNumber.includes(keyword) || roomTypeName.includes(keyword)
      })
    }
  },
  watch: {
    $route: {
      handler(route) {
        const query = route.query
        if (query) {
          this.redirect = query.redirect
          this.otherQuery = this.getOtherQuery(query)
        }
      },
      immediate: true
    }
  },
  created() {
    this.fetchRooms()
  },
  methods: {
    getOtherQuery(query) {
      return Object.keys(query).reduce((acc, cur) => {
        if (cur !== 'redirect') {
          acc[cur] = query[cur]
        }
        return acc
      }, {})
    },
    normalizeListData(data) {
      if (Array.isArray(data)) {
        return data
      }
      if (data && Array.isArray(data.records)) {
        return data.records
      }
      return []
    },
    async fetchRooms() {
      this.roomLoading = true
      try {
        const res = await browseRooms()
        this.roomList = this.normalizeListData(res.data)
      } catch (e) {
        this.$message.error(e?.message || '加载房间失败')
      } finally {
        this.roomLoading = false
      }
    },
    roomImage(room) {
      if (!room) {
        return fallbackRoomImages[0]
      }
      if (room.coverUrl) {
        return room.coverUrl
      }
      if (room.imageUrl) {
        return room.imageUrl
      }
      const key = Number(room.id || room.roomNumber || 0)
      return fallbackRoomImages[key % fallbackRoomImages.length]
    },
    roomStatusLabel(value) {
      return getRoomStatusLabel(value)
    },
    statusTagType(status) {
      if (status === 'OCCUPIED') {
        return 'danger'
      }
      if (status === 'RESERVED') {
        return 'warning'
      }
      if (status === 'MAINTENANCE') {
        return 'info'
      }
      return 'success'
    },
    formatPrice(value) {
      if (value === undefined || value === null || value === '') {
        return '-'
      }
      const num = Number(value)
      return Number.isNaN(num) ? value : `¥${num.toFixed(2)}`
    },
    openLoginDialog() {
      this.loginDialogVisible = true
    },
    openRegisterFromLogin() {
      this.loginDialogVisible = false
      this.registerDialogVisible = true
    },
    resetLoginForm() {
      this.loginLoading = false
      this.loginForm = createLoginForm()
      if (this.$refs.loginFormRef) {
        this.$refs.loginFormRef.clearValidate()
      }
    },
    resetRegisterForm() {
      this.registerLoading = false
      this.registerForm = createRegisterForm()
      if (this.$refs.registerFormRef) {
        this.$refs.registerFormRef.clearValidate()
      }
    },
    handleLogin() {
      this.$refs.loginFormRef.validate(valid => {
        if (!valid) {
          return false
        }
        this.loginLoading = true
        this.$store.dispatch('user/login', this.loginForm)
          .then(() => {
            this.$message.success('登录成功')
            this.loginDialogVisible = false
            this.$router.push({ path: this.redirect || '/', query: this.otherQuery })
          })
          .finally(() => {
            this.loginLoading = false
          })
      })
    },
    handleRegister() {
      this.$refs.registerFormRef.validate(async valid => {
        if (!valid) {
          return false
        }
        this.registerLoading = true
        try {
          await register({
            username: this.registerForm.username,
            password: this.registerForm.password,
            realName: this.registerForm.realName,
            phone: this.registerForm.phone,
            idCard: this.registerForm.idCard || null,
            gender: this.registerForm.gender
          })
          this.$message.success('注册成功，请登录')
          this.registerDialogVisible = false
          this.loginDialogVisible = true
          this.loginForm.username = this.registerForm.username
        } finally {
          this.registerLoading = false
        }
      })
    },
    async openDetailDialog(room) {
      this.detailDialogVisible = true
      this.detailLoading = true
      try {
        const res = await getRoomDetailForClient(room.id)
        this.detailRoom = Object.assign({}, room, res.data || {})
      } catch (e) {
        this.$message.error(e?.message || '加载房间详情失败')
      } finally {
        this.detailLoading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.mall-page {
  min-height: 100%;
  background: #f7f7f7;
  padding-bottom: 32px;
}

.topbar {
  height: 72px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
}

.brand-wrap {
  .brand-title {
    font-size: 30px;
    font-weight: 800;
    color: #ff5b1f;
    line-height: 1.1;
  }

  .brand-subtitle {
    color: #909399;
    font-size: 13px;
    margin-top: 4px;
  }
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 320px;
}

.banner {
  margin: 20px 32px 0;
  border-radius: 16px;
  height: 180px;
  background: linear-gradient(120deg, #ff7a18, #ffb347);
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    right: -120px;
    top: -80px;
    width: 320px;
    height: 320px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 50%;
  }

  .banner-content {
    position: relative;
    z-index: 1;
    color: #fff;
    padding: 36px 40px;

    h2 {
      margin: 0;
      font-size: 32px;
    }

    p {
      margin-top: 10px;
      font-size: 15px;
      opacity: 0.95;
    }
  }
}

.room-section {
  margin: 24px 32px 0;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 18px;
}

.loading-wrap,
.empty-wrap {
  height: 140px;
  background: #fff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  gap: 8px;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.room-card {
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 10px 24px rgba(0, 0, 0, 0.12);
  }
}

.room-cover {
  width: 100%;
  height: 150px;
  object-fit: cover;
  background: #f3f3f3;
}

.room-body {
  padding: 12px 14px 14px;
}

.room-title {
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.room-subtitle {
  color: #909399;
  margin-top: 4px;
  font-size: 13px;
}

.room-meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: #606266;
  font-size: 12px;

  span {
    background: #f5f7fa;
    border-radius: 10px;
    padding: 3px 8px;
  }
}

.room-footer {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.room-price {
  color: #ff4d1f;
  font-size: 24px;
  font-weight: 700;

  span {
    font-size: 13px;
    font-weight: 400;
    margin-left: 2px;
  }
}

.switch-tip {
  color: #606266;
  font-size: 13px;
}

.link-btn {
  color: #409eff;
  cursor: pointer;
}

.detail-wrap {
  display: grid;
  grid-template-columns: 44% 1fr;
  gap: 18px;
}

.detail-cover {
  width: 100%;
  border-radius: 10px;
  height: 290px;
  object-fit: cover;
  background: #f3f3f3;
}

.detail-info {
  h3 {
    margin: 0 0 12px;
    font-size: 20px;
    color: #303133;
  }

  p {
    margin: 8px 0;
    color: #606266;
    line-height: 1.55;
  }

  .desc {
    margin-top: 10px;
    background: #f7f8fa;
    border-radius: 8px;
    padding: 10px;
    color: #606266;
  }
}

@media (max-width: 960px) {
  .topbar {
    height: auto;
    padding: 14px 16px;
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .topbar-right {
    width: 100%;

    .search-input {
      width: 100%;
    }
  }

  .banner {
    margin: 14px 16px 0;
    height: auto;

    .banner-content {
      padding: 20px;

      h2 {
        font-size: 22px;
      }
    }
  }

  .room-section {
    margin: 16px;
  }

  .detail-wrap {
    grid-template-columns: 1fr;
  }
}
</style>
