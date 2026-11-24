package com.sz.admin.shop_system.util;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author car
 * @create 2026/1/29
 */

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务系统订单号/流水号生成工具
 */
public class SerialNumberGenerator {

    // 日期时间格式化器(线程安全)
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // 自增序列(单机环境下使用)
    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    // 机器ID(分布式环境下使用)
    private static final String MACHINE_ID = initMachineId();

    /**
     * 生成订单号(格式: ORDER + 时间 + 机器ID + 序列号)
     * 示例: ORDER2024051215230100110001
     */
    public static String generateOrderNo() {
        return generateSerialNumber("ORDER");
    }

    /**
     * 生成秒杀订单号
     */
    public static String generateFlashSaleOrderNo() {
        return generateSerialNumber("MS");
    }

    /**
     * 生成交易流水号(格式: TRANS + 时间 + 机器ID + 序列号)
     * 示例: TRANS2024051215230100110001
     */
    public static String generateTransNo() {
        return generateSerialNumber("TRANS");
    }

    /**
     * Generate express tracking number.
     * Example: EXP2024051215230100110001
     */
    public static String generateExpressNo() {
        return generateSerialNumber("EXP");
    }

    /**
     * 通用序列号生成方法
     * @param prefix 前缀(如ORDER, TRANS等)
     */
    private static String generateSerialNumber(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);

        // 1. 添加时间部分(14位)
        sb.append(DATE_TIME_FORMATTER.format(LocalDateTime.now()));

        // 2. 添加机器ID(4位)
        sb.append(MACHINE_ID);

        // 3. 添加序列号(6位)
        sb.append(String.format("%06d", SEQUENCE.incrementAndGet() % 999999));

        // 4. 添加随机后缀(2位)防止猜测
        sb.append(String.format("%02d", ThreadLocalRandom.current().nextInt(0, 99)));

        return sb.toString();
    }

    /**
     * 初始化机器ID(实际项目中可以从配置中心获取)
     */
    private static String initMachineId() {
        // 这里简单演示，实际项目中可以从配置文件或配置中心获取
        String machineId = System.getProperty("machine.id", "0001");
        return String.format("%04d", Integer.parseInt(machineId));
    }

    /**
     * 带业务类型的订单号生成
     * @param bizType 业务类型(2位字母)
     */
    public static String generateBizOrderNo(String bizType) {
        if(bizType == null || bizType.length() != 2) {
            throw new IllegalArgumentException("业务类型必须为2位字母");
        }
        return generateSerialNumber("ORD" + bizType.toUpperCase());
    }
}
