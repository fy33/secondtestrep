package org.springframework.web.servlet.theme;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SessionThemeResolver extends AbstractThemeResolver {
    public static final String THEME_SESSION_ATTRIBUTE_NAME = SessionThemeResolver.class.getName() + ".THEME";


    @Override
    public String resolveThemeName(HttpServletRequest request) {
        String themeName = (String) WebUtils.getSessionAttribute(request, THEME_SESSION_ATTRIBUTE_NAME);
        // A specific theme indicated, or do we need to fallback to the default?
        return (themeName != null ? themeName : getDefaultThemeName());
    }


    @Override
    public void setThemeName(
            HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable String themeName) {

        WebUtils.setSessionAttribute(request, THEME_SESSION_ATTRIBUTE_NAME,
                (StringUtils.hasText(themeName) ? themeName : null));
    }

}
