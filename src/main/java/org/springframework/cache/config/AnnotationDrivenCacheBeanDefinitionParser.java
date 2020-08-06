package org.springframework.cache.config;

import org.springframework.aop.config.AopNamespaceUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

class AnnotationDrivenCacheBeanDefinitionParser implements BeanDefinitionParser {

    private static final String CACHE_ASPECT_CLASS_NAME =
            "org.springframework.cache.aspectj.AnnotationCacheAspect";

    private static final String JCACHE_ASPECT_CLASS_NAME =
            "org.springframework.cache.aspectj.JCacheCacheAspect";

    private static final boolean jsr107Present;

    private static final boolean jcacheImplPresent;

    static {
        ClassLoader classLoader = AnnotationDrivenCacheBeanDefinitionParser.class.getClassLoader();
        jsr107Present = ClassUtils.isPresent("javax.cache.Cache", classLoader);
        jcacheImplPresent = ClassUtils.isPresent(
                "org.springframework.cache.jcache.interceptor.DefaultJCacheOperationSource", classLoader);
    }

    @Override
    @Nullable
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        String mode = element.getAttribute("mode");
        if ("aspectj".equals(mode)) {
            // mode="aspectj"
            registerCacheAspect(element, parserContext);
        }
        else {
            // mode="proxy"
            registerCacheAdvisor(element, parserContext);
        }

