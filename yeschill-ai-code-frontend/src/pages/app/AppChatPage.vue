<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || '网站生成器' }}</h1>
        <a-tag v-if="appInfo?.codeGenType" color="blue" class="code-gen-type-tag">
          {{ formatCodeGenType(appInfo.codeGenType) }}
        </a-tag>
        <a-tag v-if="appInfo?.genMode === 'workflow'" color="purple" class="code-gen-type-tag">
          工作流模式
        </a-tag>
      </div>
      <div class="header-right">
        <a-button type="default" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用详情
        </a-button>
        <a-button
            type="primary"
            ghost
            @click="downloadCode"
            :loading="downloading"
            :disabled="!isOwner"
        >
          <template #icon>
            <DownloadOutlined />
          </template>
          下载代码
        </a-button>
        <a-button type="primary" @click="deployApp" :loading="deploying">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          部署
        </a-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <!-- 工作流进度步骤（左侧最上方） -->
        <div v-if="appInfo?.genMode === 'workflow' && isGenerating" class="workflow-steps-panel">
          <div class="steps-title">工作流执行中</div>
          <div class="steps-row">
            <template v-for="(name, i) in workflowSteps" :key="i">
              <div
                class="step-item"
                :class="{
                  done: i < workflowCurrentStep,
                  active: i === workflowCurrentStep,
                }"
              >
                <span class="step-dot">
                  <span v-if="i < workflowCurrentStep">✓</span>
                  <a-spin v-else-if="i === workflowCurrentStep - 1" size="small" />
                  <span v-else>{{ i + 1 }}</span>
                </span>
                <span class="step-name">{{ name }}</span>
              </div>
              <span v-if="i < workflowSteps.length - 1" class="step-arrow">→</span>
            </template>
          </div>
        </div>
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory" class="load-more-container">
            <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory" size="small">
              加载更多历史消息
            </a-button>
          </div>
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <a-avatar :src="aiAvatar" />
              </div>
              <div class="message-content">
                <MarkdownRenderer v-if="message.content" :content="message.content" :streaming="index === streamingMessageIndex" />
                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small" />
                  <span>AI 正在思考...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 选中元素信息展示 -->
        <a-alert
            v-if="selectedElementInfo"
            class="selected-element-alert"
            type="info"
            closable
            @close="clearSelectedElement"
        >
          <template #message>
            <div class="selected-element-info">
              <div class="element-header">
                <span class="element-tag">
                  选中元素：{{ selectedElementInfo.tagName.toLowerCase() }}
                </span>
                <span v-if="selectedElementInfo.id" class="element-id">
                  #{{ selectedElementInfo.id }}
                </span>
                <span v-if="selectedElementInfo.className" class="element-class">
                  .{{ selectedElementInfo.className.split(' ').join('.') }}
                </span>
              </div>
              <div class="element-details">
                <div v-if="selectedElementInfo.textContent" class="element-item">
                  内容: {{ selectedElementInfo.textContent.substring(0, 50) }}
                  {{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                </div>
                <div v-if="selectedElementInfo.pagePath" class="element-item">
                  页面路径: {{ selectedElementInfo.pagePath }}
                </div>
                <div class="element-item">
                  选择器:
                  <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                </div>
              </div>
            </div>
          </template>
        </a-alert>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-tooltip v-if="!isOwner" title="无法在别人的作品下对话哦~" placement="top">
              <a-textarea
                  v-model:value="userInput"
                  :placeholder="getInputPlaceholder()"
                  :rows="4"
                  :maxlength="1000"
                  @keydown.enter.prevent="sendMessage"
                  :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
                v-else
                v-model:value="userInput"
                :placeholder="getInputPlaceholder()"
                :rows="4"
                :maxlength="1000"
                @keydown.enter.prevent="sendMessage"
                :disabled="isGenerating"
            />
            <div class="input-actions">
              <a-button
                  type="primary"
                  @click="sendMessage"
                  :loading="isGenerating"
                  :disabled="!isOwner"
              >
                <template #icon>
                  <SendOutlined />
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <h3>生成后的网页展示</h3>
          <div class="preview-actions">
            <a-button
                v-if="isOwner && previewUrl"
                type="link"
                :danger="isEditMode"
                @click="toggleEditMode"
                :class="{ 'edit-mode-active': isEditMode }"
                style="padding: 0; height: auto; margin-right: 12px"
            >
              <template #icon>
                <EditOutlined />
              </template>
              {{ isEditMode ? '退出编辑' : '编辑模式' }}
            </a-button>
            <a-button v-if="previewUrl" type="link" @click="openInNewTab">
              <template #icon>
                <ExportOutlined />
              </template>
              新窗口打开
            </a-button>
          </div>
        </div>
        <div class="preview-content">
          <div v-if="!previewUrl && !isGenerating" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p>网站文件生成完成后将在这里展示</p>
          </div>
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <p>正在生成网站...</p>
          </div>
          <iframe
              v-else
              :src="previewUrl"
              class="preview-iframe"
              frameborder="0"
              @load="onIframeLoad"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal
        v-model:open="appDetailVisible"
        :app="appInfo"
        :show-actions="isOwner || isAdmin"
        @edit="editApp"
        @delete="deleteApp"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
        v-model:open="deployModalVisible"
        :deploy-url="deployUrl"
        :deploy-status="deployStatus"
        @open-site="openDeployedSite"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  getAppVoById,
  deployApp as deployAppApi,
  getDeployStatus,
  deleteApp as deleteAppApi,
} from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { CodeGenTypeEnum, formatCodeGenType } from '@/utils/codeGenTypes'
import request from '@/request'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import aiAvatar from '@/assets/aiAvatar.png'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'

import {
  CloudUploadOutlined,
  SendOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  DownloadOutlined,
  EditOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<string>()

// 对话相关
interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()
const streamingMessageIndex = ref<number>(-1)
let eventSource: EventSource | null = null

// 工作流进度面板
const workflowActive = ref(false)
const workflowTotalSteps = ref(0)
const workflowCurrentStep = ref(0)
const workflowCurrentStepName = ref('')
const workflowSteps = ref<string[]>([])

const workflowPercent = computed(() => {
  if (workflowTotalSteps.value === 0) return 0
  return Math.round((workflowCurrentStep.value / workflowTotalSteps.value) * 100)
})

// 对话历史相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')
const deployStatus = ref('')

// 下载相关
const downloading = ref(false)

// 可视化编辑相关
const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 应用详情相关
const appDetailVisible = ref(false)

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

// 加载对话历史
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value as any,
      pageSize: 10,
    }
    // 如果是加载更多，传递最后一条消息的创建时间作为游标
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        // 将对话历史转换为消息格式，并按时间正序排列（老消息在前）
        const historyMessages: Message[] = chatHistories
            .map((chat) => ({
              type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
              content: chat.message || '',
              createTime: chat.createTime,
            }))
            .reverse() // 反转数组，让老消息在前
        if (isLoadMore) {
          // 加载更多时，将历史消息添加到开头
          messages.value.unshift(...historyMessages)
        } else {
          // 初始加载，直接设置消息列表
          messages.value = historyMessages
          // 检测最后一条消息是否为用户消息，如果是说明 AI 生成被中断
          if (historyMessages.length > 0) {
            const lastMsg = historyMessages[historyMessages.length - 1]
            if (lastMsg.type === 'user') {
              messages.value.push({
                type: 'ai',
                content: '⚠️ 上一次生成被中断，请重新发送消息',
              })
            }
          }
        }
        // 更新游标
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        // 检查是否还有更多历史
        hasMoreHistory.value = chatHistories.length === 10
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as any })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 先加载对话历史
      await loadChatHistory()
      // 如果有至少2条对话记录，展示对应的网站
      if (messages.value.length >= 2) {
        updatePreview()
      }
      // 检查是否需要自动发送初始提示词
      // 只有在是自己的应用且没有对话历史时才自动发送
      if (
          appInfo.value.initPrompt &&
          isOwner.value &&
          messages.value.length === 0 &&
          historyLoaded.value
      ) {
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  let message = userInput.value.trim()
  // 如果有选中的元素，将元素信息添加到提示词中
  if (selectedElementInfo.value) {
    let elementContext = `\n\n选中元素信息：`
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面路径: ${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签: ${selectedElementInfo.value.tagName.toLowerCase()}\n- 选择器: ${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 当前内容: ${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    message += elementContext
  }
  userInput.value = ''
  // 添加用户消息（包含元素信息）
  messages.value.push({
    type: 'user',
    content: message,
  })

  // 发送消息后，清除选中元素并退出编辑模式
  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  // 工作流模式走工作流
  if (appInfo.value?.genMode === 'workflow') {
    await generateCodeWithWorkflow(message, aiMessageIndex)
  } else {
    await generateCode(message, aiMessageIndex)
  }
}

// 工作流模式 SSE
const generateCodeWithWorkflow = async (userMessage: string, aiMessageIndex: number) => {
  try {
    streamingMessageIndex.value = aiMessageIndex
    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage,
    })
    const url = `${API_BASE_URL}/app/chat/gen/code/workflow?${params}`
    eventSource = new EventSource(url, { withCredentials: true } as EventSourceInit)
    console.log('[SSE-Workflow] 连接创建:', url)

    // 进度面板
    workflowActive.value = true
    workflowTotalSteps.value = 0
    workflowCurrentStep.value = 0
    workflowCurrentStepName.value = '正在连接...'
    workflowSteps.value = []

    let fullContent = ''

    // 工作流开始
    eventSource.addEventListener('workflow_start', (event: MessageEvent) => {
      const data = JSON.parse(event.data)
      workflowTotalSteps.value = data.totalSteps || 0
      workflowSteps.value = data.steps || []
      workflowCurrentStep.value = 0
      workflowCurrentStepName.value = data.steps ? data.steps[0] : '正在执行...'
    })

    // 步骤开始
    eventSource.addEventListener('step_start', (event: MessageEvent) => {
      const data = JSON.parse(event.data)
      workflowCurrentStep.value = data.stepNumber - 1
      workflowCurrentStepName.value = data.currentStep || ''
    })

    // 步骤完成
    eventSource.addEventListener('step_completed', (event: MessageEvent) => {
      const data = JSON.parse(event.data)
      workflowCurrentStep.value = data.stepNumber
      workflowCurrentStepName.value = data.currentStep || ''
    })

    // 代码回放（unnamed data 事件，与快速模式格式一致）
    eventSource.onmessage = (event) => {
      try {
        const parsed = JSON.parse(event.data)
        const content = parsed.d
        if (content !== undefined && content !== null) {
          fullContent += content
          messages.value[aiMessageIndex].content = fullContent
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
        }
      } catch (e) {
        console.error('[SSE-Workflow] 解析消息失败:', e)
      }
    }

    // 代码回放完成
    eventSource.addEventListener('codegen_done', () => {
      workflowActive.value = false
    })

    // 工作流完成
    eventSource.addEventListener('workflow_completed', () => {
      messages.value[aiMessageIndex].loading = false
      eventSource?.close()
      eventSource = null
      isGenerating.value = false
      streamingMessageIndex.value = -1
      workflowActive.value = false
      setTimeout(() => updatePreview(), 2000)
    })

    // 工作流错误
    eventSource.addEventListener('workflow_error', (event: MessageEvent) => {
      const data = JSON.parse(event.data)
      messages.value[aiMessageIndex].content = fullContent || '❌ ' + (data.message || '工作流执行失败')
      messages.value[aiMessageIndex].loading = false
      eventSource?.close()
      eventSource = null
      isGenerating.value = false
      streamingMessageIndex.value = -1
      workflowActive.value = false
    })

    eventSource.onerror = () => {
      console.error('[SSE-Workflow] 连接错误')
      if (!messages.value[aiMessageIndex].content) {
        messages.value[aiMessageIndex].content = '❌ 工作流连接失败，请重试'
        messages.value[aiMessageIndex].loading = false
      }
      eventSource?.close()
      eventSource = null
      isGenerating.value = false
      streamingMessageIndex.value = -1
      workflowActive.value = false
    }
  } catch (error) {
    console.error('工作流生成失败:', error)
    messages.value[aiMessageIndex].content = '❌ 工作流生成失败，请重试'
    messages.value[aiMessageIndex].loading = false
    isGenerating.value = false
    streamingMessageIndex.value = -1
    workflowActive.value = false
  }
}

// 生成代码 - 使用 EventSource 处理 SSE 流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  try {
    streamingMessageIndex.value = aiMessageIndex

    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage,
    })
    const url = `${API_BASE_URL}/app/chat/gen/code?${params}`

    eventSource = new EventSource(url, { withCredentials: true } as EventSourceInit)

    console.log('[SSE] 连接创建:', url)
    let fullContent = ''

    // 普通消息（data: 行，无 event 名称）
    eventSource.onmessage = (event) => {
      try {
        const parsed = JSON.parse(event.data)
        const content = parsed.d
        if (content !== undefined && content !== null) {
          fullContent += content
          messages.value[aiMessageIndex].content = fullContent
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
        }
      } catch (e) {
        console.error('解析消息失败:', e)
      }
    }

    // 连接成功
    eventSource.onopen = () => {
      console.log('[SSE] 连接已打开')
    }

    // 流完成
    eventSource.addEventListener('done', () => {
      console.log('[SSE] 收到 done 事件, fullContent长度:', fullContent.length)
      messages.value[aiMessageIndex].loading = false
      eventSource?.close()
      eventSource = null
      isGenerating.value = false
      streamingMessageIndex.value = -1
      setTimeout(() => updatePreview(), 2000)
    })

    // 业务错误
    eventSource.addEventListener('business-error', (event) => {
      try {
        const parsed = JSON.parse(event.data)
        const errorMessage = parsed.message || '生成过程中出现错误'
        messages.value[aiMessageIndex].content = `❌ ${errorMessage}`
        messages.value[aiMessageIndex].loading = false
        message.error(errorMessage)
      } catch (e) {
        console.error('解析错误失败:', e)
      }
      eventSource?.close()
      eventSource = null
      isGenerating.value = false
      streamingMessageIndex.value = -1
    })

    // 连接错误
    eventSource.onerror = () => {
      console.log('[SSE] onerror, readyState:', eventSource?.readyState, 'fullContent长度:', fullContent.length)
      if (eventSource?.readyState === EventSource.CLOSED) return
      eventSource?.close()
      eventSource = null
      // 如果还没收到任何内容才报错
      if (!fullContent) {
        handleError(new Error('SSE 连接失败'), aiMessageIndex)
      } else {
        messages.value[aiMessageIndex].loading = false
        isGenerating.value = false
        streamingMessageIndex.value = -1
        setTimeout(() => updatePreview(), 2000)
      }
    }
  } catch (error) {
    console.error('生成代码失败：', error)
    handleError(error, aiMessageIndex)
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
  streamingMessageIndex.value = -1
  eventSource?.close()
  eventSource = null
}

