package com.qltz.agent;

import com.qltz.agent.collector.Collector;
import com.qltz.agent.collector.SystemInfo;
import com.qltz.agent.reporter.HttpReporter;
import com.qltz.agent.reporter.Reporter;

import java.security.SecureRandom;

/**
 * QLTZ Agent 主入口 — 系统监控探针。
 *
 * 所有参数通过环境变量配置（参见 AgentConstants），
 * 无需 CLI 参数，启动后自动采集并上报系统指标。
 */
public class Main {

    private static final String VERSION = "0.1.0";

    public static void main(String[] args) throws Exception {
        String server = AgentConstants.EFFECTIVE_SERVER;
        String token  = AgentConstants.EFFECTIVE_TOKEN;
        int interval  = AgentConstants.INTERVAL;
        boolean debug = "debug".equalsIgnoreCase(AgentConstants.LOG_LEVEL);

        // 未配置服务器时直接退出（静默，无日志输出）
        if (server.isEmpty()) {
            return;
        }

        // Token 为空时自动生成
        if (token.isEmpty()) {
            token = generateUUID();
        }

        // 补全 scheme
        if (!hasScheme(server)) {
            server = "https://" + server;
        }

        Collector collector = new Collector();
        collector.setSkipConn(AgentConstants.SKIP_CONN);
        collector.setSkipProcs(AgentConstants.SKIP_PROCS);

        Reporter reporter = new HttpReporter(server, token, debug);

        // 首次延迟随机抖动
        long firstDelay = System.currentTimeMillis() / 1000 % 60;
        if (firstDelay > 0) {
            Thread.sleep(firstDelay * 1000);
        }

        collectAndReport(collector, reporter);
        while (!Thread.currentThread().isInterrupted()) {
            Thread.sleep(interval * 1000L);
            collectAndReport(collector, reporter);
        }
    }

    private static void collectAndReport(Collector collector, Reporter reporter) {
        try {
            SystemInfo info = collector.collect();
            reporter.report(info);
        } catch (Exception ignored) {
            // 静默处理，不输出到控制台
        }
    }

    private static boolean hasScheme(String u) {
        return u != null && (u.startsWith("http://") || u.startsWith("https://")
                || u.startsWith("wss://") || u.startsWith("ws://"));
    }

    static String generateUUID() {
        SecureRandom rng = new SecureRandom();
        byte[] b = new byte[16];
        rng.nextBytes(b);
        b[6] = (byte) ((b[6] & 0x0f) | 0x40);
        b[8] = (byte) ((b[8] & 0x3f) | 0x80);
        StringBuilder sb = new StringBuilder(36);
        for (int i = 0; i < 16; i++) {
            sb.append(String.format("%02x", b[i]));
            if (i == 3 || i == 5 || i == 7 || i == 9) sb.append('-');
        }
        return sb.toString();
    }
}
