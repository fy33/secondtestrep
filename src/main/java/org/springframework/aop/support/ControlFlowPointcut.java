package org.springframework.aop.support;

import jdk.nashorn.api.scripting.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.lang.reflect.Method;

public class ControlFlowPointcut implements Pointcut,ClassFilter,MethodMatcher,Serializable {

    private Class<?> clazz;

    private String methodName;

    private volatile int evaluations;

    public ControlFlowPointcut(Class<?> clazz)
    {
        this(clazz,null);
    }

    public ControlFlowPointcut(Class<?> clazz,String methodName)
    {
        Assert.notNull(clazz,"Class must not be null");
        this.clazz=clazz;
        this.methodName=methodName;
    }

    @Override
    public boolean matches(Class<?> clazz)
    {
        return true;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass)
    {
        return true;

    }

    @Override
    public boolean isRuntime()
    {
        return true;
    }

    @Override
    public boolean matches(Method method,Class<?> targetClass,Object... args)
    {
        this.evaluations++;
        for (StackTraceElement element : new Throwable().getStackTrace()) {
            if(element.getClassName().equals(this.clazz.getName())&&
                    (this.methodName==null||element.getMethodName().equals(this.methodName)))
            {
                return true;
            }
        }
        return false;
    }

    public int getEvaluations()
    {
        return this.evaluations;
    }

    @Override
    public ClassFilter getClassFilter()
    {
        return this;
    }

    @Override
    public MethodMatcher getMethodMatcher()
    {
        return this;
    }

    @Override
    public boolean equals(Object other)
    {
        if(this==other)
        {
            return true;
        }
        if(!(other instanceof ControlFlowPointcut))
        {
            return false;
        }
        ControlFlowPointcut that = (ControlFlowPointcut) (other);
        return (this.clazz.equals(that.clazz))&& ObjectUtils.nullSafeEquals(that.methodName,this.methodName);

    }

    @Override
    public int hashCode() {
        int code = this.clazz.hashCode();
        if (this.methodName != null) {
            code = 37 * code + this.methodName.hashCode();
        }
        return code;
    }
}
