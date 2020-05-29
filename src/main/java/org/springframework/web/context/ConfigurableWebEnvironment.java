package org.springframework.web.context;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.lang.Nullable;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public interface ConfigurableWebEnvironment extends ConfigurableEnvironment {
    void initPropertySources(@Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig);

}
