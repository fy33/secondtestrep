package org.springframework.aop.support;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;

import java.io.Serializable;

public class DefaultPointcutAdvisor extends AbstractGenericPointcutAdvisor implements Serializable {

    private Pointcut pointcut = Pointcut.TRUE;

    public DefaultPointcutAdvisor() {
    }

    public DefaultPointcutAdvisor(Advice advice) {
        this(Pointcut.TRUE, advice);
    }

    public DefaultPointcutAdvisor(Pointcut pointcut, Advice advice) {
        this.pointcut = pointcut;
        setAdvice(advice);
    }

    public void setPointcut(Pointcut pointcut) {
        this.pointcut = (pointcut != null ? pointcut : Pointcut.TRUE);
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": pointcut [" + getPointcut() + "]; advice [" + getAdvice() + "]";
    }

}
