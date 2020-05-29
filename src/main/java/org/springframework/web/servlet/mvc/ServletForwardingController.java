package org.springframework.web.servlet.mvc;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletForwardingController extends AbstractController implements BeanNameAware {
    @Nullable
    private String servletName;

    @Nullable
    private String beanName;


    public ServletForwardingController() {
        super(false);
    }

    public void setServletName(String servletName) {
        this.servletName = servletName;
    }


    @Override
    public void setBeanName(String name) {
        this.beanName = name;
        if (this.servletName == null) {
            this.servletName = name;
        }
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        ServletContext servletContext = getServletContext();
        Assert.state(servletContext != null, "No ServletContext");
        RequestDispatcher rd = servletContext.getNamedDispatcher(this.servletName);
        if (rd == null) {
            throw new ServletException("No servlet with name '" + this.servletName + "' defined in web.xml");
        }

        // If already included, include again, else forward.
        if (useInclude(request, response)) {
            rd.include(request, response);
            if (logger.isTraceEnabled()) {
                logger.trace("Included servlet [" + this.servletName +
                        "] in ServletForwardingController '" + this.beanName + "'");
            }
        }
        else {
            rd.forward(request, response);
            if (logger.isTraceEnabled()) {
                logger.trace("Forwarded to servlet [" + this.servletName +
                        "] in ServletForwardingController '" + this.beanName + "'");
            }
        }

        return null;
    }


    protected boolean useInclude(HttpServletRequest request, HttpServletResponse response) {
        return (WebUtils.isIncludeRequest(request) || response.isCommitted());
    }














}
