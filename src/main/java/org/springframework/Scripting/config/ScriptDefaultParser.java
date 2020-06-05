package org.springframework.Scripting.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.scripting.config.LangNamespaceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

class ScriptDefaultParser implements BeanDefinitionParser {
    private static final String REFRESH_CHECK_DELAY_ATTRIBUTE = "refresh-check-delay";

    private static final String PROXY_TARGET_CLASS_ATTRIBUTE = "proxy-target-class";

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        BeanDefinition bd =
                LangNamespaceUtils.registerScriptFactoryPostProcessorIfNecessary(parserContext.getRegistry());
        String refreshCheckDelay = element.getAttribute(REFRESH_CHECK_DELAY_ATTRIBUTE);
        if (StringUtils.hasText(refreshCheckDelay)) {
            bd.getPropertyValues().add("defaultRefreshCheckDelay", Long.valueOf(refreshCheckDelay));
        }
        String proxyTargetClass = element.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE);
        if (StringUtils.hasText(proxyTargetClass)) {
            bd.getPropertyValues().add("defaultProxyTargetClass", new TypedStringValue(proxyTargetClass, Boolean.class));
        }
        return null;
    }
}
