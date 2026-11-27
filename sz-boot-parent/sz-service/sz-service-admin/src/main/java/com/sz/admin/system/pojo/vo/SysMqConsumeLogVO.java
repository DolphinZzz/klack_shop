package com.sz.admin.system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MQ 消费记录 VO
 */
@Data
@Schema(description = "MQ消费记录VO")
public class SysMqConsumeLogVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "全局消息ID")
    private String messageId;

    @Schema(description = "消费组")
    private String consumerGroup;

    @Schema(description = "MQ引擎类型")
    private String mqType;

    @Schema(description = "消息主题")
    private String topic;

    @Schema(description = "消息标签/路由键")
    private String tag;

    @Schema(description = "业务键")
    private String bizKey;

    @Schema(description = "消息JSON载荷")
    private String payload;

    @Schema(description = "消费状态")
    private String consumeStatus;

    @Schema(description = "ACK确认状态")
    private String ackStatus;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "接收时间")
    private LocalDateTime receiveTime;

    @Schema(description = "消费成功时间")
    private LocalDateTime consumeTime;

    @Schema(description = "ACK确认时间")
    private LocalDateTime ackTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