        return null;
    }

    private void registerCacheAspect(Element element, ParserContext parserContext) {
        SpringCachingConfigurer.registerCacheAspect(element, parserContext);
        if (jsr107Present && jcacheImplPresent) {
            JCacheCachingConfigurer.registerCacheAspect(element, parserContext);
        }
    }

    private void registerCacheAdvisor(Element element, ParserContext parserContext) {
        AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(parserContext, element);
        SpringCachingConfigurer.registerCacheAdvisor(element, parserContext);
        if (jsr107Present && jcacheImplPresent) {
            JCacheCachingConfigurer.registerCacheAdvisor(element, parserContext);
        }
    }

    private static void parseCacheResolution(Element element, BeanDefinition def, boolean setBoth) {
        String name = element.getAttribute("cache-resolver");
        boolean hasText = StringUtils.hasText(name);
        if (hasText) {
            def.getPropertyValues().add("cacheResolver", new RuntimeBeanReference(name.trim()));
        }
        if (!hasText || setBoth) {
            def.getPropertyValues().add("cacheManager",
                    new RuntimeBeanReference(CacheNamespaceHandler.extractCacheManager(element)));
        }
    }

    private static void parseErrorHandler(Element element, BeanDefinition def) {
        String name = element.getAttribute("error-handler");
        if (StringUtils.hasText(name)) {
            def.getPropertyValues().add("errorHandler", new RuntimeBeanReference(name.trim()));
        }
    }

    private static class SpringCachingConfigurer {

        private static void registerCacheAdvisor(Element element, ParserContext parserContext) {
            if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME)) {
                Object eleSource = parserContext.extractSource(element);

                // Create the CacheOperationSource definition.
                RootBeanDefinition sourceDef = new RootBeanDefinition("org.springframework.cache.annotation.AnnotationCacheOperationSource");
                sourceDef.setSource(eleSource);
                sourceDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                String sourceName = parserContext.getReaderContext().registerWithGeneratedName(sourceDef);

                // Create the CacheInterceptor definition.
                RootBeanDefinition interceptorDef = new RootBeanDefinition(CacheInterceptor.class);
                interceptorDef.setSource(eleSource);
                interceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                parseCacheResolution(element, interceptorDef, false);
                parseErrorHandler(element, interceptorDef);
                CacheNamespaceHandler.parseKeyGenerator(element, interceptorDef);
                interceptorDef.getPropertyValues().add("cacheOperationSources", new RuntimeBeanReference(sourceName));
                String interceptorName = parserContext.getReaderContext().registerWithGeneratedName(interceptorDef);

                // Create the CacheAdvisor definition.
                RootBeanDefinition advisorDef = new RootBeanDefinition(BeanFactoryCacheOperationSourceAdvisor.class);
                advisorDef.setSource(eleSource);
                advisorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                advisorDef.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(sourceName));
                advisorDef.getPropertyValues().add("adviceBeanName", interceptorName);
                if (element.hasAttribute("order")) {
                    advisorDef.getPropertyValues().add("order", element.getAttribute("order"));
                }
                parserContext.getRegistry().registerBeanDefinition(CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME, advisorDef);

                CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), eleSource);
                compositeDef.addNestedComponent(new BeanComponentDefinition(sourceDef, sourceName));
                compositeDef.addNestedComponent(new BeanComponentDefinition(interceptorDef, interceptorName));
                compositeDef.addNestedComponent(new BeanComponentDefinition(advisorDef, CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME));
                parserContext.registerComponent(compositeDef);
            }
        }

        /**
         * Registers a cache aspect.
         * <pre class="code">
         * &lt;bean id="cacheAspect" class="org.springframework.cache.aspectj.AnnotationCacheAspect" factory-method="aspectOf"&gt;
         *   &lt;property name="cacheManager" ref="cacheManager"/&gt;
         *   &lt;property name="keyGenerator" ref="keyGenerator"/&gt;
         * &lt;/bean&gt;
         * </pre>
         */
        private static void registerCacheAspect(Element element, ParserContext parserContext) {
            if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.CACHE_ASPECT_BEAN_NAME)) {
                RootBeanDefinition def = new RootBeanDefinition();
                def.setBeanClassName(CACHE_ASPECT_CLASS_NAME);
                def.setFactoryMethodName("aspectOf");
                parseCacheResolution(element, def, false);
                CacheNamespaceHandler.parseKeyGenerator(element, def);
                parserContext.registerBeanComponent(new BeanComponentDefinition(def, CacheManagementConfigUtils.CACHE_ASPECT_BEAN_NAME));
            }
        }
    }



    private static class JCacheCachingConfigurer {

        private static void registerCacheAdvisor(Element element, ParserContext parserContext) {
            if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME)) {
                Object source = parserContext.extractSource(element);

                // Create the CacheOperationSource definition.
                BeanDefinition sourceDef = createJCacheOperationSourceBeanDefinition(element, source);
                String sourceName = parserContext.getReaderContext().registerWithGeneratedName(sourceDef);

                // Create the CacheInterceptor definition.
                RootBeanDefinition interceptorDef =
                        new RootBeanDefinition("org.springframework.cache.jcache.interceptor.JCacheInterceptor");
                interceptorDef.setSource(source);
                interceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                interceptorDef.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(sourceName));
                parseErrorHandler(element, interceptorDef);
                String interceptorName = parserContext.getReaderContext().registerWithGeneratedName(interceptorDef);

                // Create the CacheAdvisor definition.
                RootBeanDefinition advisorDef = new RootBeanDefinition(
                        "org.springframework.cache.jcache.interceptor.BeanFactoryJCacheOperationSourceAdvisor");
                advisorDef.setSource(source);
                advisorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                advisorDef.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(sourceName));
                advisorDef.getPropertyValues().add("adviceBeanName", interceptorName);
                if (element.hasAttribute("order")) {
                    advisorDef.getPropertyValues().add("order", element.getAttribute("order"));
                }
                parserContext.getRegistry().registerBeanDefinition(CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME, advisorDef);

                CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), source);
                compositeDef.addNestedComponent(new BeanComponentDefinition(sourceDef, sourceName));
                compositeDef.addNestedComponent(new BeanComponentDefinition(interceptorDef, interceptorName));
                compositeDef.addNestedComponent(new BeanComponentDefinition(advisorDef, CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME));
                parserContext.registerComponent(compositeDef);
            }
        }

        private static void registerCacheAspect(Element element, ParserContext parserContext) {
            if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.JCACHE_ASPECT_BEAN_NAME)) {
                Object eleSource = parserContext.extractSource(element);
                RootBeanDefinition def = new RootBeanDefinition();
                def.setBeanClassName(JCACHE_ASPECT_CLASS_NAME);
                def.setFactoryMethodName("aspectOf");
                BeanDefinition sourceDef = createJCacheOperationSourceBeanDefinition(element, eleSource);
                String sourceName =
                        parserContext.getReaderContext().registerWithGeneratedName(sourceDef);
                def.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(sourceName));

                parserContext.registerBeanComponent(new BeanComponentDefinition(sourceDef, sourceName));
                parserContext.registerBeanComponent(new BeanComponentDefinition(def, CacheManagementConfigUtils.JCACHE_ASPECT_BEAN_NAME));
            }
        }

        private static RootBeanDefinition createJCacheOperationSourceBeanDefinition(Element element, @Nullable Object eleSource) {
            RootBeanDefinition sourceDef =
                    new RootBeanDefinition("org.springframework.cache.jcache.interceptor.DefaultJCacheOperationSource");
            sourceDef.setSource(eleSource);
            sourceDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            // JSR-107 support should create an exception cache resolver with the cache manager
            // and there is no way to set that exception cache resolver from the namespace
            parseCacheResolution(element, sourceDef, true);
            CacheNamespaceHandler.parseKeyGenerator(element, sourceDef);
            return sourceDef;
        }
    }




}
