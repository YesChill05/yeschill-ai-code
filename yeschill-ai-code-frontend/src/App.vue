<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
</script>

<template>
  <BasicLayout />
</template>

<style>
/* ===== Design System — CSS Variables ===== */
:root {
  --color-primary: #4f46e5;
  --color-primary-light: #6366f1;
  --color-primary-soft: rgba(79, 70, 229, 0.08);
  --color-accent: #0d9488;
  --color-accent-soft: rgba(13, 148, 136, 0.08);
  --color-surface: #ffffff;
  --color-surface-raised: #fafbfc;
  --color-bg: #f3f4f6;
  --color-bg-warm: #f8f7f4;
  --color-text: #111827;
  --color-text-secondary: #6b7280;
  --color-text-muted: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
  --shadow-lg: 0 8px 32px rgba(0, 0, 0, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
  --shadow-xl: 0 16px 48px rgba(0, 0, 0, 0.1), 0 4px 12px rgba(0, 0, 0, 0.04);
  --font-sans: 'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', monospace;
  --transition-fast: 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-base: 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ===== Global Reset ===== */
*,
*::before,
*::after {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family: var(--font-sans);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
  background: var(--color-bg-warm);
  color: var(--color-text);
  letter-spacing: -0.01em;
}

html {
  overflow-x: hidden;
}

/* ===== Global Ant Design Overrides ===== */
.ant-btn {
  font-family: var(--font-sans);
  border-radius: var(--radius-md);
  font-weight: 500;
  transition: all var(--transition-base);
}

.ant-btn-primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.25);
}

.ant-btn-primary:hover {
  background: var(--color-primary-light) !important;
  border-color: var(--color-primary-light) !important;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.35);
  transform: translateY(-1px);
}

.ant-input,
.ant-input-password,
.ant-input-affix-wrapper {
  font-family: var(--font-sans);
  border-radius: var(--radius-md);
  border-color: var(--color-border);
  transition: all var(--transition-base);
  padding: 8px 12px;
}

.ant-input:focus,
.ant-input-password:focus,
.ant-input-affix-wrapper-focused {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-soft);
}

.ant-modal-content {
  border-radius: var(--radius-xl) !important;
  overflow: hidden;
}

.ant-modal-header {
  border-bottom: 1px solid var(--color-border-light);
  padding: 20px 24px 16px;
}

.ant-modal-title {
  font-family: var(--font-sans);
  font-weight: 600;
  font-size: 18px;
}

.ant-tag {
  font-family: var(--font-sans);
  border-radius: var(--radius-sm);
  font-weight: 500;
}

.ant-avatar {
  font-family: var(--font-sans);
}

.ant-menu-horizontal {
  border-bottom: none !important;
  font-family: var(--font-sans);
}

.ant-menu-horizontal .ant-menu-item {
  border-radius: var(--radius-md);
  margin: 0 2px;
  font-weight: 500;
}

.ant-menu-horizontal .ant-menu-item-selected {
  color: var(--color-primary) !important;
}

.ant-menu-horizontal .ant-menu-item-selected::after {
  border-bottom-color: var(--color-primary) !important;
}

.ant-pagination {
  font-family: var(--font-sans);
}

.ant-pagination-item-active {
  border-color: var(--color-primary) !important;
}

.ant-pagination-item-active a {
  color: var(--color-primary) !important;
}

.ant-spin-dot-item {
  background-color: var(--color-primary) !important;
}

.ant-message .ant-message-notice-content {
  border-radius: var(--radius-md);
  font-family: var(--font-sans);
}
</style>
