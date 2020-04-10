package org.springframework.web.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class HandlerExecutionChain {
    private static final Log logger = LogFactory.getLog(HandlerExecutionChain.class);
    private final Object handler;
    @Nullable
    private HandlerInterceptor[] interceptors;
    @Nullable
    private List<HandlerInterceptor> interceptorList;
    private int interceptorIndex = -1;

    public HandlerExecutionChain(Object handler) {
        this(handler, (HandlerInterceptor[]) null);

    }
    public void addInterceptor(HandlerInterceptor interceptor) {
        initInterceptorList().add(interceptor);
    }

    public void addInterceptor(int index, HandlerInterceptor interceptor) {
        initInterceptorList().add(index, interceptor);
    }
//todo invocablehandlemethod servletinvocablehandlemethod
    public HandlerExecutionChain(Object handler, @Nullable HandlerInterceptor... interceptors) {
        if (handler instanceof HandlerExecutionChain) {
            HandlerExecutionChain originalChain = (HandlerExecutionChain) handler;
            this.handler = originalChain.getHandler();
            this.interceptorList = new ArrayList<>();
            CollectionUtils.mergeArrayIntoCollection(originalChain.getInterceptors(), this.interceptorList);
            CollectionUtils.mergeArrayIntoCollection(interceptors, this.interceptorList);
        } else {
            this.handler = handler;
            this.interceptors = interceptors;
        }
    }

    private void addInterceptot(int index, HandlerInterceptor interceptor) {
        interceptorList.add(index, interceptor);
    }

    public void addInterceptors(HandlerInterceptor... interceptors) {
        if (!ObjectUtils.isEmpty(interceptors))
        {
            CollectionUtils.mergeArrayIntoCollection(interceptors,initInterceptorList());
        }
    }
    private List<HandlerInterceptor> initInterceptorList()
    {
        if(this.interceptorList==null)
        {
            this.interceptorList=new ArrayList<>();
            if(this.interceptors!=null)
            {
                //An interceptor array specified through the constructor
                CollectionUtils.mergeArrayIntoCollection(this.interceptors,this.interceptorList);
            }
        }
        this.interceptors=null;
        return this.interceptorList;
    }
    @Nullable
    public HandlerInterceptor[] getInterceptors()
    {
        if(this.interceptorList!=null&&this.interceptors==null)
        {
            this.interceptors=this.interceptorList.toArray(new HandlerInterceptor[0]);
        }
        return this.interceptors;
    }
    boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response)throws Exception{
        HandlerInterceptor[] interceptors=getInterceptors();
        if(!ObjectUtils.isEmpty(interceptors))
        {
            for (int i = 0; i < interceptors.length; i++) {
                HandlerInterceptor interceptor=interceptors[i];
                if(!interceptor.preHandle(request,response,this.handler)){
                    triggerAfterCompletion(request,response,null);
                    return false;
                }
                this.interceptorIndex=i;
            }
        }
        return true;
    }
    void applyPostHandle(HttpServletRequest request,HttpServletResponse response,@Nullable ModelAndView mv)throws Exception{
        HandlerInterceptor[] interceptors=getInterceptors();
        if(!ObjectUtils.isEmpty(interceptors))
        {
            for (int i = interceptors.length - 1; i > 0; i--) {
                HandlerInterceptor interceptor=interceptors[i];
                interceptor.postHandle(request,response,this.handler,mv);
            }
        }
    }
    void triggerAfterCompletion(HttpServletRequest request,HttpServletResponse response,@Nullable Exception ex )throws Exception{
        HandlerInterceptor[] interceptors=getInterceptors();
        if(!ObjectUtils.isEmpty(interceptors))
        {
            for(int i=this.interceptorIndex;i>=0;i--)
            {
                HandlerInterceptor interceptor=interceptors[i];
                try{
                    interceptor.afterCompletion(request,response,this.handler,ex);
                }catch(Throwable ex2)
                {
                    logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
                }
            }
        }
    }
    void applyAfterConcurrentHandlingStarted(HttpServletRequest request,HttpServletResponse response)
    {
        HandlerInterceptor[] interceptors=getInterceptors();
        if(!ObjectUtils.isEmpty(interceptors))
        {
            for (int i = interceptors.length - 1 ; i >= 0; i--) {
                try{
                    AsyncHandlerInterceptor asynInterceptor=(AsyncHandlerInterceptor)interceptors[i];
                    asynInterceptor.afterConcurrentHandlingStarted(request,response,this.handler);
                }catch (Throwable ex)
                {
                    logger.error("Interceptor [" + interceptors[i] + "] failed in afterConcurrentHandlingStarted", ex);
                }
            }
        }
    }
    @Override
    public String toString()
    {
        Object handler=getHandler();
        StringBuilder sb=new StringBuilder();
        sb.append("HandlerExecutionChain with [").append(handler).append("] and ");
        if(this.interceptorList!=null)
        {
          sb.append(this.interceptorList.size());
        }else if(this.interceptors!=null)
        {
            sb.append(this.interceptors.length);
        }else{
            sb.append(0);
        }
        return sb.append(" interceptors").toString();

    }
    public Object getHandler()
    {
        return this.handler;
    }
}
