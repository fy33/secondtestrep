package org.springframework.boot.builder;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;
import org.springframework.util.ObjectUtils;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer.ParentContextAvailableEvent;


import java.lang.ref.WeakReference;

public class ParentContextCloserApplicationListener implements ApplicationListener<ParentContextAvailableEvent>,
        ApplicationContextAware,Ordered{
    private int order= Ordered.LOWEST_PRECEDENCE-10;
    private ApplicationContext context;
    @Override
    public int getOrder()
    {
        return this.order;
    }
    @Override
    public void setApplicationContext(ApplicationContext context)throws BeansException{
        this.context=context;
    }
    @Override
    public void onApplicationEvent(ParentContextAvailableEvent event)
    {
        maybeInstallListenerInParent(event.getApplicationContext());
    }
    private void maybeInstallListenerInParent(ConfigurableApplicationContext child)
    {
        if(child==this.context&&child.getParent()instanceof ConfigurableApplicationContext)
        {
            ConfigurableApplicationContext parent=(ConfigurableApplicationContext)child.getParent();
            parent.addApplicationListener(createContextCloserListener(child));
        }
    }
    protected ContextCloserListener createContextCloserListener(ConfigurableApplicationContext child)
    {
        return new ContextCloserListener(child);
    }
    protected static class ContextCloserListener implements ApplicationListener<ContextClosedEvent>
    {
        private WeakReference<ConfigurableApplicationContext> childContext;
        public ContextCloserListener(ConfigurableApplicationContext childContext)
        {
            this.childContext=new WeakReference<>(childContext);
        }
        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            ConfigurableApplicationContext context=this.childContext.get();
            if((context!=null)&&(event.getApplicationContext()==context.getParent())
            &&context.isActive())
            {
                context.close();
            }
        }
        @Override
        public boolean equals(Object obj)
        {
            if(this==obj)
            {
                return true;
            }
            if(obj==null)
            {
                return false;
            }
            if(obj instanceof ContextCloserListener)
            {
                ContextCloserListener other=(ContextCloserListener)obj;
                return ObjectUtils.nullSafeEquals(this.childContext.get(),other.childContext.get());
            }
            return super.equals(obj);
        }
        @Override
        public int hashCode()
        {
            return ObjectUtils.nullSafeHashCode(this.childContext.get());
        }
    }


}
