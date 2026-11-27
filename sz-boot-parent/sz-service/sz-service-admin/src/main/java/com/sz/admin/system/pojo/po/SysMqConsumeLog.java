package com.sz.admin.system.pojo.po;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MQ 消息消费与 ACK 记录表
 */
@Data
@Accessors(chain = true)
@Table("sys_mq_consume_log")
@Schema(description = "MQ消息消费记录表")
public class SysMqConsumeLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "全局消息ID")
    private String messageId;

    @Schema(description = "消费组名称")
    private String consumerGroup;

    @Schema(description = "MQ引擎类型")
    private String mqType;

    @Schema(description = "消息主题")
    private String topic;

    @Schema(description = "消息标签/路由键")
    private String tag;

    @Schema(description = "业务键（如订单号）")
    private String bizKey;

    @Schema(description = "消息JSON载荷")
    private String payload;

    @Schema(description = "消费状态：CONSUMING/SUCCESS/RETRYING/DEAD")
    private String consumeStatus;

    @Schema(description = "ACK状态：PENDING/ACKED/NACKED_DLQ/SENT_DLT/BROKER_DLQ")
    private String ackStatus;

    @Schema(description = "已重试次数")
    private Integer retryCount;

    @Schema(description = "消费异常信息")
    private String errorMsg;

    @Schema(description = "首次接收消息时间")
    private LocalDateTime receiveTime;

    @Schema(description = "业务消费完成时间")
    private LocalDateTime consumeTime;

    @Schema(description = "ACK/死信确认时间")
    private LocalDateTime ackTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
