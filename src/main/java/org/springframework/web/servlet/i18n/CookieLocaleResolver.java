package org.springframework.web.servlet.i18n;


import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.util.CookieGenerator;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.TimeZone;

public class CookieLocaleResolver extends CookieGenerator implements LocaleContextResolver {
    public static final String LOCALE_REQUEST_ATTRIBUTE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";

    public static final String TIME_ZONE_REQUEST_ATTRIBUTE_NAME = CookieLocaleResolver.class.getName() + ".TIME_ZONE";

    public static final String DEFAULT_COOKIE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";


    private boolean languageTagCompliant = true;

    private boolean rejectInvalidCookies = true;
    @Nullable
    private Locale defaultLocale;

    @Nullable
    private TimeZone defaultTimeZone;
    public CookieLocaleResolver() {
        setCookieName(DEFAULT_COOKIE_NAME);
    }
    public void setLanguageTagCompliant(boolean languageTagCompliant) {
        this.languageTagCompliant = languageTagCompliant;
    }
    public boolean isLanguageTagCompliant() {
        return this.languageTagCompliant;
    }
    public void setRejectInvalidCookies(boolean rejectInvalidCookies) {
        this.rejectInvalidCookies = rejectInvalidCookies;
    }
    public boolean isRejectInvalidCookies() {
        return this.rejectInvalidCookies;
    }
    public void setDefaultLocale(@Nullable Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }
    @Nullable
    protected Locale getDefaultLocale() {
        return this.defaultLocale;
    }

    public void setDefaultTimeZone(@Nullable TimeZone defaultTimeZone) {
        this.defaultTimeZone = defaultTimeZone;
    }
    @Nullable
    protected TimeZone getDefaultTimeZone() {
        return this.defaultTimeZone;
    }
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        parseLocaleCookieIfNecessary(request);
        return (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
    }
    @Override
    public LocaleContext resolveLocaleContext(final HttpServletRequest request) {
        parseLocaleCookieIfNecessary(request);
        return new TimeZoneAwareLocaleContext() {
            @Override
            @Nullable
            public Locale getLocale() {
                return (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
            }
            @Override
            @Nullable
            public TimeZone getTimeZone() {
                return (TimeZone) request.getAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
            }
        };
    }
    private void parseLocaleCookieIfNecessary(HttpServletRequest request) {
        if (request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME) == null) {
            Locale locale = null;
            TimeZone timeZone = null;

            // Retrieve and parse cookie value.
            String cookieName = getCookieName();
            if (cookieName != null) {
                Cookie cookie = WebUtils.getCookie(request, cookieName);
                if (cookie != null) {
                    String value = cookie.getValue();
                    String localePart = value;
                    String timeZonePart = null;
                    int separatorIndex = localePart.indexOf('/');
                    if (separatorIndex == -1) {
                        // Leniently accept older cookies separated by a space...
                        separatorIndex = localePart.indexOf(' ');
                    }
                    if (separatorIndex >= 0) {
                        localePart = value.substring(0, separatorIndex);
                        timeZonePart = value.substring(separatorIndex + 1);
                    }
                    try {
                        locale = (!"-".equals(localePart) ? parseLocaleValue(localePart) : null);
                        if (timeZonePart != null) {
                            timeZone = StringUtils.parseTimeZoneString(timeZonePart);
                        }
                    }
                    catch (IllegalArgumentException ex) {
                        if (isRejectInvalidCookies() &&
                                request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) == null) {
                            throw new IllegalStateException("Encountered invalid locale cookie '" +
                                    cookieName + "': [" + value + "] due to: " + ex.getMessage());
                        }
                        else {
                            // Lenient handling (e.g. error dispatch): ignore locale/timezone parse exceptions
                            if (logger.isDebugEnabled()) {
                                logger.debug("Ignoring invalid locale cookie '" + cookieName +
                                        "': [" + value + "] due to: " + ex.getMessage());
                            }
                        }
                    }
                    if (logger.isTraceEnabled()) {
                        logger.trace("Parsed cookie value [" + cookie.getValue() + "] into locale '" + locale +
                                "'" + (timeZone != null ? " and time zone '" + timeZone.getID() + "'" : ""));
                    }
                }
            }

            request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME,
                    (locale != null ? locale : determineDefaultLocale(request)));
            request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME,
                    (timeZone != null ? timeZone : determineDefaultTimeZone(request)));
        }
    }
    @Override
    public void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale) {
        setLocaleContext(request, response, (locale != null ? new SimpleLocaleContext(locale) : null));
    }
    @Override
    public void setLocaleContext(HttpServletRequest request, @Nullable HttpServletResponse response,
                                 @Nullable LocaleContext localeContext) {

        Assert.notNull(response, "HttpServletResponse is required for CookieLocaleResolver");

        Locale locale = null;
        TimeZone timeZone = null;
        if (localeContext != null) {
            locale = localeContext.getLocale();
            if (localeContext instanceof TimeZoneAwareLocaleContext) {
                timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
            }
            addCookie(response,
                    (locale != null ? toLocaleValue(locale) : "-") + (timeZone != null ? '/' + timeZone.getID() : ""));
        }
        else {
            removeCookie(response);
        }
        request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME,
                (locale != null ? locale : determineDefaultLocale(request)));
        request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME,
                (timeZone != null ? timeZone : determineDefaultTimeZone(request)));
    }
    @Nullable
    protected Locale parseLocaleValue(String localeValue) {
        return StringUtils.parseLocale(localeValue);
    }

    protected String toLocaleValue(Locale locale) {
        return (isLanguageTagCompliant() ? locale.toLanguageTag() : locale.toString());
    }
    @Nullable
    protected Locale determineDefaultLocale(HttpServletRequest request) {
        Locale defaultLocale = getDefaultLocale();
        if (defaultLocale == null) {
            defaultLocale = request.getLocale();
        }
        return defaultLocale;
    }
    @Nullable
    protected TimeZone determineDefaultTimeZone(HttpServletRequest request) {
        return getDefaultTimeZone();
    }

}
