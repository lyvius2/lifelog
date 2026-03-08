package com.walter.lifelog.web.util;

import com.walter.lifelog.shared.config.GoogleDriveConfig;
import jakarta.servlet.http.HttpServletRequest;

public class RedirectUrlBuilder {
    private RedirectUrlBuilder() {}

    public static String build(HttpServletRequest request) {
        final String scheme = request.getScheme();
        final String serverName = request.getServerName();
        final int serverPort = request.getServerPort();
        String portPart = "";
        if ((!"http".equals(scheme) || serverPort != 80) && (!"https".equals(scheme) || serverPort != 443)) {
            portPart = ":" + serverPort;
        }
        return scheme + serverName + portPart + GoogleDriveConfig.CALLBACK_PATH;
    }
}
