package com.sz.admin.shop_system.service.impl;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopUserMapper;
import com.sz.admin.shop_system.pojo.dto.ShopUserDTO;
import com.sz.admin.shop_system.pojo.po.ShopUser;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserProfileVO;
import com.sz.admin.shop_system.pojo.vo.ShopUserListVO;
import com.sz.admin.shop_system.pojo.vo.ShopUserVO;
import com.sz.admin.shop_system.service.ShopUserService;
import com.sz.core.common.entity.ImportExcelDTO;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.common.exception.common.BusinessException;
import com.sz.core.util.BeanCopyUtils;
import com.sz.core.util.FileUtils;
import com.sz.core.util.PageUtils;
import com.sz.excel.core.ExcelResult;
import com.sz.excel.utils.ExcelUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author car
 * @since 2025-11-24
 */
@Service
@RequiredArgsConstructor
public class ShopUserServiceImpl extends ServiceImpl<ShopUserMapper, ShopUser> implements ShopUserService {

    @Override
    public void create(ShopUserDTO.Create dto) {
        validateAdminUserUnique(dto.getAccount(), dto.getCardId(), null);
        ShopUser shopUser = BeanCopyUtils.copy(dto, ShopUser.class);
        save(shopUser);
    }

    @Override
    public void update(ShopUserDTO.Update dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getUid() == null || mapper.countByUid(dto.getUid()) <= 0);
        validateAdminUserUnique(dto.getAccount(), dto.getCardId(), dto.getUid());
        ShopUser shopUser = BeanCopyUtils.copy(dto, ShopUser.class);
        saveOrUpdate(shopUser);
    }

    @Override
    public PageResult<ShopUserListVO> page(ShopUserDTO.ListQuery dto) {
        PageUtils.toPage(dto);
        try {
            List<ShopUserListVO> list = mapper.selectUserList(dto);
            return PageUtils.getPageResult(list);
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    public List<ShopUserListVO> list(ShopUserDTO.ListQuery dto) {
        return mapper.selectUserList(dto);
    }

    @Override
    public void remove(SelectIdsDTO dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getIds().isEmpty());
        removeByIds(dto.getIds());
    }

    @Override
    public ShopUserVO detail(Object id) {
        ShopUser shopUser = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(shopUser);
        return BeanCopyUtils.copy(shopUser, ShopUserVO.class);
    }

    @SneakyThrows
    @Override
    public void importExcel(ImportExcelDTO dto) {
        ExcelResult<ShopUserDTO.Import> excelResult = ExcelUtils.importExcel(dto.getFile().getInputStream(), ShopUserDTO.Import.class, true);
        List<ShopUserDTO.Import> list = excelResult.getList();
        List<ShopUser> importList = BeanCopyUtils.copyList(list, ShopUser.class);
        saveBatch(importList);
        List<String> errorList = excelResult.getErrorList();
        String analysis = excelResult.getAnalysis();
        System.out.println(" analysis : " + analysis);
        System.out.println(" isCover : " + dto.getIsCover());
    }

    @SneakyThrows
    @Override
    public void exportExcel(ShopUserDTO.ListQuery dto, HttpServletResponse response) {
        List<ShopUserListVO> list = list(dto);
        String fileName = "用户表模板";
        OutputStream os = FileUtils.getOutputStream(response, fileName + ".xlsx");
        ExcelUtils.exportExcel(list, "用户表", ShopUserListVO.class, os);
    }

    @Override
    public ShopUserVO getLoginUserByAccount(String account) {
        ShopUser shopUser = mapper.selectOneByAccount(account);
        return assembleLoginUser(shopUser);
    }

    @Override
    public ShopUserVO getLoginUserByPhone(String phone) {
        ShopUser shopUser = mapper.selectOneByPhone(phone);
        return assembleLoginUser(shopUser);
    }

    @Override
    public ShopUserVO buildLoginUser(String accountOrPhone, String password) {
        ShopUser user = findUserByAccountOrPhone(accountOrPhone);
        CommonResponseEnum.INVALID_USER.assertNull(user);

        if (Objects.equals(user.getStatus(), 1)) {
            throw new BusinessException(CommonResponseEnum.BAD_USERNAME_STATUS_INVALID);
        }

        boolean passwordMatch = SaSecureUtil.md5(password).equalsIgnoreCase(user.getPassword());
        CommonResponseEnum.BAD_USERNAME_OR_PASSWORD.assertFalse(passwordMatch);

        return assembleLoginUser(user);
    }

    @Override
    public void userRegister(ShopUserDTO.Register dto) {
        validateRegisterUnique(dto.getAccount(), dto.getPhone());
        ShopUser shopUser = buildRegisterUser(dto);
        save(shopUser);
    }

    @Override
    public ShopFrontUserProfileVO getFrontProfile(Long uid) {
        CommonResponseEnum.INVALID.message("用户信息异常").assertNull(uid);
        ShopUser shopUser = getById(uid);
        CommonResponseEnum.INVALID_USER.assertNull(shopUser);
        return BeanCopyUtils.copy(shopUser, ShopFrontUserProfileVO.class);
    }

    @Override
    public void updateFrontProfile(Long uid, ShopUserDTO.FrontProfileUpdate dto) {
        CommonResponseEnum.INVALID.message("用户信息异常").assertNull(uid);
        ShopUser currentUser = getById(uid);
        CommonResponseEnum.INVALID_USER.assertNull(currentUser);

        String account = StrUtil.trim(dto.getAccount());
        String phone = StrUtil.trim(dto.getPhone());
        CommonResponseEnum.INVALID.message("账号不能为空").assertTrue(StrUtil.isBlank(account));
        CommonResponseEnum.INVALID.message("手机号不能为空").assertTrue(StrUtil.isBlank(phone));

        validateFrontProfileUnique(uid, account, phone, dto.getCardId());
        persistFrontProfile(buildFrontProfileUpdateEntity(uid, account, phone, dto));
    }

    @Override
    public void updateFrontPassword(Long uid, ShopUserDTO.FrontPassword dto) {
        CommonResponseEnum.INVALID.message("用户信息异常").assertNull(uid);
        CommonResponseEnum.INVALID.message("原密码不能为空").assertTrue(StrUtil.isBlank(dto.getOldPassword()));
        CommonResponseEnum.INVALID.message("新密码不能为空").assertTrue(StrUtil.isBlank(dto.getNewPassword()));

        ShopUser currentUser = getById(uid);
        CommonResponseEnum.INVALID_USER.assertNull(currentUser);

        boolean passwordMatch = SaSecureUtil.md5(dto.getOldPassword()).equalsIgnoreCase(currentUser.getPassword());
        CommonResponseEnum.BAD_USERNAME_OR_PASSWORD.assertFalse(passwordMatch);
        CommonResponseEnum.INVALID.message("新密码不能与原密码一致").assertTrue(StrUtil.equals(dto.getOldPassword(), dto.getNewPassword()));

        persistUserPassword(buildPasswordUpdateEntity(uid, dto.getNewPassword()));
    }

    private ShopUser findUserByAccountOrPhone(String accountOrPhone) {
        if (StrUtil.isNumeric(accountOrPhone) && accountOrPhone.length() == 11) {
            return mapper.selectOneByPhone(accountOrPhone);
        }
        return mapper.selectOneByAccount(accountOrPhone);
    }

    private ShopUserVO assembleLoginUser(ShopUser user) {
        if (ObjectUtil.isNull(user)) {
            return null;
        }
        ShopUserVO loginUser = new ShopUserVO();
        BeanUtils.copyProperties(user, loginUser);
        return loginUser;
    }

    private void validateAdminUserUnique(String account, String cardId, Long excludeUid) {
        long accountCount = mapper.countByAccount(account, excludeUid);
        CommonResponseEnum.EXISTS.message("account已存在").assertTrue(accountCount > 0);
        long cardIdCount = mapper.countByCardId(cardId, excludeUid);
        CommonResponseEnum.EXISTS.message("cardId已存在").assertTrue(cardIdCount > 0);
    }

    private void validateRegisterUnique(String account, String phone) {
        long accountCount = mapper.countByAccount(account, null);
        CommonResponseEnum.EXISTS.message("账号已存在").assertTrue(accountCount > 0);
        long phoneCount = mapper.countByPhone(phone, null);
        CommonResponseEnum.EXISTS.message("手机号已存在").assertTrue(phoneCount > 0);
    }

    private void validateFrontProfileUnique(Long uid, String account, String phone, String cardId) {
        long count = mapper.countByAccount(account, uid);
        CommonResponseEnum.EXISTS.message("账号已存在").assertTrue(count > 0);

        count = mapper.countByPhone(phone, uid);
        CommonResponseEnum.EXISTS.message("手机号已存在").assertTrue(count > 0);

        if (StrUtil.isBlank(cardId)) {
            return;
        }

        count = mapper.countByCardId(StrUtil.trim(cardId), uid);
        CommonResponseEnum.EXISTS.message("身份证已存在").assertTrue(count > 0);
    }

    private ShopUser buildRegisterUser(ShopUserDTO.Register dto) {
        ShopUser shopUser = new ShopUser();
        shopUser.setAccount(dto.getAccount());
        shopUser.setPassword(SaSecureUtil.md5(dto.getPassword()));
        shopUser.setPhone(dto.getPhone());

        if (StrUtil.isNotBlank(dto.getRealName())) {
            shopUser.setRealName(dto.getRealName());
        }
        if (StrUtil.isNotBlank(dto.getNickname())) {
            shopUser.setNickname(dto.getNickname());
        }

        shopUser.setStatus(0);
        shopUser.setSex(0);
        shopUser.setLevel(1);
        shopUser.setIntegral(0);
        shopUser.setExperience(0);
        shopUser.setSignNum(0);
        shopUser.setNowMoney(new BigDecimal("0"));
        return shopUser;
    }

    private ShopUser buildFrontProfileUpdateEntity(Long uid, String account, String phone, ShopUserDTO.FrontProfileUpdate dto) {
        ShopUser updateUser = new ShopUser();
        updateUser.setUid(uid);
        updateUser.setAccount(account);
        updateUser.setRealName(StrUtil.emptyToNull(StrUtil.trim(dto.getRealName())));
        updateUser.setSex(dto.getSex());
        updateUser.setBirthday(StrUtil.emptyToNull(StrUtil.trim(dto.getBirthday())));
        updateUser.setCardId(StrUtil.emptyToNull(StrUtil.trim(dto.getCardId())));
        updateUser.setNickname(StrUtil.emptyToNull(StrUtil.trim(dto.getNickname())));
        updateUser.setAvatar(StrUtil.emptyToNull(StrUtil.trim(dto.getAvatar())));
        updateUser.setPhone(phone);
        return updateUser;
    }

    private ShopUser buildPasswordUpdateEntity(Long uid, String newPassword) {
        ShopUser updateUser = new ShopUser();
        updateUser.setUid(uid);
        updateUser.setPassword(SaSecureUtil.md5(newPassword));
        return updateUser;
    }

    private void persistFrontProfile(ShopUser updateUser) {
        updateById(updateUser);
    }

    private void persistUserPassword(ShopUser updateUser) {
        updateById(updateUser);
    }
}
