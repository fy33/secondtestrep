package org.springframework.aop.support;

import org.springframework.aop.MethodMatcher;

import java.lang.reflect.Method;

public abstract class DynamicMethodMatcher implements MethodMatcher {

    @Override
    public  final boolean isRuntime()
    {
        return true;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass)
    {
        return true;
    }
}
