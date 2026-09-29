package com.apr.community.common.requester;

import java.util.Arrays;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequesterResolver {

    public static final String REQUESTER_KEY = "x-user-id";
    public static final String DEFAULT_REQUESTER = "apr_tester";

    public String resolve(HttpServletRequest request) {
        String header = normalized(request.getHeader(REQUESTER_KEY));
        if (header != null) {
            return header;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> REQUESTER_KEY.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .map(this::normalized)
                    .filter(value -> value != null)
                    .findFirst()
                    .orElse(DEFAULT_REQUESTER);
        }
        return DEFAULT_REQUESTER;
    }

    private String normalized(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
