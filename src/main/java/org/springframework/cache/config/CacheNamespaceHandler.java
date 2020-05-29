package org.springframework.cache.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

public class CacheNamespaceHandler extends NamespaceHandlerSupport {
    static final String CACHE_MANAGER_ATTRIBUTE = "cache-manager";

    static final String DEFAULT_CACHE_MANAGER_BEAN_NAME = "cacheManager";

    static String extractCacheManager(Element element) {
        return (element.hasAttribute(CacheNamespaceHandler.CACHE_MANAGER_ATTRIBUTE) ?
                element.getAttribute(CacheNamespaceHandler.CACHE_MANAGER_ATTRIBUTE) :
                CacheNamespaceHandler.DEFAULT_CACHE_MANAGER_BEAN_NAME);
    }

    static BeanDefinition parseKeyGenerator(Element element, BeanDefinition def) {
        String name = element.getAttribute("key-generator");
        if (StringUtils.hasText(name)) {
            def.getPropertyValues().add("keyGenerator", new RuntimeBeanReference(name.trim()));
        }
        return def;
    }

    @Override
    public void init() {
        registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenCacheBeanDefinitionParser());
        registerBeanDefinitionParser("advice", new CacheAdviceParser());
    }



}
