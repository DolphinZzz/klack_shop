package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopUserDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopUser;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.entity.PageResult;
import java.util.List;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserProfileVO;
import com.sz.admin.shop_system.pojo.vo.ShopUserListVO;
import com.sz.admin.shop_system.pojo.vo.ShopUserVO;
import com.sz.core.common.entity.ImportExcelDTO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>
 * 用户表 Service
 * </p>
 *
 * @author car
 * @since 2025-11-24
 */
public interface ShopUserService extends IService<ShopUser> {

    void create(ShopUserDTO.Create dto);

    void update(ShopUserDTO.Update dto);

    PageResult<ShopUserListVO> page(ShopUserDTO.ListQuery dto);

    List<ShopUserListVO> list(ShopUserDTO.ListQuery dto);

    void remove(SelectIdsDTO dto);

    ShopUserVO detail(Object id);

    void importExcel(ImportExcelDTO dto);

    void exportExcel(ShopUserDTO.ListQuery dto, HttpServletResponse response);

    /**
     * 根据账号获取登录用户信息
     *
     * @param account 账号
     * @return ShopLoginUser
     */
    ShopUserVO getLoginUserByAccount(String account);

    /**
     * 根据手机号获取登录用户信息
     *
     * @param phone 手机号
     * @return ShopLoginUser
     */
    ShopUserVO getLoginUserByPhone(String phone);

    /**
     * 构建C端登录用户信息
     *
     * @param accountOrPhone 账号或手机号
     * @param password 密码
     * @return ShopLoginUser
     */
    ShopUserVO buildLoginUser(String accountOrPhone, String password);

    /**
     * 用户端注册
     *
     * @param dto 注册信息
     */
    void userRegister(com.sz.admin.shop_system.pojo.dto.ShopUserDTO.Register dto);

    /**
     * 查询用户端个人资料
     *
     * @param uid 用户id
     * @return 个人资料
     */
    ShopFrontUserProfileVO getFrontProfile(Long uid);

    /**
     * 修改用户端个人资料
     *
     * @param uid 用户id
     * @param dto 修改信息
     */
    void updateFrontProfile(Long uid, ShopUserDTO.FrontProfileUpdate dto);

    /**
     * 修改用户端密码
     *
     * @param uid 用户id
     * @param dto 密码信息
     */
    void updateFrontPassword(Long uid, ShopUserDTO.FrontPassword dto);
}
