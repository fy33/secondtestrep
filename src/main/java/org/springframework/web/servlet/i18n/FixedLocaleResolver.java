package org.springframework.web.servlet.i18n;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.TimeZone;

public class FixedLocaleResolver extends AbstractLocaleContextResolver {
    public FixedLocaleResolver() {
        setDefaultLocale(Locale.getDefault());
    }
    public FixedLocaleResolver(Locale locale) {
        setDefaultLocale(locale);
    }
    public FixedLocaleResolver(Locale locale, TimeZone timeZone) {
        setDefaultLocale(locale);
        setDefaultTimeZone(timeZone);
    }
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        Locale locale = getDefaultLocale();
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return locale;
    }
    @Override
    public LocaleContext resolveLocaleContext(HttpServletRequest request) {
        return new TimeZoneAwareLocaleContext() {
            @Override
            @Nullable
            public Locale getLocale() {
                return getDefaultLocale();
            }
            @Override
            public TimeZone getTimeZone() {
                return getDefaultTimeZone();
            }
        };
    }
    @Override
    public void setLocaleContext( HttpServletRequest request, @Nullable HttpServletResponse response,
                                  @Nullable LocaleContext localeContext) {

        throw new UnsupportedOperationException("Cannot change fixed locale - use a different locale resolution strategy");
    }

}
