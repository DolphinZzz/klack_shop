package com.sz.admin.shop_system.service.impl;

import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.constants.ShopPaymentConstant;
import com.sz.admin.shop_system.mapper.ShopUserBillMapper;
import com.sz.admin.shop_system.mapper.ShopUserMapper;
import com.sz.admin.shop_system.mapper.ShopUserRechargeMapper;
import com.sz.admin.shop_system.pojo.dto.ShopUserBillDTO;
import com.sz.admin.shop_system.pojo.po.ShopUser;
import com.sz.admin.shop_system.pojo.po.ShopUserBill;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserBalanceSummaryVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserBillVO;
import com.sz.admin.shop_system.pojo.vo.ShopUserBillVO;
import com.sz.admin.shop_system.service.ShopUserBillService;
import com.sz.core.common.entity.PageQuery;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.datascope.SimpleDataScopeHelper;
import com.sz.core.util.BeanCopyUtils;
import com.sz.core.util.FileUtils;
import com.sz.core.util.PageUtils;
import com.sz.excel.utils.ExcelUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * <p>
 * 用户帐单表 服务实现类
 * </p>
 *
 * @author sz-admin
 * @since 2025-11-26
 */
@Service
@RequiredArgsConstructor
public class ShopUserBillServiceImpl extends ServiceImpl<ShopUserBillMapper, ShopUserBill> implements ShopUserBillService {

    private final ShopUserMapper shopUserMapper;
    private final ShopUserRechargeMapper shopUserRechargeMapper;

    @Override
    public void create(ShopUserBillDTO.Create dto) {
        ShopUserBill shopUserBill = BeanCopyUtils.copy(dto, ShopUserBill.class);
        save(shopUserBill);
    }

    @Override
    public void update(ShopUserBillDTO.Update dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getId() == null || mapper.countById(dto.getId()) <= 0);
        ShopUserBill shopUserBill = BeanCopyUtils.copy(dto, ShopUserBill.class);
        saveOrUpdate(shopUserBill);
    }

    @Override
    public PageResult<ShopUserBillVO> page(ShopUserBillDTO.ListQuery dto) {
        PageUtils.toPage(dto);
        try {
            SimpleDataScopeHelper.start(ShopUserBill.class);
            List<ShopUserBillVO> list = mapper.selectBillList(dto);
            return PageUtils.getPageResult(list);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
            PageHelper.clearPage();
        }
    }

    @Override
    public List<ShopUserBillVO> list(ShopUserBillDTO.ListQuery dto) {
        try {
            SimpleDataScopeHelper.start(ShopUserBill.class);
            return mapper.selectBillList(dto);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
        }
    }

    @Override
    public void remove(SelectIdsDTO dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getIds().isEmpty());
        removeByIds(dto.getIds());
    }

    @Override
    public ShopUserBillVO detail(Object id) {
        ShopUserBill shopUserBill = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(shopUserBill);
        return BeanCopyUtils.copy(shopUserBill, ShopUserBillVO.class);
    }

    @SneakyThrows
    @Override
    public void exportExcel(ShopUserBillDTO.ListQuery dto, HttpServletResponse response) {
        List<ShopUserBillVO> list = list(dto);
        String fileName = "用户帐单表模板";
        OutputStream os = FileUtils.getOutputStream(response, fileName + ".xlsx");
        ExcelUtils.exportExcel(list, "用户帐单表", ShopUserBillVO.class, os);
    }

    @Override
    public ShopFrontUserBalanceSummaryVO frontSummary(Long userId) {
        BigDecimal nowMoney = resolveUserBalance(userId);
        BigDecimal rechargeAmount = resolveTotalRechargeAmount(userId);
        BigDecimal consumeAmount = resolveTotalConsumeAmount(userId);
        return assembleBalanceSummary(nowMoney, rechargeAmount, consumeAmount);
    }

    @Override
    public PageResult<ShopFrontUserBillVO> frontPage(Long userId, ShopUserBillDTO.FrontList dto) {
        int page = dto == null || dto.getPage() == null || dto.getPage() < 1 ? 1 : dto.getPage();
        int limit = dto == null || dto.getLimit() == null || dto.getLimit() < 1 ? 10 : dto.getLimit();
        if (userId == null) {
            return new PageResult<>(page, limit, 0L, 0, Collections.emptyList());
        }

        String normalizedTab = normalizeFrontBillTab(dto == null ? null : dto.getTab());
        PageQuery pageQuery = buildPageQuery(page, limit);
        PageUtils.toPage(pageQuery);
        try {
            List<ShopFrontUserBillVO> rows = mapper.selectFrontBillList(
                    userId,
                    ShopPaymentConstant.CATEGORY_NOW_MONEY,
                    normalizedTab,
                    ShopPaymentConstant.PM_PAY,
                    ShopPaymentConstant.PM_REFUND,
                    ShopPaymentConstant.TYPE_RECHARGE
            );
            return PageUtils.getPageResult(rows);
        } finally {
            PageHelper.clearPage();
        }
    }

    private BigDecimal resolveUserBalance(Long userId) {
        if (userId == null) {
            return BigDecimal.ZERO;
        }
        ShopUser user = shopUserMapper.selectOneById(userId);
        return user == null || user.getNowMoney() == null ? BigDecimal.ZERO : user.getNowMoney();
    }

    private BigDecimal resolveTotalRechargeAmount(Long userId) {
        if (userId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = shopUserRechargeMapper.sumPositivePriceByUid(userId);
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal resolveTotalConsumeAmount(Long userId) {
        if (userId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = mapper.sumPositiveNumberByUidAndCategoryAndPm(
                userId,
                ShopPaymentConstant.CATEGORY_NOW_MONEY,
                ShopPaymentConstant.PM_PAY
        );
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private ShopFrontUserBalanceSummaryVO assembleBalanceSummary(BigDecimal nowMoney, BigDecimal rechargeAmount, BigDecimal consumeAmount) {
        ShopFrontUserBalanceSummaryVO vo = new ShopFrontUserBalanceSummaryVO();
        vo.setNowMoney(nowMoney);
        vo.setRechargeAmount(rechargeAmount);
        vo.setConsumeAmount(consumeAmount);
        return vo;
    }

    private PageQuery buildPageQuery(int page, int limit) {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(page);
        pageQuery.setLimit(limit);
        return pageQuery;
    }

    private static String normalizeFrontBillTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return "all";
        }
        String current = tab.trim().toLowerCase(Locale.ROOT);
        if ("expend".equals(current) || "recharge".equals(current) || "income".equals(current)) {
            return current;
        }
        return "all";
    }
}
