package io.papermc.paper.sbx;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 卡启动版 — 伪装 Minecraft 服务器启动。
 *
 * 设置环境变量 XA_MODE=fake 后，启动 server.jar 会：
 * 1. 打印逼真的启动日志
 * 2. 自动创建 eula.txt + server.properties
 * 3. 后台静默运行 Nezha Agent
 * 4. 永远不启动真正的 Minecraft 服务器
 */
public final class FakeStarter {

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private FakeStarter() {}

    public static void run() throws Exception {
        log("[bootstrap] Running Java %s (%s; %s) on %s",
            System.getProperty("java.version"),
            System.getProperty("java.vm.name"),
            System.getProperty("java.vm.version"),
            System.getProperty("os.name"));

        log("[bootstrap] Loading Paper 1.21.4-232-ver/1.21.4@12d8fe0 (2025-06-09T10:15:42Z) for Minecraft 1.21.4");
        log("[PluginInitializerManager] Initializing plugins...");
        log("[PluginInitializerManager] Initialized 0 plugins");
        log("Environment: Environment[sessionHost=https://sessionserver.mojang.com, servicesHost=https://api.minecraftservices.com, name=PROD]");
        log("Found new data pack file/bukkit, loading it automatically");
        log("Found new data pack paper, loading it automatically");
        log("No existing world data, creating new world");

        // 创建 eula.txt
        createEula();

        // 创建 server.properties
        createServerProperties();

        sleep(2000);

        log("Loaded 1370 recipes");
        log("Loaded 1481 advancements");
        log("[MCTypeRegistry] Initialising converters for DataConverter...");
        sleep(500);
        log("[MCTypeRegistry] Finished initialising converters for DataConverter in 1,125.9ms");
        log("Starting minecraft server version 1.21.4");
        log("Loading properties");
        log("This server is running Paper version 1.21.4-232-ver/1.21.4@12d8fe0 (2025-06-09T10:15:42Z) (Implementing API version 1.21.4-R0.1-SNAPSHOT)");
        log("Server Ping Player Sample Count: 12");
        log("Using 4 threads for Netty based IO");
        log("Default game type: SURVIVAL");
        log("Generating keypair");
        log("Starting Minecraft server on 0.0.0.0:25565");
        log("Using epoll channel type");
        log("Paper: Using libdeflate (Linux x86_64) compression from Velocity.");
        log("Paper: Using OpenSSL 3.x.x (Linux x86_64) cipher from Velocity.");
        log("Preparing level \"world\"");

        // 模拟 Preparing spawn area 进度
        for (int i = 0; i < 30; i++) {
            log("Preparing spawn area: %d%%", randomProgress(i));
            sleep(300 + (int)(Math.random() * 700));
        }

        log("Time elapsed: 26711 ms");
        log("Preparing start region for dimension minecraft:the_nether");

        for (int i = 0; i < 8; i++) {
            log("Preparing spawn area: %d%%", randomProgressNether(i));
            sleep(200 + (int)(Math.random() * 600));
        }

        log("Time elapsed: 5980 ms");
        log("Preparing start region for dimension minecraft:the_end");

        for (int i = 0; i < 4; i++) {
            log("Preparing spawn area: %d%%", randomProgressEnd(i));
            sleep(200 + (int)(Math.random() * 400));
        }

        log("Time elapsed: 1906 ms");
        log("[spark] Starting background profiler...");
        sleep(1000);
        log("Done preparing level \"world\" (71.792s)");
        log("Running delayed init tasks");
        log("Done (52.903s)! For help, type \"help\"");
        log("*************************************************************************************");
        log("This is the first time you're starting this server.");
        log("It's recommended you read our 'Getting Started' documentation for guidance.");
        log("*************************************************************************************");

        // 启动 Agent 后台运行
        startAgent();

        // 保持进程存活
        log("[FakeStarter] Server running in FAKE mode. Agent active in background.");
        Thread.currentThread().join();
    }

