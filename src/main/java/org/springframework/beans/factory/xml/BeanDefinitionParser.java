package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.lang.Nullable;
import org.w3c.dom.Element;

public interface BeanDefinitionParser {
    @Nullable
    BeanDefinition parse(Element element, ParserContext parserContext);

}
