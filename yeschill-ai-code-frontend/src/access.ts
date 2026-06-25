import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import router from '@/router'

// 是否为首次获取登录用户
let firstFetchLoginUser = true

/**
 * 全局权限校验
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser
  // 确保页面刷新，首次加载时，能够等后端返回用户信息后再校验权限
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }
  const toUrl = to.fullPath
  // 登录和注册页不需要校验
  if (toUrl.startsWith('/user/login') || toUrl.startsWith('/user/register')) {
    next()
    return
  }
  // 未登录一律跳转登录页
  if (!loginUser || !loginUser.id) {
    next(`/user/login?redirect=${to.fullPath}`)
    return
  }
  // 管理页需要管理员权限
  if (toUrl.startsWith('/admin')) {
    if (loginUser.userRole !== 'admin') {
      message.error('没有权限')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  next()
})
