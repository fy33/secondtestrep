package org.springframework.web.servlet.i18n;

import net.bytebuddy.asm.Advice;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public class AcceptHeaderLocaleResolver implements LocaleResolver {
    private final List<Locale> supportedLocales = new ArrayList<>(4);

    @Nullable
    private Locale defaultLocale;
    public void setSupportedLocales(List<Locale> locales) {
        this.supportedLocales.clear();
        this.supportedLocales.addAll(locales);
    }
    public List<Locale> getSupportedLocales() {
        return this.supportedLocales;
    }
    public void setDefaultLocale(@Nullable Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }
    @Nullable
    public Locale getDefaultLocale() {
        return this.defaultLocale;
    }
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        Locale defaultLocale = getDefaultLocale();
        if (defaultLocale != null && request.getHeader("Accept-Language") == null) {
            return defaultLocale;
        }
        Locale requestLocale = request.getLocale();
        List<Locale> supportedLocales = getSupportedLocales();
        if (supportedLocales.isEmpty() || supportedLocales.contains(requestLocale)) {
            return requestLocale;
        }
        Locale supportedLocale = findSupportedLocale(request, supportedLocales);
        if (supportedLocale != null) {
            return supportedLocale;
        }
        return (defaultLocale != null ? defaultLocale : requestLocale);
    }
    @Nullable
    private Locale findSupportedLocale(HttpServletRequest request,List<Locale> supportedLocales)
    {
        Enumeration<Locale> requestLocales =request.getLocales();
        Locale languageMathch=null;
        while (requestLocales.hasMoreElements())
        {
            Locale locale=requestLocales.nextElement();
            if(supportedLocales.contains(locale))
            {
                if(languageMathch==null||languageMathch.getLanguage().equals(locale.getLanguage()))
                {
                    return locale;
                }
            }
            else if(languageMathch==null)
            {
                for (Locale candidate : supportedLocales) {
                    if(!StringUtils.hasLength(candidate.getCountry())&&candidate.getLanguage().equals(locale.getLanguage()))
                    {
                        languageMathch=candidate;
                        break;
                    }
                }
            }
        }
        return languageMathch;
    }
    @Override
    public void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale) {
        throw new UnsupportedOperationException(
                "Cannot change HTTP accept header - use a different locale resolution strategy");
    }
}
