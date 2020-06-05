package org.springframework.context.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.WebSphereMBeanServerFactoryBean;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

class MBeanServerBeanDefinitionParser extends AbstractBeanDefinitionParser {
    private static final String MBEAN_SERVER_BEAN_NAME = "mbeanServer";

    private static final String AGENT_ID_ATTRIBUTE = "agent-id";


    private static final boolean weblogicPresent;

    private static final boolean webspherePresent;

    static {
        ClassLoader classLoader = MBeanServerBeanDefinitionParser.class.getClassLoader();
        weblogicPresent = ClassUtils.isPresent("weblogic.management.Helper", classLoader);
        webspherePresent = ClassUtils.isPresent("com.ibm.websphere.management.AdminServiceFactory", classLoader);
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        String id = element.getAttribute(ID_ATTRIBUTE);
        return (StringUtils.hasText(id) ? id : MBEAN_SERVER_BEAN_NAME);
    }

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        String agentId = element.getAttribute(AGENT_ID_ATTRIBUTE);
        if (StringUtils.hasText(agentId)) {
            RootBeanDefinition bd = new RootBeanDefinition(MBeanServerFactoryBean.class);
            bd.getPropertyValues().add("agentId", agentId);
            return bd;
        }
        AbstractBeanDefinition specialServer = findServerForSpecialEnvironment();
        if (specialServer != null) {
            return specialServer;
        }
        RootBeanDefinition bd = new RootBeanDefinition(MBeanServerFactoryBean.class);
        bd.getPropertyValues().add("locateExistingServerIfPossible", Boolean.TRUE);

        // Mark as infrastructure bean and attach source location.
        bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        bd.setSource(parserContext.extractSource(element));
        return bd;
    }



    @Nullable
    static AbstractBeanDefinition findServerForSpecialEnvironment() {
        if (weblogicPresent) {
            RootBeanDefinition bd = new RootBeanDefinition(JndiObjectFactoryBean.class);
            bd.getPropertyValues().add("jndiName", "java:comp/env/jmx/runtime");
            return bd;
        }
        else if (webspherePresent) {
            return new RootBeanDefinition(WebSphereMBeanServerFactoryBean.class);
        }
        else {
            return null;
        }
    }











}
