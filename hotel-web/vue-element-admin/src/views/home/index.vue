<template>
  <div class="home-redirect" />
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'HomeRedirect',
  computed: {
    ...mapGetters(['roles'])
  },
  created() {
    const role = (this.roles && this.roles[0]) || ''
    if (role === 'CLIENT') {
      this.$router.replace('/client/orders')
      return
    }
    if (role === 'ADMIN' || role === 'RECEPTIONIST') {
      this.$router.replace('/dashboard/index')
      return
    }
    this.$router.replace('/login')
  }
}
</script>
