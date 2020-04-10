package org.springframework.web.servlet.i18n;


import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.TimeZone;

public abstract class AbstractLocaleResolver  implements LocaleResolver {

    @Nullable
    private Locale defaultLocale;
    public void setDefaultLocale(@Nullable Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }
    @Nullable
    protected Locale getDefaultLocale() {
        return this.defaultLocale;
    }
}
