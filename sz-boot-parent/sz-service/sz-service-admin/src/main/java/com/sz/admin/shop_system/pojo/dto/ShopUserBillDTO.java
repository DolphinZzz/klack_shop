package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * ShopUserBillDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopUserBillDTO {

    /**
     * <p>
     * ShopUserBill添加DTO
     * </p>
     *
     * @author sz-admin
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopUserBill添加DTO")
    public static class Create {

    }

    /**
     * <p>
     * ShopUserBill修改DTO
     * </p>
     *
     * @author sz-admin
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopUserBill修改DTO")
    public static class Update {

        @Schema(description =  "id")
        private Long id;

    }

    /**
     * <p>
     * ShopUserBill查询DTO
     * </p>
     *
     * @author sz-admin
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopUserBill查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description =  "用户id")
        private Long uid;

        @Schema(description =  "关联id")
        private String linkId;

        @Schema(description =  "收支")
        private Integer pm;

        @Schema(description =  "账单标题")
        private String title;

        @Schema(description =  "明细种类")
        private String category;

        @Schema(description =  "明细类型")
        private String type;

        @Schema(description =  "明细数字")
        private BigDecimal number;

        @Schema(description =  "剩余")
        private BigDecimal balance;

        @Schema(description =  "备注")
        private String mark;

        @Schema(description =  "状态")
        private Integer status;

    }

    @Data
    @Schema(description = "PC端用户余额账单查询DTO")
    public static class FrontList extends PageQuery {

        @Schema(description = "标签页 all-全部 expend-支出 recharge-充值 income-收入")
        private String tab;
    }

    @Data
    @Schema(description = "PC端用户积分明细查询DTO")
    public static class FrontIntegralList extends PageQuery {
    }
}
