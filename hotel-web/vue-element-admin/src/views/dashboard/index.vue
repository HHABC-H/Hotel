<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-title">当前角色</div>
          <div class="metric-value">{{ rolesText }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-title">当前用户</div>
          <div class="metric-value">{{ name || '未命名' }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-title">接口状态</div>
          <div class="metric-value">已接入 /api</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-title">版本</div>
          <div class="metric-value">V1.0 骨架</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 20px;">
      <div slot="header">说明</div>
      <p class="desc">当前为酒店管理系统前端骨架版，已完成登录鉴权、角色路由与核心业务页面框架接入。</p>
      <p class="desc">下一步可逐页补充新增/编辑弹窗、订单状态流转细节与表单校验。</p>
    </el-card>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'Dashboard',
  computed: {
    ...mapGetters(['name', 'roles']),
    rolesText() {
      return this.roles && this.roles.length ? this.roles.join(', ') : '未知'
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 20px;
}

.metric-card {
  margin-bottom: 20px;
}

.metric-title {
  color: #909399;
  margin-bottom: 12px;
}

.metric-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.desc {
  color: #606266;
  margin: 6px 0;
}
</style>
