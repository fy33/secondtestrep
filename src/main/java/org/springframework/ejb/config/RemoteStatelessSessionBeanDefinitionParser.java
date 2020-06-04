package org.springframework.ejb.config;

import org.w3c.dom.Element;

 class RemoteStatelessSessionBeanDefinitionParser extends AbstractJndiLocationBeanDefinitionParser{
    @Override
    protected String getBeanClassName(Element element) {
        return "org.springframework.ejb.access.SimpleRemoteStatelessSessionProxyFactoryBean";
    }

}
