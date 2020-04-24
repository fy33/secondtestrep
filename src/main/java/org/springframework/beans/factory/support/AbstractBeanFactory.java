package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

import java.beans.PropertyEditor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {
    @Nullable
    private BeanFactory parentBeanFactory;


    @Nullable
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


    @Nullable
    private ClassLoader tempClassLoader;


    private boolean cacheBeanMetadata = true;



    @Nullable
    private BeanExpressionResolver beanExpressionResolver;



    @Nullable
    private ConversionService conversionService;



    private final Set<PropertyEditorRegistrar> propertyEditorRegistrars = new LinkedHashSet<>(4);


    private final Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<>(4);



    @Nullable
    private TypeConverter typeConverter;


    private final List<StringValueResolver> embeddedValueResolvers = new CopyOnWriteArrayList<>();


    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();


    private volatile boolean hasInstantiationAwareBeanPostProcessors;



    private volatile boolean hasDestructionAwareBeanPostProcessors;

    private final Map<String, Scope> scopes = new LinkedHashMap<>(8);

    @Nullable
    private SecurityContextProvider securityContextProvider;


    private final Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256);

    private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256));


    private final ThreadLocal<Object> prototypesCurrentlyInCreation =
            new NamedThreadLocal<>("Prototype beans currently in creation");



    public AbstractBeanFactory() {
    }



    public AbstractBeanFactory(@Nullable BeanFactory parentBeanFactory) {
        this.parentBeanFactory = parentBeanFactory;
    }




    @Override
    public Object getBean(String name) throws BeansException {
        return doGetBean(name, null, null, false);
    }


























}
