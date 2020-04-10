package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.servlet.handler.RequestMatchResult;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Predicate;

public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping
implements MatchableHandlerMapping, EmbeddedValueResolverAware {

    private boolean useSuffixPatternMatch = true;

    private boolean useRegisteredSuffixPatternMatch = false;

    private boolean useTrailingSlashMatch = true;
    private Map<String, Predicate<Class<?>>> pathPrefixes = new LinkedHashMap<>();

    private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();


    @Nullable
    private StringValueResolver embeddedValueResolver;
    private RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();
    public void setUseSuffixPatternMatch(boolean useSuffixPatternMatch) {
        this.useSuffixPatternMatch = useSuffixPatternMatch;
    }
    public void setUseRegisteredSuffixPatternMatch(boolean useRegisteredSuffixPatternMatch) {
        this.useRegisteredSuffixPatternMatch = useRegisteredSuffixPatternMatch;
        this.useSuffixPatternMatch = (useRegisteredSuffixPatternMatch || this.useSuffixPatternMatch);
    }

    public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
        this.useTrailingSlashMatch = useTrailingSlashMatch;
    }
    public void setPathPrefixes(Map<String, Predicate<Class<?>>> prefixes) {
        this.pathPrefixes = Collections.unmodifiableMap(new LinkedHashMap<>(prefixes));
    }
    public Map<String, Predicate<Class<?>>> getPathPrefixes() {
        return this.pathPrefixes;
    }
    public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
        Assert.notNull(contentNegotiationManager, "ContentNegotiationManager must not be null");
        this.contentNegotiationManager = contentNegotiationManager;
    }
    public ContentNegotiationManager getContentNegotiationManager() {
        return this.contentNegotiationManager;
    }
    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    @Override
    public void afterPropertiesSet() {
        this.config = new RequestMappingInfo.BuilderConfiguration();
        this.config.setUrlPathHelper(getUrlPathHelper());
        this.config.setPathMatcher(getPathMatcher());
        this.config.setSuffixPatternMatch(this.useSuffixPatternMatch);
        this.config.setTrailingSlashMatch(this.useTrailingSlashMatch);
        this.config.setRegisteredSuffixPatternMatch(this.useRegisteredSuffixPatternMatch);
        this.config.setContentNegotiationManager(getContentNegotiationManager());

        super.afterPropertiesSet();
    }


    public boolean useSuffixPatternMatch() {
        return this.useSuffixPatternMatch;
    }

    public boolean useRegisteredSuffixPatternMatch() {
        return this.useRegisteredSuffixPatternMatch;
    }
    public boolean useTrailingSlashMatch() {
        return this.useTrailingSlashMatch;
    }

    @Nullable
    public List<String> getFileExtensions() {
        return this.config.getFileExtensions();
    }
    @Override
    protected boolean isHandler(Class<?> beanType) {
        return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
                AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
    }
    @Override
    @Nullable
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo info = createRequestMappingInfo(method);
        if (info != null) {
            RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
            if (typeInfo != null) {
                info = typeInfo.combine(info);
            }
            String prefix = getPathPrefix(handlerType);
            if (prefix != null) {
                info = RequestMappingInfo.paths(prefix).build().combine(info);
            }
        }
        return info;
    }

    @Nullable
    String getPathPrefix(Class<?> handlerType) {
        for (Map.Entry<String, Predicate<Class<?>>> entry : this.pathPrefixes.entrySet()) {
            if (entry.getValue().test(handlerType)) {
                String prefix = entry.getKey();
                if (this.embeddedValueResolver != null) {
                    prefix = this.embeddedValueResolver.resolveStringValue(prefix);
                }
                return prefix;
            }
        }
        return null;
    }

    @Nullable
    private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
        RequestCondition<?> condition = (element instanceof Class ?
                getCustomTypeCondition((Class<?>) element) : getCustomMethodCondition((Method) element));
        return (requestMapping != null ? createRequestMappingInfo(requestMapping, condition) : null);
    }

    @Nullable
    protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
        return null;
    }


    @Nullable
    protected RequestCondition<?> getCustomMethodCondition(Method method) {
        return null;
    }

    protected RequestMappingInfo createRequestMappingInfo(
            RequestMapping requestMapping, @Nullable RequestCondition<?> customCondition) {

        RequestMappingInfo.Builder builder = RequestMappingInfo
                .paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))
                .methods(requestMapping.method())
                .params(requestMapping.params())
                .headers(requestMapping.headers())
                .consumes(requestMapping.consumes())
                .produces(requestMapping.produces())
                .mappingName(requestMapping.name());
        if (customCondition != null) {
            builder.customCondition(customCondition);
        }
        return builder.options(this.config).build();
    }
    protected String[] resolveEmbeddedValuesInPatterns(String[] patterns) {
        if (this.embeddedValueResolver == null) {
            return patterns;
        }
        else {
            String[] resolvedPatterns = new String[patterns.length];
            for (int i = 0; i < patterns.length; i++) {
                resolvedPatterns[i] = this.embeddedValueResolver.resolveStringValue(patterns[i]);
            }
            return resolvedPatterns;
        }
    }

    @Override
    public void registerMapping(RequestMappingInfo mapping, Object handler, Method method) {
        super.registerMapping(mapping, handler, method);
        updateConsumesCondition(mapping, method);
    }


    @Override
    protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
        super.registerHandlerMethod(handler, method, mapping);
        updateConsumesCondition(mapping, method);
    }

    private void updateConsumesCondition(RequestMappingInfo info, Method method) {
        ConsumesRequestCondition condition = info.getConsumesCondition();
        if (!condition.isEmpty()) {
            for (Parameter parameter : method.getParameters()) {
                MergedAnnotation<RequestBody> annot = MergedAnnotations.from(parameter).get(RequestBody.class);
                if (annot.isPresent()) {
                    condition.setBodyRequired(annot.getBoolean("required"));
                    break;
                }
            }
        }
    }

    @Override
    public RequestMatchResult match(HttpServletRequest request, String pattern) {
        RequestMappingInfo info = RequestMappingInfo.paths(pattern).options(this.config).build();
        RequestMappingInfo matchingInfo = info.getMatchingCondition(request);
        if (matchingInfo == null) {
            return null;
        }
        Set<String> patterns = matchingInfo.getPatternsCondition().getPatterns();
        String lookupPath = getUrlPathHelper().getLookupPathForRequest(request, LOOKUP_PATH);
        return new RequestMatchResult(patterns.iterator().next(), lookupPath, getPathMatcher());
    }


















}
