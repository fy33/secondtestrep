package org.springframework.web.servlet.view.freemarker;

import org.springframework.web.servlet.view.AbstractTemplateViewResolver;

public class FreeMarkerViewResolver extends AbstractTemplateViewResolver {

    public FreeMarkerViewResolver() {
        setViewClass(requiredViewClass());
    }


    public FreeMarkerViewResolver(String prefix, String suffix) {
        this();
        setPrefix(prefix);
        setSuffix(suffix);
    }


    @Override
    protected Class<?> requiredViewClass() {
        return FreeMarkerView.class;
    }







}
