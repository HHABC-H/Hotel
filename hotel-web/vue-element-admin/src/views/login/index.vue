<template>
  <div class="mall-page">
    <header class="topbar">
      <div class="brand-wrap">
        <div class="brand-title">绿洲酒店</div>
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

        <template v-if="!token">
          <el-button type="primary" icon="el-icon-user" @click="openLoginDialog">登录</el-button>
        </template>

        <template v-else-if="isClientLoggedIn">
          <span class="welcome-text">你好，{{ currentUserName }}</span>
          <el-button plain @click="openMyOrdersDialog">我的订单</el-button>
          <el-button plain @click="openProfileDialog">个人中心</el-button>
          <el-button type="danger" plain @click="handleLogout">退出</el-button>
        </template>

        <template v-else>
          <span class="welcome-text">你好，{{ currentUserName }}</span>
          <el-button type="primary" @click="goToBackOffice">进入后台</el-button>
          <el-button type="danger" plain @click="handleLogout">退出</el-button>
        </template>
      </div>
    </header>

    <section class="banner">
      <div class="banner-content">
        <h2>精选房型，在线选房</h2>
        <p>浏览房间详情、价格和房态，登录后可直接预订并支付。</p>
      </div>
    </section>

    <section class="room-section">
      <div class="section-head">
        <div class="section-title">全部房间</div>
        <div class="section-actions">
          <el-button size="mini" :type="searchMode === 'available' ? 'success' : 'default'" @click="showAvailableRooms">
            仅看空闲
          </el-button>
          <el-button size="mini" @click="showAllRooms">显示全部</el-button>
        </div>
      </div>

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
            <div class="room-actions">
              <el-button type="text" @click.stop="openDetailDialog(room)">查看详情</el-button>
              <el-button v-if="isClientLoggedIn" type="text" @click.stop="openBookingDialog(room)">立即预订</el-button>
              <el-button v-else type="text" @click.stop="openLoginDialog">登录后预订</el-button>
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
        <el-form-item label="用户名" prop="username"><el-input v-model="registerForm.username" /></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="registerForm.password" type="password" show-password /></el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="registerForm.confirmPassword" type="password" show-password /></el-form-item>
        <el-form-item label="真实姓名" prop="realName"><el-input v-model="registerForm.realName" /></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="registerForm.phone" /></el-form-item>
        <el-form-item label="身份证号" prop="idCard"><el-input v-model="registerForm.idCard" /></el-form-item>
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
        <el-button v-if="isClientLoggedIn" type="primary" @click="openBookingFromDetail">立即预订</el-button>
        <el-button v-else type="primary" @click="openLoginDialog">登录后预订</el-button>
      </span>
    </el-dialog>

    <el-dialog title="提交预订" :visible.sync="bookingDialogVisible" width="520px" @closed="handleBookingDialogClosed">
      <el-form ref="bookingFormRef" :model="bookingForm" :rules="bookingRules" label-width="100px">
        <el-form-item label="房间号"><el-input :value="selectedRoom ? selectedRoom.roomNumber : ''" disabled /></el-form-item>
        <el-form-item label="房型"><el-input :value="selectedRoom ? selectedRoom.roomTypeName : ''" disabled /></el-form-item>
        <el-form-item label="入住日期" prop="checkInDate">
          <el-date-picker v-model="bookingForm.checkInDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择入住日期" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="退房日期" prop="checkOutDate">
          <el-date-picker v-model="bookingForm.checkOutDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择退房日期" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="bookingForm.remark" type="textarea" :rows="3" placeholder="选填" /></el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="bookingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bookingSubmitting" @click="handleConfirmBooking">确认预订</el-button>
      </span>
    </el-dialog>

    <el-dialog title="订单支付" :visible.sync="payDialogVisible" width="520px">
      <el-alert type="info" :closable="false" title="预订已创建，请完成支付。支付完成后会自动刷新房间状态。" style="margin-bottom: 16px;" />
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单号">{{ payContext.orderNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="房间号">{{ payContext.roomNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="房型">{{ payContext.roomTypeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="入住日期">{{ payContext.checkInDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="退房日期">{{ payContext.checkOutDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="应付金额">{{ formatPrice(payContext.totalAmount) }}</el-descriptions-item>
      </el-descriptions>
      <span slot="footer" class="dialog-footer">
        <el-button :loading="cancelSubmitting" @click="handleCancelNow">取消订单</el-button>
        <el-button @click="payDialogVisible = false">稍后支付</el-button>
        <el-button type="primary" :loading="paySubmitting" @click="handlePayNow">立即支付</el-button>
      </span>
    </el-dialog>

    <el-dialog title="我的订单" :visible.sync="ordersDialogVisible" width="1080px">
      <el-table v-loading="ordersLoading" :data="ordersData" border>
        <el-table-column prop="orderNumber" label="订单号" min-width="190" />
        <el-table-column label="下单时间" min-width="170"><template slot-scope="scope">{{ formatDateTime(scope.row.createTime) }}</template></el-table-column>
        <el-table-column prop="roomNumber" label="房间号" min-width="110" />
        <el-table-column prop="roomTypeName" label="房型" min-width="140" />
        <el-table-column prop="checkInDate" label="入住日期" min-width="120" />
        <el-table-column prop="checkOutDate" label="退房日期" min-width="120" />
        <el-table-column label="总金额" min-width="110"><template slot-scope="scope">{{ formatPrice(scope.row.totalAmount) }}</template></el-table-column>
        <el-table-column label="状态" min-width="120"><template slot-scope="scope">{{ orderStatusLabel(scope.row.status) }}</template></el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" />
      </el-table>
      <span slot="footer" class="dialog-footer">
        <el-button @click="fetchMyOrders">刷新</el-button>
        <el-button @click="ordersDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>

    <el-dialog title="个人中心" :visible.sync="profileDialogVisible" width="560px" @open="fetchProfileData">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="100px" class="profile-form">
        <el-form-item label="用户名"><el-input v-model="profileForm.username" disabled /></el-form-item>
        <el-form-item label="角色"><el-input :value="roleLabel(profileForm.roleCode)" disabled /></el-form-item>
        <el-form-item label="账户余额">
          <div class="balance-row">
            <el-input :value="formatPrice(profileForm.balance)" disabled />
            <el-button type="primary" plain @click="openRechargeDialog">充值</el-button>
          </div>
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName"><el-input v-model="profileForm.realName" /></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="profileForm.phone" /></el-form-item>
        <el-form-item label="身份证号" prop="idCard"><el-input v-model="profileForm.idCard" /></el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="profileForm.gender" placeholder="请选择">
            <el-option label="男" value="M" />
            <el-option label="女" value="F" />
            <el-option label="未知" value="UNKNOWN" />
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="profileDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="profileSaving" @click="handleSaveProfile">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="账户充值" :visible.sync="rechargeDialogVisible" width="460px">
      <el-form label-width="100px">
        <el-form-item label="选择金额">
          <el-radio-group v-model="rechargeAmount">
            <el-radio-button v-for="item in rechargeOptions" :key="item" :label="item">
              {{ formatPrice(item) }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRechargeConfirm">确定</el-button>
      </span>
    </el-dialog>

    <el-dialog title="扫码充值" :visible.sync="rechargeQrDialogVisible" width="420px" :close-on-click-modal="false" :show-close="false">
      <div class="qr-box">
        <el-image class="qr-image" :src="rechargeQrCodeUrl" fit="contain">
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
import { getToken } from '@/utils/auth'
import { browseRooms, listAvailableRooms, getRoomDetailForClient } from '@/api/rooms'
import { register } from '@/api/auth'
import { createBooking, listMyBookings, payBooking, cancelBooking } from '@/api/bookings'
import { getProfile, rechargeProfile, updateProfile } from '@/api/profile'
import { getRoomStatusLabel, getOrderStatusLabel, getRoleLabel } from '@/constants/dict'
import { validIdCardCN, validPhoneCN } from '@/utils/validate'

const createLoginForm = () => ({ username: '', password: '' })
const createRegisterForm = () => ({ username: '', password: '', confirmPassword: '', realName: '', phone: '', idCard: '', gender: 'UNKNOWN' })
const createBookingForm = () => ({ roomId: undefined, checkInDate: '', checkOutDate: '', remark: '' })
const createProfileForm = () => ({ username: '', roleCode: '', balance: 0, realName: '', phone: '', idCard: '', gender: 'UNKNOWN' })

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
      searchMode: 'browse',
      detailDialogVisible: false,
      detailLoading: false,
      detailRoom: {},
      bookingDialogVisible: false,
      bookingSubmitting: false,
      selectedRoom: null,
      bookingForm: createBookingForm(),
      bookingRules: {
        checkInDate: [{ required: true, message: '请选择入住日期', trigger: 'change' }],
        checkOutDate: [{ required: true, message: '请选择退房日期', trigger: 'change' }]
      },
      payDialogVisible: false,
      paySubmitting: false,
      cancelSubmitting: false,
      payContext: { orderId: undefined, orderNumber: '', roomNumber: '', roomTypeName: '', checkInDate: '', checkOutDate: '', totalAmount: '' },
      ordersDialogVisible: false,
      ordersLoading: false,
      ordersData: [],
      orderActionLoading: {},
      profileDialogVisible: false,
      profileSaving: false,
      profileForm: createProfileForm(),
      rechargeDialogVisible: false,
      rechargeQrDialogVisible: false,
      rechargeOptions: [50, 100, 200, 500, 1000],
      rechargeAmount: 100,
      rechargeSecondsLeft: 3,
      rechargeSuccess: false,
      rechargeTimer: null,
      profileRules: {
        realName: [{ required: true, message: '请填写真实姓名', trigger: 'blur' }],
        phone: [{ required: true, trigger: 'blur', validator: validatePhone }],
        idCard: [{ trigger: 'blur', validator: validateIdCard }],
        gender: [{ required: true, message: '请选择性别', trigger: 'change' }]
      },
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
        password: [{ required: true, message: '请填写密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
        confirmPassword: [{ trigger: 'blur', validator: validatePasswordConfirm }],
        realName: [{ required: true, message: '请填写真实姓名', trigger: 'blur' }],
        phone: [{ trigger: 'blur', validator: validatePhone }],
        idCard: [{ trigger: 'blur', validator: validateIdCard }],
        gender: [{ required: true, message: '请选择性别', trigger: 'change' }]
      }
    }
  },
  computed: {
    token() {
      return this.$store.getters.token
    },
    userRole() {
      const roles = this.$store.getters.roles || []
      return roles[0] || ''
    },
    isClientLoggedIn() {
      return !!this.token && this.userRole === 'CLIENT'
    },
    currentUserName() {
      const userInfo = (this.$store.state.user && this.$store.state.user.userInfo) || {}
      return userInfo.realName || userInfo.username || this.$store.getters.name || '用户'
    },
    rechargeQrCodeUrl() {
      const content = `hotel-recharge:${this.profileForm.username || 'client'}:${this.rechargeAmount}`
      return `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(content)}`
    },
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
  created() {
    this.fetchRooms()
    this.ensureUserInfoLoaded()
  },
  beforeDestroy() {
    this.clearRechargeTimer()
  },
  methods: {
    async ensureUserInfoLoaded() {
      if (!getToken()) {
        return
      }
      if ((this.$store.getters.roles || []).length > 0) {
        return
      }
      try {
        await this.$store.dispatch('user/getInfo')
      } catch (e) {
        await this.$store.dispatch('user/resetToken')
      }
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
        const response = this.searchMode === 'available' ? await listAvailableRooms() : await browseRooms()
        this.roomList = this.normalizeListData(response.data)
      } catch (e) {
        this.$message.error(e && e.message ? e.message : '加载房间失败')
      } finally {
        this.roomLoading = false
      }
    },
    showAvailableRooms() {
      this.searchMode = 'available'
      this.fetchRooms()
    },
    showAllRooms() {
      this.searchMode = 'browse'
      this.fetchRooms()
    },
    roomImage(room) {
      if (!room) {
        return fallbackRoomImages[0]
      }
      if (room.roomTypeImg) {
        return room.roomTypeImg
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
    orderStatusLabel(value) {
      return getOrderStatusLabel(value)
    },
    roleLabel(value) {
      return getRoleLabel(value)
    },
    statusTagType(status) {
      if (status === 'OCCUPIED') return 'danger'
      if (status === 'RESERVED') return 'warning'
      if (status === 'MAINTENANCE') return 'info'
      return 'success'
    },
    formatPrice(value) {
      if (value === undefined || value === null || value === '') {
        return '-'
      }
      const num = Number(value)
      return Number.isNaN(num) ? value : `¥${num.toFixed(2)}`
    },
    formatDateTime(value) {
      if (!value) {
        return '-'
      }
      return String(value).replace('T', ' ')
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
      this.$refs.loginFormRef.validate(async valid => {
        if (!valid) return false
        this.loginLoading = true
        try {
          await this.$store.dispatch('user/login', this.loginForm)
          const info = await this.$store.dispatch('user/getInfo')
          const role = (info.roles && info.roles[0]) || ''
          this.$message.success('登录成功')
          this.loginDialogVisible = false
          if (role === 'CLIENT') {
            this.fetchRooms()
            return
          }
          this.$router.push('/')
        } finally {
          this.loginLoading = false
        }
      })
    },
    handleRegister() {
      this.$refs.registerFormRef.validate(async valid => {
        if (!valid) return false
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
        this.$message.error(e && e.message ? e.message : '加载房间详情失败')
      } finally {
        this.detailLoading = false
      }
    },
    openBookingFromDetail() {
      if (!this.detailRoom || !this.detailRoom.id) {
        return
      }
      this.detailDialogVisible = false
      this.openBookingDialog(this.detailRoom)
    },
    openBookingDialog(room) {
      if (!this.isClientLoggedIn) {
        this.$message.warning('请先登录顾客账号再预订')
        this.openLoginDialog()
        return
      }
      if (room.status && room.status !== 'AVAILABLE') {
        this.$message.warning('该房间当前不可预订，请选择空闲房间')
        return
      }
      this.selectedRoom = room
      this.bookingForm = { roomId: room.id, checkInDate: '', checkOutDate: '', remark: '' }
      this.bookingDialogVisible = true
    },
    handleBookingDialogClosed() {
      this.bookingSubmitting = false
      if (this.$refs.bookingFormRef) {
        this.$refs.bookingFormRef.clearValidate()
      }
    },
    isValidDateRange() {
      if (!this.bookingForm.checkInDate || !this.bookingForm.checkOutDate) {
        return false
      }
      return this.bookingForm.checkInDate < this.bookingForm.checkOutDate
    },
    extractOrderFromBookingResponse(data) {
      const payload = (data && data.order) || data || {}
      return {
        orderId: payload.id || payload.orderId || undefined,
        orderNumber: payload.orderNumber || '',
        roomNumber: payload.roomNumber || (this.selectedRoom && this.selectedRoom.roomNumber) || '-',
        roomTypeName: payload.roomTypeName || (this.selectedRoom && this.selectedRoom.roomTypeName) || '-',
        checkInDate: payload.checkInDate || this.bookingForm.checkInDate,
        checkOutDate: payload.checkOutDate || this.bookingForm.checkOutDate,
        totalAmount: payload.totalAmount || (this.selectedRoom && (this.selectedRoom.referencePrice || this.selectedRoom.price)) || ''
      }
    },
    handleConfirmBooking() {
      this.$refs.bookingFormRef.validate(async valid => {
        if (!valid) return false
        if (!this.isValidDateRange()) {
          this.$message.warning('退房日期必须晚于入住日期')
          return
        }
        this.bookingSubmitting = true
        try {
          const res = await createBooking(this.bookingForm)
          this.payContext = this.extractOrderFromBookingResponse(res.data)
          this.bookingDialogVisible = false
          this.payDialogVisible = true
          this.$message.success('预订成功')
          this.fetchMyOrders()
        } finally {
          this.bookingSubmitting = false
        }
      })
    },
    handlePayNow() {
      if (!this.payContext.orderId) {
        this.$message.warning('未定位到订单ID，请在我的订单中完成支付')
        return
      }
      this.paySubmitting = true
      payBooking(this.payContext.orderId)
        .then(() => {
          this.$message.success('支付成功，房间状态已刷新')
          this.payDialogVisible = false
          this.searchMode = 'available'
          this.fetchRooms()
          this.fetchMyOrders()
        })
        .finally(() => {
          this.paySubmitting = false
        })
    },
    handleCancelNow() {
      if (!this.payContext.orderId) {
        this.$message.warning('未定位到订单ID，无法取消')
        return
      }
      this.cancelSubmitting = true
      cancelBooking(this.payContext.orderId)
        .then(() => {
          this.$message.success('订单已取消')
          this.payDialogVisible = false
          this.fetchRooms()
          this.fetchMyOrders()
        })
        .finally(() => {
          this.cancelSubmitting = false
        })
    },
    async openMyOrdersDialog() {
      if (!this.isClientLoggedIn) {
        this.$message.warning('请先登录顾客账号')
        this.openLoginDialog()
        return
      }
      this.ordersDialogVisible = true
      await this.fetchMyOrders()
    },
    async fetchMyOrders() {
      if (!this.isClientLoggedIn) {
        return
      }
      this.ordersLoading = true
      try {
        const res = await listMyBookings()
        const records = this.normalizeListData(res.data)
        this.ordersData = await this.enrichOrderRoomInfo(records)
      } finally {
        this.ordersLoading = false
      }
    },
    async enrichOrderRoomInfo(records) {
      if (!records.length) {
        return records
      }
      const roomIds = Array.from(new Set(records.map(item => item.roomId).filter(Boolean)))
      const roomMap = {}
      await Promise.all(roomIds.map(async roomId => {
        try {
          const res = await getRoomDetailForClient(roomId)
          roomMap[roomId] = res.data || {}
        } catch (e) {
          roomMap[roomId] = {}
        }
      }))
      return records.map(item => {
        const room = roomMap[item.roomId] || {}
        return {
          ...item,
          roomNumber: item.roomNumber || room.roomNumber || '-',
          roomTypeName: item.roomTypeName || room.roomTypeName || '-'
        }
      })
    },
    isOrderPayable(row) {
      return row.status === 'UNPAID'
    },
    isOrderCancelable(row) {
      return ['UNPAID', 'PAID'].includes(row.status) && this.isCheckoutDateNotPassed(row.checkOutDate)
    },
    isCheckoutDateNotPassed(checkOutDate) {
      if (!checkOutDate) {
        return false
      }
      const endDate = new Date(`${checkOutDate}T23:59:59`)
      return Date.now() <= endDate.getTime()
    },
    setOrderActionLoading(orderId, action) {
      this.$set(this.orderActionLoading, orderId, action)
    },
    clearOrderActionLoading(orderId) {
      this.$delete(this.orderActionLoading, orderId)
    },
    handleOrderPay(row) {
      this.setOrderActionLoading(row.id, 'pay')
      payBooking(row.id)
        .then(() => {
          this.$message.success('支付成功')
          this.fetchMyOrders()
          this.fetchRooms()
        })
        .finally(() => {
          this.clearOrderActionLoading(row.id)
        })
    },
    handleOrderCancel(row) {
      this.$confirm('确认取消该订单吗？', '提示', { type: 'warning' })
        .then(() => {
          this.setOrderActionLoading(row.id, 'cancel')
          return cancelBooking(row.id)
        })
        .then(() => {
          this.$message.success('订单已取消')
          this.fetchMyOrders()
          this.fetchRooms()
        })
        .finally(() => {
          this.clearOrderActionLoading(row.id)
        })
    },
    openProfileDialog() {
      if (!this.isClientLoggedIn) {
        this.$message.warning('请先登录顾客账号')
        this.openLoginDialog()
        return
      }
      this.profileDialogVisible = true
    },
    async fetchProfileData() {
      if (!this.isClientLoggedIn) {
        return
      }
      const res = await getProfile()
      const data = res.data || {}
      this.profileForm = {
        username: data.username || '',
        roleCode: data.role || '',
        balance: Number(data.balance || 0),
        realName: data.realName || '',
        phone: data.phone || '',
        idCard: data.idCard || '',
        gender: data.gender || 'UNKNOWN'
      }
      this.$nextTick(() => {
        if (this.$refs.profileFormRef) {
          this.$refs.profileFormRef.clearValidate()
        }
      })
    },
    handleSaveProfile() {
      this.$refs.profileFormRef.validate(valid => {
        if (!valid) return false
        const payload = {
          realName: this.profileForm.realName,
          phone: this.profileForm.phone,
          idCard: this.profileForm.idCard,
          gender: this.profileForm.gender
        }
        this.profileSaving = true
        updateProfile(payload)
          .then(async() => {
            this.$message.success('个人信息已更新')
            await this.$store.dispatch('user/getInfo')
            this.fetchProfileData()
          })
          .finally(() => {
            this.profileSaving = false
          })
      })
    },
    openRechargeDialog() {
      this.rechargeAmount = this.rechargeOptions[0]
      this.rechargeDialogVisible = true
    },
    handleRechargeConfirm() {
      this.rechargeDialogVisible = false
      this.profileDialogVisible = false
      this.rechargeQrDialogVisible = true
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
      rechargeProfile({ amount: this.rechargeAmount })
        .then(res => {
          const data = res.data || {}
          this.profileForm.balance = Number(data.balance || this.profileForm.balance)
          this.rechargeSuccess = true
          this.$message.success('充值成功')
        })
        .catch(() => {
          this.rechargeQrDialogVisible = false
          this.profileDialogVisible = true
        })
    },
    handleRechargeCancel() {
      this.clearRechargeTimer()
      this.rechargeQrDialogVisible = false
      this.rechargeSuccess = false
      this.rechargeSecondsLeft = 3
      this.profileDialogVisible = true
    },
    handleRechargeBack() {
      this.rechargeQrDialogVisible = false
      this.rechargeSuccess = false
      this.rechargeSecondsLeft = 3
      this.profileDialogVisible = true
      this.fetchProfileData()
    },
    clearRechargeTimer() {
      if (this.rechargeTimer) {
        clearInterval(this.rechargeTimer)
        this.rechargeTimer = null
      }
    },
    async handleLogout() {
      await this.$store.dispatch('user/logout')
      this.ordersDialogVisible = false
      this.profileDialogVisible = false
      this.rechargeDialogVisible = false
      this.rechargeQrDialogVisible = false
      this.clearRechargeTimer()
      this.payDialogVisible = false
      this.bookingDialogVisible = false
      this.$message.success('已退出登录')
      this.$router.replace('/login')
      this.fetchRooms()
    },
    goToBackOffice() {
      this.$router.push('/')
    }
  }
}
</script>

<style lang="scss" scoped>
.mall-page {
  min-height: 100%;
  background: linear-gradient(180deg, #f2f6fb 0%, #e8eef6 100%);
  padding-bottom: 32px;
}

.topbar {
  height: 74px;
  background: rgba(8, 16, 28, 0.84);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 20px rgba(5, 14, 24, 0.28);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  position: sticky;
  top: 0;
  z-index: 10;
}

.brand-wrap {
  .brand-title {
    font-size: 30px;
    font-weight: 800;
    color: #f97316;
    line-height: 1.1;
  }

  .brand-subtitle {
    color: rgba(240, 249, 255, 0.72);
    font-size: 13px;
    margin-top: 4px;
  }
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 320px;

  /deep/ .el-input__inner {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(255, 255, 255, 0.2);
    color: #f8fafc;
  }

  /deep/ .el-input__inner::placeholder {
    color: rgba(241, 245, 249, 0.65);
  }
}

.welcome-text {
  color: #e2e8f0;
  font-size: 13px;
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

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #102a43;
}

.section-actions {
  display: flex;
  gap: 8px;
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
  padding: 12px 14px 12px;
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

.room-actions {
  margin-top: 6px;
  border-top: 1px solid #f0f2f5;
  padding-top: 6px;
  display: flex;
  justify-content: space-between;
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

.profile-form {
  max-width: 460px;
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

@media (max-width: 1160px) {
  .topbar {
    height: auto;
    padding: 14px 16px;
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .topbar-right {
    width: 100%;
    flex-wrap: wrap;

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

  .section-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .detail-wrap {
    grid-template-columns: 1fr;
  }
}
</style>
