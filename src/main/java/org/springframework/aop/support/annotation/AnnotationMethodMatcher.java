package org.springframework.aop.support.annotation;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AnnotationMethodMatcher extends StaticMethodMatcher {

    private final Class<? extends Annotation> annotationType;

    public AnnotationMethodMatcher(Class<? extends Annotation> annotationType)
    {
        Assert.notNull(annotationType,"Annotation type must not be null");
        this.annotationType=annotationType;
    }

    public boolean matches(Method method,Class<?> targetClass)
    {
        if(method.isAnnotationPresent(this.annotationType))
        {
            return true;
        }else {
            Method specificMethod= AopUtils.getMethodSpecificMethod(method,targetClass);
            return specificMethod!=method&&specificMethod.isAnnotationPresent(this.annotationType);
        }
    }

    public boolean equals(Object other)
    {
        if(this==other)
        {
            return true;
        }else if(!(other instanceof AnnotationMethodMatcher))
        {
            return false;
        }else{
            AnnotationMethodMatcher otherMm=(AnnotationMethodMatcher)other;
            return this.annotationType.equals(otherMm.annotationType);
        }


    }

    public int hashCode()
    {
        return this.annotationType.hashCode();
    }

    public String toString()
    {
        return this.getClass().getName()+":"+this.annotationType;
    }
}
