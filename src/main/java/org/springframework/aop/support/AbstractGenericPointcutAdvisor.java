package org.springframework.aop.support;

import org.aopalliance.aop.Advice;

public class AbstractGenericPointcutAdvisor extends AbstractPointcutAdvisor{

    private Advice advice;

    public void setAdvice(Advice advice)
    {
        this.advice = advice;
    }

    @Override
    public Advice getAdvice()
    {
        return this.advice;
    }

    @Override
    public String toString()
    {
        return getClass().getName()+":advice["+getAdvice()+"]";
    }


}
