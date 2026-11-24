package com.sz.admin.shop_system.service.impl;

import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.constants.ShopPaymentConstant;
import com.sz.admin.shop_system.mapper.ShopUserRechargeActivityMapper;
import com.sz.admin.shop_system.mapper.ShopUserRechargeMapper;
import com.sz.admin.shop_system.pojo.dto.ShopUserRechargeDTO;
import com.sz.admin.shop_system.pojo.po.ShopUser;
import com.sz.admin.shop_system.pojo.po.ShopUserBill;
import com.sz.admin.shop_system.pojo.po.ShopUserRecharge;
import com.sz.admin.shop_system.pojo.po.ShopUserRechargeActivity;
import com.sz.admin.shop_system.pojo.vo.ShopUserRechargeVO;
import com.sz.admin.shop_system.service.ShopUserBillService;
import com.sz.admin.shop_system.service.ShopUserRechargeService;
import com.sz.admin.shop_system.service.ShopUserService;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.datascope.SimpleDataScopeHelper;
import com.sz.core.util.BeanCopyUtils;
import com.sz.core.util.FileUtils;
import com.sz.core.util.PageUtils;
import com.sz.excel.utils.ExcelUtils;
import com.sz.security.util.StpUserUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <p>
 * 用户充值表 服务实现类
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
@Service
@RequiredArgsConstructor
public class ShopUserRechargeServiceImpl extends ServiceImpl<ShopUserRechargeMapper, ShopUserRecharge> implements ShopUserRechargeService {

    private static final Set<String> SUPPORTED_RECHARGE_TYPES = new HashSet<>(Arrays.asList("yue", "wxpay", "alipay"));

    private final ShopUserService lbUserService;
    private final ShopUserBillService lbUserBillService;
    private final ShopUserRechargeActivityMapper lbUserRechargeActivityMapper;

    @Override
    public void create(ShopUserRechargeDTO.Create dto) {
        ShopUserRecharge lbUserRecharge = BeanCopyUtils.copy(dto, ShopUserRecharge.class);
        save(lbUserRecharge);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void frontRecharge(ShopUserRechargeDTO.Create dto) {
        CommonResponseEnum.INVALID.message("充值参数不能为空").assertNull(dto);
        CommonResponseEnum.INVALID.message("充值金额不能为空")
                .assertTrue(dto.getPrice() == null || dto.getPrice().isBlank());

        String rechargeType = resolveRechargeType(dto.getFrom());
        BigDecimal inputPrice = parseRechargePrice(dto.getPrice());
        RechargePlan plan = resolveRechargePlan(dto.getActivityId(), inputPrice);

        long userId = StpUserUtil.getLoginIdAsLong();
        ShopUser lbUser = lbUserService.getById(userId);
        CommonResponseEnum.INVALID_USER.assertNull(lbUser);

        BigDecimal totalIncome = plan.rechargePrice().add(plan.givePrice());
        BigDecimal currentMoney = lbUser.getNowMoney() == null ? BigDecimal.ZERO : lbUser.getNowMoney();
        BigDecimal newMoney = currentMoney.add(totalIncome);

        persistUserBalance(userId, newMoney);
        ShopUserRecharge recharge = buildRechargeRecord(userId, plan, rechargeType);
        save(recharge);
        ShopUserBill userBill = buildRechargeBill(userId, recharge, plan, totalIncome, newMoney);
        persistRechargeBill(userBill);
    }

    @Override
    public void update(ShopUserRechargeDTO.Update dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getId() == null || mapper.countById(dto.getId()) <= 0);
        ShopUserRecharge lbUserRecharge = BeanCopyUtils.copy(dto, ShopUserRecharge.class);
        saveOrUpdate(lbUserRecharge);
    }

    @Override
    public PageResult<ShopUserRechargeVO> page(ShopUserRechargeDTO.ListQuery dto) {
        PageUtils.toPage(dto);
        try {
            SimpleDataScopeHelper.start(ShopUserRecharge.class);
            List<ShopUserRechargeVO> list = mapper.selectRechargeList(dto);
            return PageUtils.getPageResult(list);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
            PageHelper.clearPage();
        }
    }

