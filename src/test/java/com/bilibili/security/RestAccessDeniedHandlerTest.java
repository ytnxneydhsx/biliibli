package com.bilibili.security;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

public class RestAccessDeniedHandlerTest {

    @Test
    public void handle_shouldWrite403Json() throws Exception {
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denied"));

        Assert.assertEquals(403, response.getStatus());
        String body = response.getContentAsString();
        Assert.assertTrue(body.contains("\"code\":403"));
        Assert.assertTrue(body.contains("access denied"));
    }
}

