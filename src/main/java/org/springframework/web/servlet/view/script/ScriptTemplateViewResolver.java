package org.springframework.web.servlet.view.script;

import org.springframework.web.servlet.view.UrlBasedViewResolver;

public class ScriptTemplateViewResolver extends UrlBasedViewResolver {
    public ScriptTemplateViewResolver() {
        setViewClass(requiredViewClass());
    }

    public ScriptTemplateViewResolver(String prefix, String suffix) {
        this();
        setPrefix(prefix);
        setSuffix(suffix);
    }

    @Override
    protected Class<?> requiredViewClass() {
        return ScriptTemplateView.class;
    }






}
