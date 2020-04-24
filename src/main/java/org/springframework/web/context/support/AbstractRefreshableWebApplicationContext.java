package org.springframework.web.context.support;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public  abstract  class AbstractRefreshableWebApplicationContext extends AbstractRefreshableConfigApplicationContext
implements ConfigurableWebApplicationContext, ThemeSource {
    @Nullable
    private ServletContext servletContext;

    @Nullable
    private ServletConfig servletConfig;

    @Nullable
    private String namespace;


    @Nullable
    private ThemeSource themeSource;

    public AbstractRefreshableWebApplicationContext() {
        setDisplayName("Root WebApplicationContext");
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
    public void setServletConfig(@Nullable ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
        if (servletConfig != null && this.servletContext == null) {
            setServletContext(servletConfig.getServletContext());
        }
    }


    @Override
    @Nullable
    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }


    @Override
    public void setNamespace(@Nullable String namespace) {
        this.namespace = namespace;
        if (namespace != null) {
            setDisplayName("WebApplicationContext for namespace '" + namespace + "'");
        }
    }


    @Override
    @Nullable
    public String getNamespace() {
        return this.namespace;
    }

    @Override
    public String[] getConfigLocations() {
        return super.getConfigLocations();
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
        beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext, this.servletConfig));
        beanFactory.ignoreDependencyInterface(ServletContextAware.class);
        beanFactory.ignoreDependencyInterface(ServletConfigAware.class);

        WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
        WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext, this.servletConfig);
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
            ((ConfigurableWebEnvironment) env).initPropertySources(this.servletContext, this.servletConfig);
        }
    }


    @Override
    @Nullable
    public Theme getTheme(String themeName) {
        Assert.state(this.themeSource != null, "No ThemeSource available");
        return this.themeSource.getTheme(themeName);
    }






}
