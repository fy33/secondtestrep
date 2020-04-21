package org.springframework.boot.context.properties;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;

public class ConfigurationPropertiesValueObjectBeanDefinition extends GenericBeanDefinition {
    private final BeanFactory beanFactory;

    private final String beanName;


    ConfigurationPropertiesValueObjectBeanDefinition(BeanFactory beanFactory, String beanName, Class<?> beanClass) {
        this.beanFactory = beanFactory;
        this.beanName = beanName;
        setBeanClass(beanClass);
        setInstanceSupplier(this::createBean);
    }



    private Object createBean() {
        ConfigurationPropertiesBean bean = ConfigurationPropertiesBean.forValueObject(getBeanClass(), this.beanName);
        ConfigurationPropertiesBinder binder = ConfigurationPropertiesBinder.get(this.beanFactory);
        try {
            return binder.bindOrCreate(bean);
        }
        catch (Exception ex) {
            throw new ConfigurationPropertiesBindException(bean, ex);
        }
    }








}
