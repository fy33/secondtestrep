package org.springframework.web.servlet.handler;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class MappedInterceptor implements HandlerInterceptor {

    @Nullable
    private final String[] includePatterns;

    @Nullable
    private final String[] excludePatterns;

    private final HandlerInterceptor interceptor;

    @Nullable
    private PathMatcher pathMatcher;

    public MappedInterceptor(@Nullable String[] includePatterns, HandlerInterceptor interceptor) {
        this(includePatterns, null, interceptor);
    }


    public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
                             HandlerInterceptor interceptor) {

        this.includePatterns = includePatterns;
        this.excludePatterns = excludePatterns;
        this.interceptor = interceptor;
    }

    public MappedInterceptor(@Nullable String[] includePatterns, WebRequestInterceptor interceptor) {
        this(includePatterns, null, interceptor);
    }


    public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
                             WebRequestInterceptor interceptor) {

        this(includePatterns, excludePatterns, new WebRequestHandlerInterceptorAdapter(interceptor));
    }

    public void setPathMatcher(@Nullable PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
    }


    @Nullable
    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }

    @Nullable
    public String[] getPathPatterns() {
        return this.includePatterns;
    }


    public HandlerInterceptor getInterceptor() {
        return this.interceptor;
    }


    public boolean matches(String lookupPath, PathMatcher pathMatcher) {
        PathMatcher pathMatcherToUse = (this.pathMatcher != null ? this.pathMatcher : pathMatcher);
        if (!ObjectUtils.isEmpty(this.excludePatterns)) {
            for (String pattern : this.excludePatterns) {
                if (pathMatcherToUse.match(pattern, lookupPath)) {
                    return false;
                }
            }
        }
        if (ObjectUtils.isEmpty(this.includePatterns)) {
            return true;
        }
        for (String pattern : this.includePatterns) {
            if (pathMatcherToUse.match(pattern, lookupPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        return this.interceptor.preHandle(request, response, handler);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           @Nullable ModelAndView modelAndView) throws Exception {

        this.interceptor.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                @Nullable Exception ex) throws Exception {

        this.interceptor.afterCompletion(request, response, handler, ex);
    }













}
