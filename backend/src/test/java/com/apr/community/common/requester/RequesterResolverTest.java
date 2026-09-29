package com.apr.community.common.requester;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RequesterResolverTest {

    private final RequesterResolver requesterResolver = new RequesterResolver();

    @Test
    void resolvesHeaderRequester() {
        var request = new MockHttpServletRequest();
        request.addHeader("x-user-id", " header-user ");

        assertThat(requesterResolver.resolve(request)).isEqualTo("header-user");
    }

    @Test
    void resolvesCookieRequester() {
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("x-user-id", " cookie-user "));

        assertThat(requesterResolver.resolve(request)).isEqualTo("cookie-user");
    }

    @Test
    void defaultsWhenHeaderAndCookieAreAbsent() {
        assertThat(requesterResolver.resolve(new MockHttpServletRequest()))
                .isEqualTo("apr_tester");
    }

    @Test
    void headerWinsWhenHeaderAndCookieAreBothPresent() {
        var request = new MockHttpServletRequest();
        request.addHeader("x-user-id", "header-user");
        request.setCookies(new Cookie("x-user-id", "cookie-user"));

        assertThat(requesterResolver.resolve(request)).isEqualTo("header-user");
    }

    @Test
    void blankHeaderFallsBackToCookie() {
        var request = new MockHttpServletRequest();
        request.addHeader("x-user-id", "   ");
        request.setCookies(new Cookie("x-user-id", "cookie-user"));

        assertThat(requesterResolver.resolve(request)).isEqualTo("cookie-user");
    }

    @Test
    void blankHeaderAndCookieFallBackToDefault() {
        var request = new MockHttpServletRequest();
        request.addHeader("x-user-id", "   ");
        request.setCookies(new Cookie("x-user-id", "\t"));

        assertThat(requesterResolver.resolve(request)).isEqualTo("apr_tester");
    }
}
