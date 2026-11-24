package com.sz.admin.shop_system.service.impl;

import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.constants.ShopPaymentConstant;
import com.sz.admin.shop_system.mapper.ShopUserIntegralBillMapper;
import com.sz.admin.shop_system.mapper.ShopUserMapper;
import com.sz.admin.shop_system.pojo.dto.ShopUserBillDTO;
import com.sz.admin.shop_system.pojo.po.ShopUser;
import com.sz.admin.shop_system.pojo.po.ShopUserIntegralBill;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserIntegralBillVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserIntegralSummaryVO;
import com.sz.admin.shop_system.service.ShopUserIntegralBillService;
import com.sz.core.common.entity.PageQuery;
import com.sz.core.common.entity.PageResult;
import com.sz.core.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopUserIntegralBillServiceImpl extends ServiceImpl<ShopUserIntegralBillMapper, ShopUserIntegralBill> implements ShopUserIntegralBillService {

    private final ShopUserMapper lbUserMapper;

    @Override
    public PageResult<ShopFrontUserIntegralBillVO> frontPage(Long userId, ShopUserBillDTO.FrontIntegralList dto) {
        int page = dto == null || dto.getPage() == null || dto.getPage() < 1 ? 1 : dto.getPage();
        int limit = dto == null || dto.getLimit() == null || dto.getLimit() < 1 ? 10 : dto.getLimit();
        if (userId == null) {
            return new PageResult<>(page, limit, 0L, 0, Collections.emptyList());
        }

        PageQuery pageQuery = buildPageQuery(page, limit);
        PageUtils.toPage(pageQuery);
        try {
            List<ShopFrontUserIntegralBillVO> rows = mapper.selectFrontIntegralBillList(
                    userId,
                    ShopPaymentConstant.CATEGORY_INTEGRAL
            );
            return PageUtils.getPageResult(rows);
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    public ShopFrontUserIntegralSummaryVO frontSummary(Long userId) {
        int currentIntegral = resolveCurrentIntegral(userId);
        int totalGain = resolveIntegralSum(userId, ShopPaymentConstant.PM_REFUND);
        int totalUse = resolveIntegralSum(userId, ShopPaymentConstant.PM_PAY);
        return assembleIntegralSummary(currentIntegral, totalGain, totalUse);
    }

    private int resolveCurrentIntegral(Long userId) {
        if (userId == null) {
            return 0;
        }
        ShopUser user = lbUserMapper.selectOneById(userId);
        return user == null || user.getIntegral() == null ? 0 : user.getIntegral();
    }

    private int resolveIntegralSum(Long userId, Integer pm) {
        if (userId == null) {
            return 0;
        }
        Integer sum = mapper.sumPositiveNumberByUidAndCategoryAndPm(
                userId,
                ShopPaymentConstant.CATEGORY_INTEGRAL,
                pm
        );
        return sum == null ? 0 : sum;
    }

    private ShopFrontUserIntegralSummaryVO assembleIntegralSummary(int currentIntegral, int totalGain, int totalUse) {
        ShopFrontUserIntegralSummaryVO vo = new ShopFrontUserIntegralSummaryVO();
        vo.setCurrentIntegral(currentIntegral);
        vo.setTotalGainIntegral(totalGain);
        vo.setTotalUseIntegral(totalUse);
        vo.setFreezeIntegral(0);
        return vo;
    }

    private PageQuery buildPageQuery(int page, int limit) {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(page);
        pageQuery.setLimit(limit);
        return pageQuery;
    }
}
