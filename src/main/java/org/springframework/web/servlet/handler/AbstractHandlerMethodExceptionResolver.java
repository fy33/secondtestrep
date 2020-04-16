package org.springframework.web.servlet.handler;

import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractHandlerMethodExceptionResolver extends AbstractHandlerExceptionResolver  {

    @Override
    protected boolean shouldApplyTo(HttpServletRequest request, @Nullable Object handler) {
        if (handler == null) {
            return super.shouldApplyTo(request, null);
        }
        else if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            handler = handlerMethod.getBean();
            return super.shouldApplyTo(request, handler);
        }
        else {
            return false;
        }
    }

    @Override
    @Nullable
    protected final ModelAndView doResolveException(
            HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

        return doResolveHandlerMethodException(request, response, (HandlerMethod) handler, ex);
    }

    @Nullable
    protected abstract ModelAndView doResolveHandlerMethodException(
            HttpServletRequest request, HttpServletResponse response, @Nullable HandlerMethod handlerMethod, Exception ex);


















}





