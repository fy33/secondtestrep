package org.springframework.web.servlet.i18n;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

public class LocaleChangeInterceptor extends HandlerInterceptorAdapter {
    public static final String DEFAULT_PARAM_NAME = "locale";
    protected final Log logger = LogFactory.getLog(getClass());
    private String paramName = DEFAULT_PARAM_NAME;
    @Nullable
    private String[] httpMethods;
    private boolean ignoreInvalidLocale = false;
    public void setParamName(String paramName) {
        this.paramName = paramName;
    }
    public String getParamName() {
        return this.paramName;
    }
    public void setHttpMethods(@Nullable String... httpMethods) {
        this.httpMethods = httpMethods;
    }

    @Nullable
    public String[] getHttpMethods() {
        return this.httpMethods;
    }
    public void setIgnoreInvalidLocale(boolean ignoreInvalidLocale) {
        this.ignoreInvalidLocale = ignoreInvalidLocale;
    }

    public boolean isIgnoreInvalidLocale() {
        return this.ignoreInvalidLocale;
    }
    @Deprecated
    public void setLanguageTagCompliant(boolean languageTagCompliant) {
        if (!languageTagCompliant) {
            throw new IllegalArgumentException("LocaleChangeInterceptor always accepts BCP 47 language tags");
        }
    }
    @Deprecated
    public boolean isLanguageTagCompliant() {
        return true;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws ServletException {

        String newLocale = request.getParameter(getParamName());
        if (newLocale != null) {
            if (checkHttpMethod(request.getMethod())) {
                LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
                if (localeResolver == null) {
                    throw new IllegalStateException(
                            "No LocaleResolver found: not in a DispatcherServlet request?");
                }
                try {
                    localeResolver.setLocale(request, response, parseLocaleValue(newLocale));
                }
                catch (IllegalArgumentException ex) {
                    if (isIgnoreInvalidLocale()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Ignoring invalid locale value [" + newLocale + "]: " + ex.getMessage());
                        }
                    }
                    else {
                        throw ex;
                    }
                }
            }
        }
        // Proceed in any case.
        return true;
    }
    private boolean checkHttpMethod(String currentMethod) {
        String[] configuredMethods = getHttpMethods();
        if (ObjectUtils.isEmpty(configuredMethods)) {
            return true;
        }
        for (String configuredMethod : configuredMethods) {
            if (configuredMethod.equalsIgnoreCase(currentMethod)) {
                return true;
            }
        }
        return false;
    }
    @Nullable
    protected Locale parseLocaleValue(String localeValue) {
        return StringUtils.parseLocale(localeValue);
    }



















}
