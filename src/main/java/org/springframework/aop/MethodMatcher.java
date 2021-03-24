package org.springframework.aop;

import java.lang.reflect.Method;

public interface MethodMatcher {

    boolean  matchers(Method method , Class<?> targetClass);

    boolean isRuntime();

    boolean matcher(Method method, Class<?> targetClass, Object... args);

    MethodMatcher TRUE=TrueMethodMatcher.INSTANCE;
}
