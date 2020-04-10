package org.springframework.web.servlet.handler;

import org.springframework.beans.BeansException;
import org.springframework.util.CollectionUtils;

import java.util.*;

public class SimpleUrlHandlerMapping extends AbstractUrlHandlerMapping {
    private final Map<String, Object> urlMap = new LinkedHashMap<>();
    public SimpleUrlHandlerMapping() {
    }
    public SimpleUrlHandlerMapping(Map<String, ?> urlMap) {
        setUrlMap(urlMap);
    }
    public SimpleUrlHandlerMapping(Map<String, ?> urlMap, int order) {
        setUrlMap(urlMap);
        setOrder(order);
    }

    public void setMappings(Properties mappings) {
        CollectionUtils.mergePropertiesIntoMap(mappings, this.urlMap);
    }

    public void setUrlMap(Map<String, ?> urlMap) {
        this.urlMap.putAll(urlMap);
    }


    public Map<String, ?> getUrlMap() {
        return this.urlMap;
    }


    @Override
    public void initApplicationContext() throws BeansException {
        super.initApplicationContext();
        registerHandlers(this.urlMap);
    }
    protected void registerHandlers(Map<String, Object> urlMap) throws BeansException {
        if (urlMap.isEmpty()) {
            logger.trace("No patterns in " + formatMappingName());
        }
        else {
            urlMap.forEach((url, handler) -> {
                // Prepend with slash if not already present.
                if (!url.startsWith("/")) {
                    url = "/" + url;
                }
                // Remove whitespace from handler bean name.
                if (handler instanceof String) {
                    handler = ((String) handler).trim();
                }
                registerHandler(url, handler);
            });
            if (logger.isDebugEnabled()) {
                List<String> patterns = new ArrayList<>();
                if (getRootHandler() != null) {
                    patterns.add("/");
                }
                if (getDefaultHandler() != null) {
                    patterns.add("/**");
                }
                patterns.addAll(getHandlerMap().keySet());
                logger.debug("Patterns " + patterns + " in " + formatMappingName());
            }
        }
    }










}
