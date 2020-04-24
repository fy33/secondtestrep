package org.springframework.beans.factory;

import org.springframework.lang.Nullable;

public interface HierarchicalBeanFactory extends BeanFactory {

    @Nullable
    BeanFactory getParentBeanFactory();

    boolean containsLocalBean(String name);
}
