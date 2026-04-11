<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header">系统设置</div>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="权限矩阵" name="matrix">
          <el-alert title="角色可见性已与路由权限联动。下方表格用于业务核对。" type="success" :closable="false" style="margin-bottom: 16px;" />
          <el-table :data="permissionMatrix" border>
            <el-table-column prop="roleName" label="角色" width="130" />
            <el-table-column prop="visibleModules" label="可见模块" min-width="460" />
            <el-table-column prop="hiddenModules" label="不可见模块" min-width="460" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="菜单预览" name="menus">
          <el-form :inline="true" class="preview-form">
            <el-form-item label="选择角色">
              <el-select v-model="previewRole" placeholder="请选择" style="width: 220px;">
                <el-option label="管理员（ADMIN）" value="ADMIN" />
                <el-option label="前台（RECEPTIONIST）" value="RECEPTIONIST" />
                <el-option label="顾客（CLIENT）" value="CLIENT" />
              </el-select>
            </el-form-item>
          </el-form>

          <el-tree :data="previewMenuTree" node-key="id" default-expand-all :expand-on-click-node="false">
            <span slot-scope="{ data }" class="tree-node">
              <span>{{ data.label }}</span>
              <el-tag size="mini" type="info">{{ data.path }}</el-tag>
            </span>
          </el-tree>
        </el-tab-pane>

        <el-tab-pane label="系统日志" name="logs">
          <el-alert
            type="warning"
            :closable="false"
            style="margin-bottom: 16px;"
            title="当前后端文档未明确日志接口路径，默认调用 /logs。若后端实际路径不同，请在“日志端点”中调整后查询。"
          />

          <el-form :inline="true" :model="logQuery" class="filter-form">
            <el-form-item label="日志端点">
              <el-input v-model="logEndpoint" placeholder="例如 /logs 或 /system/logs" style="width: 220px;" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="logQuery.type" clearable placeholder="全部" style="width: 160px;">
                <el-option label="登录日志" value="LOGIN" />
                <el-option label="操作日志" value="OPERATION" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键词">
              <el-input v-model="logQuery.keyword" clearable placeholder="用户/模块/动作" @keyup.enter.native="handleLogSearch" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleLogSearch">查询</el-button>
              <el-button @click="handleLogReset">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="logLoading" :data="logTableData" border>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="logType" label="类型" width="120" />
            <el-table-column prop="username" label="用户" min-width="120" />
            <el-table-column prop="module" label="模块" min-width="120" />
            <el-table-column prop="action" label="动作" min-width="120" />
            <el-table-column prop="ip" label="IP" min-width="140" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="message" label="信息" min-width="180" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" min-width="170" />
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next"
              :current-page="logQuery.pageNum"
              :page-size="logQuery.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="logTotal"
              @size-change="handleLogSizeChange"
              @current-change="handleLogCurrentChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="运行参数" name="runtime">
          <el-descriptions border :column="2">
            <el-descriptions-item label="前端 API 基址">{{ baseApi }}</el-descriptions-item>
            <el-descriptions-item label="JWT 过期时间">1800 秒（后端文档）</el-descriptions-item>
            <el-descriptions-item label="当前环境">{{ currentEnv }}</el-descriptions-item>
            <el-descriptions-item label="权限模式">角色鉴权（ADMIN / RECEPTIONIST / CLIENT）</el-descriptions-item>
          </el-descriptions>

          <el-alert
            style="margin-top: 16px;"
            type="info"
            :closable="false"
            title="如需新增按钮级权限（例如按接口动作区分），可在页面内继续补充细粒度策略。"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import { asyncRoutes } from '@/router'
import { listSystemLogs } from '@/api/systemLogs'

const createDefaultLogQuery = () => ({
  pageNum: 1,
  pageSize: 10,
  type: undefined,
  keyword: ''
})

