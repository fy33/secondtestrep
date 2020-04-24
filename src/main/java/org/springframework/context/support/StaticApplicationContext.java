package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

import java.util.Locale;

public class StaticApplicationContext extends GenericApplicationContext {
    private final StaticMessageSource staticMessageSource;

    public StaticApplicationContext() throws BeansException {
        this(null);
    }

    public StaticApplicationContext(@Nullable ApplicationContext parent) throws BeansException {
        super(parent);

        // Initialize and register a StaticMessageSource.
        this.staticMessageSource = new StaticMessageSource();
        getBeanFactory().registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.staticMessageSource);
    }


    @Override
    protected void assertBeanFactoryActive() {
    }


    public final StaticMessageSource getStaticMessageSource() {
        return this.staticMessageSource;
    }

    public void registerSingleton(String name, Class<?> clazz) throws BeansException {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(clazz);
        getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
    }



    public void registerSingleton(String name, Class<?> clazz, MutablePropertyValues pvs) throws BeansException {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(clazz);
        bd.setPropertyValues(pvs);
        getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
    }


    public void registerPrototype(String name, Class<?> clazz) throws BeansException {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        bd.setBeanClass(clazz);
        getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
    }


    public void registerPrototype(String name, Class<?> clazz, MutablePropertyValues pvs) throws BeansException {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        bd.setBeanClass(clazz);
        bd.setPropertyValues(pvs);
        getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
    }


    public void addMessage(String code, Locale locale, String defaultMessage) {
        getStaticMessageSource().addMessage(code, locale, defaultMessage);
    }













}
