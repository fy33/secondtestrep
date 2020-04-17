package org.springframework.web.servlet.view;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.servlet.View;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class UrlBasedViewResolver extends AbstractCachingViewResolver implements Ordered {
    public static final String REDIRECT_URL_PREFIX = "redirect:";

    public static final String FORWARD_URL_PREFIX = "forward:";

    @Nullable
    private Class<?> viewClass;

    private String prefix = "";

    private String suffix = "";

    @Nullable
    private String contentType;

    private boolean redirectContextRelative = true;

    private boolean redirectHttp10Compatible = true;


    @Nullable
    private String[] redirectHosts;

    @Nullable
    private String requestContextAttribute;


    private final Map<String, Object> staticAttributes = new HashMap<>();

    @Nullable
    private Boolean exposePathVariables;

    @Nullable
    private Boolean exposeContextBeansAsAttributes;

    @Nullable
    private String[] exposedContextBeanNames;

    @Nullable
    private String[] viewNames;


    private int order = Ordered.LOWEST_PRECEDENCE;

    public void setViewClass(@Nullable Class<?> viewClass) {
        if (viewClass != null && !requiredViewClass().isAssignableFrom(viewClass)) {
            throw new IllegalArgumentException("Given view class [" + viewClass.getName() +
                    "] is not of type [" + requiredViewClass().getName() + "]");
        }
        this.viewClass = viewClass;
    }
    @Nullable
    protected Class<?> getViewClass() {
        return this.viewClass;
    }


    protected Class<?> requiredViewClass() {
        return AbstractUrlBasedView.class;
    }

    public void setPrefix(@Nullable String prefix) {
        this.prefix = (prefix != null ? prefix : "");
    }


    protected String getPrefix() {
        return this.prefix;
    }

    public void setSuffix(@Nullable String suffix) {
        this.suffix = (suffix != null ? suffix : "");
    }
    protected String getSuffix() {
        return this.suffix;
    }


    public void setContentType(@Nullable String contentType) {
        this.contentType = contentType;
    }

    @Nullable
    protected String getContentType() {
        return this.contentType;
    }


    public void setRedirectContextRelative(boolean redirectContextRelative) {
        this.redirectContextRelative = redirectContextRelative;
    }


    protected boolean isRedirectContextRelative() {
        return this.redirectContextRelative;
    }

    public void setRedirectHttp10Compatible(boolean redirectHttp10Compatible) {
        this.redirectHttp10Compatible = redirectHttp10Compatible;
    }

    protected boolean isRedirectHttp10Compatible() {
        return this.redirectHttp10Compatible;
    }


    public void setRedirectHosts(@Nullable String... redirectHosts) {
        this.redirectHosts = redirectHosts;
    }


    @Nullable
    public String[] getRedirectHosts() {
        return this.redirectHosts;
    }

    public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
        this.requestContextAttribute = requestContextAttribute;
    }
    @Nullable
    protected String getRequestContextAttribute() {
        return this.requestContextAttribute;
    }

    public void setAttributes(Properties props) {
        CollectionUtils.mergePropertiesIntoMap(props, this.staticAttributes);
    }


    public void setAttributesMap(@Nullable Map<String, ?> attributes) {
        if (attributes != null) {
            this.staticAttributes.putAll(attributes);
        }
    }

    public Map<String, Object> getAttributesMap() {
        return this.staticAttributes;
    }

    public void setExposePathVariables(@Nullable Boolean exposePathVariables) {
        this.exposePathVariables = exposePathVariables;
    }

    @Nullable
    protected Boolean getExposePathVariables() {
        return this.exposePathVariables;
    }


    public void setExposeContextBeansAsAttributes(boolean exposeContextBeansAsAttributes) {
        this.exposeContextBeansAsAttributes = exposeContextBeansAsAttributes;
    }

    @Nullable
    protected Boolean getExposeContextBeansAsAttributes() {
        return this.exposeContextBeansAsAttributes;
    }


    public void setExposedContextBeanNames(@Nullable String... exposedContextBeanNames) {
        this.exposedContextBeanNames = exposedContextBeanNames;
    }

    @Nullable
    protected String[] getExposedContextBeanNames() {
        return this.exposedContextBeanNames;
    }

    public void setViewNames(@Nullable String... viewNames) {
        this.viewNames = viewNames;
    }

    @Nullable
    protected String[] getViewNames() {
        return this.viewNames;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    protected void initApplicationContext() {
        super.initApplicationContext();
        if (getViewClass() == null) {
            throw new IllegalArgumentException("Property 'viewClass' is required");
        }
    }

    @Override
    protected Object getCacheKey(String viewName, Locale locale) {
        return viewName;
    }

    @Override
    protected View createView(String viewName, Locale locale) throws Exception {
        // If this resolver is not supposed to handle the given view,
        // return null to pass on to the next resolver in the chain.
        if (!canHandle(viewName, locale)) {
            return null;
        }

        // Check for special "redirect:" prefix.
        if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
            String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
            RedirectView view = new RedirectView(redirectUrl,
                    isRedirectContextRelative(), isRedirectHttp10Compatible());
            String[] hosts = getRedirectHosts();
            if (hosts != null) {
                view.setHosts(hosts);
            }
            return applyLifecycleMethods(REDIRECT_URL_PREFIX, view);
        }

        // Check for special "forward:" prefix.
        if (viewName.startsWith(FORWARD_URL_PREFIX)) {
            String forwardUrl = viewName.substring(FORWARD_URL_PREFIX.length());
            InternalResourceView view = new InternalResourceView(forwardUrl);
            return applyLifecycleMethods(FORWARD_URL_PREFIX, view);
        }

        // Else fall back to superclass implementation: calling loadView.
        return super.createView(viewName, locale);
    }


    protected boolean canHandle(String viewName, Locale locale) {
        String[] viewNames = getViewNames();
        return (viewNames == null || PatternMatchUtils.simpleMatch(viewNames, viewName));
    }


    @Override
    protected View loadView(String viewName, Locale locale) throws Exception {
        AbstractUrlBasedView view = buildView(viewName);
        View result = applyLifecycleMethods(viewName, view);
        return (view.checkResource(locale) ? result : null);
    }


    protected AbstractUrlBasedView buildView(String viewName) throws Exception {
        Class<?> viewClass = getViewClass();
        Assert.state(viewClass != null, "No view class");

        AbstractUrlBasedView view = (AbstractUrlBasedView) BeanUtils.instantiateClass(viewClass);
        view.setUrl(getPrefix() + viewName + getSuffix());
        view.setAttributesMap(getAttributesMap());

        String contentType = getContentType();
        if (contentType != null) {
            view.setContentType(contentType);
        }

        String requestContextAttribute = getRequestContextAttribute();
        if (requestContextAttribute != null) {
            view.setRequestContextAttribute(requestContextAttribute);
        }

        Boolean exposePathVariables = getExposePathVariables();
        if (exposePathVariables != null) {
            view.setExposePathVariables(exposePathVariables);
        }
        Boolean exposeContextBeansAsAttributes = getExposeContextBeansAsAttributes();
        if (exposeContextBeansAsAttributes != null) {
            view.setExposeContextBeansAsAttributes(exposeContextBeansAsAttributes);
        }
        String[] exposedContextBeanNames = getExposedContextBeanNames();
        if (exposedContextBeanNames != null) {
            view.setExposedContextBeanNames(exposedContextBeanNames);
        }

        return view;
    }

    protected View applyLifecycleMethods(String viewName, AbstractUrlBasedView view) {
        ApplicationContext context = getApplicationContext();
        if (context != null) {
            Object initialized = context.getAutowireCapableBeanFactory().initializeBean(view, viewName);
            if (initialized instanceof View) {
                return (View) initialized;
            }
        }
        return view;
    }






}
