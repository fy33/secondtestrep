package org.springframework.boot.web.reactive.context;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;


//todo  mono 语言
public class ReactiveWebServerApplicationContext extends GenericReactiveWebApplicationContext {

    public ReactiveWebServerApplicationContext() {
    }

    public ReactiveWebServerApplicationContext(DefaultListableBeanFactory beanFactory) {
        super(beanFactory);
    }

}
