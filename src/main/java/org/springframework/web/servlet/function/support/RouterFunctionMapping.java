package org.springframework.web.servlet.function.support;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RouterFunctionMapping extends AbstractHandlerMapping implements InitializingBean {
    @Nullable
    private RouterFunction<?> routerFunction;

    private List<HttpMessageConverter<?>> messageConverters = Collections.emptyList();

    private boolean detectHandlerFunctionsInAncestorContexts = false;
    public RouterFunctionMapping() {
    }
    public RouterFunctionMapping(RouterFunction<?> routerFunction) {
        this.routerFunction = routerFunction;
    }

    public void setRouterFunction(@Nullable RouterFunction<?> routerFunction) {
        this.routerFunction = routerFunction;
    }


    @Nullable
    public RouterFunction<?> getRouterFunction() {
        return this.routerFunction;
    }


    public void setDetectHandlerFunctionsInAncestorContexts(boolean detectHandlerFunctionsInAncestorContexts) {
        this.detectHandlerFunctionsInAncestorContexts = detectHandlerFunctionsInAncestorContexts;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.routerFunction == null) {
            initRouterFunction();
        }
        if (CollectionUtils.isEmpty(this.messageConverters)) {
            initMessageConverters();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void initRouterFunction() {
        ApplicationContext applicationContext = obtainApplicationContext();
        Map<String, RouterFunction> beans =
                (this.detectHandlerFunctionsInAncestorContexts ?
                        BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, RouterFunction.class) :
                        applicationContext.getBeansOfType(RouterFunction.class));

        List<RouterFunction> routerFunctions = new ArrayList<>(beans.values());
        if (!CollectionUtils.isEmpty(routerFunctions) && logger.isInfoEnabled()) {
            routerFunctions.forEach(routerFunction -> logger.info("Mapped " + routerFunction));
        }
        this.routerFunction = routerFunctions.stream()
                .reduce(RouterFunction::andOther)
                .orElse(null);
    }

    private void initMessageConverters() {
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>(4);
        messageConverters.add(new ByteArrayHttpMessageConverter());
        messageConverters.add(new StringHttpMessageConverter());

        try {
            messageConverters.add(new SourceHttpMessageConverter<>());
        }
        catch (Error err) {
            // Ignore when no TransformerFactory implementation is available
        }
        messageConverters.add(new AllEncompassingFormHttpMessageConverter());

        this.messageConverters = messageConverters;
    }


    @Nullable
    @Override
    protected Object getHandlerInternal(@NotNull HttpServletRequest servletRequest) throws Exception {
        if (this.routerFunction != null) {
            ServerRequest request = ServerRequest.create(servletRequest, this.messageConverters);
            servletRequest.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, request);
            return this.routerFunction.route(request).orElse(null);
        }
        else {
            return null;
        }
    }











}
