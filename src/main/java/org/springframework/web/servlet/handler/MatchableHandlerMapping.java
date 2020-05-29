package org.springframework.web.servlet.handler;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;

public interface MatchableHandlerMapping extends HandlerMapping {
    @Nullable
    RequestMatchResult match(HttpServletRequest request, String pattern);
}
