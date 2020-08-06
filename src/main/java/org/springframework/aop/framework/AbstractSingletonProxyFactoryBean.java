package org.springframework.aop.framework;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

public abstract class AbstractSingletonProxyFactoryBean extends ProxyConfig
implements FactoryBean<Object>, BeanClassLoaderAware, InitializingBean {
    @Nullable
    private Object target;

    @Nullable
    private Class<?>[] proxyInterfaces;

    @Nullable
    private Object[] preInterceptors;

    @Nullable
    private Object[] postInterceptors;

    /** Default is global AdvisorAdapterRegistry. */
    private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

    @Nullable
    private transient ClassLoader proxyClassLoader;

    @Nullable
    private Object proxy;

    public void setTarget(Object target) {
        this.target = target;
    }

    public void setProxyInterfaces(Class<?>[] proxyInterfaces) {
        this.proxyInterfaces = proxyInterfaces;
    }

    public void setPreInterceptors(Object[] preInterceptors) {
        this.preInterceptors = preInterceptors;
    }

    public void setPostInterceptors(Object[] postInterceptors) {
        this.postInterceptors = postInterceptors;
    }

    public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
        this.advisorAdapterRegistry = advisorAdapterRegistry;
    }

    public void setProxyClassLoader(ClassLoader classLoader) {
        this.proxyClassLoader = classLoader;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        if (this.proxyClassLoader == null) {
            this.proxyClassLoader = classLoader;
        }
    }

    @Override
    public void afterPropertiesSet() {
        if (this.target == null) {
            throw new IllegalArgumentException("Property 'target' is required");
        }
        if (this.target instanceof String) {
            throw new IllegalArgumentException("'target' needs to be a bean reference, not a bean name as value");
        }
        if (this.proxyClassLoader == null) {
            this.proxyClassLoader = ClassUtils.getDefaultClassLoader();
        }

        ProxyFactory proxyFactory = new ProxyFactory();

        if (this.preInterceptors != null) {
            for (Object interceptor : this.preInterceptors) {
                proxyFactory.addAdvisor(this.advisorAdapterRegistry.wrap(interceptor));
            }
        }

        // Add the main interceptor (typically an Advisor).
        proxyFactory.addAdvisor(this.advisorAdapterRegistry.wrap(createMainInterceptor()));

        if (this.postInterceptors != null) {
            for (Object interceptor : this.postInterceptors) {
                proxyFactory.addAdvisor(this.advisorAdapterRegistry.wrap(interceptor));
            }
        }

        proxyFactory.copyFrom(this);

        TargetSource targetSource = createTargetSource(this.target);
        proxyFactory.setTargetSource(targetSource);

        if (this.proxyInterfaces != null) {
            proxyFactory.setInterfaces(this.proxyInterfaces);
        }
        else if (!isProxyTargetClass()) {
            // Rely on AOP infrastructure to tell us what interfaces to proxy.
            Class<?> targetClass = targetSource.getTargetClass();
            if (targetClass != null) {
                proxyFactory.setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
            }
        }

        postProcessProxyFactory(proxyFactory);

        this.proxy = proxyFactory.getProxy(this.proxyClassLoader);
    }

    protected TargetSource createTargetSource(Object target) {
        if (target instanceof TargetSource) {
            return (TargetSource) target;
        }
        else {
            return new SingletonTargetSource(target);
        }
    }

    protected void postProcessProxyFactory(ProxyFactory proxyFactory) {
    }

    @Override
    public Object getObject() {
        if (this.proxy == null) {
            throw new FactoryBeanNotInitializedException();
        }
        return this.proxy;
    }

    @Override
    @Nullable
    public Class<?> getObjectType() {
        if (this.proxy != null) {
            return this.proxy.getClass();
        }
        if (this.proxyInterfaces != null && this.proxyInterfaces.length == 1) {
            return this.proxyInterfaces[0];
        }
        if (this.target instanceof TargetSource) {
            return ((TargetSource) this.target).getTargetClass();
        }
        if (this.target != null) {
            return this.target.getClass();
        }
        return null;
    }

    @Override
    public final boolean isSingleton() {
        return true;
    }

    protected abstract Object createMainInterceptor();




}
