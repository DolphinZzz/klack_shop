package com.sz.admin.system.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.sz.admin.system.mapper.SysMqConsumeLogMapper;
import com.sz.admin.system.mapper.SysMqSendLogMapper;
import com.sz.admin.system.pojo.dto.SysMqConsumeLogListDTO;
import com.sz.admin.system.pojo.dto.SysMqSendLogListDTO;
import com.sz.admin.system.pojo.po.SysMqConsumeLog;
import com.sz.admin.system.pojo.po.SysMqSendLog;
import com.sz.admin.system.pojo.vo.SysMqConsumeLogVO;
import com.sz.admin.system.pojo.vo.SysMqSendLogVO;
import com.sz.admin.system.service.SysMqLogService;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.util.BeanCopyUtils;
import com.sz.core.util.PageUtils;
import com.sz.core.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * MQ 发送与消费记录查询服务实现
 */
@Service
@RequiredArgsConstructor
public class SysMqLogServiceImpl implements SysMqLogService {

    private final SysMqSendLogMapper sendLogMapper;
    private final SysMqConsumeLogMapper consumeLogMapper;

    @Override
    public PageResult<SysMqSendLogVO> sendPage(SysMqSendLogListDTO dto) {
        QueryWrapper wrapper = buildSendQueryWrapper(dto);
        Page<SysMqSendLogVO> page = sendLogMapper.paginateAs(PageUtils.getPage(dto), wrapper, SysMqSendLogVO.class);
        return PageUtils.getPageResult(page);
    }

    @Override
    public SysMqSendLogVO sendDetail(Long id) {
        SysMqSendLog entity = id == null ? null : sendLogMapper.selectOneById(id);
        CommonResponseEnum.INVALID_ID.assertNull(entity);
        return BeanCopyUtils.copy(entity, SysMqSendLogVO.class);
    }

    @Override
    public PageResult<SysMqConsumeLogVO> consumePage(SysMqConsumeLogListDTO dto) {
        QueryWrapper wrapper = buildConsumeQueryWrapper(dto);
        Page<SysMqConsumeLogVO> page = consumeLogMapper.paginateAs(PageUtils.getPage(dto), wrapper, SysMqConsumeLogVO.class);
        return PageUtils.getPageResult(page);
    }

    @Override
    public SysMqConsumeLogVO consumeDetail(Long id) {
        SysMqConsumeLog entity = id == null ? null : consumeLogMapper.selectOneById(id);
        CommonResponseEnum.INVALID_ID.assertNull(entity);
        return BeanCopyUtils.copy(entity, SysMqConsumeLogVO.class);
    }

    private QueryWrapper buildSendQueryWrapper(SysMqSendLogListDTO dto) {
        QueryWrapper wrapper = QueryWrapper.create().from(SysMqSendLog.class);
        if (dto != null) {
            if (Utils.isNotNull(dto.getMessageId())) {
                wrapper.like(SysMqSendLog::getMessageId, dto.getMessageId().trim());
            }
            if (Utils.isNotNull(dto.getBizKey())) {
                wrapper.like(SysMqSendLog::getBizKey, dto.getBizKey().trim());
            }
            if (Utils.isNotNull(dto.getTopic())) {
                wrapper.like(SysMqSendLog::getTopic, dto.getTopic().trim());
            }
            if (Utils.isNotNull(dto.getMqType())) {
                wrapper.eq(SysMqSendLog::getMqType, dto.getMqType().trim());
            }
            if (Utils.isNotNull(dto.getSendStatus())) {
                wrapper.eq(SysMqSendLog::getSendStatus, dto.getSendStatus().trim());
            }
            if (dto.getCreateTimeStart() != null && dto.getCreateTimeEnd() != null) {
                wrapper.between(SysMqSendLog::getCreateTime, dto.getCreateTimeStart(), dto.getCreateTimeEnd());
            }
        }
        wrapper.orderBy(SysMqSendLog::getId).desc();
        return wrapper;
    }

    private QueryWrapper buildConsumeQueryWrapper(SysMqConsumeLogListDTO dto) {
        QueryWrapper wrapper = QueryWrapper.create().from(SysMqConsumeLog.class);
        if (dto != null) {
            if (Utils.isNotNull(dto.getMessageId())) {
                wrapper.like(SysMqConsumeLog::getMessageId, dto.getMessageId().trim());
            }
            if (Utils.isNotNull(dto.getBizKey())) {
                wrapper.like(SysMqConsumeLog::getBizKey, dto.getBizKey().trim());
            }
            if (Utils.isNotNull(dto.getTopic())) {
                wrapper.like(SysMqConsumeLog::getTopic, dto.getTopic().trim());
            }
            if (Utils.isNotNull(dto.getConsumerGroup())) {
                wrapper.like(SysMqConsumeLog::getConsumerGroup, dto.getConsumerGroup().trim());
            }
            if (Utils.isNotNull(dto.getMqType())) {
                wrapper.eq(SysMqConsumeLog::getMqType, dto.getMqType().trim());
            }
            if (Utils.isNotNull(dto.getConsumeStatus())) {
                wrapper.eq(SysMqConsumeLog::getConsumeStatus, dto.getConsumeStatus().trim());
            }
            if (Utils.isNotNull(dto.getAckStatus())) {
                wrapper.eq(SysMqConsumeLog::getAckStatus, dto.getAckStatus().trim());
            }
            if (dto.getCreateTimeStart() != null && dto.getCreateTimeEnd() != null) {
                wrapper.between(SysMqConsumeLog::getCreateTime, dto.getCreateTimeStart(), dto.getCreateTimeEnd());
            }
        }
        wrapper.orderBy(SysMqConsumeLog::getId).desc();
        return wrapper;
    }
}