// 更新预览
const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    const newPreviewUrl = getStaticPreviewUrl(codeGenType, appId.value)
    previewUrl.value = newPreviewUrl
    previewReady.value = true
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 下载代码
const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/app/download/${appId.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }
    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    // 清理
    URL.revokeObjectURL(downloadUrl)
    message.success('代码下载成功')
  } catch (error) {
    console.error('下载失败：', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

// 部署应用
const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as any,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      const codeGenType = appInfo.value?.codeGenType

      if (codeGenType === CodeGenTypeEnum.VUE_PROJECT) {
        // Vue 项目是异步构建，轮询构建状态
        deployStatus.value = 'QUEUED'
        deployModalVisible.value = true
        pollDeployStatus()
      } else {
        // HTML/MULTI_FILE 同步部署，直接成功
        deployStatus.value = ''
        deployModalVisible.value = true
        message.success('部署成功')
      }
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

// 轮询部署状态（仅 Vue 项目异步构建时使用）
let pollTimer: ReturnType<typeof setInterval> | null = null
const pollDeployStatus = () => {
  const startTime = Date.now()
  const MAX_POLL_TIME = 120000 // 最多轮询 120 秒

  pollTimer = setInterval(async () => {
    try {
      const res = await getDeployStatus({ appId: Number(appId.value) })
      if (res.data.code === 0 && res.data.data) {
        const status = res.data.data
        deployStatus.value = status

        if (status === 'DONE') {
          stopPolling()
          message.success('部署成功')
          fetchAppInfo()
          updatePreview()
        } else if (status === 'FAILED') {
          stopPolling()
          message.error('构建失败，请重试')
        } else if (Date.now() - startTime > MAX_POLL_TIME) {
          stopPolling()
          message.warning('构建超时，请稍后刷新页面查看状态')
        }
      }
    } catch {
      // 轮询失败不中断，继续等待
    }
  }, 2000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// iframe加载完成
const onIframeLoad = () => {
  previewReady.value = true
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 可视化编辑相关函数
const toggleEditMode = () => {
  // 检查 iframe 是否已经加载
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe) {
    message.warning('请等待页面加载完成')
    return
  }
  // 确保 visualEditor 已初始化
  if (!previewReady.value) {
    message.warning('请等待页面加载完成')
    return
  }
  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return `正在编辑 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，描述您想要的修改...`
  }
  return '请描述你想生成的网站，越详细效果越好哦'
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()

  // 监听 iframe 消息
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
})

// 清理资源
onUnmounted(() => {
  stopPolling()
  eventSource?.close()
  eventSource = null
})
</script>

<style scoped>
#appChatPage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  background: var(--color-bg-warm);
}

/* Header bar */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  margin-bottom: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-name {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: -0.02em;
}

.header-right {
  display: flex;
  gap: 10px;
}

.header-right .ant-btn {
  border-radius: var(--radius-md);
  font-weight: 500;
}

/* Main content */
.main-content {
  flex: 1;
  display: flex;
  gap: 12px;
  overflow: hidden;
}

/* Chat panel */
.chat-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border);
  overflow: hidden;
}

