<template>
  <div class="dashboard-page">
    <div class="ops-bar">
      <div class="ops-left">
        <h2>绿洲酒店 · 管理主页</h2>
        <p>当前用户：{{ name || '未命名用户' }}，角色：{{ rolesText }}</p>
      </div>
      <div class="ops-right">
        <el-input
          v-model="moduleKeyword"
          clearable
          placeholder="搜索功能模块"
          prefix-icon="el-icon-search"
          class="module-search"
        />
        <el-button type="primary" :loading="loading" @click="fetchDashboard">刷新数据</el-button>
      </div>
    </div>

    <div class="kpi-grid">
      <el-card shadow="hover" class="kpi-card">
        <div class="kpi-label">总收入金额</div>
        <div class="kpi-value">{{ formatAmount(stats.totalRevenueAmount) }}</div>
      </el-card>
      <el-card shadow="hover" class="kpi-card">
        <div class="kpi-label">累计入住人数</div>
        <div class="kpi-value">{{ stats.accumulatedCheckInCount }}</div>
      </el-card>
      <el-card shadow="hover" class="kpi-card">
        <div class="kpi-label">总订单数</div>
        <div class="kpi-value">{{ stats.totalOrderCount }}</div>
      </el-card>
    </div>

    <div class="module-grid">
      <el-card v-for="item in filteredModules" :key="item.key" shadow="hover" class="module-card">
        <div class="module-header">
          <i :class="item.icon" />
          <span>{{ item.title }}</span>
        </div>
        <p class="module-desc">{{ item.desc }}</p>
        <el-button type="text" @click="goModule(item.path)">进入模块</el-button>
      </el-card>
    </div>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <el-card v-loading="loading" class="chart-card" shadow="hover">
          <div slot="header" class="chart-title">近7日金额趋势</div>
          <div ref="amountTrendChart" class="chart-wrap" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card v-loading="loading" class="chart-card" shadow="hover">
          <div slot="header" class="chart-title">今日金额 vs 近7日均值</div>
          <div ref="todayCompareChart" class="chart-wrap" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card v-loading="loading" class="chart-card" shadow="hover">
          <div slot="header" class="chart-title">订单状态分布</div>
          <div ref="statusPieChart" class="chart-wrap" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card v-loading="loading" class="chart-card" shadow="hover">
          <div slot="header" class="chart-title">近7日订单量与金额</div>
          <div ref="orderAmountMixChart" class="chart-wrap" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import echarts from 'echarts'
import { mapGetters } from 'vuex'
import { getDashboardStats } from '@/api/dashboard'
import { listOrders } from '@/api/orders'
import { ORDER_STATUS_OPTIONS, getOrderStatusLabel, getRoleLabel } from '@/constants/dict'

