package org.springframework.web.servlet.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class TilesConfigureBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
    /**
     * The bean name used for the {@code TilesConfigurer}.
     */
    public static final String BEAN_NAME = "mvcTilesConfigurer";

    @Override
    protected String getBeanClassName(Element element) {
        return "org.springframework.web.servlet.view.tiles3.TilesConfigurer";
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        return BEAN_NAME;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        List<Element> childElements = DomUtils.getChildElementsByTagName(element, "definitions");
        if (!childElements.isEmpty()) {
            List<String> locations = new ArrayList<>(childElements.size());
            for (Element childElement : childElements) {
                locations.add(childElement.getAttribute("location"));
            }
            builder.addPropertyValue("definitions", StringUtils.toStringArray(locations));
        }
        if (element.hasAttribute("check-refresh")) {
            builder.addPropertyValue("checkRefresh", element.getAttribute("check-refresh"));
        }
        if (element.hasAttribute("validate-definitions")) {
            builder.addPropertyValue("validateDefinitions", element.getAttribute("validate-definitions"));
        }
        if (element.hasAttribute("definitions-factory")) {
            builder.addPropertyValue("definitionsFactoryClass", element.getAttribute("definitions-factory"));
        }
        if (element.hasAttribute("preparer-factory")) {
            builder.addPropertyValue("preparerFactoryClass", element.getAttribute("preparer-factory"));
        }
    }












}
