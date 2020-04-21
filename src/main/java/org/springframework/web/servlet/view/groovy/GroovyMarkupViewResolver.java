package org.springframework.web.servlet.view.groovy;

import org.springframework.web.servlet.view.AbstractTemplateViewResolver;

import java.util.Locale;

public class GroovyMarkupViewResolver extends AbstractTemplateViewResolver {
    public GroovyMarkupViewResolver() {
        setViewClass(requiredViewClass());
    }

    public GroovyMarkupViewResolver(String prefix, String suffix) {
        this();
        setPrefix(prefix);
        setSuffix(suffix);
    }



    @Override
    protected Class<?> requiredViewClass() {
        return GroovyMarkupView.class;
    }





    @Override
    protected Object getCacheKey(String viewName, Locale locale) {
        return viewName + '_' + locale;
    }



















}
