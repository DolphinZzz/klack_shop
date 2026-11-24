package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * ShopUserFavoriteDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopUserFavoriteDTO {

    @Data
    @Schema(description = "PC端用户收藏查询DTO")
    public static class FrontList extends PageQuery {
    }
}
