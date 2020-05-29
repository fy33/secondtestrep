package org.springframework.web.servlet.theme;

import org.springframework.web.servlet.ThemeResolver;

public abstract class AbstractThemeResolver implements ThemeResolver {
    public static final String ORIGINAL_DEFAULT_THEME_NAME = "theme";

    private String defaultThemeName = ORIGINAL_DEFAULT_THEME_NAME;

    public void setDefaultThemeName(String defaultThemeName) {
        this.defaultThemeName = defaultThemeName;
    }

    public String getDefaultThemeName() {
        return this.defaultThemeName;
    }

}
