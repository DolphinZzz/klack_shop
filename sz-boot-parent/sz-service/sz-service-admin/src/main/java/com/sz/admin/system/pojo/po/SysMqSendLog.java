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
 * MQ 消息发送记录表
 */
@Data
@Accessors(chain = true)
@Table("sys_mq_send_log")
@Schema(description = "MQ消息发送记录表")
public class SysMqSendLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "全局消息ID")
    private String messageId;

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

    @Schema(description = "发送状态：PENDING/SENT/FAILED")
    private String sendStatus;

    @Schema(description = "Broker返回原始消息ID或偏移量")
    private String rawMessageId;

    @Schema(description = "发送失败错误信息")
    private String errorMsg;

    @Schema(description = "Broker确认发送成功时间")
    private LocalDateTime sentTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
