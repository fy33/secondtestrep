package org.springframework.web.servlet.method.support;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class InvocableHandlerMethod extends HandlerMethod {
    private static final Object[] EMPTY_ARGS=new Object[0];
    @Nullable
    private WebDataBinderFactory dataBinderFactory;
    private HandlerMethodArgumentResolverComposite resolvers=new HandlerMethodArgumentResolverComposite();
    private ParameterNameDiscoverer parameterNameDiscoverer=new DefaultParameterNameDiscoverer();
    public InvocableHandlerMethod(HandlerMethod handlerMethod)
    {
        super(handlerMethod);
    }
    public InvocableHandlerMethod(Object bean, Method method)
    {
        super(bean,method);
    }
    public InvocableHandlerMethod(Object bean,String methodName,Class<?>... parameterTypes)
            throws NoSuchMethodException
    {
        super(bean,methodName,parameterTypes);
    }
    public void setDataBinderFactory(WebDataBinderFactory dataBinderFactory)
    {
        this.dataBinderFactory=dataBinderFactory;
    }
    public void setHandlerMethodArgumentResolver(HandlerMethodArgumentResolverComposite argumentResolvers)
    {
        this.resolvers=argumentResolvers;
    }
    public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer)
    {
        this.parameterNameDiscoverer=parameterNameDiscoverer;
    }
    @Nullable
    public Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
                                   Object... provideArgs)throws Exception
    {
        Object[] args=getMethodArgumentValues(request,mavContainer,provideArgs);
        if(logger.isTraceEnabled())
        {
            logger.trace("Arguments:"+ Arrays.toString(args));
        }
        return doInvoke(args);
    }
    protected Object[] getMethodArgumentValues(NativeWebRequest request,@Nullable ModelAndViewContainer
                                               mavContainer,Object... provideArgs)throws Exception
    {
        MethodParameter[] parameters=getMethodParameters();
        if(ObjectUtils.isEmpty(parameters))
        {
            return EMPTY_ARGS;
        }
        Object[] args=new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter=parameters[i];
            parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
            args[i]=findProvidedArgument(parameter,provideArgs);
            if(args[i]!=null)
            {
                continue;
            }
            if(!this.resolvers.supportsParameter(parameter))
            {
                throw new IllegalStateException(formatArgumentError(parameter, "No suitable resolver"));
            }
            try{
                args[i] =this.resolvers.resolveArgument(parameter,mavContainer,request,this.dataBinderFactory);
            }catch (Exception ex)
            {
                // Leave stack trace for later, exception may actually be resolved and handled...
                if(logger.isDebugEnabled()) {
                    String exMsg = ex.getMessage();
                    if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString()))
                    {
                        logger.debug(formatArgumentError(parameter,exMsg));
                    }
                }
                throw ex;
            }
        }
        return args;
    }
    @Nullable
    protected Object doInvoke(Object... args)throws Exception
    {
        ReflectionUtils.makeAccessible(getBridgedMethod());
        try{
            return getBridgedMethod().invoke(getBean(),args);
        }catch (IllegalArgumentException ex)
        {
            assertTargetBean(getBridgedMethod(),getBean(),args);
            String text=(ex.getMessage()!=null?ex.getMessage():"Illegal argument");
            throw new IllegalStateException(formatInvokeError(text,args),ex);
        }catch (InvocationTargetException ex)
        {
            Throwable targetException =ex.getTargetException();
            if(targetException instanceof RuntimeException)
            {
                throw (RuntimeException)targetException;
            }
            else if (targetException instanceof Error) {
                throw (Error) targetException;
            }
            else if (targetException instanceof Exception) {
                throw (Exception) targetException;
            }
            else {
                throw new IllegalStateException(formatInvokeError("Invocation failure", args), targetException);
            }
        }
    }
}
