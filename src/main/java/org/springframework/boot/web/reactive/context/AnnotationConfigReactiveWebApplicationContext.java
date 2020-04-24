package org.springframework.boot.web.reactive.context;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

public class AnnotationConfigReactiveWebApplicationContext extends AnnotationConfigApplicationContext implements ConfigurableReactiveWebApplicationContext {
    public AnnotationConfigReactiveWebApplicationContext() {
    }


    public AnnotationConfigReactiveWebApplicationContext(DefaultListableBeanFactory beanFactory) {
        super(beanFactory);
    }


    public AnnotationConfigReactiveWebApplicationContext(Class<?>... annotatedClasses) {
        super(annotatedClasses);
    }


    public AnnotationConfigReactiveWebApplicationContext(String... basePackages) {
        super(basePackages);
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

    @Deprecated
    protected final BeanNameGenerator getBeanNameGenerator() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    protected ScopeMetadataResolver getScopeMetadataResolver() {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    protected AnnotatedBeanDefinitionReader getAnnotatedBeanDefinitionReader(DefaultListableBeanFactory beanFactory) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    protected ClassPathBeanDefinitionScanner getClassPathBeanDefinitionScanner(DefaultListableBeanFactory beanFactory) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    public void setConfigLocation(String location) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    public void setConfigLocations(@Nullable String... locations) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    protected String[] getConfigLocations() {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    protected String[] getDefaultConfigLocations() {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    protected String resolvePath(String path) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    protected final boolean hasBeanFactory() {
        return true;
    }


    @Deprecated
    protected DefaultListableBeanFactory createBeanFactory() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
        throw new UnsupportedOperationException();
    }













}
