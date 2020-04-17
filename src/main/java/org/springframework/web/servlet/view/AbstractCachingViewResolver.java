package org.springframework.web.servlet.view;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractCachingViewResolver extends WebApplicationObjectSupport implements ViewResolver {
    public static final int DEFAULT_CACHE_LIMIT = 1024;
    private static final View UNRESOLVED_VIEW = new View() {
        @Override
        @Nullable
        public String getContentType() {
            return null;
        }
        @Override
        public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
        }
    };


    private static final CacheFilter DEFAULT_CACHE_FILTER = (view, viewName, locale) -> true;

    private volatile int cacheLimit = DEFAULT_CACHE_LIMIT;


    private boolean cacheUnresolved = true;

    private CacheFilter cacheFilter = DEFAULT_CACHE_FILTER;

    private final Map<Object, View> viewAccessCache = new ConcurrentHashMap<>(DEFAULT_CACHE_LIMIT);

    private final Map<Object, View> viewCreationCache =
            new LinkedHashMap<Object, View>(DEFAULT_CACHE_LIMIT, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Object, View> eldest) {
                    if (size() > getCacheLimit()) {
                        viewAccessCache.remove(eldest.getKey());
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            };


    public void setCacheLimit(int cacheLimit) {
        this.cacheLimit = cacheLimit;
    }

    public int getCacheLimit() {
        return this.cacheLimit;
    }

    public void setCache(boolean cache) {
        this.cacheLimit = (cache ? DEFAULT_CACHE_LIMIT : 0);
    }
    public boolean isCache() {
        return (this.cacheLimit > 0);
    }

    public void setCacheUnresolved(boolean cacheUnresolved) {
        this.cacheUnresolved = cacheUnresolved;
    }


    public boolean isCacheUnresolved() {
        return this.cacheUnresolved;
    }


    public void setCacheFilter(CacheFilter cacheFilter) {
        Assert.notNull(cacheFilter, "CacheFilter must not be null");
        this.cacheFilter = cacheFilter;
    }
    public CacheFilter getCacheFilter() {
        return this.cacheFilter;
    }


    @Override
    @Nullable
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        if (!isCache()) {
            return createView(viewName, locale);
        }
        else {
            Object cacheKey = getCacheKey(viewName, locale);
            View view = this.viewAccessCache.get(cacheKey);
            if (view == null) {
                synchronized (this.viewCreationCache) {
                    view = this.viewCreationCache.get(cacheKey);
                    if (view == null) {
                        // Ask the subclass to create the View object.
                        view = createView(viewName, locale);
                        if (view == null && this.cacheUnresolved) {
                            view = UNRESOLVED_VIEW;
                        }
                        if (view != null && this.cacheFilter.filter(view, viewName, locale)) {
                            this.viewAccessCache.put(cacheKey, view);
                            this.viewCreationCache.put(cacheKey, view);
                        }
                    }
                }
            }
            else {
                if (logger.isTraceEnabled()) {
                    logger.trace(formatKey(cacheKey) + "served from cache");
                }
            }
            return (view != UNRESOLVED_VIEW ? view : null);
        }
    }

    private static String formatKey(Object cacheKey) {
        return "View with key [" + cacheKey + "] ";
    }



    protected Object getCacheKey(String viewName, Locale locale) {
        return viewName + '_' + locale;
    }

    public void removeFromCache(String viewName, Locale locale) {
        if (!isCache()) {
            logger.warn("Caching is OFF (removal not necessary)");
        }
        else {
            Object cacheKey = getCacheKey(viewName, locale);
            Object cachedView;
            synchronized (this.viewCreationCache) {
                this.viewAccessCache.remove(cacheKey);
                cachedView = this.viewCreationCache.remove(cacheKey);
            }
            if (logger.isDebugEnabled()) {
                // Some debug output might be useful...
                logger.debug(formatKey(cacheKey) +
                        (cachedView != null ? "cleared from cache" : "not found in the cache"));
            }
        }
    }

    public void clearCache() {
        logger.debug("Clearing all views from the cache");
        synchronized (this.viewCreationCache) {
            this.viewAccessCache.clear();
            this.viewCreationCache.clear();
        }
    }

    @Nullable
    protected View createView(String viewName, Locale locale) throws Exception {
        return loadView(viewName, locale);
    }
    @Nullable
    protected abstract View loadView(String viewName, Locale locale) throws Exception;

    @FunctionalInterface
    public interface CacheFilter {

        /**
         * Indicates whether the given view should be cached.
         * The name and locale used to resolve the view are also provided.
         * @param view the view
         * @param viewName the name used to resolve the {@code view}
         * @param locale the locale used to resolve the {@code view}
         * @return {@code true} if the view should be cached; {@code false} otherwise
         */
        boolean filter(View view, String viewName, Locale locale);
    }
}
