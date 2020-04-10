package org.springframework.boot.context.annotation;

import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public class UserConfigurations extends Configurations implements PriorityOrdered {
    protected UserConfigurations(Collection<Class<?>> classes)
    {
        super(classes);
    }
    @Override
    public int getOrder()
    {
        return Ordered.LOWEST_PRECEDENCE;
    }
    @Override
    protected UserConfigurations merge(Set<Class<?>>mergedClasses)
    {
        return new UserConfigurations(mergedClasses);
    }
    public static UserConfigurations of(Class<?>... classes)
    {
        return new UserConfigurations(Arrays.asList(classes));
    }
}
