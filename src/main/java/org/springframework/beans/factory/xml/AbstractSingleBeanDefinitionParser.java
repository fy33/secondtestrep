package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.lang.Nullable;
import org.w3c.dom.Element;

public class AbstractSingleBeanDefinitionParser extends AbstractBeanDefinitionParser {
    @Override
    protected final AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
        String parentName = getParentName(element);
        if (parentName != null) {
            builder.getRawBeanDefinition().setParentName(parentName);
        }
        Class<?> beanClass = getBeanClass(element);
        if (beanClass != null) {
            builder.getRawBeanDefinition().setBeanClass(beanClass);
        }
        else {
            String beanClassName = getBeanClassName(element);
            if (beanClassName != null) {
                builder.getRawBeanDefinition().setBeanClassName(beanClassName);
            }
        }
        builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
        BeanDefinition containingBd = parserContext.getContainingBeanDefinition();
        if (containingBd != null) {
            // Inner bean definition must receive same scope as containing bean.
            builder.setScope(containingBd.getScope());
        }
        if (parserContext.isDefaultLazyInit()) {
            // Default-lazy-init applies to custom bean definitions as well.
            builder.setLazyInit(true);
        }
        doParse(element, parserContext, builder);
        return builder.getBeanDefinition();
    }

    @Nullable
    protected String getParentName(Element element) {
        return null;
    }

    @Nullable
    protected Class<?> getBeanClass(Element element) {
        return null;
    }

    @Nullable
    protected String getBeanClassName(Element element) {
        return null;
    }

    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        doParse(element, builder);
    }

    protected void doParse(Element element, BeanDefinitionBuilder builder) {
    }

}
