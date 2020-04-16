package org.springframework.web.servlet.handler;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class SimpleMappingExceptionResolver extends AbstractHandlerExceptionResolver {
    public static final String DEFAULT_EXCEPTION_ATTRIBUTE = "exception";
    @Nullable
    private Properties exceptionMappings;
    @Nullable
    private Class<?>[] excludedExceptions;

    @Nullable
    private String defaultErrorView;

    @Nullable
    private Integer defaultStatusCode;

    private Map<String, Integer> statusCodes = new HashMap<>();

    @Nullable
    private String exceptionAttribute = DEFAULT_EXCEPTION_ATTRIBUTE;
    public void setExceptionMappings(Properties mappings) {
        this.exceptionMappings = mappings;
    }

    public void setExcludedExceptions(Class<?>... excludedExceptions) {
        this.excludedExceptions = excludedExceptions;
    }

    public void setDefaultErrorView(String defaultErrorView) {
        this.defaultErrorView = defaultErrorView;
    }

    public void setStatusCodes(Properties statusCodes) {
        for (Enumeration<?> enumeration = statusCodes.propertyNames(); enumeration.hasMoreElements();) {
            String viewName = (String) enumeration.nextElement();
            Integer statusCode = Integer.valueOf(statusCodes.getProperty(viewName));
            this.statusCodes.put(viewName, statusCode);
        }
    }

    public void addStatusCode(String viewName, int statusCode) {
        this.statusCodes.put(viewName, statusCode);
    }

    public Map<String, Integer> getStatusCodesAsMap() {
        return Collections.unmodifiableMap(this.statusCodes);
    }
    public void setDefaultStatusCode(int defaultStatusCode) {
        this.defaultStatusCode = defaultStatusCode;
    }

    public void setExceptionAttribute(@Nullable String exceptionAttribute) {
        this.exceptionAttribute = exceptionAttribute;
    }

    @Override
    @Nullable
    protected ModelAndView doResolveException(
            HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

        // Expose ModelAndView for chosen error view.
        String viewName = determineViewName(ex, request);
        if (viewName != null) {
            // Apply HTTP status code for error views, if specified.
            // Only apply it if we're processing a top-level request.
            Integer statusCode = determineStatusCode(request, viewName);
            if (statusCode != null) {
                applyStatusCodeIfPossible(request, response, statusCode);
            }
            return getModelAndView(viewName, ex, request);
        }
        else {
            return null;
        }
    }

    @Nullable
    protected String determineViewName(Exception ex, HttpServletRequest request) {
        String viewName = null;
        if (this.excludedExceptions != null) {
            for (Class<?> excludedEx : this.excludedExceptions) {
                if (excludedEx.equals(ex.getClass())) {
                    return null;
                }
            }
        }
        // Check for specific exception mappings.
        if (this.exceptionMappings != null) {
            viewName = findMatchingViewName(this.exceptionMappings, ex);
        }
        // Return default error view else, if defined.
        if (viewName == null && this.defaultErrorView != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Resolving to default view '" + this.defaultErrorView + "'");
            }
            viewName = this.defaultErrorView;
        }
        return viewName;
    }
    @Nullable
    protected String findMatchingViewName(Properties exceptionMappings, Exception ex) {
        String viewName = null;
        String dominantMapping = null;
        int deepest = Integer.MAX_VALUE;
        for (Enumeration<?> names = exceptionMappings.propertyNames(); names.hasMoreElements();) {
            String exceptionMapping = (String) names.nextElement();
            int depth = getDepth(exceptionMapping, ex);
            if (depth >= 0 && (depth < deepest || (depth == deepest &&
                    dominantMapping != null && exceptionMapping.length() > dominantMapping.length()))) {
                deepest = depth;
                dominantMapping = exceptionMapping;
                viewName = exceptionMappings.getProperty(exceptionMapping);
            }
        }
        if (viewName != null && logger.isDebugEnabled()) {
            logger.debug("Resolving to view '" + viewName + "' based on mapping [" + dominantMapping + "]");
        }
        return viewName;
    }

    protected int getDepth(String exceptionMapping, Exception ex) {
        return getDepth(exceptionMapping, ex.getClass(), 0);
    }
    private int getDepth(String exceptionMapping, Class<?> exceptionClass, int depth) {
        if (exceptionClass.getName().contains(exceptionMapping)) {
            // Found it!
            return depth;
        }
        // If we've gone as far as we can go and haven't found it...
        if (exceptionClass == Throwable.class) {
            return -1;
        }
        return getDepth(exceptionMapping, exceptionClass.getSuperclass(), depth + 1);
    }


    @Nullable
    protected Integer determineStatusCode(HttpServletRequest request, String viewName) {
        if (this.statusCodes.containsKey(viewName)) {
            return this.statusCodes.get(viewName);
        }
        return this.defaultStatusCode;
    }
    protected void applyStatusCodeIfPossible(HttpServletRequest request, HttpServletResponse response, int statusCode) {
        if (!WebUtils.isIncludeRequest(request)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Applying HTTP status " + statusCode);
            }
            response.setStatus(statusCode);
            request.setAttribute(WebUtils.ERROR_STATUS_CODE_ATTRIBUTE, statusCode);
        }
    }

    protected ModelAndView getModelAndView(String viewName, Exception ex, HttpServletRequest request) {
        return getModelAndView(viewName, ex);
    }


    protected ModelAndView getModelAndView(String viewName, Exception ex) {
        ModelAndView mv = new ModelAndView(viewName);
        if (this.exceptionAttribute != null) {
            mv.addObject(this.exceptionAttribute, ex);
        }
        return mv;
    }



}
