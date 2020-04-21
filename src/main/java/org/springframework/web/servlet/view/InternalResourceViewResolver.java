package org.springframework.web.servlet.view;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

public class InternalResourceViewResolver extends UrlBasedViewResolver {
    private static final boolean jstlPresent = ClassUtils.isPresent(
            "javax.servlet.jsp.jstl.core.Config", InternalResourceViewResolver.class.getClassLoader());

    @Nullable
    private Boolean alwaysInclude;

    public InternalResourceViewResolver() {
        Class<?> viewClass = requiredViewClass();
        if (InternalResourceView.class == viewClass && jstlPresent) {
            viewClass = JstlView.class;
        }
        setViewClass(viewClass);
    }

    public InternalResourceViewResolver(String prefix, String suffix) {
        this();
        setPrefix(prefix);
        setSuffix(suffix);
    }




    @Override
    protected Class<?> requiredViewClass() {
        return InternalResourceView.class;
    }


    public void setAlwaysInclude(boolean alwaysInclude) {
        this.alwaysInclude = alwaysInclude;
    }


    @Override
    protected AbstractUrlBasedView buildView(String viewName) throws Exception {
        InternalResourceView view = (InternalResourceView) super.buildView(viewName);
        if (this.alwaysInclude != null) {
            view.setAlwaysInclude(this.alwaysInclude);
        }
        view.setPreventDispatchLoop(true);
        return view;
    }




}
