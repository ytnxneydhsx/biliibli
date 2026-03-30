package com.bilibili.location.service.impl;

import com.bilibili.location.service.IpLocationResolver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.LinkedHashSet;

@Component
public class Ip2RegionIpLocationResolver implements IpLocationResolver {

    private static final String IPV4_XDB_RESOURCE = "ip2region/ip2region_v4.xdb";
    private static final String IPV6_XDB_RESOURCE = "ip2region/ip2region_v6.xdb";

    private volatile Ip2Region ip2Region;

    @PostConstruct
    public void init() {
        try {
            Config v4Config = buildIpv4Config();
            Config v6Config = buildIpv6Config();
            this.ip2Region = Ip2Region.create(v4Config, v6Config);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to initialize ip2region resolver", ex);
        }
    }

    @PreDestroy
    public void destroy() {
        Ip2Region current = this.ip2Region;
        if (current == null) {
            return;
        }
        try {
            current.close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public String resolveLocation(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }

        String normalizedIp = ip.trim();
        String reservedLocation = resolveReservedLocation(normalizedIp);
        if (reservedLocation != null && !reservedLocation.isBlank()) {
            return reservedLocation;
        }

        Ip2Region current = this.ip2Region;
        if (current == null) {
            return null;
        }

        try {
            return normalizeRegion(current.search(normalizedIp));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Config buildIpv4Config() throws Exception {
        ClassPathResource resource = new ClassPathResource(IPV4_XDB_RESOURCE);
        if (!resource.exists()) {
            throw new IllegalStateException("missing required ip2region resource: " + IPV4_XDB_RESOURCE);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return Config.custom()
                    .setCachePolicy(Config.BufferCache)
                    .setXdbInputStream(inputStream)
                    .asV4();
        }
    }

    private Config buildIpv6Config() throws Exception {
        ClassPathResource resource = new ClassPathResource(IPV6_XDB_RESOURCE);
        if (!resource.exists()) {
            return null;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return Config.custom()
                    .setCachePolicy(Config.BufferCache)
                    .setXdbInputStream(inputStream)
                    .asV6();
        }
    }

    private String normalizeRegion(String region) {
        if (region == null || region.isBlank()) {
            return null;
        }

        String[] segments = region.split("\\|");
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        for (String segment : segments) {
            if (segment == null) {
                continue;
            }
            String normalizedSegment = segment.trim();
            if (normalizedSegment.isEmpty() || "0".equals(normalizedSegment)) {
                continue;
            }
            resolved.add(normalizedSegment);
        }

        if (resolved.isEmpty()) {
            return null;
        }
        return String.join(" ", resolved);
    }

    private String resolveReservedLocation(String ip) {
        if (isLoopbackOrPrivate(ip)) {
            return "局域网";
        }
        return null;
    }

    private boolean isLoopbackOrPrivate(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()) {
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }

        String lower = ip.toLowerCase();
        return lower.startsWith("fc")
                || lower.startsWith("fd")
                || lower.startsWith("fe80:")
                || lower.startsWith("::1");
    }
}
