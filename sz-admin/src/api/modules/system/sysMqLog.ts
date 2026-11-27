import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage } from '@/api/types';
import type { SysMqConsumeLogQuery, SysMqConsumeLogRow, SysMqSendLogQuery, SysMqSendLogRow } from '@/api/types/system/sysMqLog';

/**
 * 查询 MQ 发送记录分页
 */
export const getSysMqSendLogListApi = (params: SysMqSendLogQuery) => {
  return http.get<IPage<SysMqSendLogRow>>(ADMIN_MODULE + `/sys-mq-log/send`, params);
};

/**
 * 查询 MQ 发送记录详情
 */
export const getSysMqSendLogDetailApi = (id: number | string) => {
  return http.get<SysMqSendLogRow>(ADMIN_MODULE + `/sys-mq-log/send/${id}`);
};

/**
 * 查询 MQ 消费记录分页
 */
export const getSysMqConsumeLogListApi = (params: SysMqConsumeLogQuery) => {
  return http.get<IPage<SysMqConsumeLogRow>>(ADMIN_MODULE + `/sys-mq-log/consume`, params);
};

/**
 * 查询 MQ 消费记录详情
 */
export const getSysMqConsumeLogDetailApi = (id: number | string) => {
  return http.get<SysMqConsumeLogRow>(ADMIN_MODULE + `/sys-mq-log/consume/${id}`);
};
