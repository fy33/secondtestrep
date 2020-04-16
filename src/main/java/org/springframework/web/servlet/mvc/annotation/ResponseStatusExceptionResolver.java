package org.springframework.web.servlet.mvc.annotation;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ResponseStatusExceptionResolver extends AbstractHandlerExceptionResolver implements MessageSourceAware {
    @Nullable
    private MessageSource messageSource;

    @Override
    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    @Nullable
    protected ModelAndView doResolveException(
            HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

        try {
            if (ex instanceof ResponseStatusException) {
                return resolveResponseStatusException((ResponseStatusException) ex, request, response, handler);
            }

            ResponseStatus status = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
            if (status != null) {
                return resolveResponseStatus(status, request, response, handler, ex);
            }

            if (ex.getCause() instanceof Exception) {
                return doResolveException(request, response, handler, (Exception) ex.getCause());
            }
        }
        catch (Exception resolveEx) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failure while trying to resolve exception [" + ex.getClass().getName() + "]", resolveEx);
            }
        }
        return null;
    }

    protected ModelAndView resolveResponseStatus(ResponseStatus responseStatus, HttpServletRequest request,
                                                 HttpServletResponse response, @Nullable Object handler, Exception ex) throws Exception {

        int statusCode = responseStatus.code().value();
        String reason = responseStatus.reason();
        return applyStatusAndReason(statusCode, reason, response);
    }


    protected ModelAndView resolveResponseStatusException(ResponseStatusException ex,
                                                          HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws Exception {

        int statusCode = ex.getStatus().value();
        String reason = ex.getReason();
        return applyStatusAndReason(statusCode, reason, response);
    }



    protected ModelAndView applyStatusAndReason(int statusCode, @Nullable String reason, HttpServletResponse response)
            throws IOException {

        if (!StringUtils.hasLength(reason)) {
            response.sendError(statusCode);
        }
        else {
            String resolvedReason = (this.messageSource != null ?
                    this.messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale()) :
                    reason);
            response.sendError(statusCode, resolvedReason);
        }
        return new ModelAndView();
    }











}