export default {
  name: 'SystemSettingsIndex',
  data() {
    return {
      activeTab: 'matrix',
      previewRole: 'ADMIN',
      baseApi: process.env.VUE_APP_BASE_API,
      currentEnv: process.env.ENV || process.env.NODE_ENV || 'development',
      permissionMatrix: [
        {
          roleName: 'ADMIN',
          visibleModules: '仪表盘、客房管理（房型/房间）、订单管理、客户管理、用户管理、系统设置',
          hiddenModules: '无'
        },
        {
          roleName: 'RECEPTIONIST',
          visibleModules: '仪表盘、客房管理（仅查看房态）、订单管理（可办理入住/退房）、客户管理（查询）',
          hiddenModules: '用户管理、系统设置、顾客下单界面'
        },
        {
          roleName: 'CLIENT',
          visibleModules: '个人中心、我的订单、下单界面',
          hiddenModules: '客房管理、订单管理（全部订单视图）、用户管理、系统设置'
        }
      ],
      logEndpoint: '/logs',
      logLoading: false,
      logTotal: 0,
      logTableData: [],
      logQuery: createDefaultLogQuery()
    }
  },
  computed: {
    previewMenuTree() {
      return this.buildMenuTreeByRole(this.previewRole)
    }
  },
  watch: {
    activeTab(value) {
      if (value === 'logs' && this.logTableData.length === 0) {
        this.fetchLogs()
      }
    }
  },
  methods: {
    hasRole(route, role) {
      const roles = route?.meta?.roles
      if (!roles || !roles.length) {
        return true
      }
      return roles.includes(role)
    },
    formatPath(parentPath, childPath) {
      if (!childPath) {
        return parentPath || '/'
      }
      if (childPath.startsWith('/')) {
        return childPath
      }
      const parent = parentPath || ''
      if (!parent || parent === '/') {
        return `/${childPath}`
      }
      return `${parent}/${childPath}`
    },
    buildMenuTreeByRole(role) {
      let id = 0
      const walk = (routes, parentPath = '') => {
        return routes
          .filter(route => !route.hidden && this.hasRole(route, role))
          .map(route => {
            const fullPath = this.formatPath(parentPath, route.path)
            const children = route.children ? walk(route.children, fullPath) : []
            const title = route.meta?.title || route.name || route.path
            id += 1
            return {
              id,
              label: title,
              path: fullPath,
              children
            }
          })
          .filter(node => node.children.length || node.label)
      }
      return walk(asyncRoutes)
    },
    normalizePageData(data) {
      if (Array.isArray(data)) {
        return { records: data, total: data.length }
      }
      return {
        records: data?.records || [],
        total: Number(data?.total || 0)
      }
    },
    fetchLogs() {
      const endpoint = (this.logEndpoint || '/logs').trim()
      if (!endpoint.startsWith('/')) {
        this.$message.warning('日志端点必须以 / 开头')
        return
      }

      this.logLoading = true
      listSystemLogs(this.logQuery, endpoint)
        .then(res => {
          const { records, total } = this.normalizePageData(res.data)
          this.logTableData = records
          this.logTotal = total
        })
        .catch(() => {
          this.logTableData = []
          this.logTotal = 0
        })
        .finally(() => {
          this.logLoading = false
        })
    },
    handleLogSearch() {
      this.logQuery.pageNum = 1
      this.fetchLogs()
    },
    handleLogReset() {
      this.logQuery = createDefaultLogQuery()
      this.fetchLogs()
    },
    handleLogSizeChange(size) {
      this.logQuery.pageSize = size
      this.logQuery.pageNum = 1
      this.fetchLogs()
    },
    handleLogCurrentChange(page) {
      this.logQuery.pageNum = page
      this.fetchLogs()
    }
  }
}
</script>

<style lang="scss" scoped>
.preview-form {
  margin-bottom: 12px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-form {
  margin-bottom: 12px;
}

.pagination-wrapper {
  margin-top: 16px;
  text-align: right;
}
</style>
