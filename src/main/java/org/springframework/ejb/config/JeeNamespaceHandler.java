package org.springframework.ejb.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class JeeNamespaceHandler extends NamespaceHandlerSupport {
    @Override
    public void init() {
        registerBeanDefinitionParser("jndi-lookup", new JndiLookupBeanDefinitionParser());
        registerBeanDefinitionParser("local-slsb", new LocalStatelessSessionBeanDefinitionParser());
        registerBeanDefinitionParser("remote-slsb", new RemoteStatelessSessionBeanDefinitionParser());
    }
}