export default {
  name: 'Dashboard',
  data() {
    return {
      loading: false,
      moduleKeyword: '',
      stats: {
        totalRevenueAmount: 0,
        accumulatedCheckInCount: 0,
        todayAmount: 0,
        totalOrderCount: 0
      },
      chartData: {
        labels: [],
        amountTrend: [],
        todayAmount: 0,
        last7AvgAmount: 0,
        orderCountTrend: [],
        statusPieData: []
      },
      chartInstances: {
        amountTrend: null,
        todayCompare: null,
        statusPie: null,
        orderAmountMix: null
      },
      resizeHandler: null
    }
  },
  computed: {
    ...mapGetters(['name', 'roles']),
    rolesText() {
      return this.roles && this.roles.length ? this.roles.map(item => getRoleLabel(item)).join('、') : '未知'
    },
    moduleEntries() {
      return [
        {
          key: 'order',
          title: '订单管理',
          desc: '查看订单并执行支付、入住、退房等状态流转',
          path: '/order/list',
          icon: 'el-icon-s-order',
          roles: ['ADMIN', 'RECEPTIONIST']
        },
        {
          key: 'room',
          title: '房间管理',
          desc: '维护房间信息与房态，支持空闲房快速检索',
          path: '/room/list',
          icon: 'el-icon-house',
          roles: ['ADMIN', 'RECEPTIONIST']
        },
        {
          key: 'roomType',
          title: '房型管理',
          desc: '维护房型名称、价格与容量等基础配置',
          path: '/room/types',
          icon: 'el-icon-collection-tag',
          roles: ['ADMIN', 'RECEPTIONIST']
        },
        {
          key: 'customer',
          title: '顾客管理',
          desc: '查询顾客档案、联系方式与账户信息',
          path: '/customer/manage',
          icon: 'el-icon-user-solid',
          roles: ['ADMIN', 'RECEPTIONIST']
        },
        {
          key: 'staff',
          title: '员工管理',
          desc: '管理员可维护员工账号与角色权限',
          path: '/system/staff',
          icon: 'el-icon-user',
          roles: ['ADMIN']
        },
        {
          key: 'profile',
          title: '个人中心',
          desc: '查看并更新当前账号的个人资料',
          path: '/staff/profile',
          icon: 'el-icon-id-card',
          roles: ['ADMIN', 'RECEPTIONIST']
        }
      ]
    },
    filteredModules() {
      const roleSet = new Set(this.roles || [])
      const available = this.moduleEntries.filter(item => item.roles.some(role => roleSet.has(role)))
      const keyword = (this.moduleKeyword || '').trim().toLowerCase()
      if (!keyword) {
        return available
      }
      return available.filter(item => (`${item.title}${item.desc}`).toLowerCase().includes(keyword))
    }
  },
  created() {
    this.fetchDashboard()
  },
  mounted() {
    this.resizeHandler = () => {
      Object.keys(this.chartInstances).forEach(key => {
        const chart = this.chartInstances[key]
        if (chart) {
          chart.resize()
        }
      })
    }
    window.addEventListener('resize', this.resizeHandler)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeHandler)
    Object.keys(this.chartInstances).forEach(key => {
      const chart = this.chartInstances[key]
      if (chart) {
        chart.dispose()
        this.chartInstances[key] = null
      }
    })
  },
  methods: {
    async fetchDashboard() {
      this.loading = true
      try {
        const responses = await Promise.all([
          getDashboardStats(),
          listOrders({ pageNum: 1, pageSize: 1000 })
        ])

        const statsRes = responses[0]
        const ordersRes = responses[1]
        const orders = this.normalizeListData(ordersRes.data)
        const accumulatedCheckInCount = this.calcAccumulatedCheckInCount(orders)
        this.stats = Object.assign({}, this.stats, statsRes.data || {}, {
          accumulatedCheckInCount
        })
        this.chartData = this.buildChartData(orders)
      } finally {
        this.loading = false
        this.$nextTick(() => {
          this.renderCharts()
        })
      }
    },
    goModule(path) {
      this.$router.push(path)
    },
    normalizeListData(data) {
      if (Array.isArray(data)) {
        return data
      }
      return data && Array.isArray(data.records) ? data.records : []
    },
    buildRecentDays(days) {
      const keys = []
      const labels = []
      const now = new Date()
      for (let i = days - 1; i >= 0; i--) {
        const date = new Date(now)
        date.setDate(now.getDate() - i)
        const key = this.formatDateKey(date)
        keys.push(key)
        labels.push(key.slice(5))
      }
      return { keys, labels }
    },
    formatDateKey(date) {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    getDateKey(timeValue) {
      if (!timeValue) {
        return ''
      }
      const text = String(timeValue)
      if (/^\d{4}-\d{2}-\d{2}/.test(text)) {
        return text.slice(0, 10)
      }
      const date = new Date(text)
      if (Number.isNaN(date.getTime())) {
        return ''
      }
      return this.formatDateKey(date)
    },
    toNumber(value) {
      const num = Number(value || 0)
      return Number.isNaN(num) ? 0 : num
    },
    isRevenueStatus(status) {
      return status === 'PAID' || status === 'COMPLETED'
    },
    calcAccumulatedCheckInCount(orders) {
      const today = this.formatDateKey(new Date())
      const customerSet = new Set()
      orders.forEach(order => {
        if (!this.isRevenueStatus(order.status)) {
          return
        }
        if (!order.customerId || !order.checkInDate) {
          return
        }
        const checkInDate = String(order.checkInDate).slice(0, 10)
        if (checkInDate <= today) {
          customerSet.add(String(order.customerId))
        }
      })
      return customerSet.size
    },
    buildChartData(orders) {
      const recent = this.buildRecentDays(7)
      const keys = recent.keys
      const labels = recent.labels
      const amountByDay = {}
      const countByDay = {}
      const statusCounter = {}

      keys.forEach(key => {
        amountByDay[key] = 0
        countByDay[key] = 0
      })
      ORDER_STATUS_OPTIONS.forEach(item => {
        statusCounter[item.value] = 0
      })

      orders.forEach(order => {
        const status = order.status || 'UNKNOWN'
        const dateKey = this.getDateKey(order.createTime || order.createdAt || order.orderTime)
        const totalAmount = this.toNumber(order.totalAmount)

        statusCounter[status] = (statusCounter[status] || 0) + 1
        if (countByDay[dateKey] !== undefined) {
          countByDay[dateKey] += 1
          if (this.isRevenueStatus(status)) {
            amountByDay[dateKey] += totalAmount
          }
        }
      })

      const amountTrend = keys.map(key => Number(amountByDay[key].toFixed(2)))
      const orderCountTrend = keys.map(key => countByDay[key])
      const avg = amountTrend.length
        ? Number((amountTrend.reduce((sum, cur) => sum + cur, 0) / amountTrend.length).toFixed(2))
        : 0
      const todayAmount = this.toNumber(this.stats.todayAmount || amountTrend[amountTrend.length - 1] || 0)

      const statusPieData = Object.keys(statusCounter)
        .filter(key => statusCounter[key] > 0)
        .map(key => ({
          name: getOrderStatusLabel(key),
          value: statusCounter[key]
        }))

      if (!statusPieData.length) {
        statusPieData.push({ name: '暂无订单', value: 1 })
      }

      return {
        labels,
        amountTrend,
        todayAmount,
        last7AvgAmount: avg,
        orderCountTrend,
        statusPieData
      }
    },
    ensureChart(refName, cacheKey) {
      const target = this.$refs[refName]
      if (!target) {
        return null
      }
      if (!this.chartInstances[cacheKey]) {
        this.chartInstances[cacheKey] = echarts.init(target)
      }
      return this.chartInstances[cacheKey]
    },
    renderCharts() {
      this.renderAmountTrendChart()
      this.renderTodayCompareChart()
      this.renderStatusPieChart()
      this.renderOrderAmountMixChart()
    },
    renderAmountTrendChart() {
      const chart = this.ensureChart('amountTrendChart', 'amountTrend')
      if (!chart) return

      chart.setOption({
        color: ['#14b8a6'],
        tooltip: {
          trigger: 'axis',
          valueFormatter: value => `¥${this.toNumber(value).toFixed(2)}`
        },
        grid: { left: 45, right: 20, bottom: 30, top: 24 },
        xAxis: { type: 'category', data: this.chartData.labels, boundaryGap: false },
        yAxis: {
          type: 'value',
          axisLabel: { formatter: value => `¥${value}` }
        },
        series: [
          {
            name: '金额',
            type: 'line',
            smooth: true,
            data: this.chartData.amountTrend,
            areaStyle: { color: 'rgba(20, 184, 166, 0.16)' }
          }
        ]
      })
    },
    renderTodayCompareChart() {
      const chart = this.ensureChart('todayCompareChart', 'todayCompare')
      if (!chart) return

      chart.setOption({
        color: ['#38bdf8', '#f59e0b'],
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          valueFormatter: value => `¥${this.toNumber(value).toFixed(2)}`
        },
        grid: { left: 50, right: 20, bottom: 30, top: 24 },
        xAxis: { type: 'category', data: ['今日金额', '近7日均值'] },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: value => `¥${value}`
          }
        },
        series: [
          {
            type: 'bar',
            barWidth: '45%',
            data: [
              Number(this.chartData.todayAmount.toFixed(2)),
              Number(this.chartData.last7AvgAmount.toFixed(2))
            ]
          }
        ]
      })
    },
    renderStatusPieChart() {
      const chart = this.ensureChart('statusPieChart', 'statusPie')
      if (!chart) return

      chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, left: 'center' },
        series: [
          {
            name: '订单状态',
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['50%', '45%'],
            avoidLabelOverlap: true,
            label: { formatter: '{b}\n{c}' },
            data: this.chartData.statusPieData
          }
        ]
      })
    },
    renderOrderAmountMixChart() {
      const chart = this.ensureChart('orderAmountMixChart', 'orderAmountMix')
      if (!chart) return

      chart.setOption({
        color: ['#2563eb', '#f97316'],
        tooltip: { trigger: 'axis' },
        legend: { data: ['订单量', '金额'] },
        grid: { left: 40, right: 40, bottom: 30, top: 30 },
        xAxis: { type: 'category', data: this.chartData.labels },
        yAxis: [
          {
            type: 'value',
            name: '订单量',
            minInterval: 1
          },
          {
            type: 'value',
            name: '金额',
            axisLabel: { formatter: value => `¥${value}` }
          }
        ],
        series: [
          { name: '订单量', type: 'bar', barWidth: '35%', data: this.chartData.orderCountTrend },
          { name: '金额', type: 'line', yAxisIndex: 1, smooth: true, data: this.chartData.amountTrend }
        ]
      })
    },
    formatAmount(value) {
      const amount = this.toNumber(value)
      return `¥${amount.toFixed(2)}`
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-page {
  padding: 20px;
}

.ops-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 14px;
  background: rgba(8, 16, 28, 0.84);
  backdrop-filter: blur(8px);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.25);
  margin-bottom: 20px;
}

