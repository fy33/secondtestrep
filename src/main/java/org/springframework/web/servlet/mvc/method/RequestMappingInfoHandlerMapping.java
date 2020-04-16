package org.springframework.web.servlet.mvc.method;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.condition.NameValueExpression;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;

public abstract class RequestMappingInfoHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {
    private static final Method HTTP_OPTIONS_HANDLE_METHOD;
    static {
        try {
            HTTP_OPTIONS_HANDLE_METHOD = HttpOptionsHandler.class.getMethod("handle");
        }
        catch (NoSuchMethodException ex) {
            // Should never happen
            throw new IllegalStateException("Failed to retrieve internal handler method for HTTP OPTIONS", ex);
        }
    }

    protected RequestMappingInfoHandlerMapping() {
        setHandlerMethodMappingNamingStrategy(new RequestMappingInfoHandlerMethodMappingNamingStrategy());
    }

    @Override
    protected Set<String> getMappingPathPatterns(RequestMappingInfo info) {
        return info.getPatternsCondition().getPatterns();
    }
    @Override
    protected RequestMappingInfo getMatchingMapping(RequestMappingInfo info, HttpServletRequest request) {
        return info.getMatchingCondition(request);
    }
    //todo 不太懂->
    @Override
    protected Comparator<RequestMappingInfo> getMappingComparator(final HttpServletRequest request) {
        return (info1, info2) -> info1.compareTo(info2, request);
    }
    @Override
    protected void handleMatch(RequestMappingInfo info, String lookupPath, HttpServletRequest request) {
        super.handleMatch(info, lookupPath, request);

        String bestPattern;
        Map<String, String> uriVariables;

        Set<String> patterns = info.getPatternsCondition().getPatterns();
        if (patterns.isEmpty()) {
            bestPattern = lookupPath;
            uriVariables = Collections.emptyMap();
        }
        else {
            bestPattern = patterns.iterator().next();
            uriVariables = getPathMatcher().extractUriTemplateVariables(bestPattern, lookupPath);
        }

        request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern);

