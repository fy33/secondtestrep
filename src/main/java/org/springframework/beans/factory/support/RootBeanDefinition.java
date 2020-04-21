package org.springframework.beans.factory.support;


import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

@SuppressWarnings("serial")
public class RootBeanDefinition extends AbstractBeanDefinition {

    @Nullable
    private BeanDefinitionHolder decoratedDefinition;

    @Nullable
    private AnnotatedElement qualifiedElement;


    volatile boolean stale;

    boolean allowCaching = true;

    boolean isFactoryMethodUnique = false;


    @Nullable
    volatile ResolvableType targetType;

    @Nullable
    volatile Class<?> resolvedTargetType;


    @Nullable
    volatile Boolean isFactoryBean;


    @Nullable
    volatile ResolvableType factoryMethodReturnType;

    @Nullable
    volatile Method factoryMethodToIntrospect;


    final Object constructorArgumentLock = new Object();

    @Nullable
    Executable resolvedConstructorOrFactoryMethod;

    boolean constructorArgumentsResolved = false;


    @Nullable
    Object[] resolvedConstructorArguments;

    @Nullable
    Object[] preparedConstructorArguments;

    final Object postProcessingLock = new Object();

    boolean postProcessed = false;

    @Nullable
    volatile Boolean beforeInstantiationResolved;

    @Nullable
    private Set<Member> externallyManagedConfigMembers;

    @Nullable
    private Set<String> externallyManagedInitMethods;

    @Nullable
    private Set<String> externallyManagedDestroyMethods;

    public RootBeanDefinition() {
        super();
    }

    public RootBeanDefinition(@Nullable Class<?> beanClass) {
        super();
        setBeanClass(beanClass);
    }

    public <T> RootBeanDefinition(@Nullable Class<T> beanClass, @Nullable Supplier<T> instanceSupplier) {
        super();
        setBeanClass(beanClass);
        setInstanceSupplier(instanceSupplier);
    }

    public <T> RootBeanDefinition(@Nullable Class<T> beanClass, String scope, @Nullable Supplier<T> instanceSupplier) {
        super();
        setBeanClass(beanClass);
        setScope(scope);
        setInstanceSupplier(instanceSupplier);
    }

    public RootBeanDefinition(@Nullable Class<?> beanClass, int autowireMode, boolean dependencyCheck) {
        super();
        setBeanClass(beanClass);
        setAutowireMode(autowireMode);
        if (dependencyCheck && getResolvedAutowireMode() != AUTOWIRE_CONSTRUCTOR) {
            setDependencyCheck(DEPENDENCY_CHECK_OBJECTS);
        }
    }


    public RootBeanDefinition(@Nullable Class<?> beanClass, @Nullable ConstructorArgumentValues cargs,
                              @Nullable MutablePropertyValues pvs) {

        super(cargs, pvs);
        setBeanClass(beanClass);
    }

    public RootBeanDefinition(String beanClassName) {
        setBeanClassName(beanClassName);
    }

