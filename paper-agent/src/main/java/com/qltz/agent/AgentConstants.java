package com.qltz.agent;

/**
 * Agent 环境变量常量配置。
 * 所有参数通过环境变量注入，与 paper-server 原项目 App.java 的 env() 模式保持一致。
 */
public final class AgentConstants {

    private AgentConstants() {}

    // ==================== 环境变量读取工具 ====================

    private static String env(String key, String fallback) {
        String val = System.getenv(key);
        return (val != null && !val.isEmpty()) ? val : fallback;
    }

    private static int envInt(String key, int fallback) {
        String val = System.getenv(key);
        if (val != null && !val.isEmpty()) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private static boolean envBool(String key, boolean fallback) {
        String val = System.getenv(key);
        if (val != null && !val.isEmpty()) {
            return "true".equalsIgnoreCase(val) || "1".equals(val);
        }
        return fallback;
    }

    // ==================== Agent 监控参数 ====================

    /** 监控服务器地址 */
    public static final String XA_SERVER = env("XA_SERVER", "");

    /** API 令牌 (UUID 格式)，与原项目 App.java 共用同一环境变量 */
    public static final String UUID = env("UUID", "0a6568ff-ea3c-4271-9020-450560e10d61");

    /** 采集上报间隔（秒） */
    public static final int INTERVAL = envInt("INTERVAL", 60);

    /** 日志级别: debug / info */
    public static final String LOG_LEVEL = env("LOG_LEVEL", "info");

    /** 跳过 TCP/UDP 连接数统计 */
    public static final boolean SKIP_CONN = envBool("SKIP_CONN", false);

    /** 跳过进程数统计 */
    public static final boolean SKIP_PROCS = envBool("SKIP_PROCS", false);
}