/* ===== 工作流步骤条（左侧顶部横向） ===== */
.workflow-steps-panel {
  padding: 10px 20px;
  border-bottom: 1px solid var(--color-border);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.04) 0%, rgba(139, 92, 246, 0.02) 100%);
}

.steps-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: 8px;
}

.steps-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--color-text-muted);
  transition: color 0.3s;
  white-space: nowrap;
}

.step-item.active {
  color: var(--color-primary);
  font-weight: 600;
}

.step-item.done {
  color: #10b981;
}

.step-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--color-border);
  font-size: 10px;
  font-weight: 700;
  color: var(--color-text-muted);
  transition: all 0.3s;
  flex-shrink: 0;
}

.step-item.active .step-dot {
  background: var(--color-primary);
  color: #fff;
}

.step-item.done .step-dot {
  background: #10b981;
  color: #fff;
}

.step-name {
  white-space: nowrap;
}

.step-arrow {
  margin: 0 6px;
  font-size: 10px;
  color: var(--color-border);
  transition: color 0.3s;
  flex-shrink: 0;
}

.step-item.done + .step-arrow,
.step-arrow:has(+ .step-item.done) {
  color: #10b981;
}

/* ===== 旧进度样式（保留兼容） ===== */

.messages-container {
  flex: 1;
  padding: 16px 20px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-item {
  margin-bottom: 16px;
}

.user-message,
.ai-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.user-message {
  justify-content: flex-end;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  line-height: 1.55;
  word-wrap: break-word;
  font-size: 14px;
}

.user-message .message-content {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-message .message-content {
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border-light);
  color: var(--color-text);
  border-bottom-left-radius: 4px;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-muted);
  font-size: 13px;
}


