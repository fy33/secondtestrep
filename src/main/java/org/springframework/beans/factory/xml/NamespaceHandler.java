package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.lang.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public interface NamespaceHandler  {
    void init();


    @Nullable
    BeanDefinition parse(Element element, ParserContext parserContext);


    @Nullable
    BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder definition, ParserContext parserContext);















}
