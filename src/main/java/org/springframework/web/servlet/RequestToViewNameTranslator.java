package org.springframework.web.servlet;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;

public interface RequestToViewNameTranslator {
    @Nullable
    String getViewName(HttpServletRequest request) throws Exception;
}
