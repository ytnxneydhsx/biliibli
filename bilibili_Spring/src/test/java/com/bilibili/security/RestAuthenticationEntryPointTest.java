package com.bilibili.security;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

public class RestAuthenticationEntryPointTest {

    @Test
    public void commence_shouldWrite401Json() throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("auth required"));

        Assert.assertEquals(401, response.getStatus());
        String body = response.getContentAsString();
        Assert.assertTrue(body.contains("\"code\":401"));
        Assert.assertTrue(body.contains("login required"));
    }
}

