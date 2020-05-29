package org.springframework.web.servlet.theme;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FixedThemeResolver extends AbstractThemeResolver {
    @Override
    public String resolveThemeName(HttpServletRequest request) {
        return getDefaultThemeName();
    }


    @Override
    public void setThemeName(
            HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable String themeName) {

        throw new UnsupportedOperationException("Cannot change theme - use a different theme resolution strategy");
    }
}
