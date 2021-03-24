package org.springframework.aop.support;

import org.springframework.util.PatternMatchUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NameMatchMethodPointcut extends StaticMethodMatcherPointcut implements Serializable {

    private List<String> mappedNames=new ArrayList<>();

    public void setMappedName(String mappedName)
    {
        setMappedNames(mappedName);
    }

    public void setMappedNames(String... mappedNames)
    {
        this.mappedNames=new ArrayList<>(Arrays.asList(mappedNames));
    }

    public NameMatchMethodPointcut addMethodName(String name)
    {
        this.mappedNames.add(name);
        return this;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass)
    {
        for (String mappedName : this.mappedNames) {
            if (mappedName.equals(method.getName()) || isMatch(method.getName(), mappedName)) {
                return true;

            }
        }
        return false;
    }

    protected  boolean isMatch(String methodName,String mappedName)
    {
        return PatternMatchUtils.simpleMatch(mappedName,methodName);
    }

    @Override
    public boolean equals(Object other)
    {
        return (this==other||(other instanceof NameMatchMethodPointcut&&
        this.mappedNames.equals(((NameMatchMethodPointcut)other).mappedNames)));
    }

    @Override
    public int hashCode()
    {
        return this.mappedNames.hashCode();
    }
}
