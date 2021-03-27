package org.springframework.aop.support;

import org.aspectj.weaver.patterns.Pointcut;

public interface ExpressionPointcut extends Pointcut {

    String getExpression();

}
