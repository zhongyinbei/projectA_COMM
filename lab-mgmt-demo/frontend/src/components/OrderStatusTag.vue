<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, default: 'PENDING' }
})

const config = {
  PENDING: { label: '待接单', type: 'warning' },
  ACCEPTED: { label: '维修中', type: 'primary' },
  REJECTED: { label: '已拒绝', type: 'danger' },
  COMPLETED: { label: '已完成', type: 'success' }
}

const current = computed(() => config[props.status] || { label: props.status || '未知', type: 'info' })
</script>

<template>
  <el-tag :type="current.type" effect="light" round class="status-tag">{{ current.label }}</el-tag>
</template>

<style scoped>
.status-tag {
  min-width: 66px;
  justify-content: center;
  border: none;
  font-weight: 600;
}
</style>
