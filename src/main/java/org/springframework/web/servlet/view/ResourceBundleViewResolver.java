package org.springframework.web.servlet.view;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.View;

import java.util.*;

public class ResourceBundleViewResolver extends AbstractCachingViewResolver implements Ordered, InitializingBean, DisposableBean {
    public static final String DEFAULT_BASENAME = "views";

    private String[] basenames = new String[] {DEFAULT_BASENAME};


    private ClassLoader bundleClassLoader = Thread.currentThread().getContextClassLoader();



    @Nullable
    private String defaultParentView;

    @Nullable
    private Locale[] localesToInitialize;


    private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered
    private final Map<Locale, BeanFactory> localeCache = new HashMap<>();

    private final Map<List<ResourceBundle>, ConfigurableApplicationContext> bundleCache = new HashMap<>();


    public void setBasename(String basename) {
        setBasenames(basename);
    }

    public void setBasenames(String... basenames) {
        this.basenames = basenames;
    }

    public void setBundleClassLoader(ClassLoader classLoader) {
        this.bundleClassLoader = classLoader;
    }

    protected ClassLoader getBundleClassLoader() {
        return this.bundleClassLoader;
    }

    public void setDefaultParentView(String defaultParentView) {
        this.defaultParentView = defaultParentView;
    }


    public void setLocalesToInitialize(Locale... localesToInitialize) {
        this.localesToInitialize = localesToInitialize;
    }

    public void setOrder(int order) {
        this.order = order;
    }
    @Override
    public int getOrder() {
        return this.order;
    }


    @Override
    public void afterPropertiesSet() throws BeansException {
        if (this.localesToInitialize != null) {
            for (Locale locale : this.localesToInitialize) {
                initFactory(locale);
            }
        }
    }

    @Override
    protected View loadView(String viewName, Locale locale) throws Exception {
        BeanFactory factory = initFactory(locale);
        try {
            return factory.getBean(viewName, View.class);
        }
        catch (NoSuchBeanDefinitionException ex) {
            // Allow for ViewResolver chaining...
            return null;
        }
    }

    protected synchronized BeanFactory initFactory(Locale locale) throws BeansException {
        // Try to find cached factory for Locale:
        // Have we already encountered that Locale before?
        if (isCache()) {
            BeanFactory cachedFactory = this.localeCache.get(locale);
            if (cachedFactory != null) {
                return cachedFactory;
            }
        }

        // Build list of ResourceBundle references for Locale.
        List<ResourceBundle> bundles = new LinkedList<>();
        for (String basename : this.basenames) {
            ResourceBundle bundle = getBundle(basename, locale);
            bundles.add(bundle);
        }

        // Try to find cached factory for ResourceBundle list:
        // even if Locale was different, same bundles might have been found.
        if (isCache()) {
            BeanFactory cachedFactory = this.bundleCache.get(bundles);
            if (cachedFactory != null) {
                this.localeCache.put(locale, cachedFactory);
                return cachedFactory;
            }
        }

        // Create child ApplicationContext for views.
        GenericWebApplicationContext factory = new GenericWebApplicationContext();
        factory.setParent(getApplicationContext());
        factory.setServletContext(getServletContext());

        // Load bean definitions from resource bundle.
        PropertiesBeanDefinitionReader reader = new PropertiesBeanDefinitionReader(factory);
        reader.setDefaultParentBean(this.defaultParentView);
        for (ResourceBundle bundle : bundles) {
            reader.registerBeanDefinitions(bundle);
        }

        factory.refresh();

        // Cache factory for both Locale and ResourceBundle list.
        if (isCache()) {
            this.localeCache.put(locale, factory);
            this.bundleCache.put(bundles, factory);
        }

        return factory;
    }


    protected ResourceBundle getBundle(String basename, Locale locale) throws MissingResourceException {
        return ResourceBundle.getBundle(basename, locale, getBundleClassLoader());
    }


    @Override
    public void destroy() throws BeansException {
        for (ConfigurableApplicationContext factory : this.bundleCache.values()) {
            factory.close();
        }
        this.localeCache.clear();
        this.bundleCache.clear();
    }













}
