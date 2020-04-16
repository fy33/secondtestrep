package org.springframework.web.servlet.mvc.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class DefaultHandlerExceptionResolver extends AbstractHandlerExceptionResolver {
    public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";
    protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

    public DefaultHandlerExceptionResolver() {
        setOrder(Ordered.LOWEST_PRECEDENCE);
        setWarnLogCategory(getClass().getName());
    }

    @Override
    @Nullable
    protected ModelAndView doResolveException(
            HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

        try {
            if (ex instanceof HttpRequestMethodNotSupportedException) {
                return handleHttpRequestMethodNotSupported(
                        (HttpRequestMethodNotSupportedException) ex, request, response, handler);
            }
            else if (ex instanceof HttpMediaTypeNotSupportedException) {
                return handleHttpMediaTypeNotSupported(
                        (HttpMediaTypeNotSupportedException) ex, request, response, handler);
            }
            else if (ex instanceof HttpMediaTypeNotAcceptableException) {
                return handleHttpMediaTypeNotAcceptable(
                        (HttpMediaTypeNotAcceptableException) ex, request, response, handler);
            }
            else if (ex instanceof MissingPathVariableException) {
                return handleMissingPathVariable(
                        (MissingPathVariableException) ex, request, response, handler);
            }
            else if (ex instanceof MissingServletRequestParameterException) {
                return handleMissingServletRequestParameter(
                        (MissingServletRequestParameterException) ex, request, response, handler);
            }
            else if (ex instanceof ServletRequestBindingException) {
                return handleServletRequestBindingException(
                        (ServletRequestBindingException) ex, request, response, handler);
            }
            else if (ex instanceof ConversionNotSupportedException) {
                return handleConversionNotSupported(
                        (ConversionNotSupportedException) ex, request, response, handler);
            }
            else if (ex instanceof TypeMismatchException) {
                return handleTypeMismatch(
                        (TypeMismatchException) ex, request, response, handler);
            }
            else if (ex instanceof HttpMessageNotReadableException) {
                return handleHttpMessageNotReadable(
                        (HttpMessageNotReadableException) ex, request, response, handler);
            }
            else if (ex instanceof HttpMessageNotWritableException) {
                return handleHttpMessageNotWritable(
                        (HttpMessageNotWritableException) ex, request, response, handler);
            }
            else if (ex instanceof MethodArgumentNotValidException) {
                return handleMethodArgumentNotValidException(
                        (MethodArgumentNotValidException) ex, request, response, handler);
            }
            else if (ex instanceof MissingServletRequestPartException) {
                return handleMissingServletRequestPartException(
                        (MissingServletRequestPartException) ex, request, response, handler);
            }
            else if (ex instanceof BindException) {
                return handleBindException((BindException) ex, request, response, handler);
            }
            else if (ex instanceof NoHandlerFoundException) {
                return handleNoHandlerFoundException(
                        (NoHandlerFoundException) ex, request, response, handler);
            }
            else if (ex instanceof AsyncRequestTimeoutException) {
                return handleAsyncRequestTimeoutException(
                        (AsyncRequestTimeoutException) ex, request, response, handler);
            }
        }
        catch (Exception handlerEx) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failure while trying to resolve exception [" + ex.getClass().getName() + "]", handlerEx);
            }
        }
        return null;
    }

    protected ModelAndView handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                               HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

        String[] supportedMethods = ex.getSupportedMethods();
        if (supportedMethods != null) {
            response.setHeader("Allow", StringUtils.arrayToDelimitedString(supportedMethods, ", "));
        }
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, ex.getMessage());
        return new ModelAndView();
    }
    protected ModelAndView handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                           HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

        response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        List<MediaType> mediaTypes = ex.getSupportedMediaTypes();
        if (!CollectionUtils.isEmpty(mediaTypes)) {
            response.setHeader("Accept", MediaType.toString(mediaTypes));
        }
        return new ModelAndView();
    }

    protected ModelAndView handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                            HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

        response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
        return new ModelAndView();
    }

    protected ModelAndView handleMissingPathVariable(MissingPathVariableException ex,
                                                     HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        return new ModelAndView();
    }

    protected ModelAndView handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

        response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        return new ModelAndView();
    }
    protected ModelAndView handleServletRequestBindingException(ServletRequestBindingException ex,
                                                                HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

        response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        return new ModelAndView();
    }

    protected ModelAndView handleConversionNotSupported(ConversionNotSupportedException ex,
                                                        HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

        sendServerError(ex, request, response);
        return new ModelAndView();
    }

    protected ModelAndView handleTypeMismatch(TypeMismatchException ex,
                                              HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return new ModelAndView();
    }
    protected ModelAndView handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                        HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return new ModelAndView();
    }

    protected ModelAndView handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
                                                        HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

        sendServerError(ex, request, response);
        return new ModelAndView();
    }
    protected ModelAndView handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
                                                                 HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return new ModelAndView();
    }

    protected ModelAndView handleMissingServletRequestPartException(MissingServletRequestPartException ex,
                                                                    HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

        response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        return new ModelAndView();
    }

    protected ModelAndView handleBindException(BindException ex, HttpServletRequest request,
                                               HttpServletResponse response, @Nullable Object handler) throws IOException {

        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return new ModelAndView();
    }
    protected ModelAndView handleNoHandlerFoundException(NoHandlerFoundException ex,
                                                         HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

        pageNotFoundLogger.warn(ex.getMessage());
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return new ModelAndView();
    }
    protected ModelAndView handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
                                                              HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

        if (!response.isCommitted()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
        else {
            logger.warn("Async request timed out");
        }
        return new ModelAndView();
    }

    protected void sendServerError(Exception ex, HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setAttribute("javax.servlet.error.exception", ex);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }


}