        if (isMatrixVariableContentAvailable()) {
            Map<String, MultiValueMap<String, String>> matrixVars = extractMatrixVariables(request, uriVariables);
            request.setAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, matrixVars);
        }

        Map<String, String> decodedUriVariables = getUrlPathHelper().decodePathVariables(request, uriVariables);
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, decodedUriVariables);

        if (!info.getProducesCondition().getProducibleMediaTypes().isEmpty()) {
            Set<MediaType> mediaTypes = info.getProducesCondition().getProducibleMediaTypes();
            request.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
        }
    }
    private boolean isMatrixVariableContentAvailable() {
        return !getUrlPathHelper().shouldRemoveSemicolonContent();
    }
    private Map<String, MultiValueMap<String, String>> extractMatrixVariables(
            HttpServletRequest request, Map<String, String> uriVariables) {

        Map<String, MultiValueMap<String, String>> result = new LinkedHashMap<>();
        uriVariables.forEach((uriVarKey, uriVarValue) -> {

            int equalsIndex = uriVarValue.indexOf('=');
            if (equalsIndex == -1) {
                return;
            }

            int semicolonIndex = uriVarValue.indexOf(';');
            if (semicolonIndex != -1 && semicolonIndex != 0) {
                uriVariables.put(uriVarKey, uriVarValue.substring(0, semicolonIndex));
            }

            String matrixVariables;
            if (semicolonIndex == -1 || semicolonIndex == 0 || equalsIndex < semicolonIndex) {
                matrixVariables = uriVarValue;
            }
            else {
                matrixVariables = uriVarValue.substring(semicolonIndex + 1);
            }

            MultiValueMap<String, String> vars = WebUtils.parseMatrixVariables(matrixVariables);
            result.put(uriVarKey, getUrlPathHelper().decodeMatrixVariables(request, vars));
        });
        return result;
    }
    @Override
    protected HandlerMethod handleNoMatch(
            Set<RequestMappingInfo> infos, String lookupPath, HttpServletRequest request) throws ServletException {

        PartialMatchHelper helper = new PartialMatchHelper(infos, request);
        if (helper.isEmpty()) {
            return null;
        }

        if (helper.hasMethodsMismatch()) {
            Set<String> methods = helper.getAllowedMethods();
            if (HttpMethod.OPTIONS.matches(request.getMethod())) {
                HttpOptionsHandler handler = new HttpOptionsHandler(methods);
                return new HandlerMethod(handler, HTTP_OPTIONS_HANDLE_METHOD);
            }
            throw new HttpRequestMethodNotSupportedException(request.getMethod(), methods);
        }

        if (helper.hasConsumesMismatch()) {
            Set<MediaType> mediaTypes = helper.getConsumableMediaTypes();
            MediaType contentType = null;
            if (StringUtils.hasLength(request.getContentType())) {
                try {
                    contentType = MediaType.parseMediaType(request.getContentType());
                }
                catch (InvalidMediaTypeException ex) {
                    throw new HttpMediaTypeNotSupportedException(ex.getMessage());
                }
            }
            throw new HttpMediaTypeNotSupportedException(contentType, new ArrayList<>(mediaTypes));
        }

        if (helper.hasProducesMismatch()) {
            Set<MediaType> mediaTypes = helper.getProducibleMediaTypes();
            throw new HttpMediaTypeNotAcceptableException(new ArrayList<>(mediaTypes));
        }

        if (helper.hasParamsMismatch()) {
            List<String[]> conditions = helper.getParamConditions();
            throw new UnsatisfiedServletRequestParameterException(conditions, request.getParameterMap());
        }

        return null;
    }
    private static class PartialMatchHelper {
        private final List<PartialMatch> partialMatches = new ArrayList<>();

        public PartialMatchHelper(Set<RequestMappingInfo> infos, HttpServletRequest request) {
            for (RequestMappingInfo info : infos) {
                if (info.getPatternsCondition().getMatchingCondition(request) != null) {
                    this.partialMatches.add(new PartialMatch(info, request));
                }
            }
        }
        public boolean isEmpty() {
            return this.partialMatches.isEmpty();
        }

        /**
         * Any partial matches for "methods"?
         */
        public boolean hasMethodsMismatch() {
            for (PartialMatch match : this.partialMatches) {
                if (match.hasMethodsMatch()) {
                    return false;
                }
            }
            return true;
        }
        public boolean hasConsumesMismatch() {
            for (PartialMatch match : this.partialMatches) {
                if (match.hasConsumesMatch()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Any partial matches for "methods", "consumes", and "produces"?
         */
        public boolean hasProducesMismatch() {
            for (PartialMatch match : this.partialMatches) {
                if (match.hasProducesMatch()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Any partial matches for "methods", "consumes", "produces", and "params"?
         */
        public boolean hasParamsMismatch() {
            for (PartialMatch match : this.partialMatches) {
                if (match.hasParamsMatch()) {
                    return false;
                }
            }
            return true;
        }
        public Set<String> getAllowedMethods() {
            Set<String> result = new LinkedHashSet<>();
            for (PartialMatch match : this.partialMatches) {
                for (RequestMethod method : match.getInfo().getMethodsCondition().getMethods()) {
                    result.add(method.name());
                }
            }
            return result;
        }

        /**
         * Return declared "consumable" types but only among those that also
         * match the "methods" condition.
         */
        public Set<MediaType> getConsumableMediaTypes() {
            Set<MediaType> result = new LinkedHashSet<>();
            for (PartialMatch match : this.partialMatches) {
                if (match.hasMethodsMatch()) {
                    result.addAll(match.getInfo().getConsumesCondition().getConsumableMediaTypes());
                }
            }
            return result;
        }
        public Set<MediaType> getProducibleMediaTypes() {
            Set<MediaType> result = new LinkedHashSet<>();
            for (PartialMatch match : this.partialMatches) {
                if (match.hasConsumesMatch()) {
                    result.addAll(match.getInfo().getProducesCondition().getProducibleMediaTypes());
                }
            }
            return result;
        }

        /**
         * Return declared "params" conditions but only among those that also
         * match the "methods", "consumes", and "params" conditions.
         */
        public List<String[]> getParamConditions() {
            List<String[]> result = new ArrayList<>();
            for (PartialMatch match : this.partialMatches) {
                if (match.hasProducesMatch()) {
                    Set<NameValueExpression<String>> set = match.getInfo().getParamsCondition().getExpressions();
                    if (!CollectionUtils.isEmpty(set)) {
                        int i = 0;
                        String[] array = new String[set.size()];
                        for (NameValueExpression<String> expression : set) {
                            array[i++] = expression.toString();
                        }
                        result.add(array);
                    }
                }
            }
            return result;
        }
        private static class PartialMatch {
            private final RequestMappingInfo info;

            private final boolean methodsMatch;

            private final boolean consumesMatch;

            private final boolean producesMatch;

            private final boolean paramsMatch;
            public PartialMatch(RequestMappingInfo info, HttpServletRequest request) {
                this.info = info;
                this.methodsMatch = (info.getMethodsCondition().getMatchingCondition(request) != null);
                this.consumesMatch = (info.getConsumesCondition().getMatchingCondition(request) != null);
                this.producesMatch = (info.getProducesCondition().getMatchingCondition(request) != null);
                this.paramsMatch = (info.getParamsCondition().getMatchingCondition(request) != null);
            }
            public RequestMappingInfo getInfo() {
                return this.info;
            }

            public boolean hasMethodsMatch() {
                return this.methodsMatch;
            }

            public boolean hasConsumesMatch() {
                return (hasMethodsMatch() && this.consumesMatch);
            }

            public boolean hasProducesMatch() {
                return (hasConsumesMatch() && this.producesMatch);
            }

            public boolean hasParamsMatch() {
                return (hasProducesMatch() && this.paramsMatch);
            }

            @Override
            public String toString() {
                return this.info.toString();
            }
        }

    }

    private static class HttpOptionsHandler {

        private final HttpHeaders headers = new HttpHeaders();

        public HttpOptionsHandler(Set<String> declaredMethods) {
            this.headers.setAllow(initAllowedHttpMethods(declaredMethods));
        }

        private static Set<HttpMethod> initAllowedHttpMethods(Set<String> declaredMethods) {
            Set<HttpMethod> result = new LinkedHashSet<>(declaredMethods.size());
            if (declaredMethods.isEmpty()) {
                for (HttpMethod method : HttpMethod.values()) {
                    if (method != HttpMethod.TRACE) {
                        result.add(method);
                    }
                }
            }
            else {
                for (String method : declaredMethods) {
                    HttpMethod httpMethod = HttpMethod.valueOf(method);
                    result.add(httpMethod);
                    if (httpMethod == HttpMethod.GET) {
                        result.add(HttpMethod.HEAD);
                    }
                }
                result.add(HttpMethod.OPTIONS);
            }
            return result;
        }

        @SuppressWarnings("unused")
        public HttpHeaders handle() {
            return this.headers;
        }
    }

}
