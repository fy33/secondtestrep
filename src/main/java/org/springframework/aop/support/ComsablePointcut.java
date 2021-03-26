package org.springframework.aop.support;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;

public class ComsablePointcut implements Pointcut,Serializable {

    private static final long serialVersionUID=-2743223737633663832L;

    private ClassFilter classFilter;

    private MethodMatcher methodMatcher;

    public ComposablePointcut()
    {
        this.classFilter=ClassFilter.TRUE;
        this.methodMatcher=MethodMatcher.TRUE;
    }

    public ComposablePointcut(ClassFilter classFilter)
    {
        Assert.notNull(classFilter,"ClassFilter must not be null");
        this.classFilter=classFilter;
        this.methodMatcher=MethodMatcher.TRUE;
    }

    public ComposablePointcut(MethodMatcher methodMatcher)
    {
        Assert.notNull(methodMatcher,"MethodMatcher must not be null");
        this.classFilter=ClassFilter.TRUE;
        this.methodMatcher=methodMatcher;
    }

    public ComposablePointcut(ClassFilter classFilter,MethodMatcher methodMatcher)
    {
        Assert.notNull(ClassFilter,"ClassFilter must not be null");
        Assert.notNull(methodMatcher,"MethodMatcher must not be null");
        this.classFilter=classFilter;
        this.methodMatcher=methodMatcher;
    }

    public ComposablePointcut union(ClassFilter other)
    {
        this.classFilter=ClassFilter.union(this.classFilter,other);
        return this;
    }

    public ComposablePointcut intersection(ClassFilter other)
    {
        this.classFilter=ClassFilter.intersection(this.classFilter,other);
        return this;
    }

    public ComposablePointcut union(MethodMatcher other)
    {
        this.methodMatcher=MethodMatchers.union(this.methodMatcher,other);
        return this;
    }

    public ComposablePointcut intersection(MethodMatcher other)
    {
        this.methodMatcher=MethodMatchers.intersection(this.methodMatcher,other);
        return this;
    }

    public ComposablePointcut union(Pointcut other)
    {
        this.methodMatcher=MethodMatcher.union(
                this.methodMatcher,this.classFilter,other.getMethodMatcher(),other.getClassFilter());
        this.classFilter=ClassFilters.union(this.classFilter,other.getClassFilter());
        return this;
    }

    public ComposablePointcut intersection(Pointcut other)
    {
        this.classFilter=ClassFilters.intersection(this.classFilter,other.getClassFilter());
        this.methodMatcher=MethodMatchers.intersection(this.methodMatcher,other.getMethodMatcher());
        return this;
    }

    @Override
    public ClassFilter getClassFilter()
    {
        return this.classFilter;
    }

    @Override
    public MethodMatcher getMethodMatcher()
    {
        return this.methodMatcher;
    }

    @Override
    public boolean equals(Object other)
    {
        if(this==other)
        {
            return true;
        }
        if(!(other instanceof ComposablePointcut))
        {
            return false;
        }
        ComposablePointcut that=(ComposablePointcut)other;
        return ObjectUtils.nullSafeEquals(that.classFilter,this.classFilter)&&
                ObjectUtils.nullSafeEquals(that.methodMatcher,this.methodMatcher);
    }

    @Override
    public int hashCode()
    {
        int code=17;
        if (this.classFilter != null) {
            code = 37 * code + this.classFilter.hashCode();
        }
        if (this.methodMatcher != null) {
            code = 37 * code + this.methodMatcher.hashCode();
        }
        return code;

    }

    @Override
    public String toString() {
        return "ComposablePointcut: " + this.classFilter + ", " +this.methodMatcher;
    }

}
