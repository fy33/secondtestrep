package org.springframework.boot.web.reactive.context;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;

public class GenericReactiveWebApplicationContext extends GenericApplicationContext implements
ConfigurableReactiveWebApplicationContext{
    public GenericReactiveWebApplicationContext() {
    }


    public GenericReactiveWebApplicationContext(DefaultListableBeanFactory beanFactory) {
        super(beanFactory);
    }


    @Override
    protected ConfigurableEnvironment createEnvironment() {
        return new StandardReactiveWebEnvironment();
    }


    @Override
    protected Resource getResourceByPath(String path) {
        // We must be careful not to expose classpath resources
        return new FilteredReactiveWebContextResource(path);
    }
}
