import { http } from '@/request/http';

export interface HomeCategoryItem {
  id: number;
  pid: number;
  cateName: string;
  children: HomeCategoryItem[];
}

export interface HomeBannerItem {
  id: number;
  title: string;
  image: string;
  link: string;
  sort: number;
}

export interface HomeNewBookItem {
  id: number;
  productName: string;
  image: string;
  price: number;
  sales: number;
  browse: number;
  isBest: number;
  createTime: string;
}

export interface HomeNewsItem {
  id: number;
  categoryId: number;
  title: string;
  tagName: string;
  coverImage: string;
  summary: string;
  content: string;
  link: string;
  source: string;
  author: string;
  browseCount: number;
  sort: number;
  isTop: number;
  status: number;
  publishTime: string;
  startTime: string;
  endTime: string;
  remark: string;
}

export interface HomeFlashSaleItem {
  id: number;
  productId: number;
  productName: string;
  image: string;
  price: number;
  flashPrice: number;
  stock: number;
  sales: number;
  browse: number;
  fictiBrowse: number;
  limitPerUser: number;
  status: number;
  statusText: string;
  canRush: boolean;
  remainingSeconds: number;
  startTime: string;
  endTime: string;
}

export const getHomeCategoryApi = () => {
  return http.get<HomeCategoryItem[]>('/client/shopCategory/index', undefined, { skipAuth: true });
};

export const getHomeBannerApi = () => {
  return http.get<HomeBannerItem[]>('/client/shopHomeBanner/index', undefined, { skipAuth: true });
};

export const getHomeNewBooksApi = (size = 8) => {
  return http.get<HomeNewBookItem[]>('/client/shopHome/recommend/new', { size }, { skipAuth: true });
};

export const getHomeGuessLikeApi = (size = 8) => {
  return http.get<HomeNewBookItem[]>('/client/shopHome/recommend/guess-like', { size }, { skipAuth: true });
};

export const getHomeNewsApi = (size = 3) => {
  return http.get<HomeNewsItem[]>('/client/shopHomeNews/index', { size }, { skipAuth: true });
};

export const getHomeFlashSaleApi = (size = 4) => {
  return http.get<HomeFlashSaleItem[]>('/client/shopHome/flash', { size }, { skipAuth: true });
};
