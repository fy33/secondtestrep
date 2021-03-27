package org.springframework.aop.support;

import org.springframework.aop.ClassFilter;

import java.io.Serializable;

public class RootClassFilter extends ClassFilter,Serializable {
    private Class<?> clazz;

    public RootClassFilter(Class<?> clazz)
    {
        this.clazz=clazz;
    }

    @Override
    public boolean matches(Class<?> candidate)
    {
        return clazz.isAssignableFrom(candidate);
    }
}
