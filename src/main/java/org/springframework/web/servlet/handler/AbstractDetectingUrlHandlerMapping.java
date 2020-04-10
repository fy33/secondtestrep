package org.springframework.web.servlet.handler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;

public abstract class AbstractDetectingUrlHandlerMapping extends AbstractUrlHandlerMapping {
    private boolean detectHandlersInAncestorContexts = false;
    public void setDetectHandlersInAncestorContexts(boolean detectHandlersInAncestorContexts) {
        this.detectHandlersInAncestorContexts = detectHandlersInAncestorContexts;
    }
    @Override
    public void initApplicationContext() throws ApplicationContextException {
        super.initApplicationContext();
        detectHandlers();
    }
    protected void detectHandlers() throws BeansException {
        ApplicationContext applicationContext = obtainApplicationContext();
        String[] beanNames = (this.detectHandlersInAncestorContexts ?
                BeanFactoryUtils.beanNamesForTypeIncludingAncestors(applicationContext, Object.class) :
                applicationContext.getBeanNamesForType(Object.class));

        // Take any bean name that we can determine URLs for.
        for (String beanName : beanNames) {
            String[] urls = determineUrlsForHandler(beanName);
            if (!ObjectUtils.isEmpty(urls)) {
                // URL paths found: Let's consider it a handler.
                registerHandler(urls, beanName);
            }
        }

        if ((logger.isDebugEnabled() && !getHandlerMap().isEmpty()) || logger.isTraceEnabled()) {
            logger.debug("Detected " + getHandlerMap().size() + " mappings in " + formatMappingName());
        }
    }
    @Override
    protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
        return null;
    }
    protected abstract String[] determineUrlsForHandler(String beanName);
}
