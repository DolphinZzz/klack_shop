package com.sz.admin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.system.pojo.dto.SysMqConsumeLogListDTO;
import com.sz.admin.system.pojo.dto.SysMqSendLogListDTO;
import com.sz.admin.system.pojo.vo.SysMqConsumeLogVO;
import com.sz.admin.system.pojo.vo.SysMqSendLogVO;
import com.sz.admin.system.service.SysMqLogService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MQ 发送与消费记录查询 Controller
 */
@Tag(name = "MQ消息记录")
@RestController
@RequestMapping("sys-mq-log")
@RequiredArgsConstructor
public class SysMqLogController {

    private final SysMqLogService sysMqLogService;

    @Operation(summary = "发送记录分页查询")
    @SaCheckPermission(value = "sys.mq.log.query_table")
    @GetMapping("/send")
    public ApiResult<PageResult<SysMqSendLogVO>> sendPage(SysMqSendLogListDTO dto) {
        return ApiPageResult.success(sysMqLogService.sendPage(dto));
    }

    @Operation(summary = "发送记录详情")
    @SaCheckPermission(value = "sys.mq.log.query_table")
    @GetMapping("/send/{id}")
    public ApiResult<SysMqSendLogVO> sendDetail(@PathVariable Long id) {
        return ApiResult.success(sysMqLogService.sendDetail(id));
    }

    @Operation(summary = "消费记录分页查询")
    @SaCheckPermission(value = "sys.mq.log.query_table")
    @GetMapping("/consume")
    public ApiResult<PageResult<SysMqConsumeLogVO>> consumePage(SysMqConsumeLogListDTO dto) {
        return ApiPageResult.success(sysMqLogService.consumePage(dto));
    }

    @Operation(summary = "消费记录详情")
    @SaCheckPermission(value = "sys.mq.log.query_table")
    @GetMapping("/consume/{id}")
    public ApiResult<SysMqConsumeLogVO> consumeDetail(@PathVariable Long id) {
        return ApiResult.success(sysMqLogService.consumeDetail(id));
    }
}
