import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/modules/auth';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/home/index.vue'),
    meta: { title: '探索工坊', public: true }
  },
  {
    path: '/flash-sale',
    name: 'FlashSale',
    component: () => import('@/views/flashSale/index.vue'),
    meta: { title: '限时闪购', public: true }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/product/detail.vue'),
    meta: { title: '外设规格', public: true }
  },
  {
    path: '/products',
    name: 'ProductList',
    component: () => import('@/views/product/list.vue'),
    meta: { title: '外设图鉴', public: true }
  },
  {
    path: '/coupon-center',
    name: 'CouponCenter',
    component: () => import('@/views/couponCenter.vue'),
    meta: { title: '礼券工坊', requiresAuth: true }
  },
  {
    path: '/notice',
    name: 'NoticeList',
    component: () => import('@/views/notice/index.vue'),
    meta: { title: '极客志', public: true }
  },
  {
    path: '/notice/:id',
    name: 'NoticeDetail',
    component: () => import('@/views/notice/detail.vue'),
    meta: { title: '极客志正文', public: true }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/cart/index.vue'),
    meta: { title: '购物车', requiresAuth: true }
  },
  {
    path: '/order/confirm',
    name: 'OrderConfirm',
    component: () => import('@/views/orderConfirm.vue'),
    meta: { title: '提交订单', requiresAuth: true }
  },
  {
    path: '/order_payment',
    name: 'OrderPayment',
    component: () => import('@/views/orderPayment.vue'),
    meta: { title: '订单支付', requiresAuth: true }
  },
  {
    path: '/user/order/list',
    name: 'UserOrderList',
    component: () => import('@/views/userOrderList.vue'),
    meta: { title: '订单中心', requiresAuth: true }
  },
  {
    path: '/user/comment/list',
    name: 'UserCommentList',
    component: () => import('@/views/userCommentList.vue'),
    meta: { title: '评价晒单', requiresAuth: true }
  },
  {
    path: '/user/favorite/list',
    name: 'UserFavoriteList',
    component: () => import('@/views/userFavorite.vue'),
    meta: { title: '收藏的宝贝', requiresAuth: true }
  },
  {
    path: '/user/order/detail',
    name: 'UserOrderDetail',
    component: () => import('@/views/userOrderDetail.vue'),
    meta: { title: '订单详情', requiresAuth: true }
  },
  {
    path: '/user/account/profile',
    name: 'UserAccountProfile',
    component: () => import('@/views/userProfile.vue'),
    meta: { title: '个人资料', requiresAuth: true }
  },
  {
    path: '/user/account/address',
    name: 'UserAccountAddress',
    component: () => import('@/views/userAddress.vue'),
    meta: { title: '收货地址', requiresAuth: true }
  },
  {
    path: '/user/account/password',
    name: 'UserAccountPassword',
    component: () => import('@/views/userPassword.vue'),
    meta: { title: '账号密码', requiresAuth: true }
  },
  {
    path: '/user/wallet/balance',
    name: 'UserWalletBalance',
    component: () => import('@/views/userBalance.vue'),
    meta: { title: '我的余额', requiresAuth: true }
  },
  {
    path: '/user/wallet/coupon',
    name: 'UserWalletCoupon',
    component: () => import('@/views/userCoupon.vue'),
    meta: { title: '我的优惠券', requiresAuth: true }
  },
  {
    path: '/user/wallet/integral',
    name: 'UserWalletIntegral',
    component: () => import('@/views/userIntegral.vue'),
    meta: { title: '我的积分', requiresAuth: true }
  },
  {
    path: '/user/history/footprint',
    name: 'UserHistoryFootprint',
    component: () => import('@/views/userFootprint.vue'),
    meta: { title: '我的足迹', requiresAuth: true }
  },
  {
    path: '/user/service/after-sale',
    name: 'UserAfterSale',
    component: () => import('@/views/userAfterSale.vue'),
    meta: { title: '售后 / 退款', requiresAuth: true }
  },
  {
    path: '/user/recharge/list',
    name: 'UserRechargeList',
    component: () => import('@/views/userRechargeList.vue'),
    meta: { title: '余额充值', requiresAuth: true }
  },
  {
    path: '/users/order_list',
    redirect: to => ({ path: '/user/order/list', query: to.query })
  },
  {
    path: '/users/comment_list',
    redirect: to => ({ path: '/user/comment/list', query: to.query })
  },
  {
    path: '/user/order/order_details',
    redirect: to => ({ path: '/user/order/detail', query: to.query })
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录 / 注册', public: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, _, next) => {
  const title = import.meta.env.VITE_APP_TITLE || '咔嗒造物商城 · KlackCraft';
  document.title = to.meta.title ? `${to.meta.title} · ${title}` : title;

  const authStore = useAuthStore();
  const requiresAuth = Boolean(to.meta.requiresAuth);
  const redirectPath = typeof to.query.redirect === 'string' ? to.query.redirect : '/home';

  if (to.path === '/login' && authStore.isLogin) {
    next(redirectPath);
    return;
  }

  if (requiresAuth && !authStore.isLogin) {
    ElMessage.warning('请先登录');
    next({
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    });
    return;
  }

  next();
});

export default router;