.load-more-container {
  text-align: center;
  padding: 8px 0;
  margin-bottom: 12px;
}

/* Input area */
.input-container {
  padding: 14px 16px;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border-light);
}

.input-wrapper {
  position: relative;
}

.input-wrapper :deep(.ant-input) {
  border-radius: var(--radius-lg) !important;
  border-color: var(--color-border) !important;
  padding-right: 52px !important;
}

.input-wrapper :deep(.ant-input):focus {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 3px var(--color-primary-soft) !important;
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

.input-actions .ant-btn {
  border-radius: 50%;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Preview panel */
.preview-section {
  flex: 3;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border);
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid var(--color-border-light);
}

.preview-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: -0.01em;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: var(--color-bg);
}

.preview-placeholder,
.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--color-text-muted);
}

.placeholder-icon { font-size: 52px; margin-bottom: 16px; opacity: 0.5; }
.preview-loading p { margin-top: 16px; }

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
}

.selected-element-alert {
  margin: 0 16px;
  border-radius: var(--radius-md);
}

/* Responsive */
@media (max-width: 1024px) {
  .main-content { flex-direction: column; }
  .chat-section, .preview-section { flex: none; height: 50vh; }
}

@media (max-width: 768px) {
  .header-bar { padding: 10px 12px; }
  .app-name { font-size: 16px; }
  .main-content { padding: 0; gap: 8px; }
  .message-content { max-width: 85%; }

  .selected-element-alert { margin: 0 12px; }
  .selected-element-info { line-height: 1.4; }
  .element-header { margin-bottom: 8px; }
  .element-details { margin-top: 8px; }
  .element-item { margin-bottom: 4px; font-size: 13px; }
  .element-item:last-child { margin-bottom: 0; }
  .element-tag {
    font-family: var(--font-mono);
    font-size: 14px;
    font-weight: 600;
    color: var(--color-primary);
  }
  .element-id { color: var(--color-accent); margin-left: 4px; }
  .element-class { color: #f59e0b; margin-left: 4px; }
  .element-selector-code {
    font-family: var(--font-mono);
    background: var(--color-bg);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 12px;
    color: #dc2626;
    border: 1px solid var(--color-border);
  }
  .edit-mode-active {
    background-color: var(--color-accent) !important;
    border-color: var(--color-accent) !important;
    color: white !important;
  }
  .edit-mode-active:hover {
    background-color: #0f766e !important;
    border-color: #0f766e !important;
  }
}
</style>
