package org.springframework.web.servlet.handler;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerExecutionChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public abstract class AbstractUrlHandlerMapping extends AbstractHandlerMapping implements MatchableHandlerMapping  {
    @Nullable
    private Object rootHandler;


    private boolean useTrailingSlashMatch = false;

    private boolean lazyInitHandlers = false;

    private final Map<String, Object> handlerMap = new LinkedHashMap<>();

    public void setRootHandler(@Nullable Object rootHandler) {
        this.rootHandler = rootHandler;
    }


    @Nullable
    public Object getRootHandler() {
        return this.rootHandler;
    }

    public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
        this.useTrailingSlashMatch = useTrailingSlashMatch;
    }
    public boolean useTrailingSlashMatch() {
        return this.useTrailingSlashMatch;
    }

    public void setLazyInitHandlers(boolean lazyInitHandlers) {
        this.lazyInitHandlers = lazyInitHandlers;
    }

    @Override
    @Nullable
    protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
        String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
        request.setAttribute(LOOKUP_PATH, lookupPath);
        Object handler = lookupHandler(lookupPath, request);
        if (handler == null) {
            // We need to care for the default handler directly, since we need to
            // expose the PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE for it as well.
            Object rawHandler = null;
            if ("/".equals(lookupPath)) {
                rawHandler = getRootHandler();
            }
            if (rawHandler == null) {
                rawHandler = getDefaultHandler();
            }
            if (rawHandler != null) {
                // Bean name or resolved handler?
                if (rawHandler instanceof String) {
                    String handlerName = (String) rawHandler;
                    rawHandler = obtainApplicationContext().getBean(handlerName);
                }
                validateHandler(rawHandler, request);
                handler = buildPathExposingHandler(rawHandler, lookupPath, lookupPath, null);
            }
        }
        return handler;
    }

    @Nullable
    protected Object lookupHandler(String urlPath, HttpServletRequest request) throws Exception {
        // Direct match?
        Object handler = this.handlerMap.get(urlPath);
        if (handler != null) {
            // Bean name or resolved handler?
            if (handler instanceof String) {
                String handlerName = (String) handler;
                handler = obtainApplicationContext().getBean(handlerName);
            }
            validateHandler(handler, request);
            return buildPathExposingHandler(handler, urlPath, urlPath, null);
        }

        // Pattern match?
        List<String> matchingPatterns = new ArrayList<>();
        for (String registeredPattern : this.handlerMap.keySet()) {
            if (getPathMatcher().match(registeredPattern, urlPath)) {
                matchingPatterns.add(registeredPattern);
            }
            else if (useTrailingSlashMatch()) {
                if (!registeredPattern.endsWith("/") && getPathMatcher().match(registeredPattern + "/", urlPath)) {
                    matchingPatterns.add(registeredPattern + "/");
                }
            }
        }

        String bestMatch = null;
        Comparator<String> patternComparator = getPathMatcher().getPatternComparator(urlPath);
        if (!matchingPatterns.isEmpty()) {
            matchingPatterns.sort(patternComparator);
            if (logger.isTraceEnabled() && matchingPatterns.size() > 1) {
                logger.trace("Matching patterns " + matchingPatterns);
            }
            bestMatch = matchingPatterns.get(0);
        }
        if (bestMatch != null) {
            handler = this.handlerMap.get(bestMatch);
            if (handler == null) {
                if (bestMatch.endsWith("/")) {
                    handler = this.handlerMap.get(bestMatch.substring(0, bestMatch.length() - 1));
                }
                if (handler == null) {
                    throw new IllegalStateException(
                            "Could not find handler for best pattern match [" + bestMatch + "]");
                }
            }
            // Bean name or resolved handler?
            if (handler instanceof String) {
                String handlerName = (String) handler;
                handler = obtainApplicationContext().getBean(handlerName);
            }
            validateHandler(handler, request);
            String pathWithinMapping = getPathMatcher().extractPathWithinPattern(bestMatch, urlPath);

            // There might be multiple 'best patterns', let's make sure we have the correct URI template variables
            // for all of them
            Map<String, String> uriTemplateVariables = new LinkedHashMap<>();
            for (String matchingPattern : matchingPatterns) {
                if (patternComparator.compare(bestMatch, matchingPattern) == 0) {
                    Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(matchingPattern, urlPath);
                    Map<String, String> decodedVars = getUrlPathHelper().decodePathVariables(request, vars);
                    uriTemplateVariables.putAll(decodedVars);
                }
            }
            if (logger.isTraceEnabled() && uriTemplateVariables.size() > 0) {
                logger.trace("URI variables " + uriTemplateVariables);
            }
            return buildPathExposingHandler(handler, bestMatch, pathWithinMapping, uriTemplateVariables);
        }

        // No handler found...
        return null;
    }
    protected void validateHandler(Object handler, HttpServletRequest request) throws Exception {
    }
    protected Object buildPathExposingHandler(Object rawHandler, String bestMatchingPattern,
                                              String pathWithinMapping, @Nullable Map<String, String> uriTemplateVariables) {

        HandlerExecutionChain chain = new HandlerExecutionChain(rawHandler);
        chain.addInterceptor(new PathExposingHandlerInterceptor(bestMatchingPattern, pathWithinMapping));
        if (!CollectionUtils.isEmpty(uriTemplateVariables)) {
            chain.addInterceptor(new UriTemplateVariablesHandlerInterceptor(uriTemplateVariables));
        }
        return chain;
    }

    protected void exposePathWithinMapping(String bestMatchingPattern, String pathWithinMapping,
                                           HttpServletRequest request) {

        request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestMatchingPattern);
        request.setAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathWithinMapping);
    }
    protected void exposeUriTemplateVariables(Map<String, String> uriTemplateVariables, HttpServletRequest request) {
        request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);
    }
    @Override
    @Nullable
    public RequestMatchResult match(HttpServletRequest request, String pattern) {
        String lookupPath = getUrlPathHelper().getLookupPathForRequest(request, LOOKUP_PATH);
        if (getPathMatcher().match(pattern, lookupPath)) {
            return new RequestMatchResult(pattern, lookupPath, getPathMatcher());
        }
        else if (useTrailingSlashMatch()) {
            if (!pattern.endsWith("/") && getPathMatcher().match(pattern + "/", lookupPath)) {
                return new RequestMatchResult(pattern + "/", lookupPath, getPathMatcher());
            }
        }
        return null;
    }
    protected void registerHandler(String[] urlPaths, String beanName) throws BeansException, IllegalStateException {
        Assert.notNull(urlPaths, "URL path array must not be null");
        for (String urlPath : urlPaths) {
            registerHandler(urlPath, beanName);
        }
    }
    protected void registerHandler(String urlPath, Object handler) throws BeansException, IllegalStateException {
        Assert.notNull(urlPath, "URL path must not be null");
        Assert.notNull(handler, "Handler object must not be null");
        Object resolvedHandler = handler;

        // Eagerly resolve handler if referencing singleton via name.
        if (!this.lazyInitHandlers && handler instanceof String) {
            String handlerName = (String) handler;
            ApplicationContext applicationContext = obtainApplicationContext();
            if (applicationContext.isSingleton(handlerName)) {
                resolvedHandler = applicationContext.getBean(handlerName);
            }
        }

        Object mappedHandler = this.handlerMap.get(urlPath);
        if (mappedHandler != null) {
            if (mappedHandler != resolvedHandler) {
                throw new IllegalStateException(
                        "Cannot map " + getHandlerDescription(handler) + " to URL path [" + urlPath +
                                "]: There is already " + getHandlerDescription(mappedHandler) + " mapped.");
            }
        }
        else {
            if (urlPath.equals("/")) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Root mapping to " + getHandlerDescription(handler));
                }
                setRootHandler(resolvedHandler);
            }
            else if (urlPath.equals("/*")) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Default mapping to " + getHandlerDescription(handler));
                }
                setDefaultHandler(resolvedHandler);
            }
            else {
                this.handlerMap.put(urlPath, resolvedHandler);
                if (logger.isTraceEnabled()) {
                    logger.trace("Mapped [" + urlPath + "] onto " + getHandlerDescription(handler));
                }
            }
        }
    }
    private String getHandlerDescription(Object handler) {
        return (handler instanceof String ? "'" + handler + "'" : handler.toString());
    }
    public final Map<String, Object> getHandlerMap() {
        return Collections.unmodifiableMap(this.handlerMap);
    }
    protected boolean supportsTypeLevelMappings() {
        return false;
    }
    private class PathExposingHandlerInterceptor extends HandlerInterceptorAdapter {

        private final String bestMatchingPattern;

        private final String pathWithinMapping;

        public PathExposingHandlerInterceptor(String bestMatchingPattern, String pathWithinMapping) {
            this.bestMatchingPattern = bestMatchingPattern;
            this.pathWithinMapping = pathWithinMapping;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            exposePathWithinMapping(this.bestMatchingPattern, this.pathWithinMapping, request);
            request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, handler);
            request.setAttribute(INTROSPECT_TYPE_LEVEL_MAPPING, supportsTypeLevelMappings());
            return true;
        }

    }
    private class UriTemplateVariablesHandlerInterceptor extends HandlerInterceptorAdapter {

        private final Map<String, String> uriTemplateVariables;

        public UriTemplateVariablesHandlerInterceptor(Map<String, String> uriTemplateVariables) {
            this.uriTemplateVariables = uriTemplateVariables;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            exposeUriTemplateVariables(this.uriTemplateVariables, request);
            return true;
        }
    }
}
