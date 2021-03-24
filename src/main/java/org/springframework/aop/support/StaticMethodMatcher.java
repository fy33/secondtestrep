package org.springframework.aop.support;

import org.springframework.aop.MethodMatcher;

import java.lang.reflect.Method;

public abstract class StaticMethodMatcher implements MethodMatcher {
    @Override
    public final boolean isRuntime()
    {
        return false;
    }

    @Override
    public final boolean matches(Method method, Class<?> targetClass, Object... args)
    {
        throw new UnsupportedOperationException("Illegal MethodMatcherusage");
     }
}
