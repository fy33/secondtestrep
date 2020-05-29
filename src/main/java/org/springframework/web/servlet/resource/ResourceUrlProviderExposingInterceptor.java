package org.springframework.web.servlet.resource;

import org.springframework.util.Assert;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResourceUrlProviderExposingInterceptor extends HandlerInterceptorAdapter {
    public static final String RESOURCE_URL_PROVIDER_ATTR = ResourceUrlProvider.class.getName();

    private final ResourceUrlProvider resourceUrlProvider;


    public ResourceUrlProviderExposingInterceptor(ResourceUrlProvider resourceUrlProvider) {
        Assert.notNull(resourceUrlProvider, "ResourceUrlProvider is required");
        this.resourceUrlProvider = resourceUrlProvider;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        try {
            request.setAttribute(RESOURCE_URL_PROVIDER_ATTR, this.resourceUrlProvider);
        }
        catch (ResourceUrlEncodingFilter.LookupPathIndexException ex) {
            throw new ServletRequestBindingException(ex.getMessage(), ex);
        }
        return true;
    }




}
