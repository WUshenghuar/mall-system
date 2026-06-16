<template>
  <router-view />
</template>

<script setup>
</script>

<style>
/* ============================================
   B2C Cross-Border E-Commerce — Design Tokens
   Global Trade Command aesthetic
   ============================================ */
:root {
  /* ── Core palette ── */
  --color-abyss:        #0B192C;
  --color-abyss-hover:  #152238;
  --color-night:        #0D1B30;
  --color-brass:        #C8963E;
  --color-brass-hover:  #B8872E;
  --color-brass-deep:   #8B691F;
  --color-brass-soft:   rgba(200, 150, 62, 0.12);
  --color-brass-glow:   rgba(200, 150, 62, 0.25);
  --color-brass-shine:  rgba(200, 150, 62, 0.06);
  --color-fog:          #F0F2F5;
  --color-fog-warm:     #F5F3EF;
  --color-surface:      #FFFFFF;
  --color-ivory:        #FAF8F5;
  --color-slate:        #64748B;
  --color-slate-light:  #94A3B8;
  --color-lead:         #1E293B;
  --color-ink:          rgba(255, 255, 255, 0.88);

  /* ── Functional ── */
  --color-success:      #2D8A56;
  --color-warning:      #D4891A;
  --color-danger:       #C53030;
  --color-info:         #3B82F6;

  /* ── Spacing (4px base) ── */
  --space-1:  4px;
  --space-2:  8px;
  --space-3:  12px;
  --space-4:  16px;
  --space-5:  20px;
  --space-6:  24px;
  --space-8:  32px;
  --space-10: 40px;
  --space-12: 48px;
  --space-16: 64px;

  /* ── Type ── */
  --font-display: 'Playfair Display', 'Noto Serif SC', Georgia, serif;
  --font-sans:    'DM Sans', -apple-system, BlinkMacSystemFont, 'Noto Sans SC', sans-serif;
  --font-mono:    'JetBrains Mono', 'SF Mono', monospace;

  --text-xs:   12px;
  --text-sm:   13px;
  --text-base: 14px;
  --text-lg:   16px;
  --text-xl:   18px;
  --text-2xl:  24px;
  --text-3xl:  30px;
  --text-4xl:  40px;

  /* ── Radii ── */
  --radius-sm: 4px;
  --radius-md: 6px;
  --radius-lg: 10px;
  --radius-xl: 14px;

  /* ── Shadows ── */
  --shadow-card: 0 1px 3px rgba(11, 25, 44, 0.04), 0 1px 2px rgba(11, 25, 44, 0.03);
  --shadow-elevated: 0 4px 16px rgba(11, 25, 44, 0.07), 0 0 0 1px rgba(11, 25, 44, 0.03);
  --shadow-glass: 0 8px 32px rgba(0, 0, 0, 0.12);
  --shadow-warm: 0 4px 24px rgba(200, 150, 62, 0.08);
  --shadow-btn: 0 2px 8px rgba(200, 150, 62, 0.25);

  /* ── Transitions ── */
  --ease-out: cubic-bezier(0.16, 1, 0.3, 1);
  --ease-in-out: cubic-bezier(0.65, 0, 0.35, 1);
  --duration-fast: 150ms;
  --duration-normal: 250ms;
  --duration-slow: 400ms;

  /* ── Layout ── */
  --sidebar-width: 240px;
  --sidebar-collapsed: 64px;
  --header-height: 56px;
}

/* ── Scrollbar ── */
::-webkit-scrollbar { width: 4px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb {
  background: var(--color-slate-light);
  border-radius: 4px;
}
::-webkit-scrollbar-thumb:hover {
  background: var(--color-slate);
}

/* ── Reset ── */
*, *::before, *::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  width: 100%;
  font-family: var(--font-sans);
  font-size: var(--text-base);
  color: var(--color-lead);
  background: var(--color-fog);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  font-variant-numeric: tabular-nums;
}

/* ── Selection ── */
::selection {
  background: var(--color-brass-soft);
  color: var(--color-lead);
}

/* ── Focus ring ── */
:focus-visible {
  outline: 2px solid var(--color-brass);
  outline-offset: 2px;
  border-radius: 2px;
}

/* ════════════════════════════════════════
   Ant Design Vue Overrides
   ════════════════════════════════════════ */

/* Layout */
.ant-layout {
  background: var(--color-fog) !important;
}

/* Cards */
.ant-card {
  border-radius: var(--radius-lg) !important;
  box-shadow: var(--shadow-card) !important;
  border: none !important;
}
.ant-card-head {
  border-bottom: 1px solid rgba(11, 25, 44, 0.05) !important;
  min-height: 48px !important;
  padding: 0 var(--space-5) !important;
}
.ant-card-head-title {
  font-family: var(--font-sans);
  font-weight: 600 !important;
  font-size: var(--text-base) !important;
  color: var(--color-lead) !important;
  padding: var(--space-4) 0 !important;
}
.ant-card-body {
  padding: var(--space-5) !important;
}

