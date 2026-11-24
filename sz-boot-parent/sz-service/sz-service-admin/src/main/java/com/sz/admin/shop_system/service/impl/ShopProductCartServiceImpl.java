package com.sz.admin.shop_system.service.impl;

import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopProductCartMapper;
import com.sz.admin.shop_system.pojo.dto.ShopProductCartDTO;
import com.sz.admin.shop_system.pojo.po.ShopProductCart;
import com.sz.admin.shop_system.pojo.vo.ShopProductCartVO;
import com.sz.admin.shop_system.service.ShopProductCartService;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.util.BeanCopyUtils;
import com.sz.core.util.PageUtils;
import com.sz.security.util.StpUserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 购物车表 服务实现类
 * </p>
 *
 * @author car
 * @since 2026-01-26
 */
@Service
@RequiredArgsConstructor
public class ShopProductCartServiceImpl extends ServiceImpl<ShopProductCartMapper, ShopProductCart> implements ShopProductCartService {

    @Override
    public void create(ShopProductCartDTO.Create dto, Long userId) {
        CommonResponseEnum.INVALID.assertNull(dto.getProductId());
        CommonResponseEnum.INVALID.assertNull(dto.getSkuId());
        int cartNum = normalizeCartNum(dto.getCartNum());

        ShopProductCart exists = mapper.selectByUidAndProductAndSku(userId, dto.getProductId(), dto.getSkuId());
        if (Objects.nonNull(exists)) {
            mergeAndUpdateExistingCart(exists, cartNum);
            return;
        }

        ShopProductCart lbProductCart = buildNewCartItem(dto, userId, cartNum);
        save(lbProductCart);
    }

    @Override
    public void update(ShopProductCartDTO.Update dto, Long userId) {
        CommonResponseEnum.INVALID_ID.assertNull(dto.getId());
        ShopProductCart exists = mapper.selectActiveByIdAndUid(dto.getId(), userId);
        CommonResponseEnum.INVALID_ID.assertNull(exists);

        Integer cartNum = dto.getCartNum();
        if (cartNum == null || cartNum <= 0) {
            removeById(exists.getId());
            return;
        }
        exists.setCartNum(cartNum);
        updateById(exists);
    }

    @Override
    public PageResult<ShopProductCartVO> page(ShopProductCartDTO.ListQuery dto) {
        long userId = StpUserUtil.getLoginIdAsLong();
        CommonResponseEnum.INVALID_TOKEN.assertNull(userId);
        PageUtils.toPage(dto);
        try {
            List<ShopProductCartVO> list = mapper.selectCartPageList(userId, dto == null ? null : dto.getStatus());
            return PageUtils.getPageResult(list);
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    public void remove(SelectIdsDTO dto, Long userId) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getIds().isEmpty());
        mapper.markRemovedByIdsAndUid(userId, dto.getIds());
    }

    @Override
    public Integer count(Long userId) {
        Integer total = mapper.sumActiveCartNumByUid(userId);
        return total == null ? 0 : total;
    }

    private int normalizeCartNum(Integer cartNum) {
        return (cartNum == null || cartNum <= 0) ? 1 : cartNum;
    }

    private void mergeAndUpdateExistingCart(ShopProductCart exists, int cartNum) {
        Integer existsStatus = exists.getStatus();
        if (existsStatus != null && existsStatus == 1) {
            exists.setCartNum(cartNum);
        } else {
            exists.setCartNum((exists.getCartNum() == null ? 0 : exists.getCartNum()) + cartNum);
        }
        exists.setStatus(0);
        updateById(exists);
    }

    private ShopProductCart buildNewCartItem(ShopProductCartDTO.Create dto, Long userId, int cartNum) {
        ShopProductCart lbProductCart = BeanCopyUtils.copy(dto, ShopProductCart.class);
        lbProductCart.setUid(userId);
        lbProductCart.setCartNum(cartNum);
        if (lbProductCart.getStatus() == null) {
            lbProductCart.setStatus(0);
        }
        return lbProductCart;
    }
}