    @Override
    public List<ShopUserRechargeVO> list(ShopUserRechargeDTO.ListQuery dto) {
        try {
            SimpleDataScopeHelper.start(ShopUserRecharge.class);
            return mapper.selectRechargeList(dto);
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
    public ShopUserRechargeVO detail(Object id) {
        ShopUserRecharge lbUserRecharge = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(lbUserRecharge);
        return BeanCopyUtils.copy(lbUserRecharge, ShopUserRechargeVO.class);
    }

    @SneakyThrows
    @Override
    public void exportExcel(ShopUserRechargeDTO.ListQuery dto, HttpServletResponse response) {
        List<ShopUserRechargeVO> list = list(dto);
        String fileName = "用户充值表模板";
        OutputStream os = FileUtils.getOutputStream(response, fileName + ".xlsx");
        ExcelUtils.exportExcel(list, "用户充值表", ShopUserRechargeVO.class, os);
    }

    private String resolveRechargeType(String rawFrom) {
        String rechargeType = rawFrom == null || rawFrom.isBlank()
                ? "yue"
                : rawFrom.trim().toLowerCase();
        CommonResponseEnum.INVALID.message("充值方式无效，支持：yue/wxpay/alipay")
                .assertTrue(!SUPPORTED_RECHARGE_TYPES.contains(rechargeType));
        return rechargeType;
    }

    private BigDecimal parseRechargePrice(String rawPrice) {
        BigDecimal inputPrice = null;
        try {
            inputPrice = new BigDecimal(rawPrice.trim());
        } catch (Exception ignored) {
        }
        CommonResponseEnum.INVALID.message("充值金额格式错误").assertNull(inputPrice);
        CommonResponseEnum.INVALID.message("充值金额必须大于0").assertTrue(inputPrice.compareTo(BigDecimal.ZERO) <= 0);
        return inputPrice;
    }

    private RechargePlan resolveRechargePlan(String rawActivityId, BigDecimal inputPrice) {
        String activityIdRaw = rawActivityId == null ? null : rawActivityId.trim();
        boolean useActivityRecharge = activityIdRaw != null && !activityIdRaw.isBlank();
        if (!useActivityRecharge) {
            CommonResponseEnum.INVALID.message("自定义充值金额只能输入整数")
                    .assertTrue(inputPrice.stripTrailingZeros().scale() > 0);
            return new RechargePlan(null, inputPrice, BigDecimal.ZERO);
        }

        Long activityId = null;
        try {
            activityId = Long.valueOf(activityIdRaw);
        } catch (Exception ignored) {
        }
        CommonResponseEnum.INVALID.message("充值活动ID格式错误").assertNull(activityId);

        ShopUserRechargeActivity activity = lbUserRechargeActivityMapper.selectActiveById(activityId);
        CommonResponseEnum.INVALID.message("充值活动不存在").assertNull(activity);
        CommonResponseEnum.INVALID.message("充值活动套餐金额不正确")
                .assertTrue(activity.getPrice() == null || activity.getPrice().compareTo(inputPrice) != 0);

        BigDecimal givePrice = activity.getGivePrice() == null ? BigDecimal.ZERO : activity.getGivePrice();
        return new RechargePlan(activity, activity.getPrice(), givePrice);
    }

    private void persistUserBalance(long userId, BigDecimal newMoney) {
        ShopUser updateUser = new ShopUser();
        updateUser.setUid(userId);
        updateUser.setNowMoney(newMoney);
        boolean userUpdated = lbUserService.updateById(updateUser);
        CommonResponseEnum.INVALID.message("用户余额更新失败，请重试").assertTrue(!userUpdated);
    }

    private ShopUserRecharge buildRechargeRecord(long userId, RechargePlan plan, String rechargeType) {
        ShopUserRecharge recharge = new ShopUserRecharge();
        recharge.setUid(userId);
        recharge.setPayActivityId(plan.activity() == null ? null : plan.activity().getId());
        recharge.setOrderId(generateRechargeOrderId());
        recharge.setPrice(plan.rechargePrice());
        recharge.setGivePrice(plan.givePrice());
        recharge.setRechargeType(rechargeType);
        recharge.setPayTime(LocalDateTime.now());
        return recharge;
    }

    private ShopUserBill buildRechargeBill(long userId, ShopUserRecharge recharge, RechargePlan plan, BigDecimal totalIncome, BigDecimal newMoney) {
        ShopUserBill userBill = new ShopUserBill();
        userBill.setUid(userId);
        userBill.setLinkId(recharge.getOrderId() == null ? null : String.valueOf(recharge.getOrderId()));
        userBill.setPm(ShopPaymentConstant.PM_REFUND);
        userBill.setTitle("账户充值");
        userBill.setCategory(ShopPaymentConstant.CATEGORY_NOW_MONEY);
        userBill.setType(ShopPaymentConstant.TYPE_RECHARGE);
        userBill.setNumber(totalIncome);
        userBill.setBalance(newMoney);
        userBill.setMark(buildRechargeBillMark(plan.activity(), plan.rechargePrice(), plan.givePrice()));
        userBill.setStatus(1);
        return userBill;
    }

    private void persistRechargeBill(ShopUserBill userBill) {
        boolean billSaved = lbUserBillService.save(userBill);
        CommonResponseEnum.INVALID.message("写入充值账单失败，请重试").assertTrue(!billSaved);
    }

    private Long generateRechargeOrderId() {
        for (int i = 0; i < 5; i++) {
            long candidate = System.currentTimeMillis() * 1000L + ThreadLocalRandom.current().nextInt(1000);
            long exists = mapper.countByOrderId(candidate);
            if (exists == 0) {
                return candidate;
            }
        }
        return System.currentTimeMillis() * 1000L + ThreadLocalRandom.current().nextInt(1000);
    }

    private static String buildRechargeBillMark(ShopUserRechargeActivity activity, BigDecimal rechargePrice, BigDecimal givePrice) {
        if (activity != null && activity.getActivityName() != null && !activity.getActivityName().isBlank()) {
            return givePrice.compareTo(BigDecimal.ZERO) > 0
                    ? activity.getActivityName() + "，到账" + rechargePrice.add(givePrice).toPlainString() + "元"
                    : activity.getActivityName();
        }
        return givePrice.compareTo(BigDecimal.ZERO) > 0
                ? "充值" + rechargePrice.toPlainString() + "元，赠送" + givePrice.toPlainString() + "元"
                : "自定义充值" + rechargePrice.toPlainString() + "元";
    }

    private record RechargePlan(ShopUserRechargeActivity activity, BigDecimal rechargePrice, BigDecimal givePrice) {
    }
}
