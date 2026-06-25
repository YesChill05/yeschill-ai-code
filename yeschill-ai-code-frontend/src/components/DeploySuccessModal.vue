<template>
  <a-modal v-model:open="visible" title="部署" :footer="null" width="600px">
    <div class="deploy-success">
      <!-- 排队中 / 构建中 -->
      <div v-if="isBuilding" class="building-status">
        <a-spin size="large" />
        <h3>{{ statusText }}</h3>
        <p v-if="!deployUrl">正在准备部署...</p>
        <p v-else>部署完成后可通过以下链接访问：</p>
      </div>
      <!-- 构建失败 -->
      <div v-else-if="deployStatus === 'FAILED'">
        <div class="status-icon">
          <CloseCircleOutlined style="color: #ff4d4f; font-size: 48px" />
        </div>
        <h3>构建失败</h3>
        <p>项目构建过程中出现错误，请返回重试。</p>
      </div>
      <!-- 部署成功 -->
      <div v-else>
        <div class="success-icon">
          <CheckCircleOutlined style="color: #52c41a; font-size: 48px" />
        </div>
        <h3>部署成功</h3>
        <p>你的网站已经成功部署，可以通过以下链接访问：</p>
      </div>
      <!-- 链接展示（排队中/构建中/成功时显示，失败时隐藏） -->
      <div v-if="deployStatus !== 'FAILED'" class="deploy-url">
        <a-input :value="deployUrl" readonly>
          <template #suffix>
            <a-button type="text" @click="handleCopyUrl">
              <CopyOutlined />
            </a-button>
          </template>
        </a-input>
      </div>
      <div class="deploy-actions">
        <a-button v-if="deployStatus !== 'FAILED'" type="primary" @click="handleOpenSite" :disabled="isBuilding">
          访问网站
        </a-button>
        <a-button @click="handleClose">{{ deployStatus === 'FAILED' ? '关闭' : '关闭' }}</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { CheckCircleOutlined, CopyOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'

interface Props {
  open: boolean
  deployUrl: string
  deployStatus?: string
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'open-site'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const isBuilding = computed(() => {
  return props.deployStatus === 'QUEUED' || props.deployStatus === 'BUILDING'
})

const statusText = computed(() => {
  if (props.deployStatus === 'QUEUED') return '排队等待构建...'
  if (props.deployStatus === 'BUILDING') return '正在构建项目...'
  return ''
})

const handleCopyUrl = async () => {
  try {
    await navigator.clipboard.writeText(props.deployUrl)
    message.success('链接已复制到剪贴板')
  } catch (error) {
    console.error('复制失败：', error)
    message.error('复制失败')
  }
}

const handleOpenSite = () => {
  emit('open-site')
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.deploy-success {
  text-align: center;
  padding: 20px 24px;
}

.success-icon, .status-icon {
  margin-bottom: 12px;
}

.building-status {
  margin-bottom: 16px;
}

.building-status h3 {
  margin: 14px 0 6px;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: -0.01em;
}

.deploy-success h3 {
  margin: 0 0 12px;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: -0.02em;
}

.deploy-success p {
  margin: 0 0 22px;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.deploy-url {
  margin-bottom: 22px;
}

.deploy-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
