package com.qltz.agent;

/**
 * Agent 环境变量常量配置。
 * 所有参数通过环境变量注入，无需 CLI 参数。
 * 遵循 paper-pro 原项目的 env() 配置模式。
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

    /** 监控服务器地址，如 https://your-server.com */
    public static final String SERVER = env("SERVER", "");

    /** API 令牌 (UUID 格式) */
    public static final String UUID = env("UUID", "");

    /** 采集上报间隔（秒） */
    public static final int INTERVAL = envInt("INTERVAL", 60);

    /** 日志级别: debug / info */
    public static final String LOG_LEVEL = env("LOG_LEVEL", "info");

    /** 跳过 TCP/UDP 连接数统计 */
    public static final boolean SKIP_CONN = envBool("SKIP_CONN", false);

    /** 跳过进程数统计 */
    public static final boolean SKIP_PROCS = envBool("SKIP_PROCS", false);

    /** 客户端 ID */
    public static final int AGENT_ID = envInt("AGENT_ID", 0);

    // ==================== 兼容旧环境变量名称 ====================

    /** QLTZ_SERVER 兼容（同 SERVER） */
    public static final String QLTZ_SERVER = env("QLTZ_SERVER", SERVER);

    /** QLTZ_TOKEN 兼容（同 UUID） */
    public static final String QLTZ_TOKEN = env("QLTZ_TOKEN", UUID);

    /** 最终使用的服务器地址：QLTZ_SERVER > SERVER > "" */
    public static final String EFFECTIVE_SERVER = QLTZ_SERVER.isEmpty() ? SERVER : QLTZ_SERVER;

    /** 最终使用的令牌：QLTZ_TOKEN > UUID > "" */
    public static final String EFFECTIVE_TOKEN = QLTZ_TOKEN.isEmpty() ? UUID : QLTZ_TOKEN;
}
