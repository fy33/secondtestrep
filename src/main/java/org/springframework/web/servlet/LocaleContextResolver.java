package org.springframework.web.servlet;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface LocaleContextResolver extends LocaleResolver {
    LocaleContext resolveLocaleContext(HttpServletRequest request);
    void setLocaleContext(HttpServletRequest request, @Nullable HttpServletResponse response,
                          @Nullable LocaleContext localeContext);
}
