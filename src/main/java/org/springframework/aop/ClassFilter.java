package org.springframework.aop;

public class ClassFilter {
    ClassFilter TRUE=TrueClassFilter.INSTANCE;

    boolean matches(Class<?> var1);
}
