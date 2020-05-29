package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.ApplicationContextAware;

public abstract class ApplicationObjectSupport implements ApplicationContextAware {
    /** Logger that is available to subclasses. */
    protected final Log logger = LogFactory.getLog(getClass());

    /** ApplicationContext this object runs in. */
    @Nullable
    private ApplicationContext applicationContext;

    /** MessageSourceAccessor for easy message access. */
    @Nullable
    private MessageSourceAccessor messageSourceAccessor;


    @Override
    public final void setApplicationContext(@Nullable ApplicationContext context) throws BeansException {
        if (context == null && !isContextRequired()) {
            // Reset internal context state.
            this.applicationContext = null;
            this.messageSourceAccessor = null;
        }
        else if (this.applicationContext == null) {
            // Initialize with passed-in context.
            if (!requiredContextClass().isInstance(context)) {
                throw new ApplicationContextException(
                        "Invalid application context: needs to be of type [" + requiredContextClass().getName() + "]");
            }
            this.applicationContext = context;
            this.messageSourceAccessor = new MessageSourceAccessor(context);
            initApplicationContext(context);
        }
        else {
            // Ignore reinitialization if same context passed in.
            if (this.applicationContext != context) {
                throw new ApplicationContextException(
                        "Cannot reinitialize with different application context: current one is [" +
                                this.applicationContext + "], passed-in one is [" + context + "]");
            }
        }
    }

    protected boolean isContextRequired() {
        return false;
    }


    protected Class<?> requiredContextClass() {
        return ApplicationContext.class;
    }


    protected void initApplicationContext(ApplicationContext context) throws BeansException {
        initApplicationContext();
    }


    protected void initApplicationContext() throws BeansException {
    }


    @Nullable
    public final ApplicationContext getApplicationContext() throws IllegalStateException {
        if (this.applicationContext == null && isContextRequired()) {
            throw new IllegalStateException(
                    "ApplicationObjectSupport instance [" + this + "] does not run in an ApplicationContext");
        }
        return this.applicationContext;
    }


    protected final ApplicationContext obtainApplicationContext() {
        ApplicationContext applicationContext = getApplicationContext();
        Assert.state(applicationContext != null, "No ApplicationContext");
        return applicationContext;
    }

    @Nullable
    protected final MessageSourceAccessor getMessageSourceAccessor() throws IllegalStateException {
        if (this.messageSourceAccessor == null && isContextRequired()) {
            throw new IllegalStateException(
                    "ApplicationObjectSupport instance [" + this + "] does not run in an ApplicationContext");
        }
        return this.messageSourceAccessor;
    }


















































}
