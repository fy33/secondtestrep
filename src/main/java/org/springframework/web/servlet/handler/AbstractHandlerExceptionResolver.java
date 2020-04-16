package org.springframework.web.servlet.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

public abstract class AbstractHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";

    protected final Log logger = LogFactory.getLog(getClass());

    private int order = Ordered.LOWEST_PRECEDENCE;

    @Nullable
    private Set<?> mappedHandlers;

    @Nullable
    private Class<?>[] mappedHandlerClasses;

    @Nullable
    private Log warnLogger;

    private boolean preventResponseCaching = false;

    public void setOrder(int order) {
        this.order = order;
    }


    @Override
    public int getOrder() {
        return this.order;
    }

    public void setMappedHandlers(Set<?> mappedHandlers) {
        this.mappedHandlers = mappedHandlers;
    }

    public void setMappedHandlerClasses(Class<?>... mappedHandlerClasses) {
        this.mappedHandlerClasses = mappedHandlerClasses;
    }
    public void setWarnLogCategory(String loggerName) {
        this.warnLogger = (StringUtils.hasLength(loggerName) ? LogFactory.getLog(loggerName) : null);
    }
    public void setPreventResponseCaching(boolean preventResponseCaching) {
        this.preventResponseCaching = preventResponseCaching;
    }

    @Override
    @Nullable
    public ModelAndView resolveException(
            HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

        if (shouldApplyTo(request, handler)) {
            prepareResponse(ex, response);
            ModelAndView result = doResolveException(request, response, handler, ex);
            if (result != null) {
                // Print debug message when warn logger is not enabled.
                if (logger.isDebugEnabled() && (this.warnLogger == null || !this.warnLogger.isWarnEnabled())) {
                    logger.debug("Resolved [" + ex + "]" + (result.isEmpty() ? "" : " to " + result));
                }
                // Explicitly configured warn logger in logException method.
                logException(ex, request);
            }
            return result;
        }
        else {
            return null;
        }
    }

    protected boolean shouldApplyTo(HttpServletRequest request, @Nullable Object handler) {
        if (handler != null) {
            if (this.mappedHandlers != null && this.mappedHandlers.contains(handler)) {
                return true;
            }
            if (this.mappedHandlerClasses != null) {
                for (Class<?> handlerClass : this.mappedHandlerClasses) {
                    if (handlerClass.isInstance(handler)) {
                        return true;
                    }
                }
            }
        }
        // Else only apply if there are no explicit handler mappings.
        return (this.mappedHandlers == null && this.mappedHandlerClasses == null);
    }

    protected void logException(Exception ex, HttpServletRequest request) {
        if (this.warnLogger != null && this.warnLogger.isWarnEnabled()) {
            this.warnLogger.warn(buildLogMessage(ex, request));
        }
    }
    protected String buildLogMessage(Exception ex, HttpServletRequest request) {
        return "Resolved [" + ex + "]";
    }

    protected void prepareResponse(Exception ex, HttpServletResponse response) {
        if (this.preventResponseCaching) {
            preventCaching(response);
        }
    }
    protected void preventCaching(HttpServletResponse response) {
        response.addHeader(HEADER_CACHE_CONTROL, "no-store");
    }

    @Nullable
    protected abstract ModelAndView doResolveException(
            HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex);




}
