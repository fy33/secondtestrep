package org.springframework.boot.web.servlet.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import java.util.Collections;

public class SpringBootServletInitializer implements WebApplicationInitializer {
    protected Log logger; //Don't initialize early
    private boolean registerErrorPageFilter=true;
    protected final void setRegisterErrorPageFilter(boolean registerErrorPageFilter)
    {
        this.registerErrorPageFilter=registerErrorPageFilter;
    }
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        this.logger= LogFactory.getLog(getClass());
        WebApplicationContext rootAppContext=createRootApplicationContext(servletContext);
        if(rootAppContext!=null)
        {
            servletContext.addListener(new ContextLoaderListener(rootAppContext){
                @Override
                public void contextInitialized(ServletContextEvent event)
                {

                }
            });
        }else {
            this.logger.debug("No ContextLoaderListener registered , as "
            +"createRootApplicationContext() did not "
            +" return an application context");

        }
    }
    protected WebApplicationContext createRootApplicationContext(ServletContext servletContext)
    {
        SpringApplicationBuilder builder=createSpringApplicationBuilder();
        builder.main(getClass());
        ApplicationContext parent=getExistingRootWebApplicationContext(servletContext);
        if(parent!=null)
        {
            this.logger.info(" Root context already created (using as parent).");
            servletContext.setAttribute(
                    WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,null
            );
            builder.initializers(new ParentContextApplicationContextInitializer(parent));
        }
        builder.initializers(new ServletContextApplicationContextInitializer(servletContext));
        builder.contextClass(AnnotationConfigServletWebServerApplicationContext.class);
        builder=configure(builder);
        builder.listeners(new WebEnvironmentPropertySourceInitializer(servletContext));
        SpringApplication application=builder.build();
        if(application.getAllSources()
        .isEmpty()&& AnnotationUtils.findAnnotation(getClass(), Configuration.class)!=null)
        {
            application.addPrimarySources(Collections.singleton(getClass()));
        }
        Assert.state(!application.getAllSources().isEmpty(),
                "No SpringApplication sources have been defined. Either override the " +
                        "configure method or add an @Configuration annotation");
        //Ensure error pages are registered
        if(this.registerErrorPageFilter)
        {
            application.addPrimarySources(Collections.singleton(ErrorPageFilterConfiguration.class));
        }
        return run(application);
    }
    protected WebApplicationContext run(SpringApplication application)
    {
        return (WebApplicationContext)application.run();
    }
    private ApplicationContext getExistingRootWebApplicationContext(ServletContext servletContext)
    {
        Object context=servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        if(context instanceof ApplicationContext)
        {
            return (ApplicationContext)context;
        }
        return null;
    }
    protected SpringApplicationBuilder createSpringApplicationBuilder()
    {
        return new SpringApplicationBuilder();
    }
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder)
    {
        return builder;
    }
    private static final class WebEnvironmentPropertySourceInitializer implements
            ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered{
        private final ServletContext servletContext;
        private WebEnvironmentPropertySourceInitializer(ServletContext servletContext)
        {
            this.servletContext=servletContext;
        }
        @Override
        public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event)
        {
            ConfigurableEnvironment environment=event.getEnvironment();
            if(environment instanceof ConfigurableWebEnvironment)
            {
                ((ConfigurableWebEnvironment)environment).initPropertySources(
                        this.servletContext,null
                );
            }
        }
        @Override
        public int getOrder()
        {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}
