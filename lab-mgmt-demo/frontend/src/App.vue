<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <RouterView />
</template>

<style>
:root {
  --brand: #0f766e;
  --brand-deep: #115e59;
  --brand-soft: #e6f4f1;
  --ink: #102a43;
  --muted: #6b7c8f;
  --line: #e3ebf1;
  --page: #f3f6f8;
  --danger: #d14343;
  --el-color-primary: #0f766e;
  --el-color-primary-light-3: #3f948d;
  --el-color-primary-light-5: #77b3ad;
  --el-color-primary-light-7: #b0d4d0;
  --el-color-primary-light-8: #cde4e1;
  --el-color-primary-light-9: #e6f4f1;
  --el-color-primary-dark-2: #0c5f58;
  --el-border-radius-base: 9px;
}

* {
  box-sizing: border-box;
}

html,
body,
#app {
  min-height: 100%;
  margin: 0;
}

body {
  background: var(--page);
  color: var(--ink);
  font-family: "Microsoft YaHei", "PingFang SC", "Hiragino Sans GB", "Noto Sans CJK SC", sans-serif;
  -webkit-font-smoothing: antialiased;
}

button,
input,
textarea,
select {
  font-family: inherit;
}

a {
  color: inherit;
  text-decoration: none;
}

.page {
  width: 100%;
  max-width: 1480px;
  margin: 0 auto;
  padding: 26px 30px 42px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-head h2 {
  margin: 0 0 6px;
  font-size: 24px;
  letter-spacing: -0.4px;
}

.page-head p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.page-head .head-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.panel {
  min-width: 0;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 16px;
  box-shadow: 0 6px 24px rgba(16, 42, 67, 0.045);
}

.panel-pad {
  padding: 20px 22px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--line);
}

.toolbar .grow {
  flex: 1;
  min-width: 0;
}

.toolbar-tip {
  color: var(--muted);
  font-size: 13px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  min-height: 126px;
  padding: 20px 20px 18px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 16px;
  box-shadow: 0 6px 24px rgba(16, 42, 67, 0.04);
}

.stat-card::after {
  content: "";
  position: absolute;
  right: -24px;
  top: -30px;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: var(--brand-soft);
  opacity: 0.8;
}

.stat-card .stat-label {
  color: var(--muted);
  font-size: 13px;
  letter-spacing: 0.4px;
}

.stat-card .stat-value {
  margin: 12px 0 6px;
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -1px;
}

.stat-card .stat-foot {
  color: #8a9aab;
  font-size: 12px;
}

.stat-card.warm {
  background: linear-gradient(145deg, #fff8ed, #fff);
  border-color: #f5dfbb;
}

.stat-card.warm::after {
  background: #fde9c8;
}

.stat-card.danger {
  background: linear-gradient(145deg, #fff4f2, #fff);
  border-color: #f2d2cd;
}

.stat-card.danger::after {
  background: #f9dedb;
}

.muted {
  color: var(--muted);
}

.mono {
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0 0 14px;
}

.section-title h3 {
  margin: 0;
  font-size: 16px;
  letter-spacing: -0.2px;
}

.section-title .hint {
  color: var(--muted);
  font-size: 12px;
}

.split-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(280px, 0.75fr);
  gap: 16px;
}

.split-grid > * {
  min-width: 0;
}

.soft-box {
  border: 1px solid var(--line);
  border-radius: 13px;
  background: #f8fafb;
}

.kv-list {
  display: grid;
  gap: 10px;
}

.kv-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  color: var(--muted);
  font-size: 13px;
}

.kv-row b {
  color: var(--ink);
  font-weight: 600;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding: 16px 18px;
}

.empty-copy {
  padding: 46px 18px;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}

.table-title {
  display: flex;
  align-items: center;
  gap: 9px;
  font-weight: 600;
}

.table-sub {
  margin-top: 3px;
  color: var(--muted);
  font-size: 12px;
}

.el-button {
  font-weight: 500;
}

.el-table {
  --el-table-border-color: #edf2f6;
  --el-table-header-bg-color: #f8fafb;
  --el-table-row-hover-bg-color: #f6fbfa;
  --el-table-header-text-color: #637588;
}

.el-table th.el-table__cell {
  font-weight: 600;
  font-size: 12px;
  letter-spacing: 0.3px;
}

.el-table .cell {
  line-height: 1.55;
}

.el-dialog {
  border-radius: 16px;
}

.el-dialog__header {
  padding: 22px 24px 10px;
}

.el-dialog__body {
  padding: 12px 24px;
}

.el-dialog__footer {
  padding: 10px 24px 22px;
}

