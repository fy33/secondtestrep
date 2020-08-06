package org.springframework.aop.support;

import org.aopalliance.aop.Advice;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.ObjectInputStream;

public abstract class AbstractBeanFactoryPointcutAdvisor extends AbstractPointcutAdvisor
implements BeanFactoryAware {

    @Nullable
    private String adviceBeanName;

    @Nullable
    private BeanFactory beanFactory;

    @Nullable
    private transient volatile Advice advice;

    private transient volatile Object adviceMonitor = new Object();

    public void setAdviceBeanName(@Nullable String adviceBeanName) {
        this.adviceBeanName = adviceBeanName;
    }

    @Nullable
    public String getAdviceBeanName() {
        return this.adviceBeanName;
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        resetAdviceMonitor();
    }

    private void resetAdviceMonitor() {
        if (this.beanFactory instanceof ConfigurableBeanFactory) {
            this.adviceMonitor = ((ConfigurableBeanFactory) this.beanFactory).getSingletonMutex();
        } else {
            this.adviceMonitor = new Object();

        }
    }

    public void setAdvice(Advice advice) {
        synchronized (this.adviceMonitor) {
            this.advice = advice;
        }
    }

    @Override
    public Advice getAdvice() {
        Advice advice = this.advice;
        if (advice != null) {
            return advice;
        }

        Assert.state(this.adviceBeanName != null, "'adviceBeanName' must be specified");
        Assert.state(this.beanFactory != null, "BeanFactory must be set to resolve 'adviceBeanName'");

        if (this.beanFactory.isSingleton(this.adviceBeanName)) {
            // Rely on singleton semantics provided by the factory.
            advice = this.beanFactory.getBean(this.adviceBeanName, Advice.class);
            this.advice = advice;
            return advice;
        }
        else {
            // No singleton guarantees from the factory -> let's lock locally but
            // reuse the factory's singleton lock, just in case a lazy dependency
            // of our advice bean happens to trigger the singleton lock implicitly...
            synchronized (this.adviceMonitor) {
                advice = this.advice;
                if (advice == null) {
                    advice = this.beanFactory.getBean(this.adviceBeanName, Advice.class);
                    this.advice = advice;
                }
                return advice;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName());
        sb.append(": advice ");
        if (this.adviceBeanName != null) {
            sb.append("bean '").append(this.adviceBeanName).append("'");
        }
        else {
            sb.append(this.advice);
        }
        return sb.toString();
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Rely on default serialization, just initialize state after deserialization.
        ois.defaultReadObject();

        // Initialize transient fields.
        resetAdviceMonitor();
    }




}
