package org.springframework.boot.builder;


import org.springframework.context.*;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;

public class ParentContextApplicationContextInitializer
implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    private int order=Ordered.HIGHEST_PRECEDENCE;
    private final ApplicationContext parent;
    public ParentContextApplicationContextInitializer(ApplicationContext parent)
    {
        this.parent=parent;
    }
    public void setOrder(int order)
    {
        this.order=order;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

        if(applicationContext!=this.parent)
        {
            applicationContext.setParent(this.parent);
            applicationContext.addApplicationListener(EventPublisher.INSTANCE);
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
    private static class EventPublisher implements
            ApplicationListener<ContextRefreshedEvent>,Ordered{
        private static final EventPublisher INSTANCE=new EventPublisher();

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            ApplicationContext context=event.getApplicationContext();
            if(context instanceof ConfigurableApplicationContext&&context==event.getSource())
            {
                context.publishEvent(new ParentContextAvailableEvent(
                        (ConfigurableApplicationContext) context
                ));
            }
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }
    public static class ParentContextAvailableEvent extends ApplicationEvent {

        public ParentContextAvailableEvent(ConfigurableApplicationContext applicationContext) {
            super(applicationContext);
        }
        public ConfigurableApplicationContext getApplicationContext()
        {
            return (ConfigurableApplicationContext)getSource();
        }
    }

}
