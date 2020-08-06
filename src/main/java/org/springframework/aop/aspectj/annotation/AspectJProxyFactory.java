package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJProxyUtils;
import org.springframework.aop.aspectj.SimpleAspectInstanceFactory;
import org.springframework.aop.framework.ProxyCreatorSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AspectJProxyFactory extends ProxyCreatorSupport {

    private static final Map<Class<?>, Object> aspectCache = new ConcurrentHashMap<>();

    private final AspectJAdvisorFactory aspectFactory = new ReflectiveAspectJAdvisorFactory();

    public AspectJProxyFactory() {
    }

    public AspectJProxyFactory(Object target) {
        Assert.notNull(target, "Target object must not be null");
        setInterfaces(ClassUtils.getAllInterfaces(target));
        setTarget(target);
    }

    public AspectJProxyFactory(Class<?>... interfaces) {
        setInterfaces(interfaces);
    }

    public void addAspect(Object aspectInstance) {
        Class<?> aspectClass = aspectInstance.getClass();
        String aspectName = aspectClass.getName();
        AspectMetadata am = createAspectMetadata(aspectClass, aspectName);
        if (am.getAjType().getPerClause().getKind() != PerClauseKind.SINGLETON) {
            throw new IllegalArgumentException(
                    "Aspect class [" + aspectClass.getName() + "] does not define a singleton aspect");
        }
        addAdvisorsFromAspectInstanceFactory(
                new SingletonMetadataAwareAspectInstanceFactory(aspectInstance, aspectName));
    }

    public void addAspect(Class<?> aspectClass) {
        String aspectName = aspectClass.getName();
        AspectMetadata am = createAspectMetadata(aspectClass, aspectName);
        MetadataAwareAspectInstanceFactory instanceFactory = createAspectInstanceFactory(am, aspectClass, aspectName);
        addAdvisorsFromAspectInstanceFactory(instanceFactory);
    }

    private void addAdvisorsFromAspectInstanceFactory(MetadataAwareAspectInstanceFactory instanceFactory) {
        List<Advisor> advisors = this.aspectFactory.getAdvisors(instanceFactory);
        Class<?> targetClass = getTargetClass();
        Assert.state(targetClass != null, "Unresolvable target class");
        advisors = AopUtils.findAdvisorsThatCanApply(advisors, targetClass);
        AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(advisors);
        AnnotationAwareOrderComparator.sort(advisors);
        addAdvisors(advisors);
    }

    private AspectMetadata createAspectMetadata(Class<?> aspectClass, String aspectName) {
        AspectMetadata am = new AspectMetadata(aspectClass, aspectName);
        if (!am.getAjType().isAspect()) {
            throw new IllegalArgumentException("Class [" + aspectClass.getName() + "] is not a valid aspect type");
        }
        return am;
    }

    private MetadataAwareAspectInstanceFactory createAspectInstanceFactory(
            AspectMetadata am, Class<?> aspectClass, String aspectName) {

        MetadataAwareAspectInstanceFactory instanceFactory;
        if (am.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
            // Create a shared aspect instance.
            Object instance = getSingletonAspectInstance(aspectClass);
            instanceFactory = new SingletonMetadataAwareAspectInstanceFactory(instance, aspectName);
        }
        else {
            // Create a factory for independent aspect instances.
            instanceFactory = new SimpleMetadataAwareAspectInstanceFactory(aspectClass, aspectName);
        }
        return instanceFactory;
    }

    private Object getSingletonAspectInstance(Class<?> aspectClass) {
        // Quick check without a lock...
        Object instance = aspectCache.get(aspectClass);
        if (instance == null) {
            synchronized (aspectCache) {
                // To be safe, check within full lock now...
                instance = aspectCache.get(aspectClass);
                if (instance == null) {
                    instance = new SimpleAspectInstanceFactory(aspectClass).getAspectInstance();
                    aspectCache.put(aspectClass, instance);
                }
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T> T getProxy() {
        return (T) createAopProxy().getProxy();
    }


    @SuppressWarnings("unchecked")
    public <T> T getProxy(ClassLoader classLoader) {
        return (T) createAopProxy().getProxy(classLoader);
    }






}
