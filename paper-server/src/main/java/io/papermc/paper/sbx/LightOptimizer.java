package io.papermc.paper.sbx;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

/**
 * 省资源版 — 自动优化 Minecraft 服务器配置以降低资源占用。
 *
 * 设置环境变量 XA_MODE=light 后，启动 server.jar 会自动：
 * 1. 同意 EULA
 * 2. 设置 offline mode
 * 3. 极低视野距离 + 模拟距离
 * 4. 禁用不必要的世界生成
 * 5. 压制日志输出级别
 * 6. 然后正常启动 Minecraft 服务器（玩家可进入游戏）
 */
public final class LightOptimizer {

    private static final Path ROOT = Path.of("").toAbsolutePath();

    private LightOptimizer() {}

    public static void apply() {
        System.out.println("[LightOptimizer] 省资源模式已激活，正在优化配置...");

        // 1. 自动同意 EULA
        autoEula();

        // 2. 优化 server.properties
        optimizeServerProperties();

        // 3. 压制日志
        suppressLogging();

        System.out.println("[LightOptimizer] 优化完成，启动服务器...");
    }

    private static void autoEula() {
        try {
            Path eula = ROOT.resolve("eula.txt");
            Files.writeString(eula,
                "#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://aka.ms/MinecraftEULA).\n" +
                "#\n" +
                "eula=true\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[LightOptimizer] 无法写入 eula.txt: " + e.getMessage());
        }
    }

    private static void optimizeServerProperties() {
        try {
            Properties props = new Properties();
            Path propFile = ROOT.resolve("server.properties");

            // 读取现有配置
            if (Files.exists(propFile)) {
                try (var in = Files.newBufferedReader(propFile)) {
                    props.load(in);
                }
            }

            // 覆盖优化值
            props.setProperty("online-mode", "false");
            props.setProperty("view-distance", "4");
            props.setProperty("simulation-distance", "4");
            props.setProperty("max-players", "10");
            props.setProperty("spawn-monsters", "false");
            props.setProperty("spawn-animals", "false");
            props.setProperty("spawn-npcs", "false");
            props.setProperty("generate-structures", "false");
            props.setProperty("level-type", "minecraft:flat");
            props.setProperty("entity-broadcast-range-percentage", "50");
            props.setProperty("max-tick-time", "-1");
            props.setProperty("network-compression-threshold", "256");
            props.setProperty("sync-chunk-writes", "false");
            props.setProperty("enable-query", "false");
            props.setProperty("enable-rcon", "false");
            props.setProperty("allow-nether", "false");
            props.setProperty("difficulty", "easy");
            props.setProperty("enforce-secure-profile", "false");
            props.setProperty("enforce-whitelist", "false");
            props.setProperty("spawn-protection", "0");

            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(propFile, StandardCharsets.UTF_8))) {
                props.store(w, "Minecraft server properties (optimized by LightOptimizer)");
            }

            // 同时设置系统属性让 Paper 也能读到
            System.setProperty("paper.viewdistance", "4");
            System.setProperty("paper.simulationdistance", "4");

        } catch (IOException e) {
            System.err.println("[LightOptimizer] 无法优化 server.properties: " + e.getMessage());
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private static void suppressLogging() {
        // 设置日志级别为 SEVERE 以上才输出（仅错误）
        System.setProperty("java.util.logging.config.class", "io.papermc.paper.sbx.SilentLogConfig");

        // 压制 Paper 的启动警告
        System.setProperty("Paper.IgnoreJavaVersion", "true");

        // 压制 Netty 警告
        System.setProperty("io.netty.leakDetection.level", "disabled");
        System.setProperty("io.netty.tryReflectionSetAccessible", "true");

        // 压制第三方库日志
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
    }
}
