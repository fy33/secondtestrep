package org.springframework.web.method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

public class HandlerMethod {
    protected final Log logger= LogFactory.getLog(getClass());
    private final Object bean;
    @Nullable
    private final BeanFactory beanFactory;
    private final Class<?> beanType;
    private final Method method;
    private final Method bridgedMethod;
    private final MethodParameter[] parameters;
    @Nullable
    private HttpStatus responseStatus;
    @Nullable
    private String responseStatusReason;
    @Nullable
    private HandlerMethod resolvedFromHandlerMethod;
    @Nullable
    private volatile List<Annotation[][]> interfaceParameterAnnotations;
    private final String description;
    public HandlerMethod(Object bean,Method method)
    {
        Assert.notNull(bean, "Bean is required");
        Assert.notNull(method, "Method is required");
        this.bean=bean;
        this.beanFactory=null;
        this.beanType= ClassUtils.getUserClass(bean);
        this.method=method;
        this.bridgedMethod= BridgeMethodResolver.findBridgedMethod(method);
        this.parameters=initMethodParameters();
        evaluateResponseStatus();
        this.description=initDescription(this.beanType,this.method);
    }
    public HandlerMethod(Object bean,String methodName,Class<?>... parameterTypes)throws NoSuchMethodException
    {
        Assert.notNull(bean, "Bean is required");
        Assert.notNull(methodName, "Method name is required");
        this.bean = bean;
        this.beanFactory = null;
        this.beanType = ClassUtils.getUserClass(bean);
        this.method = bean.getClass().getMethod(methodName, parameterTypes);
        this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(this.method);
        this.parameters = initMethodParameters();
        evaluateResponseStatus();
        this.description = initDescription(this.beanType, this.method);
    }
    public HandlerMethod(String beanName,BeanFactory beanFactory,Method method)
    {
        Assert.hasText(beanName, "Bean name is required");
        Assert.notNull(beanFactory, "BeanFactory is required");
        Assert.notNull(method, "Method is required");
        this.bean = beanName;
        this.beanFactory = beanFactory;
        Class<?> beanType = beanFactory.getType(beanName);
        if (beanType == null) {
            throw new IllegalStateException("Cannot resolve bean type for bean with name '" + beanName + "'");
        }
        this.beanType = ClassUtils.getUserClass(beanType);
        this.method = method;
        this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
        this.parameters = initMethodParameters();
        evaluateResponseStatus();
        this.description = initDescription(this.beanType, this.method);
    }
    protected HandlerMethod(HandlerMethod handlerMethod) {
        Assert.notNull(handlerMethod, "HandlerMethod is required");
        this.bean = handlerMethod.bean;
        this.beanFactory = handlerMethod.beanFactory;
        this.beanType = handlerMethod.beanType;
        this.method = handlerMethod.method;
        this.bridgedMethod = handlerMethod.bridgedMethod;
        this.parameters = handlerMethod.parameters;
        this.responseStatus = handlerMethod.responseStatus;
        this.responseStatusReason = handlerMethod.responseStatusReason;
        this.description = handlerMethod.description;
        this.resolvedFromHandlerMethod = handlerMethod.resolvedFromHandlerMethod;
    }
    private HandlerMethod(HandlerMethod handlerMethod, Object handler) {
        Assert.notNull(handlerMethod, "HandlerMethod is required");
        Assert.notNull(handler, "Handler object is required");
        this.bean = handler;
        this.beanFactory = handlerMethod.beanFactory;
        this.beanType = handlerMethod.beanType;
        this.method = handlerMethod.method;
        this.bridgedMethod = handlerMethod.bridgedMethod;
        this.parameters = handlerMethod.parameters;
        this.responseStatus = handlerMethod.responseStatus;
        this.responseStatusReason = handlerMethod.responseStatusReason;
        this.resolvedFromHandlerMethod = handlerMethod;
        this.description = handlerMethod.description;
    }
    private MethodParameter[] initMethodParameters()
    {
        int count=this.bridgedMethod.getParameterCount();
        MethodParameter[] result=new MethodParameter[count];
        for (int i = 0; i < count; i++) {
//            result[i]=new HandlerMethodParameter(i);
        }
        return result;
    }
    private void evaluateResponseStatus()
    {
        ResponseStatus annotation=getMethodAnnotation(ResponseStatus.class);
        if(annotation==null)
        {
            annotation= AnnotatedElementUtils.findMergedAnnotation(getBeanType(),ResponseStatus.class);
        }
        if(annotation!=null)
        {
            this.responseStatus=annotation.code();
            this.responseStatusReason=annotation.reason();
        }
    }
    private static String initDescription(Class<?> beanType,Method method)
    {
        StringJoiner joiner =new StringJoiner(",","(",")");
        for (Class<?> paramType : method.getParameterTypes()) {
            joiner.add(paramType.getSimpleName());
        }
        return beanType.getName()+"#"+method.getName()+joiner.toString();
    }
    public Object getBean()
    {
        return this.bean;
    }
    public Method getMethod()
    {
        return this.method;
    }
    public Class<?> getBeanType()
    {
        return this.beanType;
    }
    protected Method getBridgedMethod()
    {
        return this.bridgedMethod;
    }
    public MethodParameter[] getMethodParameters() {
        return this.parameters;
    }
    @Nullable
    protected HttpStatus getResponseStatus() {
        return this.responseStatus;
    }
    @Nullable
    protected String getResponseStatusReason() {
        return this.responseStatusReason;
    }
//    public MethodParameter getReturnType() {
//        return new HandlerMethodParameter(-1);
//    }
    public boolean isVoid()
    {
        return Void.TYPE.equals(getReturnType().getParameterType());
    }
    @Nullable
    public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType)
    {
        return AnnotatedElementUtils.findMergedAnnotation(this.method,annotationType);
    }
    public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType)
    {
        return AnnotatedElementUtils.hasAnnotation(this.method,annotationType);
    }
    @Nullable
    public HandlerMethod getResolvedFromHandlerMethod()
    {
        return this.resolvedFromHandlerMethod;
    }
    public HandlerMethod createWithResolveBean()
    {
        Object handler=this.bean;
        if(this.bean instanceof String)
        {
            Assert.state(this.beanFactory!=null);
        }
    }
    public MethodParameter getReturnType(){
        return new HandlerMethodParameter(-1);
    }

    private boolean isOverrideFor(Method candidate)
    {
        if(!candidate.getName().equals(this.method.getName())||
        candidate.getParameterCount()!=this.method.getParameterCount())
        {
            return false;
        }
        Class<?>[] paramTypes=this.method.getParameterTypes();
        if(Arrays.equals(candidate.getParameterTypes(),paramTypes))
        {
            return true;
        }
        for(int i=0;i<paramTypes.length;i++)
        {
            if(paramTypes[i]!=
                    ResolvableType.forMethodParameter(candidate,i,this.method.getDeclaringClass()).resolve())
            {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public List<Annotation[][]> getInterfaceParameterAnnotations() {
        List<Annotation[][]> parameterAnnotations=this.interfaceParameterAnnotations;
        if(parameterAnnotations==null)
        {
            parameterAnnotations=new ArrayList<>();
            for (Class<?> ifc : this.method.getDeclaringClass().getInterfaces()) {
                for (Method candidate : ifc.getMethods()) {
                    if(isOverrideFor(candidate))
                    {
                        parameterAnnotations.add(candidate.getParameterAnnotations());
                    }
                }
            }
            this.interfaceParameterAnnotations=parameterAnnotations;
        }
        return parameterAnnotations;
    }

    protected class HandlerMethodParameter extends SynthesizingMethodParameter{
        @Nullable
        private volatile Annotation[] combinedAnnotations;
        public HandlerMethodParameter(int index)
        {
            super(HandlerMethod.this.bridgedMethod,index);
        }
        protected HandlerMethodParameter(HandlerMethodParameter original)
        {
            super(original);
        }
        @Override
        public Class<?> getContainingClass()
        {
            return HandlerMethod.this.getBeanType();
        }
        @Override
        public <T extends Annotation> T getMethodAnnotation(Class<T> annotaionType)
        {
            return HandlerMethod.this.getMethodAnnotation(annotaionType);
        }
        @Override
        public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType)
        {
            return HandlerMethod.this.hasMethodAnnotation(annotationType);
        }
        @Override
        public Annotation[] getParameterAnnotations()
        {
            Annotation[] anns=this.combinedAnnotations;
            if(anns==null)
            {
                anns=super.getParameterAnnotations();
                int index=getParameterIndex();
                if(index>0)
                {
                    for (Annotation[][] ifcAnns : getInterfaceParameterAnnotations()) {
                        if(index<ifcAnns.length)
                        {
                            Annotation[] paramAnns=ifcAnns[index];
                            if(paramAnns.length>0)
                            {
                                List<Annotation> merged=new ArrayList<>(anns.length+paramAnns.length);
                                merged.addAll(Arrays.asList(anns));
                                for (Annotation paramAnn : paramAnns) {
                                    boolean existingType=false;
                                    for (Annotation ann : anns) {
                                        if(ann.annotationType()==paramAnn.annotationType())
                                        {
                                            existingType=true;
                                            break;
                                        }
                                    }
                                    if(!existingType)
                                    {
                                        merged.add(adaptAnnotation(paramAnn));
                                    }
                                }
                                anns=merged.toArray(new Annotation[0]);
                            }
                        }
                    }
                }
                this.combinedAnnotations=anns;
            }
            return anns;
        }
        @Override
        public HandlerMethodParameter clone(){
            return new HandlerMethodParameter(this);
        }
    }
    private class ReturnValueMethodParameter extends HandlerMethodParameter{
        @Nullable
        private final Object returnValue;
        public ReturnValueMethodParameter(@Nullable Object returnValue)
        {
            super(-1);
            this.returnValue=returnValue;
        }
        protected ReturnValueMethodParameter(ReturnValueMethodParameter original)
        {
            super(original);
            this.returnValue=original.returnValue;
        }
        @Override
        public Class<?> getParameterType()
        {
            return (this.returnValue!=null?this.returnValue.getClass():super.getParameterType());
        }
        @Override
        public ReturnValueMethodParameter clone()
        {
            return new ReturnValueMethodParameter(this);
        }
    }
}
