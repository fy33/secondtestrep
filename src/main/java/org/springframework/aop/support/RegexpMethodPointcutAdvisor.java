package org.springframework.aop.support;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;

public class RegexpMethodPointcutAdvisor extends AbstractGenericPointcutAdvisor {

    private String[] patterns;

    private AbstractRegexpMethodPointcut pointcut;

    private final Object pointcutMonitor = new RegexpMethodPointcutAdvisor.SerializableMonitor();

    public RegexpMethodPointcutAdvisor() {
    }

    public RegexpMethodPointcutAdvisor(Advice advice) {
        setAdvice(advice);
    }

    public RegexpMethodPointcutAdvisor(String pattern, Advice advice) {
        setPattern(pattern);
        setAdvice(advice);
    }

    public RegexpMethodPointcutAdvisor(String[] patterns, Advice advice) {
        setPatterns(patterns);
        setAdvice(advice);
    }

    public void setPattern(String pattern) {
        setPatterns(pattern);
    }

    public void setPatterns(String... patterns) {
        this.patterns = patterns;
    }

    @Override
    public Pointcut getPointcut() {
        synchronized (this.pointcutMonitor) {
            if (this.pointcut == null) {
                this.pointcut = createPointcut();
                this.pointcut.setPatterns(this.patterns);
            }
            return pointcut;
        }
    }

    protected AbstractRegexpMethodPointcut createPointcut() {
        return new JdkRegexpMethodPointcut();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": advice [" + getAdvice() +
                "], pointcut patterns " + ObjectUtils.nullSafeToString(this.patterns);
    }

    private static class SerializableMonitor implements Serializable {
    }


}
