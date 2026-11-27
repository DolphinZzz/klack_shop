package com.sz.admin.system.service;

import com.sz.admin.system.pojo.dto.SysMqConsumeLogListDTO;
import com.sz.admin.system.pojo.dto.SysMqSendLogListDTO;
import com.sz.admin.system.pojo.vo.SysMqConsumeLogVO;
import com.sz.admin.system.pojo.vo.SysMqSendLogVO;
import com.sz.core.common.entity.PageResult;

/**
 * MQ 发送与消费记录查询服务接口
 */
public interface SysMqLogService {

    PageResult<SysMqSendLogVO> sendPage(SysMqSendLogListDTO dto);

    SysMqSendLogVO sendDetail(Long id);

    PageResult<SysMqConsumeLogVO> consumePage(SysMqConsumeLogListDTO dto);

    SysMqConsumeLogVO consumeDetail(Long id);
}
