package org.springframework.boot.builder;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpringApplicationBuilder {
    private final SpringApplication application;
    private ConfigurableApplicationContext context;
    private SpringApplicationBuilder parent;
    private final AtomicBoolean running=new AtomicBoolean(false);
    private final Set<Class<?>> sources=new LinkedHashSet<>();
    private final Map<String,Object> defaultProperties=new LinkedHashMap<>();
    private ConfigurableEnvironment environment;
    private Set<String> additionalProfiles=new LinkedHashSet<>();
    private boolean registerShudownHookApplied;
    private boolean configuredAsChild=false;
    public SpringApplicationBuilder(Class<?>... sources)
    {
        this.application=createSpringApplication(sources);
    }
    protected SpringApplication createSpringApplication(Class<?>... sources)
    {
        return new SpringApplication(sources);
    }
    public ConfigurableApplicationContext context()
    {
        return this.context;
    }
    public SpringApplication application()
    {
        return this.application;
    }
    public ConfigurableApplicationContext run(String... args)
    {
        if(this.running.get())
        {
            return this.context;
        }
        configureAsChildIfNecessary(args);
        if(this.running.compareAndSet(false,true))
        {
            synchronized (this.running)
            {
                //If not already running copy the sources over and then run.
                this.context=build().run(args);
            }
        }
        return this.context;
    }
    private void configureAsChildIfNecessary(String... args)
    {
        if(this.parent!=null&&!this.configuredAsChild)
        {
            this.configuredAsChild=true;
            if(!this.registerShudownHookApplied)
            {
                this.application.setRegisterShutdownHook(false);
            }
            initializers(new ParentContextApplicationContextInitializer(
                    this.parent.run(args)
            ));
        }
    }
    public SpringApplication build()
    {
        return build(new String[0]);
    }
    public SpringApplication build(String... args)
    {
        configureAsChildIfNecessary(args);
        this.application.addPrimarySources(this.sources);
        return this.application;
    }
    public SpringApplicationBuilder initializers(ApplicationContextInitializer<?>... initializers)
    {
        this.application.addInitializers(initializers);
        return this;
    }
    public SpringApplicationBuilder main(Class<?> mainApplicationClass)
    {
        this.application.setMainApplicationClass(mainApplicationClass);
        return this;
    }
    public SpringApplicationBuilder listeners(ApplicationListener<?>... listeners)
    {
        this.application.addListeners(listeners);
        return this;
    }
    public SpringApplicationBuilder contextClass(Class<? extends ConfigurableApplicationContext> cls)
    {
        this.application.setApplicationContextClass(cls);
        return this;
    }
    public SpringApplicationBuilder sources(Class<?>... sources)
    {
        this.sources.addAll(new LinkedHashSet<>(Arrays.asList(sources)));
        return this;
    }
}
