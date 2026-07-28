<script setup lang="ts">
import { message } from 'ant-design-vue'
import { Cropper } from 'vue-advanced-cropper'
import 'vue-advanced-cropper/dist/style.css'
import http from '@/api'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const visible = ref(false)
const uploading = ref(false)
const imgSrc = ref('')
const cropperRef = ref()

function onFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    message.warning('请选择图片文件')
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    imgSrc.value = reader.result as string
    visible.value = true
  }
  reader.readAsDataURL(file)
  // reset input so same file can be selected again
  ;(e.target as HTMLInputElement).value = ''
}

async function cropAndUpload() {
  if (!cropperRef.value) return
  uploading.value = true
  try {
    const { canvas } = cropperRef.value.getResult()
    const blob = await new Promise<Blob>((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.85))
    const form = new FormData()
    form.append('file', blob, 'avatar.jpg')
    form.append('fileType', 'avatar')
    const { data } = await http.post('/api/file/upload', form)
    if (data.code === 200) {
      emit('update:modelValue', data.data.fileUrl)
      message.success('头像已更新')
    }
    visible.value = false
  } catch {
    message.error('上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="avatar-cropper">
    <div class="avatar-preview" @click="visible = false">
      <img :src="modelValue || '/placeholder.png'" v-if="modelValue" />
      <span v-else class="avatar-placeholder">?</span>
      <div class="avatar-overlay">更换头像</div>
    </div>

    <input type="file" accept="image/*" @change="onFileChange" style="display:none" ref="fileInput" />
    <a-button size="small" @click="($refs.fileInput as HTMLInputElement).click()">选择图片</a-button>

    <a-modal v-model:open="visible" title="裁剪头像" :footer="null" width="520px" @cancel="imgSrc = ''">
      <div class="cropper-wrapper" v-if="imgSrc">
        <Cropper ref="cropperRef" :src="imgSrc" :stencil-props="{ aspectRatio: 1 }" />
      </div>
      <div class="cropper-actions">
        <a-button @click="visible = false; imgSrc = ''">取消</a-button>
        <a-button type="primary" :loading="uploading" @click="cropAndUpload">确认上传</a-button>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.avatar-cropper { display:flex; align-items:center; gap:12px; }
.avatar-preview {
  width:64px; height:64px; border-radius:50%; overflow:hidden;
  cursor:pointer; position:relative; border:2px dashed #d9d9d9;
  display:flex; align-items:center; justify-content:center;
}
.avatar-preview img { width:100%; height:100%; object-fit:cover; }
.avatar-placeholder { font-size:24px; color:#ccc; }
.avatar-overlay {
  position:absolute; inset:0; background:rgba(0,0,0,0.4); color:#fff;
  display:flex; align-items:center; justify-content:center;
  font-size:11px; opacity:0; transition:opacity 0.2s;
}
.avatar-preview:hover .avatar-overlay { opacity:1; }
.cropper-wrapper { height:340px; background:#000; margin-bottom:16px; }
.cropper-actions { display:flex; justify-content:flex-end; gap:8px; }
</style>
