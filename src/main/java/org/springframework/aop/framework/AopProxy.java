package org.springframework.aop.framework;

import org.springframework.lang.Nullable;

public interface AopProxy {
    Object getProxy();

    Object getProxy(@Nullable ClassLoader classLoader);
}