.ops-left {
  h2 {
    margin: 0;
    color: #f8fafc;
    font-size: 24px;
  }

  p {
    margin: 8px 0 0;
    color: rgba(241, 245, 249, 0.72);
    font-size: 13px;
  }
}

.ops-right {
  display: flex;
  gap: 10px;
}

.module-search {
  width: 260px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.kpi-card {
  .kpi-label {
    color: #64748b;
    font-size: 14px;
    margin-bottom: 10px;
  }

  .kpi-value {
    font-size: 30px;
    font-weight: 700;
    color: #0f172a;
  }
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.module-card {
  .module-header {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #0f172a;
    font-weight: 600;
    font-size: 16px;

    i {
      font-size: 18px;
      color: #14b8a6;
    }
  }

  .module-desc {
    margin: 10px 0;
    min-height: 36px;
    line-height: 18px;
    color: #64748b;
    font-size: 13px;
  }
}

.chart-card {
  margin-bottom: 20px;
}

.chart-title {
  font-size: 14px;
  color: #0f172a;
  font-weight: 600;
}

.chart-wrap {
  width: 100%;
  height: 320px;
}

@media (max-width: 960px) {
  .ops-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .ops-right {
    width: 100%;
    flex-wrap: wrap;
  }

  .module-search {
    width: 100%;
  }
}
</style>
