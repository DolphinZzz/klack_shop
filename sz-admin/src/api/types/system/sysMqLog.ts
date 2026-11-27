import type { IPageQuery } from '@/api/types';

export type SysMqSendLogQuery = IPageQuery & {
  messageId?: string;
  bizKey?: string;
  topic?: string;
  mqType?: string;
  sendStatus?: string;
  createTimeStart?: string;
  createTimeEnd?: string;
};

export type SysMqSendLogRow = {
  id: number;
  messageId: string;
  mqType: string;
  topic: string;
  tag: string;
  bizKey: string;
  payload: string;
  sendStatus: string;
  rawMessageId: string;
  errorMsg: string;
  sentTime: string;
  createTime: string;
  updateTime: string;
};

export type SysMqConsumeLogQuery = IPageQuery & {
  messageId?: string;
  bizKey?: string;
  topic?: string;
  consumerGroup?: string;
  mqType?: string;
  consumeStatus?: string;
  ackStatus?: string;
  createTimeStart?: string;
  createTimeEnd?: string;
};

export type SysMqConsumeLogRow = {
  id: number;
  messageId: string;
  consumerGroup: string;
  mqType: string;
  topic: string;
  tag: string;
  bizKey: string;
  payload: string;
  consumeStatus: string;
  ackStatus: string;
  retryCount: number;
  errorMsg: string;
  receiveTime: string;
  consumeTime: string;
  ackTime: string;
  createTime: string;
  updateTime: string;
};