/* Buttons */
.ant-btn-primary {
  background: linear-gradient(135deg, var(--color-brass) 0%, var(--color-brass-hover) 100%) !important;
  border-color: var(--color-brass) !important;
  box-shadow: var(--shadow-btn) !important;
  border: none !important;
  font-weight: 500 !important;
  letter-spacing: 0.3px !important;
  transition: all var(--duration-fast) var(--ease-out) !important;
}
.ant-btn-primary:hover {
  background: linear-gradient(135deg, var(--color-brass-hover) 0%, #A67A2D 100%) !important;
  border-color: var(--color-brass-hover) !important;
  box-shadow: 0 4px 16px rgba(200, 150, 62, 0.35) !important;
  transform: translateY(-1px);
}

/* Table */
.ant-table-thead > tr > th {
  background: var(--color-fog) !important;
  color: var(--color-slate) !important;
  font-weight: 600 !important;
  font-size: var(--text-xs) !important;
  text-transform: uppercase !important;
  letter-spacing: 0.5px !important;
  border-bottom: 1px solid rgba(11, 25, 44, 0.06) !important;
  padding: var(--space-3) var(--space-4) !important;
}
.ant-table-tbody > tr > td {
  padding: var(--space-3) var(--space-4) !important;
  border-bottom: 1px solid rgba(11, 25, 44, 0.04) !important;
}
.ant-table-tbody > tr:hover > td {
  background: rgba(200, 150, 62, 0.03) !important;
}
.ant-table-row.ant-table-row-selected > td {
  background: var(--color-brass-soft) !important;
}

/* Pagination */
.ant-pagination-item-active {
  border-color: var(--color-brass) !important;
  background: var(--color-brass-soft) !important;
}
.ant-pagination-item-active a {
  color: var(--color-brass) !important;
  font-weight: 600 !important;
}
.ant-pagination-item:hover {
  border-color: var(--color-brass) !important;
}
.ant-pagination-item:hover a {
  color: var(--color-brass) !important;
}

/* Tag */
.ant-tag {
  border-radius: var(--radius-sm) !important;
  padding: 2px 8px !important;
  font-size: 12px !important;
  font-weight: 500 !important;
  line-height: 20px !important;
  border: none !important;
}

/* Input */
.ant-input, .ant-input-password {
  border-radius: var(--radius-md) !important;
  border: 1px solid rgba(11, 25, 44, 0.1) !important;
  transition: all var(--duration-fast) var(--ease-out) !important;
}
.ant-input:hover, .ant-input-password:hover {
  border-color: var(--color-brass) !important;
}
.ant-input:focus, .ant-input-password:focus,
.ant-input-focused, .ant-input-password-focused {
  border-color: var(--color-brass) !important;
  box-shadow: 0 0 0 2px var(--color-brass-soft) !important;
}

/* Select */
.ant-select:not(.ant-select-customize-input) .ant-select-selector {
  border-radius: var(--radius-md) !important;
  border-color: rgba(11, 25, 44, 0.1) !important;
}
.ant-select-focused:not(.ant-select-disabled).ant-select:not(.ant-select-customize-input) .ant-select-selector {
  border-color: var(--color-brass) !important;
  box-shadow: 0 0 0 2px var(--color-brass-soft) !important;
}

/* Menu dropdown */
.ant-dropdown-menu {
  border-radius: var(--radius-md) !important;
  padding: var(--space-1) !important;
  box-shadow: var(--shadow-glass) !important;
  border: 1px solid rgba(11, 25, 44, 0.06) !important;
}
.ant-dropdown-menu-item {
  border-radius: var(--radius-sm) !important;
  padding: 6px 12px !important;
}

/* Tabs */
.ant-tabs-tab {
  transition: all var(--duration-fast) var(--ease-out) !important;
}
.ant-tabs-tab:hover {
  color: var(--color-brass) !important;
}
.ant-tabs-tab.ant-tabs-tab-active .ant-tabs-tab-btn {
  color: var(--color-brass) !important;
  font-weight: 500 !important;
}
.ant-tabs-ink-bar {
  background: var(--color-brass) !important;
}

/* Spin */
.ant-spin-dot-item {
  background-color: var(--color-brass) !important;
}

/* Skeleton */
.ant-skeleton-header .ant-skeleton-avatar {
  background: var(--color-fog) !important;
}

/* ── Reduced motion ── */
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
