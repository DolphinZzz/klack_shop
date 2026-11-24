export type UserCenterMenuKey =
  | 'orders'
  | 'comments'
  | 'favorite'
  | 'balance'
  | 'coupon'
  | 'integral'
  | 'footprint'
  | 'afterSale'
  | 'address'
  | 'profile'
  | 'password';

export const siteNavList = [
  { key: 'home', name: '工坊首页' },
  { key: 'products', name: '外设图鉴' },
  { key: 'flash', name: '限时闪购' },
  { key: 'coupon', name: '礼券工坊' },
  { key: 'notice', name: '极客志' },
  { key: 'order', name: '订单中心' },
  { key: 'mine-coupon', name: '我的优惠券' }
];

export const userCenterNavList = siteNavList;

export const userCenterRouteMap: Record<UserCenterMenuKey, string> = {
  orders: '/user/order/list',
  comments: '/user/comment/list',
  favorite: '/user/favorite/list',
  balance: '/user/wallet/balance',
  coupon: '/user/wallet/coupon',
  integral: '/user/wallet/integral',
  footprint: '/user/history/footprint',
  afterSale: '/user/service/after-sale',
  address: '/user/account/address',
  profile: '/user/account/profile',
  password: '/user/account/password'
};

export const userOrderDetailRoute = '/user/order/detail';

export const createUserCenterSideGroups = (activeKey: UserCenterMenuKey) => [
  {
    title: '订单与评价',
    items: [
      { label: '我的订单', key: 'orders' as const, active: activeKey === 'orders' },
      { label: '评价晒单', key: 'comments' as const, active: activeKey === 'comments' }
    ]
  },
  {
    title: '造物收藏',
    items: [
      { label: '收藏清单', key: 'favorite' as const, active: activeKey === 'favorite' },
      { label: '浏览足迹', key: 'footprint' as const, active: activeKey === 'footprint' }
    ]
  },
  {
    title: '资产与权益',
    items: [
      { label: '我的余额', key: 'balance' as const, active: activeKey === 'balance' },
      { label: '我的优惠券', key: 'coupon' as const, active: activeKey === 'coupon' },
      { label: '造物积分', key: 'integral' as const, active: activeKey === 'integral' }
    ]
  },
  {
    title: '售后保障',
    items: [{ label: '售后 / 退款', key: 'afterSale' as const, active: activeKey === 'afterSale' }]
  },
  {
    title: '账户设置',
    items: [
      { label: '收货地址', key: 'address' as const, active: activeKey === 'address' },
      { label: '个人资料', key: 'profile' as const, active: activeKey === 'profile' },
      { label: '账号密码', key: 'password' as const, active: activeKey === 'password' }
    ]
  }
];
