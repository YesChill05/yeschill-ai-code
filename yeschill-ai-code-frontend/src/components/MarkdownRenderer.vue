<template>
  <div class="markdown-content" v-html="renderedHtml"></div>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

import 'highlight.js/styles/github.css'

interface Props {
  content: string
  streaming?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  streaming: false,
})

const md: MarkdownIt = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return (
          '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>'
        )
      } catch {
        // 忽略错误，使用默认处理
      }
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  },
})

const renderedHtml = ref('')
let rafId: number | null = null

const renderNow = () => {
  renderedHtml.value = md.render(props.content)
}

const throttledRender = () => {
  if (rafId !== null) return
  rafId = requestAnimationFrame(() => {
    rafId = null
    renderNow()
  })
}

watch(
  () => props.content,
  () => {
    if (props.streaming) {
      throttledRender()
    } else {
      renderNow()
    }
  },
  { immediate: true },
)

watch(
  () => props.streaming,
  (isStreaming, wasStreaming) => {
    if (wasStreaming && !isStreaming) {
      renderNow()
    }
  },
)

onUnmounted(() => {
  if (rafId !== null) {
    cancelAnimationFrame(rafId)
    rafId = null
  }
})
</script>

<style scoped>
.markdown-content {
  line-height: 1.65;
  color: var(--color-text);
  word-wrap: break-word;
  font-size: 14px;
}

.markdown-content :deep(h1), .markdown-content :deep(h2), .markdown-content :deep(h3),
.markdown-content :deep(h4), .markdown-content :deep(h5), .markdown-content :deep(h6) {
  margin: 1.4em 0 0.5em;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.01em;
}

.markdown-content :deep(h1) { font-size: 1.4em; border-bottom: 1px solid var(--color-border); padding-bottom: 0.3em; }
.markdown-content :deep(h2) { font-size: 1.2em; border-bottom: 1px solid var(--color-border); padding-bottom: 0.3em; }
.markdown-content :deep(h3) { font-size: 1.05em; }
.markdown-content :deep(p) { margin: 0.7em 0; }
.markdown-content :deep(ul), .markdown-content :deep(ol) { margin: 0.7em 0; padding-left: 1.5em; }
.markdown-content :deep(li) { margin: 0.25em 0; }

.markdown-content :deep(blockquote) {
  margin: 1em 0; padding: 0.5em 1em;
  border-left: 3px solid var(--color-primary);
  background: var(--color-surface-raised);
  color: var(--color-text-secondary);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.markdown-content :deep(code) {
  background: var(--color-bg);
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 0.88em;
  color: #dc2626;
}

.markdown-content :deep(pre) {
  background: #f8f9fa;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1em;
  overflow-x: auto;
  margin: 1em 0;
}

.markdown-content :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #24292f;
  font-size: 0.85em;
  line-height: 1.5;
}

.markdown-content :deep(table) { border-collapse: collapse; margin: 1em 0; width: 100%; }
.markdown-content :deep(table th), .markdown-content :deep(table td) {
  border: 1px solid var(--color-border); padding: 0.5em 0.8em; text-align: left;
}
.markdown-content :deep(table th) { background: var(--color-surface-raised); font-weight: 600; }

.markdown-content :deep(a) { color: var(--color-primary); text-decoration: none; font-weight: 500; }
.markdown-content :deep(a:hover) { text-decoration: underline; }
.markdown-content :deep(img) { max-width: 100%; border-radius: var(--radius-sm); margin: 0.5em 0; }
.markdown-content :deep(hr) { border: none; border-top: 1px solid var(--color-border); margin: 1.5em 0; }

.markdown-content :deep(.hljs) {
  background: #f8f9fa !important;
  border-radius: var(--radius-md);
  font-family: var(--font-mono);
  font-size: 0.85em;
  line-height: 1.5;
}
</style>
