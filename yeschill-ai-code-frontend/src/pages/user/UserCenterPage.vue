<template>
  <div id="userCenterPage">
    <h2 class="title">个人中心</h2>
    <div class="desc">修改基本资料和密码</div>

    <!-- 基本资料 -->
    <h3 class="section-title">基本资料</h3>
    <a-form :model="profileForm" autocomplete="off" @finish="handleUpdateProfile">
      <a-form-item name="userName">
        <a-input v-model:value="profileForm.userName" placeholder="请输入昵称" />
      </a-form-item>
      <a-form-item name="userAvatar">
        <div class="avatar-upload">
          <a-avatar v-if="avatarUrl" :src="avatarUrl" :size="80" />
          <a-upload
            :show-upload-list="false"
            :before-upload="beforeUpload"
            :custom-request="handleUpload"
            accept="image/png,image/jpeg,image/gif,image/webp"
          >
            <a-button>
              <template #icon>
                <UploadOutlined />
              </template>
              选择图片上传
            </a-button>
          </a-upload>
        </div>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">保存</a-button>
      </a-form-item>
    </a-form>

    <!-- 修改密码 -->
    <h3 class="section-title">修改密码</h3>
    <a-form :model="passwordForm" autocomplete="off" @finish="handleChangePassword">
      <a-form-item name="oldPassword" :rules="[{ required: true, message: '请输入旧密码' }]">
        <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入旧密码" />
      </a-form-item>
      <a-form-item
        name="newPassword"
        :rules="[
          { required: true, message: '请输入新密码' },
          { min: 8, message: '密码长度不能小于 8 位' },
        ]"
      >
        <a-input-password v-model:value="passwordForm.newPassword" placeholder="请输入新密码" />
      </a-form-item>
      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请确认新密码' },
          { min: 8, message: '密码长度不能小于 8 位' },
        ]"
      >
        <a-input-password v-model:value="passwordForm.checkPassword" placeholder="请确认新密码" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">修改密码</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { updateMyUser, changePassword, uploadAvatar } from '@/api/userController.ts'

const loginUserStore = useLoginUserStore()

const profileForm = reactive<API.UserUpdateMyRequest>({
  userName: '',
  userAvatar: '',
})

const passwordForm = reactive<API.UserChangePasswordRequest>({
  oldPassword: '',
  newPassword: '',
  checkPassword: '',
})

const avatarUrl = ref('')
const uploading = ref(false)

onMounted(() => {
  profileForm.userName = loginUserStore.loginUser.userName ?? ''
  avatarUrl.value = loginUserStore.loginUser.userAvatar ?? ''
})

/**
 * 上传前校验
 */
const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

/**
 * 自定义上传
 */
const handleUpload = async (options: any) => {
  uploading.value = true
  try {
    const res = await uploadAvatar(options.file)
    if (res.data.code === 0 && res.data.data) {
      avatarUrl.value = res.data.data
      profileForm.userAvatar = res.data.data
      await loginUserStore.fetchLoginUser()
      message.success('头像上传成功')
    } else {
      message.error('上传失败，' + res.data.message)
    }
  } catch {
    message.error('上传失败')
  } finally {
    uploading.value = false
  }
}

/**
 * 更新基本资料
 */
const handleUpdateProfile = async () => {
  const res = await updateMyUser({
    userName: profileForm.userName,
    userAvatar: profileForm.userAvatar || avatarUrl.value,
  })
  if (res.data.code === 0) {
    await loginUserStore.fetchLoginUser()
    message.success('保存成功')
  } else {
    message.error('保存失败，' + res.data.message)
  }
}

/**
 * 修改密码
 */
const handleChangePassword = async () => {
  if (passwordForm.newPassword !== passwordForm.checkPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  const res = await changePassword({
    oldPassword: passwordForm.oldPassword,
    newPassword: passwordForm.newPassword,
    checkPassword: passwordForm.checkPassword,
  })
  if (res.data.code === 0) {
    message.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.checkPassword = ''
  } else {
    message.error('修改失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userCenterPage {
  background: var(--color-surface);
  max-width: 480px;
  padding: 36px 32px;
  margin: 40px auto;
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
}

.title {
  text-align: center;
  margin-bottom: 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: -0.02em;
}

.desc {
  text-align: center;
  color: var(--color-text-muted);
  margin-bottom: 28px;
  font-size: 14px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border-light);
  letter-spacing: -0.01em;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 16px;
}
</style>
