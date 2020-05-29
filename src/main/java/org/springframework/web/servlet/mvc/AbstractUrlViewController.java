package org.springframework.web.servlet.mvc;

import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractUrlViewController extends AbstractController {
    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
        this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
    }

    public void setUrlDecode(boolean urlDecode) {
        this.urlPathHelper.setUrlDecode(urlDecode);
    }


    public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
        this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
    }


    public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
        Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
        this.urlPathHelper = urlPathHelper;
    }


    protected UrlPathHelper getUrlPathHelper() {
        return this.urlPathHelper;
    }


    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {
        String viewName = getViewNameForRequest(request);
        if (logger.isTraceEnabled()) {
            logger.trace("Returning view name '" + viewName + "'");
        }
        return new ModelAndView(viewName, RequestContextUtils.getInputFlashMap(request));
    }

    protected abstract String getViewNameForRequest(HttpServletRequest request);







}
