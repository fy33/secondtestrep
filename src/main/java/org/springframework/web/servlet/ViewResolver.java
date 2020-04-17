package org.springframework.web.servlet;

import org.springframework.lang.Nullable;

import java.util.Locale;

public interface ViewResolver {
    @Nullable
    View resolveViewName(String viewName, Locale locale) throws Exception;
}
