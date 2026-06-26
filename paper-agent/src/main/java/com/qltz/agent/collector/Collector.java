package com.qltz.agent.collector;

import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 系统信息采集器，使用 OSHI 库进行跨平台采集。
 * 对应 Go 版本的 collector.DefaultCollector。
 */
public class Collector {

    public static final String VERSION = "0.1.0";

    private boolean skipConn;
    private boolean skipProcs;

    public Collector() {
    }

    public void setSkipConn(boolean skip) { this.skipConn = skip; }
    public void setSkipProcs(boolean skip) { this.skipProcs = skip; }

    /**
     * 采集系统信息
     */
    public SystemInfo collect() {
        SystemInfo info = new SystemInfo();
        info.setTimestamp(Instant.now());

        oshi.SystemInfo oshiSi = new oshi.SystemInfo();
        OperatingSystem os = oshiSi.getOperatingSystem();
        CentralProcessor cpu = oshiSi.getHardware().getProcessor();
        GlobalMemory mem = oshiSi.getHardware().getMemory();

        // 主机信息
        info.setHostname(getHostname());
        info.setOs(os.getFamily());
        info.setPlatform(System.getProperty("os.name"));
        info.setVersion(os.getVersionInfo() != null
                ? os.getFamily() + " " + os.getVersionInfo().getVersion() + " (" + os.getVersionInfo().getBuildNumber() + ")"
                : System.getProperty("os.version"));

        // CPU
        SystemInfo.CPUInfo cpuInfo = new SystemInfo.CPUInfo();
        cpuInfo.setCores(Runtime.getRuntime().availableProcessors());
        cpuInfo.setModelName(cpu.getProcessorIdentifier().getName());
        cpuInfo.setArch(System.getProperty("os.arch"));
        // CPU 使用率：使用 OSHI 计算（需要两次采样取差值，这里取一个较短间隔）
        long[] prevTicks = cpu.getSystemCpuLoadTicks();
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        double cpuLoad = cpu.getSystemCpuLoadBetweenTicks(prevTicks) * 100.0;
        cpuInfo.setUsage(Math.max(0, Math.min(100, cpuLoad)));
        // 温度（如果有传感器）
        try {
            double temp = oshiSi.getHardware().getSensors().getCpuTemperature();
            if (temp > 0) cpuInfo.setTemperature(temp);
        } catch (Exception ignored) {}
        info.setCpuInfo(cpuInfo);

        // 内存
        SystemInfo.MemoryInfo memInfo = new SystemInfo.MemoryInfo();
        memInfo.setTotal(mem.getTotal());
        memInfo.setUsed(mem.getTotal() - mem.getAvailable());
        memInfo.setFree(mem.getAvailable());
        if (mem.getTotal() > 0) {
            memInfo.setUsageRate(((double)(mem.getTotal() - mem.getAvailable()) / mem.getTotal()) * 100.0);
        }
        info.setMemoryInfo(memInfo);

        // 磁盘 — 使用 Java NIO FileStore 枚举挂载点，容器兼容性更好
        List<SystemInfo.DiskInfo> disks = new ArrayList<>();
        try {
            for (java.nio.file.FileStore store : java.nio.file.FileSystems.getDefault().getFileStores()) {
                String mount = store.toString(); // e.g. "/ (/dev/sda1)" or "/ (overlay)"
                String fsType = store.type();

                // 提取挂载点
                String mountPoint = "/";
                int spaceIdx = mount.indexOf(' ');
                if (spaceIdx > 0) mountPoint = mount.substring(0, spaceIdx);

                if (isVirtualFS(fsType)) continue;
                // 容器根文件系统通常是 overlay，不跳过；非根 overlay 则跳过
                if (("overlay".equals(fsType) || "overlayfs".equals(fsType)) && !"/".equals(mountPoint)) continue;

                SystemInfo.DiskInfo di = new SystemInfo.DiskInfo();
                di.setMountPoint(mountPoint);
                di.setFsType(fsType);
                di.setTotal(store.getTotalSpace());
                di.setFree(store.getUsableSpace());
                di.setUsed(store.getTotalSpace() - store.getUsableSpace());
                di.setUsageRate(di.getTotal() > 0
                        ? ((double) di.getUsed() / di.getTotal()) * 100.0 : 0);
                // 尝试取设备名
                di.setDevice(extractDeviceName(mount));
                disks.add(di);
            }
        } catch (Exception e) {
            // 极端情况 fallback 到根目录
            try {
                java.io.File root = new java.io.File("/");
                SystemInfo.DiskInfo di = new SystemInfo.DiskInfo();
                di.setDevice("rootfs");
                di.setMountPoint("/");
                di.setTotal(root.getTotalSpace());
                di.setFree(root.getFreeSpace());
                di.setUsed(root.getTotalSpace() - root.getFreeSpace());
                di.setUsageRate(root.getTotalSpace() > 0
                        ? ((double)(root.getTotalSpace() - root.getFreeSpace()) / root.getTotalSpace()) * 100.0 : 0);
                di.setFsType("unknown");
                disks.add(di);
            } catch (Exception ignored) {}
        }
        info.setDiskInfo(disks);

        // 网络
        List<SystemInfo.NetworkInfo> nets = new ArrayList<>();
        for (NetworkIF netIF : oshiSi.getHardware().getNetworkIFs()) {
            if ("lo".equals(netIF.getName())) continue;
            netIF.updateAttributes();
            SystemInfo.NetworkInfo ni = new SystemInfo.NetworkInfo();
            ni.setIface(netIF.getName());
            ni.setBytesSent(netIF.getBytesSent());
            ni.setBytesRecv(netIF.getBytesRecv());
            ni.setPacketsSent(netIF.getPacketsSent());
            ni.setPacketsRecv(netIF.getPacketsRecv());
            nets.add(ni);
        }
        info.setNetworkInfo(nets);

        // 系统负载
        SystemInfo.LoadInfo loadInfo = new SystemInfo.LoadInfo();
        double[] loadAvg = cpu.getSystemLoadAverage(3);
        if (loadAvg.length >= 3) {
            loadInfo.setLoad1(loadAvg[0]);
            loadInfo.setLoad5(loadAvg[1]);
            loadInfo.setLoad15(loadAvg[2]);
        }
        info.setLoadInfo(loadInfo);

        // 进程数
        if (!skipProcs) {
            info.setProcessCount(os.getProcessCount());
        }

        // TCP/UDP 连接数 — 解析 /proc/net/tcp 和 /proc/net/udp
        if (!skipConn) {
            try {
                info.setTcpCount(countProcNetLines("/proc/net/tcp"));
                info.setUdpCount(countProcNetLines("/proc/net/udp"));
            } catch (Exception e) {
                // 非 Linux 或无权读取时保持 0
            }
        }

        // 启动时间
        info.setBootTime(Instant.ofEpochSecond(os.getSystemBootTime()));

        // Agent 版本
        info.setAgentVersion(VERSION);

        return info;
    }