/* ---------- 三端共用框架 ---------- */
.shell {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 20;
  display: flex;
  flex-direction: column;
  width: 238px;
  padding: 22px 16px 16px;
  color: #dbe6ee;
  background:
    radial-gradient(circle at 15% 8%, rgba(45, 212, 191, 0.16), transparent 34%),
    linear-gradient(180deg, #0b1728 0%, #102335 100%);
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 8px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  color: #062c2a;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: -0.5px;
  background: linear-gradient(140deg, #6ee7d8, #2dd4bf);
  box-shadow: 0 8px 20px rgba(45, 212, 191, 0.25);
}

.brand-name {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.2px;
  line-height: 1.2;
}

.brand-sub {
  margin-top: 3px;
  color: #8fa6b8;
  font-size: 11px;
  letter-spacing: 0.7px;
}

.role-badge {
  align-self: flex-start;
  margin: 24px 8px 10px;
  padding: 4px 9px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 999px;
  color: #93c5bd;
  background: rgba(15, 118, 110, 0.15);
  font-size: 11px;
  letter-spacing: 0.4px;
}

.side-nav {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 5px;
  margin-top: 8px;
}

.side-nav a {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 10px;
  color: #aebfcd;
  font-size: 13.5px;
  transition: 0.18s ease;
}

.side-nav a:hover {
  color: #f0f6f9;
  background: rgba(255, 255, 255, 0.055);
}

.side-nav a.active {
  color: #effcf9;
  background: linear-gradient(90deg, rgba(45, 212, 191, 0.18), rgba(45, 212, 191, 0.04));
}

.side-nav a.active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 9px;
  bottom: 9px;
  width: 2px;
  border-radius: 3px;
  background: #2dd4bf;
}

.nav-dot {
  display: inline-grid;
  place-items: center;
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  border-radius: 7px;
  color: #6ee7d8;
  background: rgba(15, 118, 110, 0.2);
  font-size: 11px;
  font-weight: 700;
}

.sidebar-foot {
  padding-top: 14px;
  border-top: 1px solid rgba(148, 163, 184, 0.14);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 8px;
}

.avatar {
  display: grid;
  place-items: center;
  width: 33px;
  height: 33px;
  flex-shrink: 0;
  border-radius: 10px;
  color: #062c2a;
  font-size: 13px;
  font-weight: 700;
  background: #99f6e4;
}

.user-name {
  color: #ecf6f8;
  font-size: 13px;
  font-weight: 600;
}

.user-meta {
  margin-top: 2px;
  color: #7f97a9;
  font-size: 11px;
}

.logout-btn {
  width: 100%;
  margin-top: 8px;
  padding: 9px 10px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 9px;
  color: #aebfcd;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
}

.logout-btn:hover {
  color: #fecaca;
  border-color: rgba(248, 113, 113, 0.35);
  background: rgba(248, 113, 113, 0.07);
}

.main-area {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  margin-left: 238px;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 76px;
  padding: 14px 30px;
  border-bottom: 1px solid rgba(227, 235, 241, 0.9);
  background: rgba(243, 246, 248, 0.88);
  backdrop-filter: blur(12px);
}

.topbar-kicker {
  color: #8a9aab;
  font-size: 10px;
  letter-spacing: 1.4px;
  text-transform: uppercase;
}

.topbar h1 {
  margin: 4px 0 0;
  font-size: 19px;
  letter-spacing: -0.25px;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.content {
  flex: 1;
}

@media (max-width: 980px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .split-grid {
    grid-template-columns: 1fr;
  }

  .page {
    padding: 22px 20px 34px;
  }
}

@media (max-width: 760px) {
  .shell {
    display: block;
  }

  .sidebar {
    position: relative;
    width: auto;
    min-height: 0;
    padding: 15px 14px 12px;
  }

  .brand {
    padding: 0;
  }

  .brand-mark {
    width: 36px;
    height: 36px;
  }

  .role-badge {
    display: none;
  }

  .side-nav {
    flex-direction: row;
    gap: 6px;
    margin-top: 14px;
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .side-nav a {
    flex-shrink: 0;
    padding: 8px 10px;
    font-size: 12px;
    background: rgba(255, 255, 255, 0.04);
  }

  .nav-dot {
    display: none;
  }

  .sidebar-foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-top: 10px;
    padding-top: 10px;
  }

  .user-chip {
    padding: 3px 0;
  }

  .logout-btn {
    width: auto;
    margin-top: 0;
    padding: 7px 12px;
  }

  .main-area {
    margin-left: 0;
  }

  .topbar {
    min-height: 64px;
    padding: 12px 18px;
  }

  .topbar h1 {
    font-size: 17px;
  }

  .page {
    padding: 18px 14px 30px;
  }

  .page-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .page-head .head-actions {
    width: 100%;
  }

  .stat-grid {
    gap: 10px;
  }

  .stat-card {
    min-height: 108px;
    padding: 16px;
  }

  .stat-card .stat-value {
    font-size: 27px;
  }

  .toolbar {
    padding: 13px;
  }
}
</style>
