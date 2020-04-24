package org.springframework.web.context.support;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class GenericWebApplicationContext extends GenericApplicationContext implements ConfigurableWebApplicationContext, ThemeSource {
    @Nullable
    private ServletContext servletContext;

    @Nullable
    private ThemeSource themeSource;

    public GenericWebApplicationContext() {
        super();
    }



    public GenericWebApplicationContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }



    public GenericWebApplicationContext(DefaultListableBeanFactory beanFactory) {
        super(beanFactory);
    }


    public GenericWebApplicationContext(DefaultListableBeanFactory beanFactory, ServletContext servletContext) {
        super(beanFactory);
        this.servletContext = servletContext;
    }

    @Override
    public void setServletContext(@Nullable ServletContext servletContext) {
        this.servletContext = servletContext;
    }


    @Override
    @Nullable
    public ServletContext getServletContext() {
        return this.servletContext;
    }


    @Override
    public String getApplicationName() {
        return (this.servletContext != null ? this.servletContext.getContextPath() : "");
    }



    @Override
    protected ConfigurableEnvironment createEnvironment() {
        return new StandardServletEnvironment();
    }


    @Override
    protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        if (this.servletContext != null) {
            beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext));
            beanFactory.ignoreDependencyInterface(ServletContextAware.class);
        }
        WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
        WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext);
    }


    @Override
    protected Resource getResourceByPath(String path) {
        Assert.state(this.servletContext != null, "No ServletContext available");
        return new ServletContextResource(this.servletContext, path);
    }

    @Override
    protected ResourcePatternResolver getResourcePatternResolver() {
        return new ServletContextResourcePatternResolver(this);
    }

    @Override
    protected void onRefresh() {
        this.themeSource = UiApplicationContextUtils.initThemeSource(this);
    }


    @Override
    protected void initPropertySources() {
        ConfigurableEnvironment env = getEnvironment();
        if (env instanceof ConfigurableWebEnvironment) {
            ((ConfigurableWebEnvironment) env).initPropertySources(this.servletContext, null);
        }
    }

    @Override
    @Nullable
    public Theme getTheme(String themeName) {
        Assert.state(this.themeSource != null, "No ThemeSource available");
        return this.themeSource.getTheme(themeName);
    }


    @Override
    public void setServletConfig(@Nullable ServletConfig servletConfig) {
        // no-op
    }

    @Override
    @Nullable
    public ServletConfig getServletConfig() {
        throw new UnsupportedOperationException(
                "GenericWebApplicationContext does not support getServletConfig()");
    }

    @Override
    public void setNamespace(@Nullable String namespace) {
        // no-op
    }



    @Override
    @Nullable
    public String getNamespace() {
        throw new UnsupportedOperationException(
                "GenericWebApplicationContext does not support getNamespace()");
    }


    @Override
    public void setConfigLocation(String configLocation) {
        if (StringUtils.hasText(configLocation)) {
            throw new UnsupportedOperationException(
                    "GenericWebApplicationContext does not support setConfigLocation(). " +
                            "Do you still have an 'contextConfigLocations' init-param set?");
        }
    }


    @Override
    public void setConfigLocations(String... configLocations) {
        if (!ObjectUtils.isEmpty(configLocations)) {
            throw new UnsupportedOperationException(
                    "GenericWebApplicationContext does not support setConfigLocations(). " +
                            "Do you still have an 'contextConfigLocations' init-param set?");
        }
    }



    @Override
    public String[] getConfigLocations() {
        throw new UnsupportedOperationException(
                "GenericWebApplicationContext does not support getConfigLocations()");
    }








}
