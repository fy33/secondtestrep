package org.springframework.web.servlet.handler;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodIntrospector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {
    private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";
    private static final HandlerMethod PREFLIGHT_AMBIGUOUS_MATCH =
            new HandlerMethod(new EmptyHandler(), ClassUtils.getMethod(EmptyHandler.class, "handle"));
    private static final CorsConfiguration ALLOW_CORS_CONFIG = new CorsConfiguration();
    static {
        ALLOW_CORS_CONFIG.addAllowedOrigin("*");
        ALLOW_CORS_CONFIG.addAllowedMethod("*");
        ALLOW_CORS_CONFIG.addAllowedHeader("*");
        ALLOW_CORS_CONFIG.setAllowCredentials(true);
    }
    private boolean detectHandlerMethodsInAncestorContexts = false;
    @Nullable
    private HandlerMethodMappingNamingStrategy<T> namingStrategy;

    private final MappingRegistry mappingRegistry = new MappingRegistry();
    public void setDetectHandlerMethodsInAncestorContexts(boolean detectHandlerMethodsInAncestorContexts) {
        this.detectHandlerMethodsInAncestorContexts = detectHandlerMethodsInAncestorContexts;
    }
    public void setHandlerMethodMappingNamingStrategy(HandlerMethodMappingNamingStrategy<T> namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    @Nullable
    public HandlerMethodMappingNamingStrategy<T> getNamingStrategy() {
        return this.namingStrategy;
    }

    public Map<T, HandlerMethod> getHandlerMethods() {
        this.mappingRegistry.acquireReadLock();
        try {
            return Collections.unmodifiableMap(this.mappingRegistry.getMappings());
        }
        finally {
            this.mappingRegistry.releaseReadLock();
        }
    }

    @Nullable
    public List<HandlerMethod> getHandlerMethodsForMappingName(String mappingName) {
        return this.mappingRegistry.getHandlerMethodsByMappingName(mappingName);
    }
    MappingRegistry getMappingRegistry() {
        return this.mappingRegistry;
    }

    public void registerMapping(T mapping, Object handler, Method method) {
        if (logger.isTraceEnabled()) {
            logger.trace("Register \"" + mapping + "\" to " + method.toGenericString());
        }
        this.mappingRegistry.register(mapping, handler, method);
    }
    public void unregisterMapping(T mapping) {
        if (logger.isTraceEnabled()) {
            logger.trace("Unregister mapping \"" + mapping + "\"");
        }
        this.mappingRegistry.unregister(mapping);
    }
    @Override
    public void afterPropertiesSet() {
        initHandlerMethods();
    }

    protected void initHandlerMethods() {
        for (String beanName : getCandidateBeanNames()) {
            if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
                processCandidateBean(beanName);
            }
        }
        handlerMethodsInitialized(getHandlerMethods());
    }
    protected String[] getCandidateBeanNames() {
        return (this.detectHandlerMethodsInAncestorContexts ?
                BeanFactoryUtils.beanNamesForTypeIncludingAncestors(obtainApplicationContext(), Object.class) :
                obtainApplicationContext().getBeanNamesForType(Object.class));
    }

    protected void processCandidateBean(String beanName) {
        Class<?> beanType = null;
        try {
            beanType = obtainApplicationContext().getType(beanName);
        }
        catch (Throwable ex) {
            // An unresolvable bean type, probably from a lazy bean - let's ignore it.
            if (logger.isTraceEnabled()) {
                logger.trace("Could not resolve type for bean '" + beanName + "'", ex);
            }
        }
        if (beanType != null && isHandler(beanType)) {
            detectHandlerMethods(beanName);
        }
    }
    protected void detectHandlerMethods(Object handler) {
        Class<?> handlerType = (handler instanceof String ?
                obtainApplicationContext().getType((String) handler) : handler.getClass());

        if (handlerType != null) {
            Class<?> userType = ClassUtils.getUserClass(handlerType);
            Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
                    (MethodIntrospector.MetadataLookup<T>) method -> {
                        try {
                            return getMappingForMethod(method, userType);
                        }
                        catch (Throwable ex) {
                            throw new IllegalStateException("Invalid mapping on handler class [" +
                                    userType.getName() + "]: " + method, ex);
                        }
                    });
            if (logger.isTraceEnabled()) {
                logger.trace(formatMappings(userType, methods));
            }
            methods.forEach((method, mapping) -> {
                Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
                registerHandlerMethod(handler, invocableMethod, mapping);
            });
        }
    }
    private String formatMappings(Class<?> userType, Map<Method, T> methods) {
        String formattedType = Arrays.stream(ClassUtils.getPackageName(userType).split("\\."))
                .map(p -> p.substring(0, 1))
                .collect(Collectors.joining(".", "", "." + userType.getSimpleName()));
        Function<Method, String> methodFormatter = method -> Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(",", "(", ")"));
        return methods.entrySet().stream()
                .map(e -> {
                    Method method = e.getKey();
                    return e.getValue() + ": " + method.getName() + methodFormatter.apply(method);
                })
                .collect(Collectors.joining("\n\t", "\n\t" + formattedType + ":" + "\n\t", ""));
    }

    protected void registerHandlerMethod(Object handler, Method method, T mapping) {
        this.mappingRegistry.register(mapping, handler, method);
    }


    protected HandlerMethod createHandlerMethod(Object handler, Method method) {
        if (handler instanceof String) {
            return new HandlerMethod((String) handler,
                    obtainApplicationContext().getAutowireCapableBeanFactory(), method);
        }
        return new HandlerMethod(handler, method);
    }

    @Nullable
    protected CorsConfiguration initCorsConfiguration(Object handler, Method method, T mapping) {
        return null;
    }


    protected void handlerMethodsInitialized(Map<T, HandlerMethod> handlerMethods) {
        // Total includes detected mappings + explicit registrations via registerMapping
        int total = handlerMethods.size();
        if ((logger.isTraceEnabled() && total == 0) || (logger.isDebugEnabled() && total > 0) ) {
            logger.debug(total + " mappings in " + formatMappingName());
        }
    }
    @Override
    protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
        String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
        request.setAttribute(LOOKUP_PATH, lookupPath);
        this.mappingRegistry.acquireReadLock();
        try {
            HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
            return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
        }
        finally {
            this.mappingRegistry.releaseReadLock();
        }
    }
    @Nullable
    protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
        List<Match> matches = new ArrayList<>();
        List<T> directPathMatches = this.mappingRegistry.getMappingsByUrl(lookupPath);
        if (directPathMatches != null) {
            addMatchingMappings(directPathMatches, matches, request);
        }
        if (matches.isEmpty()) {
            // No choice but to go through all mappings...
            addMatchingMappings(this.mappingRegistry.getMappings().keySet(), matches, request);
        }

        if (!matches.isEmpty()) {
            Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
            matches.sort(comparator);
            Match bestMatch = matches.get(0);
            if (matches.size() > 1) {
                if (logger.isTraceEnabled()) {
                    logger.trace(matches.size() + " matching mappings: " + matches);
                }
                if (CorsUtils.isPreFlightRequest(request)) {
                    return PREFLIGHT_AMBIGUOUS_MATCH;
                }
                Match secondBestMatch = matches.get(1);
                if (comparator.compare(bestMatch, secondBestMatch) == 0) {
                    Method m1 = bestMatch.handlerMethod.getMethod();
                    Method m2 = secondBestMatch.handlerMethod.getMethod();
                    String uri = request.getRequestURI();
                    throw new IllegalStateException(
                            "Ambiguous handler methods mapped for '" + uri + "': {" + m1 + ", " + m2 + "}");
                }
            }
            request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, bestMatch.handlerMethod);
            handleMatch(bestMatch.mapping, lookupPath, request);
            return bestMatch.handlerMethod;
        }
        else {
            return handleNoMatch(this.mappingRegistry.getMappings().keySet(), lookupPath, request);
        }
    }
    private void addMatchingMappings(Collection<T> mappings, List<Match> matches, HttpServletRequest request) {
        for (T mapping : mappings) {
            T match = getMatchingMapping(mapping, request);
            if (match != null) {
                matches.add(new Match(match, this.mappingRegistry.getMappings().get(mapping)));
            }
        }
    }
    protected void handleMatch(T mapping, String lookupPath, HttpServletRequest request) {
        request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, lookupPath);
    }
    @Nullable
    protected HandlerMethod handleNoMatch(Set<T> mappings, String lookupPath, HttpServletRequest request)
            throws Exception {

        return null;
    }

    @Override
    protected boolean hasCorsConfigurationSource(Object handler) {
        return super.hasCorsConfigurationSource(handler) ||
                (handler instanceof HandlerMethod && this.mappingRegistry.getCorsConfiguration((HandlerMethod) handler) != null) ||
                handler.equals(PREFLIGHT_AMBIGUOUS_MATCH);
    }

    @Override
    protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
        CorsConfiguration corsConfig = super.getCorsConfiguration(handler, request);
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            if (handlerMethod.equals(PREFLIGHT_AMBIGUOUS_MATCH)) {
                return AbstractHandlerMethodMapping.ALLOW_CORS_CONFIG;
            }
            else {
                CorsConfiguration corsConfigFromMethod = this.mappingRegistry.getCorsConfiguration(handlerMethod);
                corsConfig = (corsConfig != null ? corsConfig.combine(corsConfigFromMethod) : corsConfigFromMethod);
            }
        }
        return corsConfig;
    }
    protected abstract boolean isHandler(Class<?> beanType);
    @Nullable
    protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

    protected abstract Set<String> getMappingPathPatterns(T mapping);


    @Nullable
    protected abstract T getMatchingMapping(T mapping, HttpServletRequest request);


    protected abstract Comparator<T> getMappingComparator(HttpServletRequest request);

    class MappingRegistry {
        private final Map<T, MappingRegistration<T>> registry = new HashMap<>();

        private final Map<T, HandlerMethod> mappingLookup = new LinkedHashMap<>();

        private final MultiValueMap<String, T> urlLookup = new LinkedMultiValueMap<>();

        private final Map<String, List<HandlerMethod>> nameLookup = new ConcurrentHashMap<>();

        private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();

        private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();


        public Map<T, HandlerMethod> getMappings() {
            return this.mappingLookup;
        }

        @Nullable
        public List<T> getMappingsByUrl(String urlPath) {
            return this.urlLookup.get(urlPath);
        }


        public List<HandlerMethod> getHandlerMethodsByMappingName(String mappingName) {
            return this.nameLookup.get(mappingName);
        }

        @Nullable
        public CorsConfiguration getCorsConfiguration(HandlerMethod handlerMethod) {
            HandlerMethod original = handlerMethod.getResolvedFromHandlerMethod();
            return this.corsLookup.get(original != null ? original : handlerMethod);
        }


        public void acquireReadLock() {
            this.readWriteLock.readLock().lock();
        }

        public void releaseReadLock() {
            this.readWriteLock.readLock().unlock();
        }

        public void register(T mapping, Object handler, Method method) {
            // Assert that the handler method is not a suspending one.
            //todo  注释掉 报错
//            if (KotlinDetector.isKotlinType(method.getDeclaringClass()) && KotlinDelegate.isSuspend(method)) {
//                throw new IllegalStateException("Unsupported suspending handler method detected: " + method);
//            }
            this.readWriteLock.writeLock().lock();
            try {
                HandlerMethod handlerMethod = createHandlerMethod(handler, method);
                validateMethodMapping(handlerMethod, mapping);
                this.mappingLookup.put(mapping, handlerMethod);

                List<String> directUrls = getDirectUrls(mapping);
                for (String url : directUrls) {
                    this.urlLookup.add(url, mapping);
                }

                String name = null;
                if (getNamingStrategy() != null) {
                    name = getNamingStrategy().getName(handlerMethod, mapping);
                    addMappingName(name, handlerMethod);
                }

                CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);
                if (corsConfig != null) {
                    this.corsLookup.put(handlerMethod, corsConfig);
                }

                this.registry.put(mapping, new MappingRegistration<>(mapping, handlerMethod, directUrls, name));
            }
            finally {
                this.readWriteLock.writeLock().unlock();
            }
        }
        private void validateMethodMapping(HandlerMethod handlerMethod, T mapping) {
            // Assert that the supplied mapping is unique.
            HandlerMethod existingHandlerMethod = this.mappingLookup.get(mapping);
            if (existingHandlerMethod != null && !existingHandlerMethod.equals(handlerMethod)) {
                throw new IllegalStateException(
                        "Ambiguous mapping. Cannot map '" + handlerMethod.getBean() + "' method \n" +
                                handlerMethod + "\nto " + mapping + ": There is already '" +
                                existingHandlerMethod.getBean() + "' bean method\n" + existingHandlerMethod + " mapped.");
            }
        }
        private List<String> getDirectUrls(T mapping) {
            List<String> urls = new ArrayList<>(1);
            for (String path : getMappingPathPatterns(mapping)) {
                if (!getPathMatcher().isPattern(path)) {
                    urls.add(path);
                }
            }
            return urls;
        }

        private void addMappingName(String name, HandlerMethod handlerMethod) {
            List<HandlerMethod> oldList = this.nameLookup.get(name);
            if (oldList == null) {
                oldList = Collections.emptyList();
            }

            for (HandlerMethod current : oldList) {
                if (handlerMethod.equals(current)) {
                    return;
                }
            }

            List<HandlerMethod> newList = new ArrayList<>(oldList.size() + 1);
            newList.addAll(oldList);
            newList.add(handlerMethod);
            this.nameLookup.put(name, newList);
        }

        public void unregister(T mapping) {
            this.readWriteLock.writeLock().lock();
            try {
                MappingRegistration<T> definition = this.registry.remove(mapping);
                if (definition == null) {
                    return;
                }

                this.mappingLookup.remove(definition.getMapping());

                for (String url : definition.getDirectUrls()) {
                    List<T> list = this.urlLookup.get(url);
                    if (list != null) {
                        list.remove(definition.getMapping());
                        if (list.isEmpty()) {
                            this.urlLookup.remove(url);
                        }
                    }
                }

                removeMappingName(definition);

                this.corsLookup.remove(definition.getHandlerMethod());
            }
            finally {
                this.readWriteLock.writeLock().unlock();
            }
        }
        private void removeMappingName(MappingRegistration<T> definition) {
            String name = definition.getMappingName();
            if (name == null) {
                return;
            }
            HandlerMethod handlerMethod = definition.getHandlerMethod();
            List<HandlerMethod> oldList = this.nameLookup.get(name);
            if (oldList == null) {
                return;
            }
            if (oldList.size() <= 1) {
                this.nameLookup.remove(name);
                return;
            }
            List<HandlerMethod> newList = new ArrayList<>(oldList.size() - 1);
            for (HandlerMethod current : oldList) {
                if (!current.equals(handlerMethod)) {
                    newList.add(current);
                }
            }
            this.nameLookup.put(name, newList);
        }
    }


    private static class MappingRegistration<T> {
        private final T mapping;

        private final HandlerMethod handlerMethod;

        private final List<String> directUrls;

        @Nullable
        private final String mappingName;

        public MappingRegistration(T mapping, HandlerMethod handlerMethod,
                                   @Nullable List<String> directUrls, @Nullable String mappingName) {

            Assert.notNull(mapping, "Mapping must not be null");
            Assert.notNull(handlerMethod, "HandlerMethod must not be null");
            this.mapping = mapping;
            this.handlerMethod = handlerMethod;
            this.directUrls = (directUrls != null ? directUrls : Collections.emptyList());
            this.mappingName = mappingName;
        }
        public T getMapping() {
            return this.mapping;
        }

        public HandlerMethod getHandlerMethod() {
            return this.handlerMethod;
        }

        public List<String> getDirectUrls() {
            return this.directUrls;
        }

        @Nullable
        public String getMappingName() {
            return this.mappingName;
        }
    }
    private class Match {

        private final T mapping;

        private final HandlerMethod handlerMethod;

        public Match(T mapping, HandlerMethod handlerMethod) {
            this.mapping = mapping;
            this.handlerMethod = handlerMethod;
        }

        @Override
        public String toString() {
            return this.mapping.toString();
        }
    }
    private class MatchComparator implements Comparator<Match> {

        private final Comparator<T> comparator;

        public MatchComparator(Comparator<T> comparator) {
            this.comparator = comparator;
        }

        @Override
        public int compare(Match match1, Match match2) {
            return this.comparator.compare(match1.mapping, match2.mapping);
        }
    }
    private static class EmptyHandler {

        @SuppressWarnings("unused")
        public void handle() {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    private static class KotlinDelegate {
//todo 注释掉

//        static private boolean isSuspend(Method method) {
//            KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
//            return function != null && function.isSuspend();
//        }
    }









}
