package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface AsyncHandlerInterceptor extends HandlerInterceptor {
    default void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response,
                                                Object handler) throws Exception {
    }
}
