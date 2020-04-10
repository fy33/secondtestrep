package org.springframework.web.servlet.handler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.cors.*;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport implements
        HandlerMapping, Ordered, BeanNameAware {
    @Nullable
    private Object defaultHandler;
    private UrlPathHelper urlPathHelper = new UrlPathHelper();
    private PathMatcher pathMatcher = new AntPathMatcher();
    private final List<Object> interceptors = new ArrayList<>();
    private final List<HandlerInterceptor> adaptedInterceptors = new ArrayList<>();

    @Nullable
    private CorsConfigurationSource corsConfigurationSource;
    private CorsProcessor corsProcessor = new DefaultCorsProcessor();
    private int order = Ordered.LOWEST_PRECEDENCE;


    @Nullable
    private String beanName;
    public void setDefaultHandler(@Nullable Object defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    @Nullable
    public Object getDefaultHandler() {
        return this.defaultHandler;
    }

    public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
        this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
        if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
            ((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setAlwaysUseFullPath(alwaysUseFullPath);
        }
    }
    public void setUrlDecode(boolean urlDecode) {
        this.urlPathHelper.setUrlDecode(urlDecode);
        if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
            ((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setUrlDecode(urlDecode);
        }
    }
    public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
        this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
        if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
            ((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setRemoveSemicolonContent(removeSemicolonContent);
        }
    }
    public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
        Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
        this.urlPathHelper = urlPathHelper;
        if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
            ((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setUrlPathHelper(urlPathHelper);
        }
    }
    public UrlPathHelper getUrlPathHelper() {
        return this.urlPathHelper;
    }


    public void setPathMatcher(PathMatcher pathMatcher) {
        Assert.notNull(pathMatcher, "PathMatcher must not be null");
        this.pathMatcher = pathMatcher;
        if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
            ((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setPathMatcher(pathMatcher);
        }
    }

    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }

    public void setInterceptors(Object... interceptors) {
        this.interceptors.addAll(Arrays.asList(interceptors));
    }
    public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
        Assert.notNull(corsConfigurations, "corsConfigurations must not be null");
        if (!corsConfigurations.isEmpty()) {
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.setCorsConfigurations(corsConfigurations);
            source.setPathMatcher(this.pathMatcher);
            source.setUrlPathHelper(this.urlPathHelper);
            source.setLookupPathAttributeName(LOOKUP_PATH);
            this.corsConfigurationSource = source;
        }
        else {
            this.corsConfigurationSource = null;
        }
    }
    public void setCorsConfigurationSource(CorsConfigurationSource corsConfigurationSource) {
        Assert.notNull(corsConfigurationSource, "corsConfigurationSource must not be null");
        this.corsConfigurationSource = corsConfigurationSource;
    }
    public void setCorsProcessor(CorsProcessor corsProcessor) {
        Assert.notNull(corsProcessor, "CorsProcessor must not be null");
        this.corsProcessor = corsProcessor;
    }

    public CorsProcessor getCorsProcessor() {
        return this.corsProcessor;
    }
    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }


    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }


    protected String formatMappingName() {
        return this.beanName != null ? "'" + this.beanName + "'" : "<unknown>";
    }

    @Override
    protected void initApplicationContext() throws BeansException {
        extendInterceptors(this.interceptors);
        detectMappedInterceptors(this.adaptedInterceptors);
        initInterceptors();
    }
    protected void extendInterceptors(List<Object> interceptors) {
    }
    protected void detectMappedInterceptors(List<HandlerInterceptor> mappedInterceptors) {
        mappedInterceptors.addAll(
                BeanFactoryUtils.beansOfTypeIncludingAncestors(
                        obtainApplicationContext(), MappedInterceptor.class, true, false).values());
    }

    protected void initInterceptors() {
        if (!this.interceptors.isEmpty()) {
            for (int i = 0; i < this.interceptors.size(); i++) {
                Object interceptor = this.interceptors.get(i);
                if (interceptor == null) {
                    throw new IllegalArgumentException("Entry number " + i + " in interceptors array is null");
                }
                this.adaptedInterceptors.add(adaptInterceptor(interceptor));
            }
        }
    }
    protected HandlerInterceptor adaptInterceptor(Object interceptor) {
        if (interceptor instanceof HandlerInterceptor) {
            return (HandlerInterceptor) interceptor;
        }
        else if (interceptor instanceof WebRequestInterceptor) {
            return new WebRequestHandlerInterceptorAdapter((WebRequestInterceptor) interceptor);
        }
        else {
            throw new IllegalArgumentException("Interceptor type not supported: " + interceptor.getClass().getName());
        }
    }
    @Nullable
    protected final HandlerInterceptor[] getAdaptedInterceptors() {
        return (!this.adaptedInterceptors.isEmpty() ?
                this.adaptedInterceptors.toArray(new HandlerInterceptor[0]) : null);
    }
    @Nullable
    protected final MappedInterceptor[] getMappedInterceptors() {
        List<MappedInterceptor> mappedInterceptors = new ArrayList<>(this.adaptedInterceptors.size());
        for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
            if (interceptor instanceof MappedInterceptor) {
                mappedInterceptors.add((MappedInterceptor) interceptor);
            }
        }
        return (!mappedInterceptors.isEmpty() ? mappedInterceptors.toArray(new MappedInterceptor[0]) : null);
    }
    public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        Object handler = getHandlerInternal(request);
        if (handler == null) {
            handler = getDefaultHandler();
        }
        if (handler == null) {
            return null;
        }
        // Bean name or resolved handler?
        if (handler instanceof String) {
            String handlerName = (String) handler;
            handler = obtainApplicationContext().getBean(handlerName);
        }

        HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);

        if (logger.isTraceEnabled()) {
            logger.trace("Mapped to " + handler);
        }
        else if (logger.isDebugEnabled() && !request.getDispatcherType().equals(DispatcherType.ASYNC)) {
            logger.debug("Mapped to " + executionChain.getHandler());
        }

        if (hasCorsConfigurationSource(handler)) {
            CorsConfiguration config = (this.corsConfigurationSource != null ? this.corsConfigurationSource.getCorsConfiguration(request) : null);
            CorsConfiguration handlerConfig = getCorsConfiguration(handler, request);
            config = (config != null ? config.combine(handlerConfig) : handlerConfig);
            executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
        }

        return executionChain;
    }

    @Nullable
    protected abstract Object getHandlerInternal(HttpServletRequest request) throws Exception;

    protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
        HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?
                (HandlerExecutionChain) handler : new HandlerExecutionChain(handler));

        String lookupPath = this.urlPathHelper.getLookupPathForRequest(request, LOOKUP_PATH);
        for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
            if (interceptor instanceof MappedInterceptor) {
                MappedInterceptor mappedInterceptor = (MappedInterceptor) interceptor;
                if (mappedInterceptor.matches(lookupPath, this.pathMatcher)) {
                    chain.addInterceptor(mappedInterceptor.getInterceptor());
                }
            }
            else {
                chain.addInterceptor(interceptor);
            }
        }
        return chain;
    }
    protected boolean hasCorsConfigurationSource(Object handler) {
        return (handler instanceof CorsConfigurationSource || this.corsConfigurationSource != null);
    }

    @Nullable
    protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
        Object resolvedHandler = handler;
        if (handler instanceof HandlerExecutionChain) {
            resolvedHandler = ((HandlerExecutionChain) handler).getHandler();
        }
        if (resolvedHandler instanceof CorsConfigurationSource) {
            return ((CorsConfigurationSource) resolvedHandler).getCorsConfiguration(request);
        }
        return null;
    }
    protected HandlerExecutionChain getCorsHandlerExecutionChain(HttpServletRequest request,
                                                                 HandlerExecutionChain chain, @Nullable CorsConfiguration config) {

        if (CorsUtils.isPreFlightRequest(request)) {
            HandlerInterceptor[] interceptors = chain.getInterceptors();
            chain = new HandlerExecutionChain(new PreFlightHandler(config), interceptors);
        }
        else {
            chain.addInterceptor(0, new CorsInterceptor(config));
        }
        return chain;
    }
    private class PreFlightHandler implements HttpRequestHandler, CorsConfigurationSource {

        @Nullable
        private final CorsConfiguration config;

        public PreFlightHandler(@Nullable CorsConfiguration config) {
            this.config = config;
        }

        @Override
        public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
            corsProcessor.processRequest(this.config, request, response);
        }

        @Override
        @Nullable
        public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
            return this.config;
        }
    }

    private class CorsInterceptor extends HandlerInterceptorAdapter implements CorsConfigurationSource {

        @Nullable
        private final CorsConfiguration config;

        public CorsInterceptor(@Nullable CorsConfiguration config) {
            this.config = config;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {

            return corsProcessor.processRequest(this.config, request, response);
        }

        @Override
        @Nullable
        public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
            return this.config;
        }
    }


}
