<script setup lang="ts">
import { onMounted, ref } from 'vue'
import api from '@/api'

interface Contact { id: number; contactUserId: number; remark: string }

const contacts = ref<Contact[]>([])

onMounted(async () => {
  const { data } = await api.get('/api/contact/list')
  if (data.code === 200) contacts.value = data.data
})
</script>

<template>
  <div class="contact-page">
    <h3>好友列表</h3>
    <a-list :data-source="contacts" :loading="false">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta :title="item.remark || `用户${item.contactUserId}`" />
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<style scoped>
.contact-page { padding: 20px; }
</style>
