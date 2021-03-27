package org.springframework.aop.support;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.IntroductionInfo;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IntroductionInfoSupport implements IntroductionInfo,Serializable {

    protected final Set<Class<?>> publishedInterfaces=new LinkedHashSet<Class<?>>();

    private transient Map<Method,Boolean> rememberedMethods=new ConcurrentHashMap<Method,Boolean>(32);

    public void suppressInterface(Class<?> intf)
    {
        this.publishedInterfaces.remove(intf);
    }

    @Override
    public Class<?>[] getInterfaces() {
        return this.publishedInterfaces.toArray(new Class<?>[this.publishedInterfaces.size()]);
    }

    public boolean implementsInterface(Class<?> ifc) {
        for (Class<?> pubIfc : this.publishedInterfaces) {
            if (ifc.isInterface() && ifc.isAssignableFrom(pubIfc)) {
                return true;
            }
        }
        return false;
    }

    protected void implementInterfacesOnObject(Object delegate) {
        this.publishedInterfaces.addAll(ClassUtils.getAllInterfacesAsSet(delegate));
    }

    protected final boolean isMethodOnIntroducedInterface(MethodInvocation mi) {
        Boolean rememberedResult = this.rememberedMethods.get(mi.getMethod());
        if (rememberedResult != null) {
            return rememberedResult;
        }
        else {
            // Work it out and cache it.
            boolean result = implementsInterface(mi.getMethod().getDeclaringClass());
            this.rememberedMethods.put(mi.getMethod(), result);
            return result;
        }
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Rely on default serialization; just initialize state after deserialization.
        ois.defaultReadObject();
        // Initialize transient fields.
        this.rememberedMethods = new ConcurrentHashMap<Method, Boolean>(32);
    }





}
