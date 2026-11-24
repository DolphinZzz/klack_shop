export interface ApiResult<T = unknown> {
  code: string;
  message: string;
  data: T;
}

export interface PageResult<T = unknown> {
  current: number;
  limit: number;
  totalPage: number;
  total: number;
  rows: T[];
}

export interface PageQuery {
  page?: number;
  limit?: number;
}
