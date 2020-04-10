package org.springframework.web.servlet.i18n;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.LocaleContextResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.TimeZone;

public abstract class AbstractLocaleContextResolver extends AbstractLocaleResolver implements LocaleContextResolver {
    @Nullable
    private TimeZone defaultTimeZone;
    public void setDefaultTimeZone(@Nullable TimeZone defaultTimeZone) {
        this.defaultTimeZone = defaultTimeZone;
    }
    @Nullable
    public TimeZone getDefaultTimeZone() {
        return this.defaultTimeZone;
    }
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        Locale locale = resolveLocaleContext(request).getLocale();
        return (locale != null ? locale : request.getLocale());
    }
    @Override
    public void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale) {
        setLocaleContext(request, response, (locale != null ? new SimpleLocaleContext(locale) : null));
    }
}
