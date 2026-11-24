package com.sz.admin.shop_system.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopProductCouponMapper;
import com.sz.admin.shop_system.mapper.ShopProductCouponUserMapper;
import com.sz.admin.shop_system.pojo.dto.ShopProductCouponDTO;
import com.sz.admin.shop_system.pojo.po.ShopProductCoupon;
import com.sz.admin.shop_system.pojo.po.ShopProductCouponUser;
import com.sz.admin.shop_system.pojo.vo.ShopCouponAndUserVO;
import com.sz.admin.shop_system.service.ShopProductCouponUserService;
import com.sz.core.common.entity.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券使用表 服务实现类
 * </p>
 *
 * @author car
 * @since 2026-02-10
 */
@Service
@RequiredArgsConstructor
public class ShopProductCouponUserServiceImpl extends ServiceImpl<ShopProductCouponUserMapper, ShopProductCouponUser> implements ShopProductCouponUserService {

    private final ShopProductCouponMapper shopProductCouponMapper;

    @Override
    public PageResult<ShopCouponAndUserVO> frontPage(Long userId, ShopProductCouponDTO.FrontUserList dto) {
        int page = dto == null || dto.getPage() == null || dto.getPage() < 1 ? 1 : dto.getPage();
        int limit = dto == null || dto.getLimit() == null || dto.getLimit() < 1 ? 9 : dto.getLimit();
        if (userId == null) {
            return new PageResult<>(page, limit, 0L, 0, Collections.emptyList());
        }

        List<ShopProductCouponUser> userCoupons = mapper.selectByUidOrderByCreateTimeDesc(userId);
        if (userCoupons.isEmpty()) {
            return new PageResult<>(page, limit, 0L, 0, Collections.emptyList());
        }

        Map<Long, ShopProductCoupon> couponMap = loadCouponMap(userCoupons);
        LocalDateTime now = LocalDateTime.now();
        String tab = dto == null ? null : dto.getTab();

        List<ShopCouponAndUserVO> rows = userCoupons.stream()
                .map(item -> assembleCouponUserVO(item, couponMap.get(item.getCid()), now))
                .filter(Objects::nonNull)
                .filter(item -> matchesFrontCouponTab(item, tab))
                .toList();

        return paginateInMemory(rows, page, limit);
    }

    private Map<Long, ShopProductCoupon> loadCouponMap(List<ShopProductCouponUser> userCoupons) {
        List<Long> couponIds = userCoupons.stream()
                .map(ShopProductCouponUser::getCid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (couponIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return shopProductCouponMapper.selectByIds(couponIds).stream()
                .collect(Collectors.toMap(ShopProductCoupon::getId, item -> item, (a, b) -> a));
    }

    private ShopCouponAndUserVO assembleCouponUserVO(ShopProductCouponUser item, ShopProductCoupon coupon, LocalDateTime now) {
        if (coupon == null) {
            return null;
        }
        int status = normalizeCouponStatus(item, coupon, now);
        syncCouponStatusIfChanged(item, status, now);

        ShopCouponAndUserVO vo = new ShopCouponAndUserVO();
        vo.setId(item.getId());
        vo.setCid(item.getCid());
        vo.setName(coupon.getName());
        vo.setMoney(coupon.getMoney());
        vo.setMinMoney(coupon.getMinMoney());
        vo.setCouponEndTime(coupon.getCouponEndTime());
        vo.setUseStartTime(coupon.getUseStartTime());
        vo.setUseEndTime(coupon.getUseEndTime());
        vo.setUid(item.getUid());
        vo.setType(item.getType());
        vo.setStatus(status);
        vo.setCreateTime(item.getCreateTime());
        vo.setUpdateTime(item.getUpdateTime());
        vo.setUseTime(item.getUseTime());
        vo.setUseType(item.getUseType());
        return vo;
    }

    private void syncCouponStatusIfChanged(ShopProductCouponUser item, int status, LocalDateTime now) {
        if (!Objects.equals(status, item.getStatus())) {
            ShopProductCouponUser update = new ShopProductCouponUser();
            update.setId(item.getId());
            update.setStatus(status);
            update.setUpdateTime(now);
            updateById(update);
            item.setStatus(status);
        }
    }

    private static PageResult<ShopCouponAndUserVO> paginateInMemory(List<ShopCouponAndUserVO> rows, int page, int limit) {
        int total = rows.size();
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex = Math.min(fromIndex + limit, total);
        long totalPage = total == 0 ? 0 : (total + limit - 1L) / limit;
        return new PageResult<>(page, limit, totalPage, total, rows.subList(fromIndex, toIndex));
    }

    private static int normalizeCouponStatus(ShopProductCouponUser userCoupon, ShopProductCoupon coupon, LocalDateTime now) {
        int status = userCoupon.getStatus() == null ? 0 : userCoupon.getStatus();
        boolean expired = (coupon.getCouponEndTime() != null && now.isAfter(coupon.getCouponEndTime()))
                || (coupon.getUseEndTime() != null && now.isAfter(coupon.getUseEndTime()));
        if (status != 1 && (status == 2 || expired)) {
            return 2;
        }
        return status;
    }

    private static boolean matchesFrontCouponTab(ShopCouponAndUserVO item, String tab) {
        String current = tab == null ? "all" : tab.trim().toLowerCase(Locale.ROOT);
        if (current.isBlank() || "all".equals(current)) {
            return true;
        }
        if ("unused".equals(current)) {
            return item.getStatus() != null && item.getStatus() == 0;
        }
        if ("used_expired".equals(current)) {
            return item.getStatus() != null && item.getStatus() != 0;
        }
        return true;
    }
}
