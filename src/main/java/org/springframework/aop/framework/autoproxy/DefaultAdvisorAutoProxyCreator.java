package org.springframework.aop.framework.autoproxy;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;

public class DefaultAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator implements BeanNameAware {

    public static final String SEPARATOR = ".";

    private boolean usePrefix = false;

    @Nullable
    private String advisorBeanNamePrefix;

    public void setUsePrefix(boolean usePrefix) {
        this.usePrefix = usePrefix;
    }

    public boolean isUsePrefix() {
        return this.usePrefix;
    }

    public void setAdvisorBeanNamePrefix(@Nullable String advisorBeanNamePrefix) {
        this.advisorBeanNamePrefix = advisorBeanNamePrefix;
    }

    @Nullable
    public String getAdvisorBeanNamePrefix() {
        return this.advisorBeanNamePrefix;
    }

    @Override
    public void setBeanName(String name) {
        // If no infrastructure bean name prefix has been set, override it.
        if (this.advisorBeanNamePrefix == null) {
            this.advisorBeanNamePrefix = name + SEPARATOR;
        }
    }

    @Override
    protected boolean isEligibleAdvisorBean(String beanName) {
        if (!isUsePrefix()) {
            return true;
        }
        String prefix = getAdvisorBeanNamePrefix();
        return (prefix != null && beanName.startsWith(prefix));
    }




}