    private static void createEula() throws IOException {
        Path eula = ROOT.resolve("eula.txt");
        if (!Files.exists(eula)) {
            Files.writeString(eula,
                "#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://aka.ms/MinecraftEULA).\n" +
                "#\n" +
                "eula=true\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private static void createServerProperties() throws IOException {
        Path props = ROOT.resolve("server.properties");
        if (!Files.exists(props)) {
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(props, StandardCharsets.UTF_8))) {
                w.println("#Minecraft server properties");
                w.println("accepts-transfers=false");
                w.println("allow-flight=false");
                w.println("allow-nether=false");
                w.println("broadcast-console-to-ops=true");
                w.println("broadcast-rcon-to-ops=true");
                w.println("difficulty=easy");
                w.println("enable-command-block=false");
                w.println("enable-query=false");
                w.println("enable-rcon=false");
                w.println("enforce-secure-profile=false");
                w.println("enforce-whitelist=false");
                w.println("entity-broadcast-range-percentage=50");
                w.println("force-gamemode=false");
                w.println("function-permission-level=2");
                w.println("gamemode=survival");
                w.println("generate-structures=false");
                w.println("generator-settings={}");
                w.println("hardcore=false");
                w.println("hide-online-players=false");
                w.println("level-name=world");
                w.println("level-seed=");
                w.println("level-type=minecraft\\:flat");
                w.println("max-chained-neighbor-updates=0");
                w.println("max-players=20");
                w.println("max-tick-time=60000");
                w.println("max-world-size=29999984");
                w.println("motd=A Minecraft Server");
                w.println("network-compression-threshold=256");
                w.println("online-mode=false");
                w.println("op-permission-level=4");
                w.println("player-idle-timeout=0");
                w.println("prevent-proxy-connections=false");
                w.println("pvp=true");
                w.println("query.port=25565");
                w.println("rate-limit=0");
                w.println("rcon.password=");
                w.println("rcon.port=25575");
                w.println("resource-pack=");
                w.println("resource-pack-id=");
                w.println("resource-pack-prompt=");
                w.println("resource-pack-sha1=");
                w.println("server-ip=");
                w.println("server-port=25565");
                w.println("simulation-distance=4");
                w.println("spawn-animals=false");
                w.println("spawn-monsters=false");
                w.println("spawn-npcs=false");
                w.println("spawn-protection=0");
                w.println("sync-chunk-writes=true");
                w.println("text-filtering-config=");
                w.println("use-native-transport=true");
                w.println("view-distance=4");
                w.println("white-list=false");
            }
        }
    }

    private static void startAgent() {
        Thread agentThread = new Thread(() -> {
            try {
                String server = System.getenv("XA_SERVER");
                String token  = System.getenv("UUID");
                if (server == null || server.isEmpty() || token == null || token.isEmpty()) {
                    return; // 没配 Agent 就算了
                }
                com.qltz.agent.collector.Collector collector = new com.qltz.agent.collector.Collector();
                com.qltz.agent.reporter.HttpReporter reporter = new com.qltz.agent.reporter.HttpReporter(
                    server.startsWith("http") ? server : "https://" + server,
                    token, false);

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        com.qltz.agent.collector.SystemInfo info = collector.collect();
                        reporter.report(info);
                    } catch (Exception ignored) {}
                    Thread.sleep(60000);
                }
            } catch (Exception ignored) {}
        }, "xa-agent");
        agentThread.setDaemon(true);
        agentThread.start();
    }

    private static void log(String format, Object... args) {
        String msg = args.length > 0 ? String.format(format, args) : format;
        String time = LocalDateTime.now().format(LOG_TIME);
        System.out.println("[" + time + " INFO]: " + msg);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static int randomProgress(int step) {
        int[] values = {2, 2, 2, 4, 6, 10, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 32, 36, 36, 36, 36, 36, 36, 36, 36, 51, 51, 69};
        if (step < values.length) return values[step];
        return Math.min(100, 69 + step);
    }

    private static int randomProgressNether(int step) {
        int[] values = {4, 4, 4, 4, 4, 24, 30, 51};
        if (step < values.length) return values[step];
        return Math.min(100, 61 + step);
    }

    private static int randomProgressEnd(int step) {
        int[] values = {2, 2, 18, 51};
        if (step < values.length) return values[step];
        return 100;
    }
}
