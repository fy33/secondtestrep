package org.springframework.beans;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.KotlinDetector;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

public abstract class BeanUtils {
    private static final Log logger= LogFactory.getLog(BeanUtils.class);
    private static final Set<Class<?>> unknownEditorTypes=
            Collections.newSetFromMap(new ConcurrentReferenceHashMap<>(64));

    @Deprecated
    public static <T> T instantiate(Class<T> clazz) throws BeanInstantiationException {
        Assert.notNull(clazz, "Class must not be null");
        if (clazz.isInterface()) {
            throw new BeanInstantiationException(clazz, "Specified class is an interface");
        }
        try {
            return clazz.newInstance();
        }
        catch (InstantiationException ex) {
            throw new BeanInstantiationException(clazz, "Is it an abstract class?", ex);
        }
        catch (IllegalAccessException ex) {
            throw new BeanInstantiationException(clazz, "Is the constructor accessible?", ex);
        }
    }
    public static <T> T instantiateClass(Class<T> clazz) throws BeanInstantiationException{
        Assert.notNull(clazz, "Class must not be null");
        if (clazz.isInterface()) {
            throw new BeanInstantiationException(clazz, "Specified class is an interface");
        }
        try {
            Constructor<T> ctor = (KotlinDetector.isKotlinType(clazz) ?
                    KotlinDelegate.getPrimaryConstructor(clazz) : clazz.getDeclaredConstructor());
            return instantiateClass(ctor);
        }
        catch (NoSuchMethodException ex) {
            throw new BeanInstantiationException(clazz, "No default constructor found", ex);
        }
        catch (LinkageError err) {
            throw new BeanInstantiationException(clazz, "Unresolvable class definition", err);
        }
    }
    public static <T> T instantiateClass(Class<?> clazz, Class<T> assignableTo) throws BeanInstantiationException {
        Assert.isAssignable(assignableTo, clazz);
        return (T) instantiateClass(clazz);
    }
    public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws BeanInstantiationException {
        Assert.notNull(ctor, "Constructor must not be null");
        try {
            ReflectionUtils.makeAccessible(ctor);
            return (KotlinDetector.isKotlinType(ctor.getDeclaringClass()) ?
                    KotlinDelegate.instantiateClass(ctor, args) : ctor.newInstance(args));
        }
        catch (InstantiationException ex) {
            throw new BeanInstantiationException(ctor, "Is it an abstract class?", ex);
        }
        catch (IllegalAccessException ex) {
            throw new BeanInstantiationException(ctor, "Is the constructor accessible?", ex);
        }
        catch (IllegalArgumentException ex) {
            throw new BeanInstantiationException(ctor, "Illegal arguments for constructor", ex);
        }
        catch (InvocationTargetException ex) {
            throw new BeanInstantiationException(ctor, "Constructor threw exception", ex.getTargetException());
        }
    }
    public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz){
        Assert.notNull(clazz, "Class must not be null");
        if (KotlinDetector.isKotlinType(clazz)) {
            Constructor<T> kotlinPrimaryConstructor = KotlinDelegate.findPrimaryConstructor(clazz);
            if (kotlinPrimaryConstructor != null) {
                return kotlinPrimaryConstructor;
            }
        }
        return null;
    }
    public static Method findMethod(Class<?> clazz,String methodName,Class<?>... paramTypes)
    {
        try {
            return clazz.getMethod(methodName, paramTypes);
        }
        catch (NoSuchMethodException ex) {
            return findDeclaredMethod(clazz, methodName, paramTypes);
        }
    }
    public static Method findDeclaredMethod(Class<?> clazz,String methodName,Class<?>... paramTypes)
    {
        try {
            return clazz.getDeclaredMethod(methodName, paramTypes);
        }
        catch (NoSuchMethodException ex) {
            if (clazz.getSuperclass() != null) {
                return findDeclaredMethod(clazz.getSuperclass(), methodName, paramTypes);
            }
            return null;
        }
    }
    public static Method findMethodWithMinimalParameters(Class<?> clazz,String methodName)
    throws IllegalArgumentException{
        Method targetMethod = findMethodWithMinimalParameters(clazz.getMethods(), methodName);
        if (targetMethod == null) {
            targetMethod = findDeclaredMethodWithMinimalParameters(clazz, methodName);
        }
        return targetMethod;
    }
    public static Method findDeclaredMethodWithMinimalParameters(Class<?> clazz, String methodName)
            throws IllegalArgumentException {

        Method targetMethod = findMethodWithMinimalParameters(clazz.getDeclaredMethods(), methodName);
        if (targetMethod == null && clazz.getSuperclass() != null) {
            targetMethod = findDeclaredMethodWithMinimalParameters(clazz.getSuperclass(), methodName);
        }
        return targetMethod;
    }
    public static Method findMethodWithMinimalParameters(Method[] methods,String methodName)
        throws IllegalArgumentException{
        Method targetMethod=null;
        int numMethodsFoundWithCurrentMinimumArgs=0;
        for (Method method : methods) {
            if(method.getName().equals(methodName))
            {
                int numParams=method.getParameterCount();
                if(targetMethod==null||numParams<targetMethod.getParameterCount())
                {
                    targetMethod=method;
                    numMethodsFoundWithCurrentMinimumArgs=1;
                }
                else if(!method.isBridge()&&targetMethod.getParameterCount()==numParams)
                {
                    if(targetMethod.isBridge())
                    {
                        targetMethod=method;
                    }
                    else {
                        numMethodsFoundWithCurrentMinimumArgs++;
                    }
                }
            }
        }
        if(numMethodsFoundWithCurrentMinimumArgs>1)
        {
            throw new IllegalArgumentException("Cannot resolve method '" + methodName +
                    "' to a unique method. Attempted to resolve to overloaded method with " +
                    "the least number of parameters but there were " +
                    numMethodsFoundWithCurrentMinimumArgs + " candidates.");
        }
        return targetMethod;
    }
}
