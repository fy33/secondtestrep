package org.springframework.aop.support;

import net.bytebuddy.asm.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.util.Assert;

import java.io.Serializable;

public abstract class StaticMethodmatcherPointcutAdvisor extends StaticMethodMatcherPointcut implements PointcutAdvisor,Ordered,Serializable{
    private Advice advice=EMPTY_ADVICE;

    private int order=Ordered.LOWEST_PRECEDENCE;

    public StaticMethodMatcherPointcutAdvisor()
    {

    }

    public StaticMethodMatcherPointcutAdvisor(Advice advice)
    {
        Assert.notNull(advice,"Advice must not be null");
        this.advice=advice;

    }

    public void setOrder(int order)
    {
        this.order=order;
    }

    @Override
    public int getOrder()
    {
        return this.order;
    }

    public void setAdvice(Advice advice)
    {
        this.advice=advice;
    }

    @Override
    public Advice getAdvice()
    {
        return this.advice;
    }

    @Override
    public boolean isPerinstance()
    {
        return true;
    }

    @Override
    public Pointcut getPointcut()
    {
        return this;
    }

}
