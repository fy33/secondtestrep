package org.springframework.aop.support;

import org.springframework.aop.Pointcut;

public class DefaultBeanFactoryPointcutAdvisor extends AbstractBeanFactoryPointcutAdvisor {

    private Pointcut pointcut = Pointcut.TRUE;

    public void setPointcut(Pointcut pointcut)
    {
        this.pointcut=((pointcut!=null?pointcut:Pointcut.TRUE);
    }

    @Override
    public Pointcut getPointcut()
    {
        return this.pointcut;
    }

    @Override
    public String toString()
    {
        return getClass().getName()+": pointcut ["+getPointcut()+"];advice bean '"+getAdviceBeanName()+"'";
    }
}
