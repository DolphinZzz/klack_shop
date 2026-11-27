package com.sz.admin.system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MQ 发送记录 VO
 */
@Data
@Schema(description = "MQ发送记录VO")
public class SysMqSendLogVO {

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

    @Schema(description = "业务键")
    private String bizKey;

    @Schema(description = "消息JSON载荷")
    private String payload;

    @Schema(description = "发送状态")
    private String sendStatus;

    @Schema(description = "Broker原始消息ID")
    private String rawMessageId;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "发送成功时间")
    private LocalDateTime sentTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
