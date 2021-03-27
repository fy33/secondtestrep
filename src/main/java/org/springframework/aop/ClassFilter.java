package org.springframework.aop;

public interface ClassFilter {
    ClassFilter TRUE=TrueClassFilter.INSTANCE;

    boolean matches(Class<?> var1);
}
