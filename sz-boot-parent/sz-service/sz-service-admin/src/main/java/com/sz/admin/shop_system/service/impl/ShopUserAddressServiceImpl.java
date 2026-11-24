package com.sz.admin.shop_system.service.impl;

import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopUserAddressMapper;
import com.sz.admin.shop_system.pojo.dto.ShopUserAddressDTO;
import com.sz.admin.shop_system.pojo.po.ShopUserAddress;
import com.sz.admin.shop_system.pojo.vo.ShopUserAddressVO;
import com.sz.admin.shop_system.service.ShopUserAddressService;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.datascope.SimpleDataScopeHelper;
import com.sz.core.util.BeanCopyUtils;
import com.sz.core.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 用户地址表 服务实现类
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
@Service
@RequiredArgsConstructor
public class ShopUserAddressServiceImpl extends ServiceImpl<ShopUserAddressMapper, ShopUserAddress> implements ShopUserAddressService {

    @Override
    public void create(ShopUserAddressDTO.Create dto) {
        ShopUserAddress shopUserAddress = buildCreateAddress(dto);
        save(shopUserAddress);
        normalizeDefaultAddress(dto.getUid(), shopUserAddress.getId(), dto.getIsDefault());
    }

    @Override
    public void update(ShopUserAddressDTO.Update dto) {
        ShopUserAddress currentAddress = getById(dto.getId());
        CommonResponseEnum.INVALID_ID.assertNull(currentAddress);
        CommonResponseEnum.INVALID.message("无权操作该地址").assertTrue(!Objects.equals(currentAddress.getUid(), dto.getUid()));

        ShopUserAddress shopUserAddress = buildUpdateAddress(dto);
        saveOrUpdate(shopUserAddress);
        normalizeDefaultAddress(dto.getUid(), shopUserAddress.getId(), dto.getIsDefault());
    }

    @Override
    public PageResult<ShopUserAddressVO> page(ShopUserAddressDTO.ListQuery dto) {
        PageUtils.toPage(dto);
        try {
            SimpleDataScopeHelper.start(ShopUserAddress.class);
            List<ShopUserAddressVO> list = mapper.selectAddressList(dto);
            return PageUtils.getPageResult(list);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
            PageHelper.clearPage();
        }
    }

    @Override
    public List<ShopUserAddressVO> list(ShopUserAddressDTO.ListQuery dto) {
        try {
            SimpleDataScopeHelper.start(ShopUserAddress.class);
            return mapper.selectAddressList(dto);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
        }
    }

    @Override
    public void remove(SelectIdsDTO dto, Long uid) {
        CommonResponseEnum.INVALID.message("用户信息异常").assertNull(uid);
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getIds().isEmpty());
        List<Long> ids = mapper.selectIdsByIdsAndUid(dto.getIds(), uid)
                .stream()
                .filter(Objects::nonNull)
                .toList();
        CommonResponseEnum.INVALID_ID.assertTrue(ids.isEmpty());
        removeByIds(ids);
        normalizeDefaultAddress(uid, null, null);
    }

    @Override
    public ShopUserAddressVO detail(Object id) {
        ShopUserAddress shopUserAddress = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(shopUserAddress);
        return BeanCopyUtils.copy(shopUserAddress, ShopUserAddressVO.class);
    }

    private ShopUserAddress buildCreateAddress(ShopUserAddressDTO.Create dto) {
        return BeanCopyUtils.copy(dto, ShopUserAddress.class);
    }

    private ShopUserAddress buildUpdateAddress(ShopUserAddressDTO.Update dto) {
        return BeanCopyUtils.copy(dto, ShopUserAddress.class);
    }

    private void normalizeDefaultAddress(Long uid, Long currentAddressId, Integer requestedDefault) {
        CommonResponseEnum.INVALID.message("用户信息异常").assertNull(uid);
        List<ShopUserAddress> addressList = mapper.selectActiveByUid(uid);
        if (addressList == null || addressList.isEmpty()) {
            return;
        }

        Long targetDefaultId = resolveTargetDefaultAddressId(addressList, currentAddressId, requestedDefault);
        if (targetDefaultId == null) {
            return;
        }
        persistDefaultAddressFlags(addressList, targetDefaultId);
    }

    private Long resolveTargetDefaultAddressId(List<ShopUserAddress> addressList, Long currentAddressId, Integer requestedDefault) {
        if (requestedDefault != null && requestedDefault == 1 && currentAddressId != null) {
            return currentAddressId;
        }

        List<ShopUserAddress> defaultList = addressList.stream()
                .filter(address -> address.getIsDefault() != null && address.getIsDefault() == 1)
                .toList();
        List<ShopUserAddress> candidates = defaultList.isEmpty() ? addressList : defaultList;
        return candidates.stream()
                .max(Comparator
                        .comparing(ShopUserAddress::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ShopUserAddress::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ShopUserAddress::getId)
                .orElse(null);
    }

    private void persistDefaultAddressFlags(List<ShopUserAddress> addressList, Long targetDefaultId) {
        for (ShopUserAddress address : addressList) {
            Integer targetDefault = targetDefaultId.equals(address.getId()) ? 1 : 0;
            if (address.getIsDefault() == null || !address.getIsDefault().equals(targetDefault)) {
                ShopUserAddress update = new ShopUserAddress();
                update.setId(address.getId());
                update.setIsDefault(targetDefault);
                updateById(update);
            }
        }
    }
}
