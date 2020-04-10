package org.springframework.boot;

import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.HashSet;
import java.util.Set;

class BeanDefinitionLoader {
    private final Object[] sources;
    private final AnnotatedBeanDefinitionReader annotatedReader;
    private final XmlBeanDefinitionReader xmlReader;
    private BeanDefinitionReader groovyReader;
    private final ClassPathBeanDefinitionScanner scanner;
    private ResourceLoader resourceLoader;
    BeanDefinitionLoader(BeanDefinitionRegistry registry,Object... sources) {
        Assert.notNull(registry, "Registry must not be null");
        Assert.notEmpty(sources, "Sources must not be empty");
        this.sources = sources;
        this.annotatedReader = new AnnotatedBeanDefinitionReader(registry);
        this.xmlReader = new XmlBeanDefinitionReader(registry);
        if (isGroovyPresent())
        {
            this.groovyReader=new GroovyBeanDefinitionReader(registry);
        }
        this.scanner=new ClassPathBeanDefinitionScanner(registry);
        this.scanner.addExcludeFilter(new ClassExcludeFilter(sources));
    }
    private boolean isGroovyPresent()
    {
        return ClassUtils.isPresent("groovy.lang.MetaClass",null);
    }
    private static class ClassExcludeFilter extends AbstractTypeHierarchyTraversingFilter {
        private final Set<String> classNames=new HashSet<>();
        ClassExcludeFilter(Object... sources)
        {
            super(false,false);
            for (Object source : sources) {
                if(source instanceof Class<?>)
                {
                    this.classNames.add(((Class<?>)source).getName());
                }
            }
        }
        @Override
        protected boolean matchClassName(String className)
        {
            return this.classNames.contains(className);
        }
    }
    void setBeanNameGenerator(BeanNameGenerator beanNameGenerator)
    {
        this.annotatedReader.setBeanNameGenerator(beanNameGenerator);
        this.xmlReader.setBeanNameGenerator(beanNameGenerator);
        this.scanner.setBeanNameGenerator(beanNameGenerator);
    }
    void setResourceLoader(ResourceLoader resourceLoader)
    {
        this.resourceLoader=resourceLoader;
        this.xmlReader.setResourceLoader(resourceLoader);
        this.scanner.setResourceLoader(resourceLoader);
    }
    void setEnvironment(ConfigurableEnvironment environment)
    {
        this.annotatedReader.setEnvironment(environment);
        this.xmlReader.setEnvironment(environment);
        this.scanner.setEnvironment(environment);
    }
    int load()
    {
        int count=0;
        for (Object source : this.sources) {
            count+=load(source);
        }
        return count;
    }
    private int load(Object source)
    {
        Assert.notNull(source,"Source must not be null");
        if(source instanceof Class<?>)
        {
            return load((Class<?>) source);
        }
        if(source instanceof Resource)
        {
            return load((Resource)source);
        }
        if(source instanceof Package)
        {
            return load((Package)source);
        }
        if(source instanceof CharSequence)
        {
            return load((CharSequence)source);
        }
        throw new IllegalArgumentException("Invalid source type"+source.getClass());
    }
}
