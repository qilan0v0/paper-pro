package com.qltz.agent;

import com.qltz.agent.collector.Collector;
import com.qltz.agent.collector.SystemInfo;
import com.qltz.agent.reporter.HttpReporter;
import com.qltz.agent.reporter.Reporter;

/**
 * QLTZ Agent 主入口 — 系统监控探针。
 *
 * 所有参数通过环境变量配置（参见 AgentConstants），
 * XA_SERVER 和 UUID 与原项目 App.java 共用同一套环境变量。
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String server = AgentConstants.XA_SERVER;
        String token  = AgentConstants.UUID;
        int interval  = AgentConstants.INTERVAL;
        boolean debug = "debug".equalsIgnoreCase(AgentConstants.LOG_LEVEL);

        // 未配置服务器时直接退出（静默）
        if (server.isEmpty()) {
            return;
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
}
