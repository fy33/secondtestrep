package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {
    String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;


    String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;

    int ROLE_APPLICATION = 0;

    int ROLE_SUPPORT = 1;

    int ROLE_INFRASTRUCTURE = 2;

    void setParentName(@Nullable String parentName);


    @Nullable
    String getParentName();


    void setBeanClassName(@Nullable String beanClassName);


    @Nullable
    String getBeanClassName();


    void setScope(@Nullable String scope);

    @Nullable
    String getScope();

    void setLazyInit(boolean lazyInit);


    boolean isLazyInit();
    void setDependsOn(@Nullable String... dependsOn);


    @Nullable
    String[] getDependsOn();

    void setAutowireCandidate(boolean autowireCandidate);

    boolean isAutowireCandidate();

    void setPrimary(boolean primary);
    boolean isPrimary();

    void setFactoryBeanName(@Nullable String factoryBeanName);


    @Nullable
    String getFactoryBeanName();

    void setFactoryMethodName(@Nullable String factoryMethodName);


    @Nullable
    String getFactoryMethodName();

    ConstructorArgumentValues getConstructorArgumentValues();

    default boolean hasConstructorArgumentValues() {
        return !getConstructorArgumentValues().isEmpty();
    }


    MutablePropertyValues getPropertyValues();

    default boolean hasPropertyValues() {
        return !getPropertyValues().isEmpty();
    }

    void setInitMethodName(@Nullable String initMethodName);

    @Nullable
    String getInitMethodName();

    void setDestroyMethodName(@Nullable String destroyMethodName);

    @Nullable
    String getDestroyMethodName();

    void setRole(int role);

    int getRole();

    void setDescription(@Nullable String description);

    @Nullable
    String getDescription();

    ResolvableType getResolvableType();

    boolean isSingleton();

    boolean isPrototype();

    boolean isAbstract();

    @Nullable
    String getResourceDescription();


    @Nullable
    BeanDefinition getOriginatingBeanDefinition();






}