    /**
     * 读取 /proc/net/tcp 或 /proc/net/udp，统计连接行数（跳过表头）
     */
    private int countProcNetLines(String path) {
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.FileReader(path));
            int count = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.startsWith("  sl")) count++;
            }
            reader.close();
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 从 FileStore.toString() 中提取设备名，如 "/ (/dev/sda1)" → "sda1"
     */
    private String extractDeviceName(String storeStr) {
        int start = storeStr.indexOf('(');
        int end = storeStr.lastIndexOf(')');
        if (start >= 0 && end > start) {
            String inner = storeStr.substring(start + 1, end);
            int slash = inner.lastIndexOf('/');
            return slash >= 0 ? inner.substring(slash + 1) : inner;
        }
        return "unknown";
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取本机第一个非回环 IPv4 地址
     */
    public static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {}
        return "unknown";
    }

    /**
     * 判断是否为虚拟文件系统，对应 Go 的 isVirtualFS
     */
    private boolean isVirtualFS(String fstype) {
        if (fstype == null) return true;
        return switch (fstype.toLowerCase()) {
            case "tmpfs", "devtmpfs", "devfs", "aufs",
                 "proc", "sysfs", "cgroup", "cgroup2", "pstore", "bpf", "tracefs",
                 "debugfs", "securityfs", "configfs", "fusectl", "mqueue", "hugetlbfs",
                 "ramfs", "nsfs", "autofs", "binfmt_misc", "squashfs", "fuse.lxcfs",
                 "rpc_pipefs", "selinuxfs", "efivarfs", "none", "" -> true;
            default -> false;
        };
    }
}
