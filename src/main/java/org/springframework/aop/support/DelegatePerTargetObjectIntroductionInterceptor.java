package org.springframework.aop.support;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.ProxyMethodInvocation;

import java.util.Map;
import java.util.WeakHashMap;

public class DelegatePerTargetObjectIntroductionInterceptor extends IntroductionInfoSupport
implements IntroductionInterceptor{

    private final Map<Object, Object> delegateMap = new WeakHashMap<Object, Object>();

    private Class<?> defaultImplType;

    private Class<?> interfaceType;

    public DelegatePerTargetObjectIntroductionInterceptor(Class<?> defaultImplType, Class<?> interfaceType) {
        this.defaultImplType = defaultImplType;
        this.interfaceType = interfaceType;
        // Create a new delegate now (but don't store it in the map).
        // We do this for two reasons:
        // 1) to fail early if there is a problem instantiating delegates
        // 2) to populate the interface map once and once only
        Object delegate = createNewDelegate();
        implementInterfacesOnObject(delegate);
        suppressInterface(IntroductionInterceptor.class);
        suppressInterface(DynamicIntroductionAdvice.class);
    }

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        if (isMethodOnIntroducedInterface(mi)) {
            Object delegate = getIntroductionDelegateFor(mi.getThis());

            // Using the following method rather than direct reflection,
            // we get correct handling of InvocationTargetException
            // if the introduced method throws an exception.
            Object retVal = AopUtils.invokeJoinpointUsingReflection(delegate, mi.getMethod(), mi.getArguments());

            // Massage return value if possible: if the delegate returned itself,
            // we really want to return the proxy.
            if (retVal == delegate && mi instanceof ProxyMethodInvocation) {
                retVal = ((ProxyMethodInvocation) mi).getProxy();
            }
            return retVal;
        }

        return doProceed(mi);
    }

    protected Object doProceed(MethodInvocation mi) throws Throwable {
        // If we get here, just pass the invocation on.
        return mi.proceed();
    }

    private Object getIntroductionDelegateFor(Object targetObject) {
        synchronized (this.delegateMap) {
            if (this.delegateMap.containsKey(targetObject)) {
                return this.delegateMap.get(targetObject);
            }
            else {
                Object delegate = createNewDelegate();
                this.delegateMap.put(targetObject, delegate);
                return delegate;
            }
        }
    }

    private Object createNewDelegate() {
        try {
            return this.defaultImplType.newInstance();
        }
        catch (Throwable ex) {
            throw new IllegalArgumentException("Cannot create default implementation for '" +
                    this.interfaceType.getName() + "' mixin (" + this.defaultImplType.getName() + "): " + ex);
        }
    }


}