    public RootBeanDefinition(String beanClassName, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {
        super(cargs, pvs);
        setBeanClassName(beanClassName);
    }

    public RootBeanDefinition(RootBeanDefinition original) {
        super(original);
        this.decoratedDefinition = original.decoratedDefinition;
        this.qualifiedElement = original.qualifiedElement;
        this.allowCaching = original.allowCaching;
        this.isFactoryMethodUnique = original.isFactoryMethodUnique;
        this.targetType = original.targetType;
        this.factoryMethodToIntrospect = original.factoryMethodToIntrospect;
    }

    RootBeanDefinition(BeanDefinition original) {
        super(original);
    }


    @Override
    public String getParentName() {
        return null;
    }




    @Override
    public void setParentName(@Nullable String parentName) {
        if (parentName != null) {
            throw new IllegalArgumentException("Root bean cannot be changed into a child bean with parent reference");
        }
    }

    public void setDecoratedDefinition(@Nullable BeanDefinitionHolder decoratedDefinition) {
        this.decoratedDefinition = decoratedDefinition;
    }

    @Nullable
    public BeanDefinitionHolder getDecoratedDefinition() {
        return this.decoratedDefinition;
    }

    public void setQualifiedElement(@Nullable AnnotatedElement qualifiedElement) {
        this.qualifiedElement = qualifiedElement;
    }


    @Nullable
    public AnnotatedElement getQualifiedElement() {
        return this.qualifiedElement;
    }

    public void setTargetType(ResolvableType targetType) {
        this.targetType = targetType;
    }


    public void setTargetType(@Nullable Class<?> targetType) {
        this.targetType = (targetType != null ? ResolvableType.forClass(targetType) : null);
    }

    @Nullable
    public Class<?> getTargetType() {
        if (this.resolvedTargetType != null) {
            return this.resolvedTargetType;
        }
        ResolvableType targetType = this.targetType;
        return (targetType != null ? targetType.resolve() : null);
    }




    @Override
    public ResolvableType getResolvableType() {
        ResolvableType targetType = this.targetType;
        if (targetType != null) {
            return targetType;
        }
        ResolvableType returnType = this.factoryMethodReturnType;
        if (returnType != null) {
            return returnType;
        }
        Method factoryMethod = this.factoryMethodToIntrospect;
        if (factoryMethod != null) {
            return ResolvableType.forMethodReturnType(factoryMethod);
        }
        return super.getResolvableType();
    }

    @Nullable
    public Constructor<?>[] getPreferredConstructors() {
        return null;
    }


    public void setUniqueFactoryMethodName(String name) {
        Assert.hasText(name, "Factory method name must not be empty");
        setFactoryMethodName(name);
        this.isFactoryMethodUnique = true;
    }

    public void setNonUniqueFactoryMethodName(String name) {
        Assert.hasText(name, "Factory method name must not be empty");
        setFactoryMethodName(name);
        this.isFactoryMethodUnique = false;
    }

    public boolean isFactoryMethod(Method candidate) {
        return candidate.getName().equals(getFactoryMethodName());
    }


    public void setResolvedFactoryMethod(@Nullable Method method) {
        this.factoryMethodToIntrospect = method;
    }

    @Nullable
    public Method getResolvedFactoryMethod() {
        return this.factoryMethodToIntrospect;
    }

    public void registerExternallyManagedConfigMember(Member configMember) {
        synchronized (this.postProcessingLock) {
            if (this.externallyManagedConfigMembers == null) {
                this.externallyManagedConfigMembers = new HashSet<>(1);
            }
            this.externallyManagedConfigMembers.add(configMember);
        }
    }


    public boolean isExternallyManagedConfigMember(Member configMember) {
        synchronized (this.postProcessingLock) {
            return (this.externallyManagedConfigMembers != null &&
                    this.externallyManagedConfigMembers.contains(configMember));
        }
    }

    public void registerExternallyManagedInitMethod(String initMethod) {
        synchronized (this.postProcessingLock) {
            if (this.externallyManagedInitMethods == null) {
                this.externallyManagedInitMethods = new HashSet<>(1);
            }
            this.externallyManagedInitMethods.add(initMethod);
        }
    }

    public boolean isExternallyManagedInitMethod(String initMethod) {
        synchronized (this.postProcessingLock) {
            return (this.externallyManagedInitMethods != null &&
                    this.externallyManagedInitMethods.contains(initMethod));
        }
    }

    public void registerExternallyManagedDestroyMethod(String destroyMethod) {
        synchronized (this.postProcessingLock) {
            if (this.externallyManagedDestroyMethods == null) {
                this.externallyManagedDestroyMethods = new HashSet<>(1);
            }
            this.externallyManagedDestroyMethods.add(destroyMethod);
        }
    }

    public boolean isExternallyManagedDestroyMethod(String destroyMethod) {
        synchronized (this.postProcessingLock) {
            return (this.externallyManagedDestroyMethods != null &&
                    this.externallyManagedDestroyMethods.contains(destroyMethod));
        }
    }



    @Override
    public RootBeanDefinition cloneBeanDefinition() {
        return new RootBeanDefinition(this);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return (this == other || (other instanceof RootBeanDefinition && super.equals(other)));
    }

    @Override
    public String toString() {
        return "Root bean: " + super.toString();
    }








}
