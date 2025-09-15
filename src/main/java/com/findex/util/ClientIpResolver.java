package com.findex.util;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpResolver {

    public static String resolve(HttpServletRequest request) {
        String ip = null;
        // 우선순위: X-Real-IP > X-Forwarded-For(첫번째) > RemoteAddr
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            ip = xRealIp.trim();
        } else {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                ip = xff.split(",")[0].trim();
            }
        }

        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }

        // IPv6 표기 정규화
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        if (ip.startsWith("::ffff:")) {
            ip = ip.substring(7); // IPv4-mapped
        }
        int pct = ip.indexOf('%');
        if (pct > 0) {
            ip = ip.substring(0, pct); // zoneId 제거
        }

        return ip;
    }
}