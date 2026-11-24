package com.sz.admin.shop_system.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * ShopUserFootprintDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopUserFootprintDTO {

    @Data
    @Schema(description = "PC端用户足迹查询DTO")
    public static class FrontList {

        @Schema(description = "展示条数，默认20，最大20")
        private Integer limit;
    }
}
