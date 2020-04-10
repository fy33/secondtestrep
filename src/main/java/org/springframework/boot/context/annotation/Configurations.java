package org.springframework.boot.context.annotation;


import org.springframework.core.OrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract  class Configurations {
    private static final Comparator<Object> COMPARATOR= OrderComparator.INSTANCE
            .thenComparing(other->other.getClass().getName());
    private final Set<Class<?>> classes;
    protected Configurations(Collection<Class<?>> classes)
    {
        Assert.notNull(classes,"Classes must not be null");
        Collection<Class<?>> sorted=sort(classes);
        this.classes= Collections.unmodifiableSet(new LinkedHashSet<>(sorted));
    }
    protected Collection<Class<?>> sort(Collection<Class<?>> classes)
    {
        return classes;
    }
    protected final Set<Class<?>> getClasses()
    {
        return this.classes;
    }
    protected Configurations merge(Configurations other)
    {
        Set<Class<?>> mergedClasses=new LinkedHashSet<>(getClasses());
        mergedClasses.addAll(other.getClasses());
        return merge(mergedClasses);
    }
    protected abstract Configurations merge(Set<Class<?>> mergedClasses);
    public static Class<?>[] getClasses(Configurations... configurations)
    {
        return getClasses(Arrays.asList(configurations));
    }
    public static Class<?>[] getClasses(Collection<Configurations> configurations)
    {
        List<Configurations> ordered=new ArrayList<>(configurations);
        ordered.sort(COMPARATOR);
        List<Configurations> collated=collate(ordered);
        LinkedHashSet<Class<?>> classes=collated.stream().flatMap(Configurations::streamClasses)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return ClassUtils.toClassArray(classes);

    }
    private static Stream<Class<?>> streamClasses(Configurations configurations)
    {
        return configurations.getClasses().stream();
    }
    private static List<Configurations> collate(List<Configurations> orderedConfigurations)
    {
        LinkedList<Configurations> collated=new LinkedList<>();
        for (Configurations item : orderedConfigurations) {
            if(collated.isEmpty()||collated.getLast().getClass()!=item.getClass())
            {
                collated.add(item);
            }
            else{
                collated.set(collated.size()-1,collated.getLast().merge(item));
            }
        }
        return collated;
    }
}
