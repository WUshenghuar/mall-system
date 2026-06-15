<template>
  <router-view />
</template>

<script setup>
</script>

<style>
/* ============================================
   B2C Cross-Border E-Commerce — Design Tokens
   ============================================ */
:root {
  /* Core palette */
  --color-abyss:        #0B192C;
  --color-abyss-hover:  #152238;
  --color-brass:        #C8963E;
  --color-brass-hover:  #B8872E;
  --color-brass-soft:   rgba(200, 150, 62, 0.12);
  --color-fog:          #F6F8FA;
  --color-surface:      #FFFFFF;
  --color-slate:        #64748B;
  --color-slate-light:  #94A3B8;
  --color-lead:         #1E293B;

  /* Functional */
  --color-success:      #2D8A56;
  --color-warning:      #D4891A;
  --color-danger:       #C53030;
  --color-info:         #3B82F6;

  /* Spacing scale (4px base) */
  --space-1:  4px;
  --space-2:  8px;
  --space-3:  12px;
  --space-4:  16px;
  --space-5:  20px;
  --space-6:  24px;
  --space-8:  32px;
  --space-10: 40px;
  --space-12: 48px;

  /* Type scale */
  --text-xs:   12px;
  --text-sm:   13px;
  --text-base: 14px;
  --text-lg:   16px;
  --text-xl:   18px;
  --text-2xl:  24px;

  /* Radii */
  --radius-sm: 4px;
  --radius-md: 6px;
  --radius-lg: 10px;

  /* Shadows */
  --shadow-card: 0 1px 3px rgba(11, 25, 44, 0.04), 0 1px 2px rgba(11, 25, 44, 0.03);
  --shadow-elevated: 0 4px 16px rgba(11, 25, 44, 0.07), 0 0 0 1px rgba(11, 25, 44, 0.03);

  /* Transitions */
  --ease-out: cubic-bezier(0.16, 1, 0.3, 1);
  --duration-fast: 150ms;
  --duration-normal: 250ms;

  /* Layout */
  --sidebar-width: 220px;
  --sidebar-collapsed: 64px;
  --header-height: 56px;
}

/* Reset */
*, *::before, *::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  width: 100%;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
               "Helvetica Neue", Arial, "Noto Sans SC", sans-serif;
  font-size: var(--text-base);
  color: var(--color-lead);
  background: var(--color-fog);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* Ant Design Vue overrides */
.ant-layout {
  background: var(--color-fog) !important;
}

.ant-card {
  border-radius: var(--radius-lg) !important;
  box-shadow: var(--shadow-card) !important;
  border: none !important;
}

.ant-card-head {
  border-bottom: 1px solid var(--color-fog) !important;
}

.ant-btn-primary {
  background: var(--color-brass) !important;
  border-color: var(--color-brass) !important;
  box-shadow: none !important;
}
.ant-btn-primary:hover {
  background: var(--color-brass-hover) !important;
  border-color: var(--color-brass-hover) !important;
}

.ant-table-thead > tr > th {
  background: var(--color-fog) !important;
  color: var(--color-slate) !important;
  font-weight: 500 !important;
  border-bottom: 1px solid var(--color-fog) !important;
}

.ant-pagination-item-active {
  border-color: var(--color-brass) !important;
}
.ant-pagination-item-active a {
  color: var(--color-brass) !important;
}

/* Scrollbar */
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb {
  background: var(--color-slate-light);
  border-radius: 3px;
}

/* Focus ring */
:focus-visible {
  outline: 2px solid var(--color-brass);
  outline-offset: 2px;
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
