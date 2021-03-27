package org.springframework.aop;

import java.lang.reflect.Method;

public interface AfterReturiningAdvice extends AfterAdvice {

    void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable;

}
