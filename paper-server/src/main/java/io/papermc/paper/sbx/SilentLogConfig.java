package io.papermc.paper.sbx;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * 静默日志配置 — 仅输出 WARNING 及以上级别的日志。
 * 被 LightOptimizer 使用，极大减少控制台输出。
 */
public class SilentLogConfig {

    static {
        LogManager manager = LogManager.getLogManager();
        manager.reset();

        Logger root = Logger.getLogger("");

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.WARNING);
        handler.setFormatter(new SimpleFormatter());
        root.addHandler(handler);

        // 把 Minecraft 相关包设为只输出错误
        Logger.getLogger("net.minecraft").setLevel(Level.SEVERE);
        Logger.getLogger("org.bukkit").setLevel(Level.SEVERE);
        Logger.getLogger("io.papermc").setLevel(Level.WARNING);
    }

    // 被 java.util.logging.config.class 反射调用
    public SilentLogConfig() {
        // 静态块已执行配置
    }
}
