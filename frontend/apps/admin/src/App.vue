<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <a-layout style="min-height:100vh">
    <a-layout-sider collapsible>
      <div class="logo">OnlineChat Admin</div>
      <a-menu theme="dark" mode="inline">
        <a-menu-item key="dashboard">
          <router-link to="/">仪表盘</router-link>
        </a-menu-item>
        <a-menu-item key="users">
          <router-link to="/users">用户管理</router-link>
        </a-menu-item>
        <a-menu-item key="groups">
          <router-link to="/groups">群组管理</router-link>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout-content style="padding:24px">
      <router-view />
    </a-layout-content>
  </a-layout>
</template>

<style scoped>
.logo { height:64px; display:flex; align-items:center; justify-content:center;
  color:#fff; font-size:18px; font-weight:bold; }
</style>
