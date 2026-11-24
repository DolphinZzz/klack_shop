package com.sz.security.util;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;

/**
 * User-side Sa-Token helper with isolated loginType.
 */
public class StpUserUtil {

    public static final String LOGIN_TYPE = "shop_user";
    private static final StpLogic STP_LOGIC = new StpLogicJwtForSimple(LOGIN_TYPE);

    private StpUserUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static StpLogic getStpLogic() {
        return STP_LOGIC;
    }

    public static void login(Object loginId) {
        STP_LOGIC.login(loginId);
    }

    public static void login(Object loginId, SaLoginParameter parameter) {
        STP_LOGIC.login(loginId, parameter);
    }

    public static void logout() {
        STP_LOGIC.logout();
    }

    public static boolean isLogin() {
        return STP_LOGIC.isLogin();
    }

    public static void checkLogin() {
        STP_LOGIC.checkLogin();
    }

    public static String getTokenValue() {
        return STP_LOGIC.getTokenValue();
    }

    public static long getTokenTimeout() {
        return STP_LOGIC.getTokenTimeout();
    }

    public static SaSession getTokenSession() {
        return STP_LOGIC.getTokenSession();
    }

    public static SaSession getTokenSessionByToken(String token) {
        return STP_LOGIC.getTokenSessionByToken(token);
    }

    public static SaSession getSession() {
        return STP_LOGIC.getSession();
    }

    public static long getLoginIdAsLong() {
        return STP_LOGIC.getLoginIdAsLong();
    }
}
