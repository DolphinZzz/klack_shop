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
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.exception.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysMqLogServiceImplTest {

    @Mock
    private SysMqSendLogMapper sendLogMapper;
    @Mock
    private SysMqConsumeLogMapper consumeLogMapper;

    private SysMqLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SysMqLogServiceImpl(sendLogMapper, consumeLogMapper);
    }

    @Test
    void shouldReturnPagedSendAndConsumeLogs() {
        SysMqSendLogVO sendVo = new SysMqSendLogVO();
        sendVo.setId(1L);
        sendVo.setMessageId("m1");
        Page<SysMqSendLogVO> sendPage = new Page<>(List.of(sendVo), 1, 10, 1);
        when(sendLogMapper.paginateAs(any(Page.class), any(QueryWrapper.class), eq(SysMqSendLogVO.class)))
                .thenReturn(sendPage);

        PageResult<SysMqSendLogVO> sendRes = service.sendPage(new SysMqSendLogListDTO().setMessageId("m1"));
        assertEquals(1, sendRes.getTotal());
        assertEquals("m1", sendRes.getRows().get(0).getMessageId());

        SysMqConsumeLogVO consumeVo = new SysMqConsumeLogVO();
        consumeVo.setId(2L);
        consumeVo.setAckStatus("ACKED");
        Page<SysMqConsumeLogVO> consumePage = new Page<>(List.of(consumeVo), 1, 10, 1);
        when(consumeLogMapper.paginateAs(any(Page.class), any(QueryWrapper.class), eq(SysMqConsumeLogVO.class)))
                .thenReturn(consumePage);

        PageResult<SysMqConsumeLogVO> consumeRes = service.consumePage(new SysMqConsumeLogListDTO().setAckStatus("ACKED"));
        assertEquals(1, consumeRes.getTotal());
        assertEquals("ACKED", consumeRes.getRows().get(0).getAckStatus());
    }

    @Test
    void shouldReturnDetailOrThrowWhenNotFound() {
        SysMqSendLog send = new SysMqSendLog().setId(1L).setMessageId("m1");
        when(sendLogMapper.selectOneById(1L)).thenReturn(send);
        assertEquals("m1", service.sendDetail(1L).getMessageId());

        when(sendLogMapper.selectOneById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.sendDetail(999L));

        SysMqConsumeLog consume = new SysMqConsumeLog().setId(2L).setAckStatus("ACKED");
        when(consumeLogMapper.selectOneById(2L)).thenReturn(consume);
        assertEquals("ACKED", service.consumeDetail(2L).getAckStatus());

        when(consumeLogMapper.selectOneById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.consumeDetail(999L));
    }
}
