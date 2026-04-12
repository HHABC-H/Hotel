<template>
  <div class="dashboard-container">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="meta-wrap">
          <span class="meta-item">当前用户：{{ name || '未命名' }}</span>
          <span class="meta-item">角色：{{ rolesText }}</span>
        </div>
        <el-button size="mini" type="primary" :loading="loading" @click="fetchDashboard">刷新数据</el-button>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :xs="24" :sm="12" :lg="8">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-title">总收入金额</div>
          <div class="metric-value">{{ formatAmount(stats.totalRevenueAmount) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="8">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-title">累计入住人数</div>
          <div class="metric-value">{{ stats.accumulatedCheckInCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="8">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-title">总订单数</div>
          <div class="metric-value">{{ stats.totalOrderCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <el-card v-loading="loading" class="chart-card" shadow="hover">
          <div slot="header" class="chart-title">近 7 日金额趋势</div>
          <div ref="amountTrendChart" class="chart-wrap" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card v-loading="loading" class="chart-card" shadow="hover">
          <div slot="header" class="chart-title">今日金额 vs 近 7 日均值</div>
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
          <div slot="header" class="chart-title">每日订单量 + 金额趋势（近 7 天）</div>
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
        const [statsRes, ordersRes] = await Promise.all([
          getDashboardStats(),
          listOrders({ pageNum: 1, pageSize: 1000 })
        ])

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
    normalizeListData(data) {
      if (Array.isArray(data)) {
        return data
      }
      return data?.records || []
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
      const { keys, labels } = this.buildRecentDays(7)
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
      if (!chart) {
        return
      }
      chart.setOption({
        color: ['#409EFF'],
        tooltip: {
          trigger: 'axis',
          valueFormatter: value => `¥${this.toNumber(value).toFixed(2)}`
        },
        grid: {
          left: 40,
          right: 20,
          bottom: 30,
          top: 24
        },
        xAxis: {
          type: 'category',
          data: this.chartData.labels,
          boundaryGap: false
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: value => `¥${value}`
          }
        },
        series: [
          {
            name: '金额',
            type: 'line',
            smooth: true,
            data: this.chartData.amountTrend,
            areaStyle: { color: 'rgba(64, 158, 255, 0.15)' }
          }
        ]
      })
    },
    renderTodayCompareChart() {
      const chart = this.ensureChart('todayCompareChart', 'todayCompare')
      if (!chart) {
        return
      }
      chart.setOption({
        color: ['#67C23A', '#E6A23C'],
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          valueFormatter: value => `¥${this.toNumber(value).toFixed(2)}`
        },
        grid: {
          left: 50,
          right: 20,
          bottom: 30,
          top: 24
        },
        xAxis: {
          type: 'category',
          data: ['今日金额', '近7日均值']
        },
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
      if (!chart) {
        return
      }
      chart.setOption({
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          bottom: 0,
          left: 'center'
        },
        series: [
          {
            name: '订单状态',
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['50%', '45%'],
            avoidLabelOverlap: true,
            label: {
              formatter: '{b}\n{c}'
            },
            data: this.chartData.statusPieData
          }
        ]
      })
    },
    renderOrderAmountMixChart() {
      const chart = this.ensureChart('orderAmountMixChart', 'orderAmountMix')
      if (!chart) {
        return
      }
      chart.setOption({
        color: ['#5470C6', '#EE6666'],
        tooltip: { trigger: 'axis' },
        legend: {
          data: ['订单量', '金额']
        },
        grid: {
          left: 40,
          right: 40,
          bottom: 30,
          top: 30
        },
        xAxis: {
          type: 'category',
          data: this.chartData.labels
        },
        yAxis: [
          {
            type: 'value',
            name: '订单量',
            minInterval: 1
          },
          {
            type: 'value',
            name: '金额',
            axisLabel: {
              formatter: value => `¥${value}`
            }
          }
        ],
        series: [
          {
            name: '订单量',
            type: 'bar',
            barWidth: '35%',
            data: this.chartData.orderCountTrend
          },
          {
            name: '金额',
            type: 'line',
            yAxisIndex: 1,
            smooth: true,
            data: this.chartData.amountTrend
          }
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
.dashboard-container {
  padding: 20px;
}

.toolbar-card {
  .toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .meta-wrap {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }

  .meta-item {
    font-size: 14px;
    color: #606266;
  }
}

.metric-card {
  margin-bottom: 20px;
}

.metric-title {
  color: #909399;
  margin-bottom: 12px;
}

.metric-value {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.chart-card {
  margin-bottom: 20px;
}

.chart-title {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.chart-wrap {
  width: 100%;
  height: 320px;
}
</style>
