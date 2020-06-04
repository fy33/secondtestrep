package org.springframework.ejb.config;

import org.w3c.dom.Element;
//todo 为什么不加public
 class LocalStatelessSessionBeanDefinitionParser extends AbstractJndiLocationBeanDefinitionParser{
    @Override
    protected String getBeanClassName(Element element) {
        return "org.springframework.ejb.access.LocalStatelessSessionProxyFactoryBean";
    }
}
