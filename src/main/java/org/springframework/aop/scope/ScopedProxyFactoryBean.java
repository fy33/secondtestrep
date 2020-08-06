package org.springframework.aop.scope;

import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.aop.target.SimpleBeanTargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Modifier;

public class ScopedProxyFactoryBean extends ProxyConfig
implements FactoryBean<Object>, BeanFactoryAware, AopInfrastructureBean {

    /** The TargetSource that manages scoping. */
    private final SimpleBeanTargetSource scopedTargetSource = new SimpleBeanTargetSource();

    /** The name of the target bean. */
    @Nullable
    private String targetBeanName;

    /** The cached singleton proxy. */
    @Nullable
    private Object proxy;

    public ScopedProxyFactoryBean() {
        setProxyTargetClass(true);
    }

    public void setTargetBeanName(String targetBeanName) {
        this.targetBeanName = targetBeanName;
        this.scopedTargetSource.setTargetBeanName(targetBeanName);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (!(beanFactory instanceof ConfigurableBeanFactory)) {
            throw new IllegalStateException("Not running in a ConfigurableBeanFactory: " + beanFactory);
        }
        ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) beanFactory;

        this.scopedTargetSource.setBeanFactory(beanFactory);

        ProxyFactory pf = new ProxyFactory();
        pf.copyFrom(this);
        pf.setTargetSource(this.scopedTargetSource);

        Assert.notNull(this.targetBeanName, "Property 'targetBeanName' is required");
        Class<?> beanType = beanFactory.getType(this.targetBeanName);
        if (beanType == null) {
            throw new IllegalStateException("Cannot create scoped proxy for bean '" + this.targetBeanName +
                    "': Target type could not be determined at the time of proxy creation.");
        }
        if (!isProxyTargetClass() || beanType.isInterface() || Modifier.isPrivate(beanType.getModifiers())) {
            pf.setInterfaces(ClassUtils.getAllInterfacesForClass(beanType, cbf.getBeanClassLoader()));
        }

        // Add an introduction that implements only the methods on ScopedObject.
        ScopedObject scopedObject = new DefaultScopedObject(cbf, this.scopedTargetSource.getTargetBeanName());
        pf.addAdvice(new DelegatingIntroductionInterceptor(scopedObject));

        // Add the AopInfrastructureBean marker to indicate that the scoped proxy
        // itself is not subject to auto-proxying! Only its target bean is.
        pf.addInterface(AopInfrastructureBean.class);

        this.proxy = pf.getProxy(cbf.getBeanClassLoader());
    }

    @Override
    public Object getObject() {
        if (this.proxy == null) {
            throw new FactoryBeanNotInitializedException();
        }
        return this.proxy;
    }

    @Override
    public Class<?> getObjectType() {
        if (this.proxy != null) {
            return this.proxy.getClass();
        }
        return this.scopedTargetSource.getTargetClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }



}
