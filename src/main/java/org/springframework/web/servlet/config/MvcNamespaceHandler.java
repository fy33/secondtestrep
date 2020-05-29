package org.springframework.web.servlet.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class MvcNamespaceHandler extends NamespaceHandlerSupport {
    @Override
    public void init() {
        registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenBeanDefinitionParser());
        registerBeanDefinitionParser("default-servlet-handler", new DefaultServletHandlerBeanDefinitionParser());
        registerBeanDefinitionParser("interceptors", new InterceptorsBeanDefinitionParser());
        registerBeanDefinitionParser("resources", new ResourcesBeanDefinitionParser());
        registerBeanDefinitionParser("view-controller", new ViewControllerBeanDefinitionParser());
        registerBeanDefinitionParser("redirect-view-controller", new ViewControllerBeanDefinitionParser());
        registerBeanDefinitionParser("status-controller", new ViewControllerBeanDefinitionParser());
        registerBeanDefinitionParser("view-resolvers", new ViewResolversBeanDefinitionParser());
        registerBeanDefinitionParser("tiles-configurer", new TilesConfigurerBeanDefinitionParser());
        registerBeanDefinitionParser("freemarker-configurer", new FreeMarkerConfigurerBeanDefinitionParser());
        registerBeanDefinitionParser("groovy-configurer", new GroovyMarkupConfigurerBeanDefinitionParser());
        registerBeanDefinitionParser("script-template-configurer", new ScriptTemplateConfigurerBeanDefinitionParser());
        registerBeanDefinitionParser("cors", new CorsBeanDefinitionParser());
    }
}
