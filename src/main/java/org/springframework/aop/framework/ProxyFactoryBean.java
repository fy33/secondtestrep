package org.springframework.aop.framework;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.Interceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.UnknownAdviceTypeException;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProxyFactoryBean extends ProxyCreatorSupport
implements FactoryBean<Object>, BeanClassLoaderAware, BeanFactoryAware {

    public static final String GLOBAL_SUFFIX = "*";


    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private String[] interceptorNames;

    @Nullable
    private String targetName;

    private boolean autodetectInterfaces = true;

    private boolean singleton = true;

    private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

    private boolean freezeProxy = false;

    @Nullable
    private transient ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

    private transient boolean classLoaderConfigured = false;

    @Nullable
    private transient BeanFactory beanFactory;

    /** Whether the advisor chain has already been initialized. */
    private boolean advisorChainInitialized = false;

    /** If this is a singleton, the cached singleton proxy instance. */
    @Nullable
    private Object singletonInstance;

    public void setProxyInterfaces(Class<?>[] proxyInterfaces) throws ClassNotFoundException {
        setInterfaces(proxyInterfaces);
    }

    public void setInterceptorNames(String... interceptorNames) {
        this.interceptorNames = interceptorNames;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public void setAutodetectInterfaces(boolean autodetectInterfaces) {
        this.autodetectInterfaces = autodetectInterfaces;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
        this.advisorAdapterRegistry = advisorAdapterRegistry;
    }

    @Override
    public void setFrozen(boolean frozen) {
        this.freezeProxy = frozen;
    }

    public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
        this.proxyClassLoader = classLoader;
        this.classLoaderConfigured = (classLoader != null);
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        if (!this.classLoaderConfigured) {
            this.proxyClassLoader = classLoader;
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        checkInterceptorNames();
    }

    @Override
    @Nullable
    public Object getObject() throws BeansException {
        initializeAdvisorChain();
        if (isSingleton()) {
            return getSingletonInstance();
        }
        else {
            if (this.targetName == null) {
                logger.info("Using non-singleton proxies with singleton targets is often undesirable. " +
                        "Enable prototype proxies by setting the 'targetName' property.");
            }
            return newPrototypeInstance();
        }
    }

    @Override
    public Class<?> getObjectType() {
        synchronized (this) {
            if (this.singletonInstance != null) {
                return this.singletonInstance.getClass();
            }
        }
        Class<?>[] ifcs = getProxiedInterfaces();
        if (ifcs.length == 1) {
            return ifcs[0];
        }
        else if (ifcs.length > 1) {
            return createCompositeInterface(ifcs);
        }
        else if (this.targetName != null && this.beanFactory != null) {
            return this.beanFactory.getType(this.targetName);
        }
        else {
            return getTargetClass();
        }
    }

    @Override
    public boolean isSingleton() {
        return this.singleton;
    }

    protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
        return ClassUtils.createCompositeInterface(interfaces, this.proxyClassLoader);
    }

    private synchronized Object getSingletonInstance() {
        if (this.singletonInstance == null) {
            this.targetSource = freshTargetSource();
            if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
                // Rely on AOP infrastructure to tell us what interfaces to proxy.
                Class<?> targetClass = getTargetClass();
                if (targetClass == null) {
                    throw new FactoryBeanNotInitializedException("Cannot determine target class for proxy");
                }
                setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
            }
            // Initialize the shared singleton instance.
            super.setFrozen(this.freezeProxy);
            this.singletonInstance = getProxy(createAopProxy());
        }
        return this.singletonInstance;
    }

    private synchronized Object newPrototypeInstance() {
        // In the case of a prototype, we need to give the proxy
        // an independent instance of the configuration.
        // In this case, no proxy will have an instance of this object's configuration,
        // but will have an independent copy.
        ProxyCreatorSupport copy = new ProxyCreatorSupport(getAopProxyFactory());

        // The copy needs a fresh advisor chain, and a fresh TargetSource.
        TargetSource targetSource = freshTargetSource();
        copy.copyConfigurationFrom(this, targetSource, freshAdvisorChain());
        if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
            // Rely on AOP infrastructure to tell us what interfaces to proxy.
            Class<?> targetClass = targetSource.getTargetClass();
            if (targetClass != null) {
                copy.setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
            }
        }
        copy.setFrozen(this.freezeProxy);

        return getProxy(copy.createAopProxy());
    }

    protected Object getProxy(AopProxy aopProxy) {
        return aopProxy.getProxy(this.proxyClassLoader);
    }


    private void checkInterceptorNames() {
        if (!ObjectUtils.isEmpty(this.interceptorNames)) {
            String finalName = this.interceptorNames[this.interceptorNames.length - 1];
            if (this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
                // The last name in the chain may be an Advisor/Advice or a target/TargetSource.
                // Unfortunately we don't know; we must look at type of the bean.
                if (!finalName.endsWith(GLOBAL_SUFFIX) && !isNamedBeanAnAdvisorOrAdvice(finalName)) {
                    // The target isn't an interceptor.
                    this.targetName = finalName;
                    if (logger.isDebugEnabled()) {
                        logger.debug("Bean with name '" + finalName + "' concluding interceptor chain " +
                                "is not an advisor class: treating it as a target or TargetSource");
                    }
                    this.interceptorNames = Arrays.copyOf(this.interceptorNames, this.interceptorNames.length - 1);
                }
            }
        }
    }

    private boolean isNamedBeanAnAdvisorOrAdvice(String beanName) {
        Assert.state(this.beanFactory != null, "No BeanFactory set");
        Class<?> namedBeanClass = this.beanFactory.getType(beanName);
        if (namedBeanClass != null) {
            return (Advisor.class.isAssignableFrom(namedBeanClass) || Advice.class.isAssignableFrom(namedBeanClass));
        }
        // Treat it as an target bean if we can't tell.
        if (logger.isDebugEnabled()) {
            logger.debug("Could not determine type of bean with name '" + beanName +
                    "' - assuming it is neither an Advisor nor an Advice");
        }
        return false;
    }

    private synchronized void initializeAdvisorChain() throws AopConfigException, BeansException {
        if (this.advisorChainInitialized) {
            return;
        }

        if (!ObjectUtils.isEmpty(this.interceptorNames)) {
            if (this.beanFactory == null) {
                throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
                        "- cannot resolve interceptor names " + Arrays.asList(this.interceptorNames));
            }

            // Globals can't be last unless we specified a targetSource using the property...
            if (this.interceptorNames[this.interceptorNames.length - 1].endsWith(GLOBAL_SUFFIX) &&
                    this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
                throw new AopConfigException("Target required after globals");
            }

            // Materialize interceptor chain from bean names.
            for (String name : this.interceptorNames) {
                if (name.endsWith(GLOBAL_SUFFIX)) {
                    if (!(this.beanFactory instanceof ListableBeanFactory)) {
                        throw new AopConfigException(
                                "Can only use global advisors or interceptors with a ListableBeanFactory");
                    }
                    addGlobalAdvisors((ListableBeanFactory) this.beanFactory,
                            name.substring(0, name.length() - GLOBAL_SUFFIX.length()));
                }

                else {
                    // If we get here, we need to add a named interceptor.
                    // We must check if it's a singleton or prototype.
                    Object advice;
                    if (this.singleton || this.beanFactory.isSingleton(name)) {
                        // Add the real Advisor/Advice to the chain.
                        advice = this.beanFactory.getBean(name);
                    }
                    else {
                        // It's a prototype Advice or Advisor: replace with a prototype.
                        // Avoid unnecessary creation of prototype bean just for advisor chain initialization.
                        advice = new PrototypePlaceholderAdvisor(name);
                    }
                    addAdvisorOnChainCreation(advice);
                }
            }
        }

        this.advisorChainInitialized = true;
    }

    private List<Advisor> freshAdvisorChain() {
        Advisor[] advisors = getAdvisors();
        List<Advisor> freshAdvisors = new ArrayList<>(advisors.length);
        for (Advisor advisor : advisors) {
            if (advisor instanceof PrototypePlaceholderAdvisor) {
                PrototypePlaceholderAdvisor pa = (PrototypePlaceholderAdvisor) advisor;
                if (logger.isDebugEnabled()) {
                    logger.debug("Refreshing bean named '" + pa.getBeanName() + "'");
                }
                // Replace the placeholder with a fresh prototype instance resulting from a getBean lookup
                if (this.beanFactory == null) {
                    throw new IllegalStateException("No BeanFactory available anymore (probably due to " +
                            "serialization) - cannot resolve prototype advisor '" + pa.getBeanName() + "'");
                }
                Object bean = this.beanFactory.getBean(pa.getBeanName());
                Advisor refreshedAdvisor = namedBeanToAdvisor(bean);
                freshAdvisors.add(refreshedAdvisor);
            }
            else {
                // Add the shared instance.
                freshAdvisors.add(advisor);
            }
        }
        return freshAdvisors;
    }

    private void addGlobalAdvisors(ListableBeanFactory beanFactory, String prefix) {
        String[] globalAdvisorNames =
                BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Advisor.class);
        String[] globalInterceptorNames =
                BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Interceptor.class);
        if (globalAdvisorNames.length > 0 || globalInterceptorNames.length > 0) {
            List<Object> beans = new ArrayList<>(globalAdvisorNames.length + globalInterceptorNames.length);
            for (String name : globalAdvisorNames) {
                if (name.startsWith(prefix)) {
                    beans.add(beanFactory.getBean(name));
                }
            }
            for (String name : globalInterceptorNames) {
                if (name.startsWith(prefix)) {
                    beans.add(beanFactory.getBean(name));
                }
            }
            AnnotationAwareOrderComparator.sort(beans);
            for (Object bean : beans) {
                addAdvisorOnChainCreation(bean);
            }
        }
    }

    private void addAdvisorOnChainCreation(Object next) {
        // We need to convert to an Advisor if necessary so that our source reference
        // matches what we find from superclass interceptors.
        addAdvisor(namedBeanToAdvisor(next));
    }

    private TargetSource freshTargetSource() {
        if (this.targetName == null) {
            // Not refreshing target: bean name not specified in 'interceptorNames'
            return this.targetSource;
        }
        else {
            if (this.beanFactory == null) {
                throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
                        "- cannot resolve target with name '" + this.targetName + "'");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Refreshing target with name '" + this.targetName + "'");
            }
            Object target = this.beanFactory.getBean(this.targetName);
            return (target instanceof TargetSource ? (TargetSource) target : new SingletonTargetSource(target));
        }
    }

    private Advisor namedBeanToAdvisor(Object next) {
        try {
            return this.advisorAdapterRegistry.wrap(next);
        }
        catch (UnknownAdviceTypeException ex) {
            // We expected this to be an Advisor or Advice,
            // but it wasn't. This is a configuration error.
            throw new AopConfigException("Unknown advisor type " + next.getClass() +
                    "; can only include Advisor or Advice type beans in interceptorNames chain " +
                    "except for last entry which may also be target instance or TargetSource", ex);
        }
    }

    @Override
    protected void adviceChanged() {
        super.adviceChanged();
        if (this.singleton) {
            logger.debug("Advice has changed; re-caching singleton instance");
            synchronized (this) {
                this.singletonInstance = null;
            }
        }
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Rely on default serialization; just initialize state after deserialization.
        ois.defaultReadObject();

        // Initialize transient fields.
        this.proxyClassLoader = ClassUtils.getDefaultClassLoader();
    }

    private static class PrototypePlaceholderAdvisor implements Advisor, Serializable {

        private final String beanName;

        private final String message;

        public PrototypePlaceholderAdvisor(String beanName) {
            this.beanName = beanName;
            this.message = "Placeholder for prototype Advisor/Advice with bean name '" + beanName + "'";
        }

        public String getBeanName() {
            return this.beanName;
        }

        @Override
        public Advice getAdvice() {
            throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
        }

        @Override
        public boolean isPerInstance() {
            throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
        }

        @Override
        public String toString() {
            return this.message;
        }
    }
}
