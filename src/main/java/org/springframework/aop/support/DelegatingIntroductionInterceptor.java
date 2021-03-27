package org.springframework.aop.support;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.util.Assert;

public class DelegatingIntroductionInterceptor extends IntroductionInfoSupport implements
        IntroductionInterceptor {

    private Object delegate;

    private Object createNewDelegate() {
        try {
            return this.defaultImplType.newInstance();
        }
        catch (Throwable ex) {
            throw new IllegalArgumentException("Cannot create default implementation for '" +
                    this.interfaceType.getName() + "' mixin (" + this.defaultImplType.getName() + "): " + ex);
        }
    }

    public DelegatingIntroductionInterceptor(Object delegate) {
        init(delegate);
    }

    protected DelegatingIntroductionInterceptor() {
        init(this);
    }

    private void init(Object delegate) {
        Assert.notNull(delegate, "Delegate must not be null");
        this.delegate = delegate;
        implementInterfacesOnObject(delegate);

        // We don't want to expose the control interface
        suppressInterface(IntroductionInterceptor.class);
        suppressInterface(DynamicIntroductionAdvice.class);
    }

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        if (isMethodOnIntroducedInterface(mi)) {
            // Using the following method rather than direct reflection, we
            // get correct handling of InvocationTargetException
            // if the introduced method throws an exception.
            Object retVal = AopUtils.invokeJoinpointUsingReflection(this.delegate, mi.getMethod(), mi.getArguments());

            // Massage return value if possible: if the delegate returned itself,
            // we really want to return the proxy.
            if (retVal == this.delegate && mi instanceof ProxyMethodInvocation) {
                Object proxy = ((ProxyMethodInvocation) mi).getProxy();
                if (mi.getMethod().getReturnType().isInstance(proxy)) {
                    retVal = proxy;
                }
            }
            return retVal;
        }

        return doProceed(mi);
    }

    protected Object doProceed(MethodInvocation mi) throws Throwable {
        // If we get here, just pass the invocation on.
        return mi.proceed();
    }

}
