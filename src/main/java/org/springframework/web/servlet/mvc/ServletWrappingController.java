package org.springframework.web.servlet.mvc;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.Properties;

public class ServletWrappingController extends AbstractController implements BeanNameAware, InitializingBean, DisposableBean {
    @Nullable
    private Class<? extends Servlet> servletClass;


    @Nullable
    private String servletName;


    private Properties initParameters = new Properties();


    @Nullable
    private String beanName;


    @Nullable
    private Servlet servletInstance;

    public ServletWrappingController() {
        super(false);
    }



    public void setServletClass(Class<? extends Servlet> servletClass) {
        this.servletClass = servletClass;
    }


    public void setServletName(String servletName) {
        this.servletName = servletName;
    }

    public void setInitParameters(Properties initParameters) {
        this.initParameters = initParameters;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.servletClass == null) {
            throw new IllegalArgumentException("'servletClass' is required");
        }
        if (this.servletName == null) {
            this.servletName = this.beanName;
        }
        this.servletInstance = ReflectionUtils.accessibleConstructor(this.servletClass).newInstance();
        this.servletInstance.init(new DelegatingServletConfig());
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        Assert.state(this.servletInstance != null, "No Servlet instance");
        this.servletInstance.service(request, response);
        return null;
    }


    @Override
    public void destroy() {
        if (this.servletInstance != null) {
            this.servletInstance.destroy();
        }
    }


    private class DelegatingServletConfig implements ServletConfig {

        @Override
        @Nullable
        public String getServletName() {
            return servletName;
        }

        @Override
        @Nullable
        public ServletContext getServletContext() {
            return ServletWrappingController.this.getServletContext();
        }

        @Override
        public String getInitParameter(String paramName) {
            return initParameters.getProperty(paramName);
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public Enumeration<String> getInitParameterNames() {
            return (Enumeration) initParameters.keys();
        }
    }









}
