package com.sz.admin.system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * MQ 消费记录查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "MQ消费记录查询DTO")
public class SysMqConsumeLogListDTO extends PageQuery {

    @Schema(description = "全局消息ID")
    private String messageId;

    @Schema(description = "业务键（如订单号）")
    private String bizKey;

    @Schema(description = "消息主题")
    private String topic;

    @Schema(description = "消费组")
    private String consumerGroup;

    @Schema(description = "MQ引擎类型")
    private String mqType;

    @Schema(description = "消费状态：CONSUMING/SUCCESS/RETRYING/DEAD")
    private String consumeStatus;

    @Schema(description = "ACK状态：PENDING/ACKED/NACKED_DLQ/SENT_DLT/BROKER_DLQ")
    private String ackStatus;

    @Schema(description = "创建时间开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeStart;

    @Schema(description = "创建时间结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeEnd;
}
