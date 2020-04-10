package org.springframework.web.servlet.handler;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class BeanNameUrlHandlerMapping extends AbstractDetectingUrlHandlerMapping {
    @Override
    protected String[] determineUrlsForHandler(String beanName) {
        List<String> urls = new ArrayList<>();
        if (beanName.startsWith("/")) {
            urls.add(beanName);
        }
        String[] aliases = obtainApplicationContext().getAliases(beanName);
        for (String alias : aliases) {
            if (alias.startsWith("/")) {
                urls.add(alias);
            }
        }
        return StringUtils.toStringArray(urls);
    }
}
