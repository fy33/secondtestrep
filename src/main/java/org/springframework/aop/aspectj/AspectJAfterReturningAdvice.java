package org.springframework.aop.aspectj;

import com.sun.istack.Nullable;
import org.springframework.aop.AfterAdvice;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.util.ClassUtils;
import org.springframework.util.TypeUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class AspectJAfterReturningAdvice extends AbstractAspectJAdvice implements AfterReturningAdvice, AfterAdvice, Serializable {

    public AspectJAfterReturningAdvice(
            Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {

        super(aspectJBeforeAdviceMethod, pointcut, aif);
    }


    @Override
    public boolean isBeforeAdvice()
    {
        return false;
    }

    @Override
    public boolean isAfterAdvice()
    {
        return true;
    }

    @Override
    public  void setReturningName(String name)
    {
        setReturningNameNoCheck(name);
    }


    @Override
    public void afterReturning(@Nullable Object returnValue,Method method,Object[] args,@Nullable Object target)throws Throwable{
        if(shouldInvokeOnReturnValueOf(method,returnValue))
        {
            invokeAdviceMethod(getJoinPointMatch(),returnValue,null);
        }
    }

    private boolean shouldInvokeOnReturnValueOf(Method method ,@Nullable Object returnValue)
    {
        Class<?> type=getDiscoveredReturningType();
        Type genericType=getDiscoveredReturningType();
        return (matchesReturnValue(type,method,returnValue)&&(genericType==null||genericType==type||
                TypeUtils.isAssignable(genericType,method.getGenericReturnType())));


    }

    private boolean matchesReturnValue(Class<?> type,Method method,@Nullable Object returnValue)
    {
        if(returnValue!=null)
        {
            return ClassUtils.isAssignableValue(type,returnValue);
        }
        else if(Object.class==type&&void.class==method.getReturnType())
        {
            return true;
        }
        else
        {
            return ClassUtils.isAssignable(type,method.getReturnType()):
        }
    }
}
